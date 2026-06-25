package com.freqvox.data.repository

import com.freqvox.data.local.dao.PlaylistDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val tagDao: TagDao,
    private val trackRepository: TrackRepository
) {
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun createDynamicPlaylist(name: String, includedTagIds: List<Long>, excludedTagIds: List<Long> = emptyList()) {
        val queryJson = """{"include": ${includedTagIds}, "exclude": ${excludedTagIds}}"""
        playlistDao.insertPlaylist(
            PlaylistEntity(name = name, type = "query", queryJson = queryJson)
        )
    }

    suspend fun createManualPlaylist(name: String) {
        playlistDao.insertPlaylist(
            PlaylistEntity(name = name, type = "manual")
        )
    }

    /** Creates a manual playlist pre-populated with [trackIds], preserving order. */
    suspend fun createManualPlaylistWithTracks(name: String, trackIds: List<Long>) {
        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = name, type = "manual"))
        trackIds.forEachIndexed { index, trackId ->
            playlistDao.insertPlaylistTrack(
                com.freqvox.data.local.entities.PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = trackId,
                    orderIndex = index
                )
            )
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long): AddTrackResult {
        if (playlistDao.isTrackInPlaylist(playlistId, trackId)) {
            return AddTrackResult.AlreadyInPlaylist
        }
        playlistDao.insertPlaylistTrack(
            com.freqvox.data.local.entities.PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                orderIndex = 0 // Future work: Handle correct orderIndex
            )
        )
        return AddTrackResult.Added
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) playlistDao.renamePlaylist(id, trimmed)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun getTracksForPlaylist(playlist: PlaylistEntity): List<TrackEntity> =
        observeTracksForPlaylist(playlist).first()

    /** Strips [tagId] from every smart-playlist query so deleting a tag doesn't
     *  leave a dead id behind (which would silently make the playlist match nothing). */
    suspend fun removeTagFromQueries(tagId: Long) {
        val queryPlaylists = playlistDao.getAllPlaylists().first().filter { it.type == "query" }
        for (pl in queryPlaylists) {
            val q = parseQuery(pl.queryJson) ?: continue
            if (tagId in q.include || tagId in q.exclude) {
                val newJson = """{"include": ${q.include - tagId}, "exclude": ${q.exclude - tagId}}"""
                playlistDao.insertPlaylist(pl.copy(queryJson = newJson)) // REPLACE by id
            }
        }
    }

    /**
     * Observes the live track list for a playlist. The returned flow re-emits
     * whenever the underlying data changes — adding/removing a track from a
     * manual playlist updates the UI immediately, and changing a tag updates
     * smart (dynamic) playlists immediately.
     */
    fun observeTracksForPlaylist(playlist: PlaylistEntity): Flow<List<TrackEntity>> {
        if (playlist.type == "manual") {
            return playlistDao.getTracksForPlaylist(playlist.id)
        }

        val query = parseQuery(playlist.queryJson) ?: return flowOf(emptyList())

        // Combine all tracks with all tag cross-refs so a tag toggle on any
        // song re-emits this flow. Building the trackId -> tagIds map once
        // per emission is O(N) instead of the old per-track DAO call.
        return combine(
            trackRepository.allTracks,
            trackRepository.allTrackTagCrossRefs
        ) { tracks, crossRefs ->
            val tagsByTrack = crossRefs.groupBy { it.trackId }
                .mapValues { entry -> entry.value.map { it.tagId }.toSet() }
            tracks.filter { track ->
                matchesQuery(track.id, tagsByTrack, query)
            }
        }
    }

    /**
     * Per-playlist track count for every playlist in one map. Manual counts
     * come from a single GROUP BY on the cross-ref table; smart counts are
     * derived from the same tag/track inputs that drive `observeTracksForPlaylist`,
     * so they stay in sync with what the user would see if they expanded the
     * playlist. Avoids the old "Tap to load" placeholder for collapsed rows.
     */
    val playlistTrackCounts: Flow<Map<Long, Int>> = combine(
        playlistDao.getAllPlaylists(),
        playlistDao.getManualPlaylistTrackCounts(),
        trackRepository.allTracks,
        trackRepository.allTrackTagCrossRefs
    ) { playlists, manualCounts, tracks, crossRefs ->
        val manualMap = manualCounts.associate { it.playlistId to it.count }
        val tagsByTrack: Map<Long, Set<Long>> = crossRefs.groupBy { it.trackId }
            .mapValues { entry -> entry.value.map { it.tagId }.toSet() }
        playlists.associate { playlist ->
            val count = when (playlist.type) {
                "manual" -> manualMap[playlist.id] ?: 0
                "query"  -> {
                    val query = parseQuery(playlist.queryJson)
                    if (query == null) 0
                    else tracks.count { track -> matchesQuery(track.id, tagsByTrack, query) }
                }
                else -> 0
            }
            playlist.id to count
        }
    }.distinctUntilChanged()

    suspend fun getPlaylistById(id: Long): PlaylistEntity? = playlistDao.getPlaylistById(id)

    private fun matchesQuery(trackId: Long, tagsByTrack: Map<Long, Set<Long>>, query: TagQuery): Boolean {
        val trackTags = tagsByTrack[trackId] ?: emptySet()
        val matchesInclude = query.include.isEmpty() || trackTags.containsAll(query.include)
        val matchesExclude = query.exclude.any { trackTags.contains(it) }
        return matchesInclude && !matchesExclude
    }

    // Regex tolerates whitespace, missing spaces, key order, and unknown keys.
    // The old substring parser broke whenever the producer formatting changed
    // and silently returned null (-> "Empty" playlist), so this is more forgiving.
    private val tagArrayRegex = Regex("\"(include|exclude)\"\\s*:\\s*\\[([^\\]]*)\\]")

    private fun parseQuery(json: String?): TagQuery? {
        if (json == null) return null
        return try {
            val groups = tagArrayRegex.findAll(json).associate { match ->
                val key = match.groupValues[1]
                val ids = match.groupValues[2]
                    .split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                key to ids
            }
            TagQuery(
                include = groups["include"] ?: emptyList(),
                exclude = groups["exclude"] ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    private data class TagQuery(val include: List<Long>, val exclude: List<Long>)
}
