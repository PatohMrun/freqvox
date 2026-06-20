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

    // Library scan path. MUST NOT use OnConflictStrategy.REPLACE — REPLACE is
    // implemented as DELETE+INSERT in SQLite, which fires the ForeignKey.CASCADE
    // on `playlist_tracks` (and any future cascaded child table) and wipes the
    // user's playlist memberships every single scan. @Upsert performs an
    // INSERT-or-UPDATE in place, so existing rows are mutated, no row is
    // deleted, and no cascade fires for stable MediaStore IDs.
    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id NOT IN (:remainingIds)")
    suspend fun deleteRemovedTracks(remainingIds: List<Long>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteTracksByIds(ids: List<Long>)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("DELETE FROM track_tags WHERE trackId = :trackId")
    suspend fun deleteTrackTagsForTrack(trackId: Long)

    @Query("DELETE FROM play_history WHERE trackId = :trackId")
    suspend fun deletePlayHistoryForTrack(trackId: Long)

    @Query("DELETE FROM skip_history WHERE trackId = :trackId")
    suspend fun deleteSkipHistoryForTrack(trackId: Long)

    @Query("DELETE FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun deletePlaylistTracksForTrack(trackId: Long)

    // ── Track-id migration ────────────────────────────────────────────────
    // MediaStore reissues IDs whenever it re-indexes a file (reboot, file edit,
    // some content-provider events). The old PK row would otherwise be deleted
    // by `deleteRemovedTracks`, CASCADE-wiping playlist_tracks/track_tags/etc.
    // Instead we look up by path, then rewrite every cross-reference table from
    // the old id to the new id BEFORE deleting the old row. UPDATE OR IGNORE
    // is used so an already-existing (newId, x) row doesn't crash the update;
    // the leftover (oldId, x) rows are then dropped explicitly.

    @Query("UPDATE OR IGNORE playlist_tracks SET trackId = :newId WHERE trackId = :oldId")
    suspend fun rewritePlaylistTrackId(oldId: Long, newId: Long)

    @Query("UPDATE OR IGNORE track_tags SET trackId = :newId WHERE trackId = :oldId")
    suspend fun rewriteTrackTagTrackId(oldId: Long, newId: Long)

    @Query("UPDATE play_history SET trackId = :newId WHERE trackId = :oldId")
    suspend fun rewritePlayHistoryTrackId(oldId: Long, newId: Long)

    @Query("UPDATE skip_history SET trackId = :newId WHERE trackId = :oldId")
    suspend fun rewriteSkipHistoryTrackId(oldId: Long, newId: Long)

    @androidx.room.Transaction
    suspend fun migrateTrackReferences(oldId: Long, newId: Long) {
        // Order matters: rewrite refs first, drop leftover stale refs, only
        // then delete the old PK row so CASCADE can't fire on anything we
        // still want to keep.
        rewritePlaylistTrackId(oldId, newId)
        deletePlaylistTracksForTrack(oldId)
        rewriteTrackTagTrackId(oldId, newId)
        deleteTrackTagsForTrack(oldId)
        rewritePlayHistoryTrackId(oldId, newId)
        rewriteSkipHistoryTrackId(oldId, newId)
        deleteTrackById(oldId)
    }

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50")
    fun getMostPlayedTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayed = :timestamp, totalPlayTime = totalPlayTime + :durationPlayed WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long, timestamp: Long, durationPlayed: Long)

    // Adds listen time + refreshes lastPlayed WITHOUT bumping playCount — used for
    // the 2nd, 3rd… segment of a track that has already been counted as one play.
    @Query("UPDATE tracks SET lastPlayed = :timestamp, totalPlayTime = totalPlayTime + :durationPlayed WHERE id = :trackId")
    suspend fun addPlayTime(trackId: Long, timestamp: Long, durationPlayed: Long)

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

    @Query("""
        SELECT * FROM tracks
        WHERE id NOT IN (
            SELECT trackId FROM track_tags
            WHERE tagId = (SELECT id FROM tags WHERE LOWER(name) = 'disliked' LIMIT 1)
        )
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomTracks(limit: Int = 15): List<TrackEntity>

    /** Random library tracks, excluding a given set of ids — used by autoplay to
     *  keep the queue flowing past a finished playlist without re-queuing the
     *  same tracks. Also excludes disliked tracks. */
    @Query("""
        SELECT * FROM tracks
        WHERE id NOT IN (:excludeIds)
        AND id NOT IN (
            SELECT trackId FROM track_tags
            WHERE tagId = (SELECT id FROM tags WHERE LOWER(name) = 'disliked' LIMIT 1)
        )
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomTracksExcluding(excludeIds: List<Long>, limit: Int = 15): List<TrackEntity>
}
