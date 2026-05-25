package com.freqvox.data.repository

import com.freqvox.data.local.dao.PlaylistDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    suspend fun getTracksForPlaylist(playlist: PlaylistEntity): List<TrackEntity> =
        observeTracksForPlaylist(playlist).first()

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
                val trackTags = tagsByTrack[track.id] ?: emptySet()
                val matchesInclude = query.include.isEmpty() || trackTags.containsAll(query.include)
                val matchesExclude = query.exclude.any { trackTags.contains(it) }
                matchesInclude && !matchesExclude
            }
        }
    }

    suspend fun getPlaylistById(id: Long): PlaylistEntity? = playlistDao.getPlaylistById(id)

    private fun parseQuery(json: String?): TagQuery? {
        if (json == null) return null
        // Simple manual parsing since we don't have a JSON lib yet
        // In a real app, use kotlinx.serialization
        return try {
            val include = json.substringAfter("\"include\": [").substringBefore("]").split(",").mapNotNull { it.trim().toLongOrNull() }
            val exclude = json.substringAfter("\"exclude\": [").substringBefore("]").split(",").mapNotNull { it.trim().toLongOrNull() }
            TagQuery(include, exclude)
        } catch (e: Exception) {
            null
        }
    }

    private data class TagQuery(val include: List<Long>, val exclude: List<Long>)
}
