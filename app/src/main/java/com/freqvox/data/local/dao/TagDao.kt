package com.freqvox.data.local.dao

import androidx.room.*
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.data.local.entities.TrackTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackTagCrossRef(crossRef: TrackTagCrossRef)

    @Delete
    suspend fun deleteTrackTagCrossRef(crossRef: TrackTagCrossRef)

    @Transaction
    @Query("""
        SELECT * FROM tracks 
        INNER JOIN track_tags ON tracks.id = track_tags.trackId 
        WHERE track_tags.tagId = :tagId
    """)
    fun getTracksForTag(tagId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tags INNER JOIN track_tags ON tags.id = track_tags.tagId WHERE track_tags.trackId = :trackId")
    fun getTagsForTrack(trackId: Long): Flow<List<TagEntity>>
    
    @Query("SELECT tagId FROM track_tags WHERE trackId = :trackId")
    suspend fun getTagIdsForTrack(trackId: Long): List<Long>

    @Query("SELECT * FROM track_tags")
    fun getAllTrackTagCrossRefs(): Flow<List<TrackTagCrossRef>>
}
