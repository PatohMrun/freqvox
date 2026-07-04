package com.freqvox.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.freqvox.data.artwork.ArtworkResolver
import com.freqvox.data.local.dao.PlayHistoryDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.data.local.entities.TrackTagCrossRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val tagDao: TagDao,
    private val playHistoryDao: PlayHistoryDao,
    private val artworkResolver: ArtworkResolver
) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()
    val allTrackTagCrossRefs: Flow<List<TrackTagCrossRef>> = tagDao.getAllTrackTagCrossRefs()

    // Folders the user has chosen to keep out of the library (ringtones, voice
    // notes, etc.). Stored as raw folderPath strings. Exclusion is applied as a live
    // filter in MainViewModel (allTracks/activeTracks) — not at scan time — so hiding
    // takes effect immediately and non-destructively (rows stay in the DB).
    private val scanPrefs = context.getSharedPreferences("freqvox_scan", Context.MODE_PRIVATE)

    fun getExcludedFolders(): Set<String> =
        scanPrefs.getStringSet("excluded_folders", emptySet()) ?: emptySet()

    fun setExcludedFolders(folders: Set<String>) {
        scanPrefs.edit().putStringSet("excluded_folders", folders).apply()
    }

    fun getRecentPlayHistory(): Flow<List<PlayHistoryEntity>> {
        val since = System.currentTimeMillis() - 90L * 86_400_000
        return playHistoryDao.getHistorySinceFlow(since)
    }

    fun getActiveTracks(recentThreshold: Long): Flow<List<TrackEntity>> = 
        trackDao.getActiveTracks(recentThreshold)

    suspend fun scanLibrary() = withContext(Dispatchers.IO) {
        val contentResolver: ContentResolver = context.contentResolver

        // Snapshot of what's already in the DB, indexed by PATH so we can
        // correlate to MediaStore rows even when the underlying numeric
        // MediaStore ID has been reissued.
        val existingByPath: Map<String, TrackEntity> =
            trackDao.getAllTracks().first().associateBy { it.path }

        val tracksInMediaStore = mutableListOf<TrackEntity>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val rawTitle = cursor.getString(titleColumn) ?: continue
                if (rawTitle.isBlank()) continue
                val rawArtist = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn) ?: ""
                val duration = cursor.getLong(durationColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val rawDataPath = cursor.getString(dataColumn) ?: ""

                // Excluded folders are NOT skipped here — they stay in the DB (so
                // playlist/tag/stat relations survive) and are filtered out for display
                // in MainViewModel instead.
                val folder = java.io.File(rawDataPath).parent ?: "Unknown"

                val path = android.content.ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                // Underscores → spaces FIRST so the space-based cleanup regexes match
                // (side-loaded filenames like "Artist_-_Title_Official_Video" use underscores).
                var finalTitle = cleanMetadata(rawTitle.replace("_", " "))
                var finalArtist = when {
                    rawArtist.isNullOrBlank() || rawArtist == "<unknown>" -> "Unknown Artist"
                    else -> rawArtist
                }

                // Explicit Fallback: If MediaStore failed to read the metadata, try extracting it directly
                if (finalArtist == "Unknown Artist") {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(context, android.net.Uri.parse(path))
                        val embeddedArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val embeddedTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                        
                        if (!embeddedArtist.isNullOrBlank()) finalArtist = embeddedArtist
                        if (!embeddedTitle.isNullOrBlank()) finalTitle = embeddedTitle
                        retriever.release()
                    } catch (e: Exception) {
                        // Ignore extraction errors
                    }
                }

                // Heuristic 1: Extract from "Artist - Title" filename format
                if (finalArtist == "Unknown Artist" && finalTitle.contains(" - ")) {
                    val parts = finalTitle.split(" - ", limit = 2)
                    if (parts.size == 2) {
                        finalArtist = parts[0].trim()
                        finalTitle = parts[1].trim()
                    }
                }

                // Heuristic 2: Extract from parent folder name
                if (finalArtist == "Unknown Artist") {
                    val folderName = java.io.File(rawDataPath).parentFile?.name
                    if (!folderName.isNullOrBlank() && 
                        !folderName.equals("Music", ignoreCase = true) && 
                        !folderName.equals("Download", ignoreCase = true) &&
                        !folderName.equals("Downloads", ignoreCase = true) &&
                        !folderName.equals("Audio", ignoreCase = true)) {
                        finalArtist = folderName
                    }
                }

                // STATS PRESERVATION: look up by PATH (stable across MediaStore
                // re-indexes), not by id. Without this every scan would either
                // miss the row entirely (id changed) or overwrite playCount /
                // lastPlayed / totalPlayTime / skipCount with the defaults via
                // @Upsert, making Recently/Most/Least Played go empty and any
                // cached artwork be re-extracted on every launch.
                val existing = existingByPath[path]

                tracksInMediaStore.add(
                    TrackEntity(
                        id = id,
                        title = finalTitle,
                        artist = finalArtist,
                        album = album,
                        duration = duration,
                        dateAdded = dateAdded,
                        path = path,
                        folderPath = folder,
                        artworkPath = existing?.artworkPath,
                        playCount = existing?.playCount ?: 0,
                        lastPlayed = existing?.lastPlayed,
                        totalPlayTime = existing?.totalPlayTime ?: 0,
                        skipCount = existing?.skipCount ?: 0
                    )
                )
            }
        }

        if (tracksInMediaStore.isNotEmpty()) {
            // PLAYLIST PRESERVATION PATH:
            // MediaStore re-issues row IDs whenever it re-indexes a file (reboot,
            // file edited externally, etc). The old approach — "delete every track
            // whose MediaStore id isn't in this scan" — then cascaded into
            // playlist_tracks/track_tags and wiped the user's playlists. Now we
            // match existing tracks to the new scan by PATH (stable per file),
            // and:
            //   - if the same path comes back with a different id → migrate
            //     all references to the new id, then drop the old PK row;
            //   - if a path is truly gone from MediaStore → delete that row.
            val newTracksByPath: Map<String, Long> = tracksInMediaStore.associate { it.path to it.id }

            val idMigrations = mutableListOf<Pair<Long, Long>>() // oldId -> newId
            val trulyRemoved = mutableListOf<Long>()
            for (existing in existingByPath.values) {
                val newId = newTracksByPath[existing.path]
                when {
                    newId == null -> trulyRemoved += existing.id
                    newId != existing.id -> idMigrations += existing.id to newId
                }
            }

            // Upsert (INSERT-or-UPDATE in place) instead of REPLACE. This is
            // critical: REPLACE deletes the existing row before inserting the
            // new one, which fires the ForeignKey.CASCADE on playlist_tracks
            // and silently wipes every track's playlist membership on every
            // scan. @Upsert mutates the row in place, so cross-refs survive.
            trackDao.upsertTracks(tracksInMediaStore)

            idMigrations.forEach { (oldId, newId) ->
                trackDao.migrateTrackReferences(oldId, newId)
            }

            if (trulyRemoved.isNotEmpty()) {
                trackDao.deleteTracksByIds(trulyRemoved)
            }
        }

        ensureDefaultTags()

        // Resolve LOCAL artwork for tracks that don't have it yet — runs after tracks
        // are visible in UI. Online lookup is deliberately NOT done here (it would be a
        // network burst over the whole library); it happens on demand, post-consent.
        tracksInMediaStore.filter { it.artworkPath == null }.forEach { track ->
            val artworkPath = artworkResolver.resolveLocalArtwork(track)
            if (artworkPath != null) {
                trackDao.updateTrack(track.copy(artworkPath = artworkPath))
            }
        }
    }

    // ── Online artwork (demand-driven, never a launch-time burst) ───────────────
    // Spacing between backfill fetches; keeps the long-tail sweep gentle on free APIs.
    private val backfillThrottleMs = 600L

    // Tracks currently being fetched, so a now-playing priority fetch and the
    // background backfill never hit the network twice for the same track.
    private val inFlightArtwork = mutableSetOf<Long>()
    private val inFlightMutex = Mutex()

    /**
     * Fetches online art for a single track on demand (e.g. the now-playing one) and
     * persists it. No-ops if the track already has art or a fetch is already running for
     * it. All gating (consent, policy, negative cache) lives in [ArtworkResolver].
     */
    suspend fun fetchOnlineArtwork(track: TrackEntity): Boolean {
        if (track.artworkPath != null) return false
        val claimed = inFlightMutex.withLock {
            if (track.id in inFlightArtwork) false else inFlightArtwork.add(track.id)
        }
        if (!claimed) return false
        return try {
            val path = artworkResolver.resolveOnlineArtwork(track)
            if (path != null) {
                // Single-column write: can't clobber play stats accrued during the
                // (slow) fetch, unlike updateTrack(track.copy(...)) on a stale row.
                trackDao.setArtworkPath(track.id, path)
                true
            } else false
        } finally {
            inFlightMutex.withLock { inFlightArtwork.remove(track.id) }
        }
    }

    // How many resolved covers to accumulate before committing them in one
    // transaction. Each flush is a single Room invalidation, so the whole
    // library's worth of art writes costs ~candidates/this many re-emits instead
    // of one per track — which is what made the UI grind during a link/backfill.
    private val artFlushBatch = 12

    /**
     * Slowly backfills online art for the rest of the library. Throttled and one-at-a-time
     * so a large library never produces a request burst, and it bails the moment
     * consent/connection no longer permits fetching. Negative-cached tracks are filtered
     * out so we don't re-walk them on every launch.
     *
     * Resolved covers are written in batches (one transaction per [artFlushBatch]) so a
     * library-wide sweep doesn't re-emit the tracks Flow — and recompute everything derived
     * from it (listening stats, totals, search, the list itself) — once per track.
     */
    suspend fun backfillOnlineArtwork() = withContext(Dispatchers.IO) {
        if (!artworkResolver.canFetchOnline()) return@withContext
        val candidates = allTracks.first().filter {
            it.artworkPath == null && !artworkResolver.alreadyTriedOnline(it.id)
        }
        val pending = LinkedHashMap<Long, String>()
        // Misses are collected and written to the negative cache ONCE at the end, not
        // per miss: each markLookupFailed rewrites the whole growing set, so per-miss
        // marking is O(n²) over a full-library sweep. Failures don't need to surface
        // mid-sweep, so a single write is enough (and any interruption just means those
        // tracks get retried next time, which is harmless).
        val failed = ArrayList<Long>()
        suspend fun flushArt() {
            if (pending.isEmpty()) return
            trackDao.setArtworkPaths(pending)
            pending.clear()
        }
        for (track in candidates) {
            if (!artworkResolver.canFetchOnline()) break
            // Claim the in-flight guard so a now-playing priority fetch and this
            // backfill never hit the network twice for the same track.
            val claimed = inFlightMutex.withLock {
                if (track.id in inFlightArtwork) false else inFlightArtwork.add(track.id)
            }
            if (!claimed) { delay(backfillThrottleMs); continue }
            val path = try {
                artworkResolver.resolveOnlineArtwork(track, markFailures = false)
            } finally {
                inFlightMutex.withLock { inFlightArtwork.remove(track.id) }
            }
            if (path != null) {
                pending[track.id] = path
                if (pending.size >= artFlushBatch) flushArt()
            } else {
                failed.add(track.id)
            }
            delay(backfillThrottleMs)
        }
        flushArt()
        if (failed.isNotEmpty()) artworkResolver.markLookupFailed(failed)
    }

    /**
     * Called after a VidMate folder is linked. Re-queues ONLY the tracks that now have a
     * known thumbnail and still lack art (scoped negative-cache clear), instead of wiping
     * the whole cache and re-probing the entire library. Returns how many were re-queued;
     * the caller then runs [backfillOnlineArtwork], which will pick exactly these up.
     */
    suspend fun requeueVidmateLinkable(): Int = withContext(Dispatchers.IO) {
        val missing = allTracks.first()
            .filter { it.artworkPath == null }
            .mapTo(HashSet()) { it.id }
        artworkResolver.requeueVidmateLinkable(missing)
    }

    private suspend fun ensureDefaultTags() {
        val defaultTags = listOf(
            "liked"       to 0xFFFF4D7C.toInt(),
            "disliked"    to 0xFF78909C.toInt(),
            "chill"       to 0xFF42A5F5.toInt(),
            "energetic"   to 0xFFFF6D00.toInt(),
            "focus"       to 0xFF4DFFB4.toInt(),
            "hype"        to 0xFFFFA726.toInt(),
            "melancholy"  to 0xFF6B3FA0.toInt(),
            "late night"  to 0xFF7C4DFF.toInt(),
            "workout"     to 0xFFFF4D7C.toInt(),
            "drive"       to 0xFF00BCD4.toInt(),
            "sleep"       to 0xFF3F51B5.toInt(),
            "party"       to 0xFFE91E63.toInt(),
            "morning"     to 0xFFFFEB3B.toInt(),
            "throwback"   to 0xFF795548.toInt(),
            "underrated"  to 0xFF4CAF50.toInt(),
            "sad"         to 0xFF607D8B.toInt(),
            "vibes"       to 0xFF9C27B0.toInt()
        )
        val existingNames = tagDao.getAllTags().first().map { it.name.lowercase() }.toSet()
        defaultTags.forEach { (name, color) ->
            if (name !in existingNames) {
                tagDao.insertTag(TagEntity(name = name, color = color))
            }
        }
    }

    private fun cleanMetadata(title: String): String {
        // Strips the promo/format junk common in side-loaded (YouTube/Vidmate) filenames
        // so BOTH the displayed title and the online art-lookup query stay clean — noisy
        // tokens otherwise return wrong matches and trip the lookup's confidence gate.
        // Order matters: remove tagged blocks first, then collapse the gaps they leave.
        val ci = RegexOption.IGNORE_CASE
        var t = title

        // Bitrate/quality tags Vidmate appends: (256k), (128kbps), (0)
        t = t.replace(Regex("\\(\\s*\\d+\\s*k(bps)?\\s*\\)", ci), "")
        t = t.replace(Regex("\\(\\s*0\\s*\\)"), "")

        // Skiza ringtone promos: "[Skiza 8545083]", "SMS SKIZA 8548829 to 811",
        // "For Skiza SmS skiza 69813080 To 811", "DIAL 811 174# To set as Skiza".
        t = t.replace(Regex("\\[[^\\]]*skiza[^\\]]*\\]", ci), "")
        t = t.replace(Regex("dial\\s+[0-9#\\s]+to\\s+set\\s+as\\s+skiza", ci), "")
        t = t.replace(Regex("(for\\s+)?(sms\\s+)?skiza[\\s\\d#]*to\\s+\\d+", ci), "")

        // "Official"/format tags — parenthesized, bracketed, or bare.
        t = t.replace(Regex("[\\(\\[]\\s*(official\\s+)?(music\\s+)?(lyric\\s+)?(video|audio|lyrics?|visuali[sz]er)\\s*[\\)\\]]", ci), "")
        t = t.replace(Regex("\\bofficial\\s+(music\\s+)?(lyric\\s+)?(video|audio)\\b", ci), "")
        t = t.replace(Regex("\\b(visuali[sz]er|lyric\\s+video)\\b", ci), "")

        // Resolution tags: (4K), [HD], (1080p)
        t = t.replace(Regex("[\\(\\[]\\s*(4k|8k|hd|fhd|\\d{3,4}p)\\s*[\\)\\]]", ci), "")

        // Collapse the whitespace / empty brackets / stray separators left behind.
        t = t.replace(Regex("[\\(\\[]\\s*[\\)\\]]"), "")
        t = t.replace(Regex("\\s{2,}"), " ").trim()
        t = t.trim(' ', '-', '|', '_', '·', '•').trim()
        return t
    }

    fun getTagsForTrack(trackId: Long): Flow<List<TagEntity>> = tagDao.getTagsForTrack(trackId)
    
    suspend fun toggleTagForTrack(trackId: Long, tagId: Long) {
        val currentTags = tagDao.getTagIdsForTrack(trackId)
        if (currentTags.contains(tagId)) {
            tagDao.deleteTrackTagCrossRef(TrackTagCrossRef(trackId, tagId))
        } else {
            tagDao.insertTrackTagCrossRef(TrackTagCrossRef(trackId, tagId))
        }
    }

    suspend fun getSimilarTracks(trackId: Long): List<TrackEntity> {
        return trackDao.getSimilarTracks(trackId)
    }

    /** Idempotent add (unlike [toggleTagForTrack]); used for bulk tagging a selection. */
    suspend fun addTagToTrack(trackId: Long, tagId: Long) {
        tagDao.insertTrackTagCrossRef(TrackTagCrossRef(trackId, tagId))
    }

    /** Removes all app data for a track after the media file has been deleted (or user chose library-only removal). */
    suspend fun purgeTrackFromLibrary(trackId: Long) = withContext(Dispatchers.IO) {
        val path = trackDao.getTrackById(trackId)?.path
        trackDao.deleteTrackTagsForTrack(trackId)
        trackDao.deletePlayHistoryForTrack(trackId)
        trackDao.deleteSkipHistoryForTrack(trackId)
        trackDao.deleteTrackById(trackId)
        // Also drop any stale duplicate of the same file (old MediaStore id, never
        // migrated) so the deleted song can't linger in the list/search until a rescan.
        if (path != null) trackDao.deleteTracksByPath(path)
        artworkResolver.deleteArtwork(trackId)
    }

    suspend fun getTracksByFolder(folderPath: String): List<TrackEntity> {
        return allTracks.first().filter { it.path.startsWith(folderPath) }
    }

    suspend fun createCustomTag(name: String, color: Int) {
        val existingNames = tagDao.getAllTags().first().map { it.name.lowercase() }.toSet()
        if (name.lowercase() !in existingNames) {
            tagDao.insertTag(TagEntity(name = name.lowercase(), color = color))
        }
    }

    suspend fun updateTag(tag: TagEntity) {
        tagDao.updateTag(tag)
    }

    /** Deletes a tag and removes it from every track it was applied to. */
    suspend fun deleteTag(tag: TagEntity) {
        tagDao.deleteCrossRefsForTag(tag.id)
        tagDao.deleteTag(tag)
    }

    /** Used by the media notification like action and in-app like toggle. */
    suspend fun isTrackLiked(trackId: Long): Boolean {
        val likedTagId = tagDao.getAllTags().first()
            .find { it.name.equals("liked", ignoreCase = true) }
            ?.id ?: return false
        return tagDao.getTagIdsForTrack(trackId).contains(likedTagId)
    }

    suspend fun isTrackDisliked(trackId: Long): Boolean {
        val dislikedTagId = tagDao.getAllTags().first()
            .find { it.name.equals("disliked", ignoreCase = true) }
            ?.id ?: return false
        return tagDao.getTagIdsForTrack(trackId).contains(dislikedTagId)
    }

    suspend fun toggleLikeForTrack(trackId: Long) {
        val tags = tagDao.getAllTags().first()
        val likedTag = tags.find { it.name.equals("liked", ignoreCase = true) } ?: return
        val dislikedTag = tags.find { it.name.equals("disliked", ignoreCase = true) }
        val current = tagDao.getTagIdsForTrack(trackId)
        if (current.contains(likedTag.id)) {
            tagDao.deleteTrackTagCrossRef(TrackTagCrossRef(trackId, likedTag.id))
        } else {
            dislikedTag?.let { dis ->
                if (current.contains(dis.id)) {
                    tagDao.deleteTrackTagCrossRef(TrackTagCrossRef(trackId, dis.id))
                }
            }
            tagDao.insertTrackTagCrossRef(TrackTagCrossRef(trackId, likedTag.id))
        }
    }

    /**
     * Toggles the "disliked" tag for the track and removes the "liked" tag if it
     * was set (mutually exclusive). Returns the resulting dislike state:
     *   true  → the track is now disliked
     *   false → the dislike was removed (or no "disliked" tag exists)
     * UI callers use the return to decide between snackbar copy and whether to
     * skip the currently-playing track.
     */
    suspend fun toggleDislikeForTrack(trackId: Long): Boolean {
        val tags = tagDao.getAllTags().first()
        val dislikedTag = tags.find { it.name.equals("disliked", ignoreCase = true) } ?: return false
        val likedTag = tags.find { it.name.equals("liked", ignoreCase = true) }
        val current = tagDao.getTagIdsForTrack(trackId)
        return if (current.contains(dislikedTag.id)) {
            tagDao.deleteTrackTagCrossRef(TrackTagCrossRef(trackId, dislikedTag.id))
            false
        } else {
            likedTag?.let { liked ->
                if (current.contains(liked.id)) {
                    tagDao.deleteTrackTagCrossRef(TrackTagCrossRef(trackId, liked.id))
                }
            }
            tagDao.insertTrackTagCrossRef(TrackTagCrossRef(trackId, dislikedTag.id))
            true
        }
    }
}
