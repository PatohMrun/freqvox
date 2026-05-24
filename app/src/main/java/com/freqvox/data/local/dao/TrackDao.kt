package com.freqvox.data.local.dao

import androidx.room.*
import com.freqvox.data.local.entities.SkipHistoryEntity
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("""
        SELECT * FROM tracks 
        WHERE id NOT IN (
            SELECT trackId FROM skip_history 
            WHERE skippedAt > :recentThreshold 
            GROUP BY trackId 
            HAVING COUNT(*) >= 3
        )
        ORDER BY dateAdded DESC
    """)
    fun getActiveTracks(recentThreshold: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id IN (:trackIds)")
    suspend fun getTracksByIds(trackIds: List<Long>): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id NOT IN (:remainingIds)")
    suspend fun deleteRemovedTracks(remainingIds: List<Long>)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50")
    fun getMostPlayedTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayed = :timestamp, totalPlayTime = totalPlayTime + :durationPlayed WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long, timestamp: Long, durationPlayed: Long)

    @Query("UPDATE tracks SET skipCount = skipCount + 1 WHERE id = :trackId")
    suspend fun incrementTrackSkipCount(trackId: Long)

    @Insert
    suspend fun insertSkipEvent(skip: SkipHistoryEntity)

    @Query("""
        SELECT t.* FROM tracks t
        JOIN track_tags tt ON t.id = tt.trackId
        WHERE tt.tagId IN (SELECT tagId FROM track_tags WHERE trackId = :trackId)
        AND t.id != :trackId
        GROUP BY t.id
        ORDER BY COUNT(tt.tagId) DESC
        LIMIT :limit
    """)
    suspend fun getSimilarTracks(trackId: Long, limit: Int = 20): List<TrackEntity>

    @Query("""
        SELECT * FROM tracks 
        WHERE id IN (
            SELECT trackId FROM skip_history 
            WHERE skippedAt > :recentThreshold 
            GROUP BY trackId 
            HAVING COUNT(*) >= 3
        )
    """)
    fun getHiddenTracks(recentThreshold: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomTracks(limit: Int = 15): List<TrackEntity>
}
