package com.freqvox.data.repository

import com.freqvox.data.local.dao.PlaylistDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val tagDao: TagDao,
    private val trackRepository: TrackRepository
) {
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createDynamicPlaylist(name: String, includedTagIds: List<Long>, excludedTagIds: List<Long> = emptyList()) {
        val queryJson = """{"include": ${includedTagIds}, "exclude": ${excludedTagIds}}"""
        playlistDao.insertPlaylist(
            PlaylistEntity(name = name, type = "query", queryJson = queryJson)
        )
    }

    suspend fun getTracksForPlaylist(playlist: PlaylistEntity): List<TrackEntity> {
        if (playlist.type == "manual") {
            // TODO: Implement manual playlist membership
            return emptyList()
        }
        
        // Execute dynamic query
        val allTracks = trackRepository.allTracks.first()
        val query = parseQuery(playlist.queryJson) ?: return emptyList()
        
        val filteredTracks = mutableListOf<TrackEntity>()
        
        for (track in allTracks) {
            val trackTagIds = tagDao.getTagIdsForTrack(track.id)
            
            val matchesInclude = query.include.isEmpty() || trackTagIds.containsAll(query.include)
            val matchesExclude = query.exclude.any { trackTagIds.contains(it) }
            
            if (matchesInclude && !matchesExclude) {
                filteredTracks.add(track)
            }
        }
        
        return filteredTracks
    }

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
