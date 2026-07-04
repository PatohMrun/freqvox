package com.freqvox.data.backup

import android.content.Context
import android.net.Uri
import com.freqvox.data.local.dao.PlayHistoryDao
import com.freqvox.data.local.dao.PlaylistDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.repository.TrackRepository
import com.freqvox.data.artwork.ArtworkPreferences
import com.freqvox.data.prefs.ThemeMode
import com.freqvox.data.prefs.ThemePreferences
import com.freqvox.playback.CrossfadePreferences
import com.freqvox.playback.FreqvoxEqualizerManager
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.PlaylistTrackCrossRef
import com.freqvox.data.local.entities.SkipHistoryEntity
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
 * assignments, listening stats (aggregates + timestamped play/skip history),
 * playlists, hidden folders, and app settings (crossfade / album mode / volume
 * normalization / theme / album-art policy / equalizer) — to a single JSON file
 * the user picks via the Storage Access Framework (no storage permission required).
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
    private val playlistDao: PlaylistDao,
    private val playHistoryDao: PlayHistoryDao,
    private val trackRepository: TrackRepository,
    private val crossfadePrefs: CrossfadePreferences,
    private val themePrefs: ThemePreferences,
    private val artworkPrefs: ArtworkPreferences,
    private val eqManager: FreqvoxEqualizerManager
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

        // Hidden folders — user curation that's otherwise lost on a restore. Stored as
        // raw folderPath strings (matched by name on import), same as in TrackRepository.
        val foldersArr = JSONArray()
        trackRepository.getExcludedFolders().forEach { foldersArr.put(it) }
        root.put("excludedFolders", foldersArr)

        // Timestamped play + skip history — the raw events behind the listening-stats graphs
        // (peak hour, hourly buckets). Aggregates already ride on each track above; this keeps
        // the detail too. Keyed by track PATH so it survives a reinstall / new device.
        val historyObj = JSONObject()
        val playArr = JSONArray()
        playHistoryDao.getAll().forEach { h ->
            val path = idToPath[h.trackId] ?: return@forEach
            playArr.put(
                JSONObject()
                    .put("path", path)
                    .put("playedAt", h.playedAt)
                    .put("durationPlayed", h.durationPlayed)
                    .put("completed", h.completed)
            )
        }
        historyObj.put("plays", playArr)
        val skipArr = JSONArray()
        trackDao.getAllSkipHistory().forEach { s ->
            val path = idToPath[s.trackId] ?: return@forEach
            skipArr.put(JSONObject().put("path", path).put("skippedAt", s.skippedAt))
        }
        historyObj.put("skips", skipArr)
        root.put("history", historyObj)

        // App settings — so a fresh install / new device comes back configured. Consent flags
        // are intentionally NOT exported: the user should re-acknowledge network use per device.
        val settings = JSONObject()
            .put("crossfadeSeconds", crossfadePrefs.durationSeconds)
            .put("albumMode", crossfadePrefs.albumMode)
            .put("volumeNormalization", crossfadePrefs.volumeNormalization)
            .put("themeMode", themePrefs.mode.value.name)
            .put("artOnlineEnabled", artworkPrefs.onlineEnabled)
            .put("artAllowCellular", artworkPrefs.allowCellular)
            .put("artAllowMeteredWifi", artworkPrefs.allowMeteredWifi)
            .put("eqEnabled", eqManager.isEnabled.value)
            .put("eqPreset", eqManager.currentPreset.value.toInt())
        // Equalizer band levels (band index → millibel), for a custom (non-preset) curve.
        val eqBands = JSONObject()
        eqManager.bandLevels.value.forEach { (band, level) -> eqBands.put(band.toString(), level) }
        settings.put("eqBands", eqBands)
        root.put("settings", settings)

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

        // 4) Hidden folders — merge (union) with whatever's already excluded, so a
        //    restore never un-hides folders nor erases existing hides.
        val foldersArr = root.optJSONArray("excludedFolders")
        if (foldersArr != null && foldersArr.length() > 0) {
            val merged = trackRepository.getExcludedFolders().toMutableSet()
            for (i in 0 until foldersArr.length()) {
                foldersArr.optString(i).takeIf { it.isNotBlank() }?.let { merged.add(it) }
            }
            trackRepository.setExcludedFolders(merged)
        }

        // 5) Timestamped history — insert play/skip events for tracks that resolve by path,
        //    skipping any that already exist (dedupe by trackId + timestamp) so re-importing on
        //    the same device never double-counts. (v3+; older backups lack the key.)
        val historyObj = root.optJSONObject("history")
        if (historyObj != null) {
            val existingPlays = playHistoryDao.getAll().mapTo(HashSet()) { it.trackId to it.playedAt }
            val playArr = historyObj.optJSONArray("plays") ?: JSONArray()
            for (i in 0 until playArr.length()) {
                val o = playArr.getJSONObject(i)
                val track = tracksByPath[o.optString("path")] ?: continue
                val playedAt = o.optLong("playedAt")
                if ((track.id to playedAt) in existingPlays) continue
                playHistoryDao.insert(
                    PlayHistoryEntity(
                        trackId = track.id,
                        playedAt = playedAt,
                        durationPlayed = o.optLong("durationPlayed", 0L),
                        completed = o.optBoolean("completed", false)
                    )
                )
            }
            val existingSkips = trackDao.getAllSkipHistory().mapTo(HashSet()) { it.trackId to it.skippedAt }
            val skipArr = historyObj.optJSONArray("skips") ?: JSONArray()
            for (i in 0 until skipArr.length()) {
                val o = skipArr.getJSONObject(i)
                val track = tracksByPath[o.optString("path")] ?: continue
                val skippedAt = o.optLong("skippedAt")
                if ((track.id to skippedAt) in existingSkips) continue
                trackDao.insertSkipEvent(SkipHistoryEntity(trackId = track.id, skippedAt = skippedAt))
            }
        }

        // 6) App settings — apply if present. Older backups simply won't have this key.
        root.optJSONObject("settings")?.let { s ->
            if (s.has("crossfadeSeconds")) crossfadePrefs.durationSeconds = s.optInt("crossfadeSeconds")
            if (s.has("albumMode")) crossfadePrefs.albumMode = s.optBoolean("albumMode", true)
            if (s.has("volumeNormalization")) crossfadePrefs.volumeNormalization = s.optBoolean("volumeNormalization", false)
            if (s.has("themeMode")) {
                runCatching { ThemeMode.valueOf(s.getString("themeMode")) }.getOrNull()?.let { themePrefs.setMode(it) }
            }
            if (s.has("artOnlineEnabled")) artworkPrefs.onlineEnabled = s.optBoolean("artOnlineEnabled")
            if (s.has("artAllowCellular")) artworkPrefs.allowCellular = s.optBoolean("artAllowCellular")
            if (s.has("artAllowMeteredWifi")) artworkPrefs.allowMeteredWifi = s.optBoolean("artAllowMeteredWifi", true)

            if (s.has("eqEnabled") || s.has("eqBands") || s.has("eqPreset")) {
                val bands = mutableMapOf<Int, Int>()
                s.optJSONObject("eqBands")?.let { bo ->
                    bo.keys().forEach { k -> k.toIntOrNull()?.let { band -> bands[band] = bo.optInt(k) } }
                }
                eqManager.importState(
                    enabled = s.optBoolean("eqEnabled", false),
                    preset = s.optInt("eqPreset", -1).toShort(),
                    levels = bands
                )
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
        // v2 adds "excludedFolders". v3 adds "history" (timestamped play/skip events) and a
        // "settings" block (crossfade / album mode / volume normalization / theme / art policy).
        // v4 extends "settings" with the equalizer (enabled / preset / band levels). Older files
        // simply lack the newer keys and restore fine — every added block/field is guarded by a
        // presence check, so v1–v3 backups remain fully compatible.
        private const val BACKUP_VERSION = 4
    }
}
