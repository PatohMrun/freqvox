package com.freqvox.data.local.dao

import androidx.room.*
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM playlist_tracks
            WHERE playlistId = :playlistId AND trackId = :trackId
        )
    """)
    suspend fun isTrackInPlaylist(playlistId: Long, trackId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(crossRef: com.freqvox.data.local.entities.PlaylistTrackCrossRef)

    @Delete
    suspend fun deletePlaylistTrack(crossRef: com.freqvox.data.local.entities.PlaylistTrackCrossRef)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("""
        SELECT tracks.* FROM tracks 
        INNER JOIN playlist_tracks ON tracks.id = playlist_tracks.trackId 
        WHERE playlist_tracks.playlistId = :playlistId 
        ORDER BY playlist_tracks.orderIndex ASC, playlist_tracks.addedAt DESC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    // Aggregate count of tracks per manual playlist. Returned as a list of
    // (playlistId, count) rows; the repository maps it to a Map<Long, Int>
    // and merges in smart-playlist counts derived from tags.
    data class PlaylistTrackCount(val playlistId: Long, val count: Int)

    @Query("""
        SELECT playlistId, COUNT(*) AS count
        FROM playlist_tracks
        GROUP BY playlistId
    """)
    fun getManualPlaylistTrackCounts(): Flow<List<PlaylistTrackCount>>

    // Whole-table snapshot used by backup/export. Ordered so a manual playlist's
    // membership round-trips in the same order the user arranged it.
    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, orderIndex ASC")
    suspend fun getAllPlaylistTracks(): List<com.freqvox.data.local.entities.PlaylistTrackCrossRef>
}
