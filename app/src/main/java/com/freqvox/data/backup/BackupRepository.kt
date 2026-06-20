package com.freqvox.data.backup

import android.content.Context
import android.net.Uri
import com.freqvox.data.local.dao.PlaylistDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.PlaylistTrackCrossRef
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackTagCrossRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports / restores everything the user *curates* — tags, per-track tag
 * assignments, listening stats, and playlists — to a single JSON file the user
 * picks via the Storage Access Framework (no storage permission required).
 *
 * Design notes:
 *  - Tracks are keyed by **path**, not the MediaStore id, because MediaStore
 *    reissues ids on re-index. Path is stable per file, so a backup restores
 *    correctly after a reinstall or even on a new device (for the same files).
 *  - Tags are matched by **name**. Smart-playlist queries store tag *ids*, so we
 *    translate id→name on export and name→id on import; otherwise a restored
 *    smart playlist would reference dead tag ids.
 *  - Import is **non-destructive**: missing tags/playlists are created, existing
 *    ones are merged, and stats are merged by taking the larger value so a
 *    restore never erases listening history accrued since the backup.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val tagDao: TagDao,
    private val playlistDao: PlaylistDao
) {
    sealed class Result {
        data class Success(val message: String) : Result()
        data class Error(val message: String) : Result()
    }

    // Tolerant id-array parser, mirrors PlaylistRepository.parseQuery.
    private val tagArrayRegex = Regex("\"(include|exclude)\"\\s*:\\s*\\[([^\\]]*)\\]")

    // ── Export ────────────────────────────────────────────────────────────────

    suspend fun exportToUri(uri: Uri): Result = withContext(Dispatchers.IO) {
        try {
            val json = buildBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.Error("Couldn't open the chosen file for writing")
            Result.Success("Library backed up")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Backup failed")
        }
    }

    private suspend fun buildBackupJson(): String {
        val tracks = trackDao.getAllTracks().first()
        val idToPath = tracks.associate { it.id to it.path }
        val tags = tagDao.getAllTags().first()
        val idToTagName = tags.associate { it.id to it.name }
        val crossRefs = tagDao.getAllTrackTagCrossRefs().first()
        val tagIdsByTrack = crossRefs.groupBy { it.trackId }
        val playlists = playlistDao.getAllPlaylists().first()
        val pathsByPlaylist = playlistDao.getAllPlaylistTracks().groupBy { it.playlistId }

        val root = JSONObject()
        root.put("app", "freqvox")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        // Tags (name + color; id is intentionally omitted).
        val tagsArr = JSONArray()
        tags.forEach { t ->
            tagsArr.put(JSONObject().put("name", t.name).put("color", t.color))
        }
        root.put("tags", tagsArr)

        // Tracks — only those carrying tags or non-default stats, to keep the file small.
        val tracksArr = JSONArray()
        tracks.forEach { tr ->
            val tagNames = (tagIdsByTrack[tr.id] ?: emptyList()).mapNotNull { idToTagName[it.tagId] }
            val hasStats = tr.playCount > 0 || tr.totalPlayTime > 0 ||
                tr.skipCount > 0 || tr.lastPlayed != null
            if (tagNames.isEmpty() && !hasStats) return@forEach

            val o = JSONObject()
            o.put("path", tr.path)
            o.put("title", tr.title)
            o.put("artist", tr.artist)
            o.put("playCount", tr.playCount)
            tr.lastPlayed?.let { o.put("lastPlayed", it) }
            o.put("totalPlayTime", tr.totalPlayTime)
            o.put("skipCount", tr.skipCount)
            val tn = JSONArray()
            tagNames.forEach { tn.put(it) }
            o.put("tags", tn)
            tracksArr.put(o)
        }
        root.put("tracks", tracksArr)

        // Playlists.
        val plArr = JSONArray()
        playlists.forEach { pl ->
            val o = JSONObject()
            o.put("name", pl.name)
            o.put("type", pl.type)
            if (pl.type == "manual") {
                val paths = JSONArray()
                (pathsByPlaylist[pl.id] ?: emptyList())
                    .sortedBy { it.orderIndex }
                    .forEach { pt -> idToPath[pt.trackId]?.let { paths.put(it) } }
                o.put("trackPaths", paths)
            } else {
                o.put("query", queryIdsToNames(pl.queryJson, idToTagName))
            }
            plArr.put(o)
        }
        root.put("playlists", plArr)

        return root.toString(2)
    }

    /** {"include":[ids],"exclude":[ids]} → {"include":[names],"exclude":[names]} */
    private fun queryIdsToNames(queryJson: String?, idToTagName: Map<Long, String>): JSONObject {
        val out = JSONObject()
        val include = JSONArray()
        val exclude = JSONArray()
        if (queryJson != null) {
            tagArrayRegex.findAll(queryJson).forEach { match ->
                val key = match.groupValues[1]
                val target = if (key == "include") include else exclude
                match.groupValues[2].split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                    .mapNotNull { idToTagName[it] }
                    .forEach { target.put(it) }
            }
        }
        out.put("include", include)
        out.put("exclude", exclude)
        return out
    }

    // ── Import ──────────────────────────────────────────────────────────────────

    suspend fun importFromUri(uri: Uri): Result = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()
                ?.use { it.readText() }
                ?: return@withContext Result.Error("Couldn't open the chosen file")
            val root = JSONObject(text)
            if (root.optString("app") != "freqvox") {
                return@withContext Result.Error("Not a Freqvox backup file")
            }
            restore(root)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Restore failed")
        }
    }

    private suspend fun restore(root: JSONObject): Result {
        // 1) Tags — insert any that don't already exist (by name), then build name→id.
        val existingTagNames = tagDao.getAllTags().first().map { it.name.lowercase() }.toMutableSet()
        val tagsArr = root.optJSONArray("tags") ?: JSONArray()
        for (i in 0 until tagsArr.length()) {
            val t = tagsArr.getJSONObject(i)
            val name = t.optString("name").trim()
            if (name.isEmpty() || name.lowercase() in existingTagNames) continue
            tagDao.insertTag(TagEntity(name = name, color = t.optInt("color", 0xFF7C4DFF.toInt())))
            existingTagNames += name.lowercase()
        }
        val nameToTagId = tagDao.getAllTags().first().associate { it.name.lowercase() to it.id }

        // 2) Tracks — resolve path → current row; apply tags + merge stats.
        val tracksByPath = trackDao.getAllTracks().first().associateBy { it.path }
        var tracksMatched = 0
        var tagsApplied = 0
        val tracksArr = root.optJSONArray("tracks") ?: JSONArray()
        for (i in 0 until tracksArr.length()) {
            val o = tracksArr.getJSONObject(i)
            val track = tracksByPath[o.optString("path")] ?: continue
            tracksMatched++

            val tagNamesArr = o.optJSONArray("tags") ?: JSONArray()
            for (j in 0 until tagNamesArr.length()) {
                val tagId = nameToTagId[tagNamesArr.getString(j).lowercase()] ?: continue
                tagDao.insertTrackTagCrossRef(TrackTagCrossRef(track.id, tagId))
                tagsApplied++
            }

            val backupLastPlayed = if (o.has("lastPlayed")) o.optLong("lastPlayed") else null
            val merged = track.copy(
                playCount = maxOf(track.playCount, o.optInt("playCount", 0)),
                totalPlayTime = maxOf(track.totalPlayTime, o.optLong("totalPlayTime", 0L)),
                skipCount = maxOf(track.skipCount, o.optInt("skipCount", 0)),
                lastPlayed = maxOfNullable(track.lastPlayed, backupLastPlayed)
            )
            if (merged != track) trackDao.updateTrack(merged)
        }

        // 3) Playlists — create missing (by name), merge memberships.
        val existingPlaylists = playlistDao.getAllPlaylists().first()
        val byName = existingPlaylists.associateBy { it.name.lowercase() }
        var playlistsRestored = 0
        val plArr = root.optJSONArray("playlists") ?: JSONArray()
        for (i in 0 until plArr.length()) {
            val o = plArr.getJSONObject(i)
            val name = o.optString("name").trim()
            if (name.isEmpty()) continue
            val type = o.optString("type", "manual")

            if (type == "manual") {
                val existing = byName[name.lowercase()]
                val playlistId = existing?.id
                    ?: playlistDao.insertPlaylist(PlaylistEntity(name = name, type = "manual"))
                if (existing == null) playlistsRestored++
                val pathsArr = o.optJSONArray("trackPaths") ?: JSONArray()
                var order = 0
                for (j in 0 until pathsArr.length()) {
                    val track = tracksByPath[pathsArr.getString(j)] ?: continue
                    if (!playlistDao.isTrackInPlaylist(playlistId, track.id)) {
                        playlistDao.insertPlaylistTrack(
                            PlaylistTrackCrossRef(playlistId, track.id, orderIndex = order++)
                        )
                    }
                }
            } else {
                // Smart playlist: rebuild queryJson from tag names → current ids.
                if (byName.containsKey(name.lowercase())) continue
                val query = o.optJSONObject("query")
                val include = namesToIds(query?.optJSONArray("include"), nameToTagId)
                val exclude = namesToIds(query?.optJSONArray("exclude"), nameToTagId)
                val queryJson = """{"include": $include, "exclude": $exclude}"""
                playlistDao.insertPlaylist(
                    PlaylistEntity(name = name, type = "query", queryJson = queryJson)
                )
                playlistsRestored++
            }
        }

        return Result.Success(
            "Restored: $tagsApplied tag(s) on $tracksMatched track(s), $playlistsRestored playlist(s)"
        )
    }

    private fun namesToIds(arr: JSONArray?, nameToTagId: Map<String, Long>): List<Long> {
        if (arr == null) return emptyList()
        val ids = mutableListOf<Long>()
        for (i in 0 until arr.length()) {
            nameToTagId[arr.getString(i).lowercase()]?.let { ids.add(it) }
        }
        return ids
    }

    private fun maxOfNullable(a: Long?, b: Long?): Long? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }
}
