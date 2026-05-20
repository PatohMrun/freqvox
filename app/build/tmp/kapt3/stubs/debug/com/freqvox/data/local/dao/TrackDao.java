package com.freqvox.data.local.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0007\bg\u0018\u00002\u00020\u0001J\u001c\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u001c\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u00050\t2\u0006\u0010\u000b\u001a\u00020\u0006H\'J\u0014\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u00050\tH\'J\u001c\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u00050\t2\u0006\u0010\u000b\u001a\u00020\u0006H\'J\u0014\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u00050\tH\'J&\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\n0\u00052\u0006\u0010\u0010\u001a\u00020\u00062\b\b\u0002\u0010\u0011\u001a\u00020\u0012H\u00a7@\u00a2\u0006\u0002\u0010\u0013J\u0018\u0010\u0014\u001a\u0004\u0018\u00010\n2\u0006\u0010\u0010\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0015J&\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0017\u001a\u00020\u00062\u0006\u0010\u0018\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0019J\u0016\u0010\u001a\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\u0016\u0010\u001b\u001a\u00020\u00032\u0006\u0010\u001c\u001a\u00020\u001dH\u00a7@\u00a2\u0006\u0002\u0010\u001eJ\u001c\u0010\u001f\u001a\u00020\u00032\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\n0\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0016\u0010!\u001a\u00020\u00032\u0006\u0010\"\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010#\u00a8\u0006$"}, d2 = {"Lcom/freqvox/data/local/dao/TrackDao;", "", "deleteRemovedTracks", "", "remainingIds", "", "", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getActiveTracks", "Lkotlinx/coroutines/flow/Flow;", "Lcom/freqvox/data/local/entities/TrackEntity;", "recentThreshold", "getAllTracks", "getHiddenTracks", "getMostPlayedTracks", "getSimilarTracks", "trackId", "limit", "", "(JILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTrackById", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "incrementPlayCount", "timestamp", "durationPlayed", "(JJJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "incrementTrackSkipCount", "insertSkipEvent", "skip", "Lcom/freqvox/data/local/entities/SkipHistoryEntity;", "(Lcom/freqvox/data/local/entities/SkipHistoryEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertTracks", "tracks", "updateTrack", "track", "(Lcom/freqvox/data/local/entities/TrackEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface TrackDao {
    
    @androidx.room.Query(value = "\n        SELECT * FROM tracks \n        WHERE id NOT IN (\n            SELECT trackId FROM skip_history \n            WHERE skippedAt > :recentThreshold \n            GROUP BY trackId \n            HAVING COUNT(*) >= 3\n        )\n        ORDER BY dateAdded DESC\n    ")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> getActiveTracks(long recentThreshold);
    
    @androidx.room.Query(value = "SELECT * FROM tracks ORDER BY dateAdded DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> getAllTracks();
    
    @androidx.room.Query(value = "SELECT * FROM tracks WHERE id = :trackId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getTrackById(long trackId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.freqvox.data.local.entities.TrackEntity> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertTracks(@org.jetbrains.annotations.NotNull()
    java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateTrack(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackEntity track, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM tracks WHERE id NOT IN (:remainingIds)")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteRemovedTracks(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Long> remainingIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM tracks ORDER BY playCount DESC LIMIT 50")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> getMostPlayedTracks();
    
    @androidx.room.Query(value = "UPDATE tracks SET playCount = playCount + 1, lastPlayed = :timestamp, totalPlayTime = totalPlayTime + :durationPlayed WHERE id = :trackId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object incrementPlayCount(long trackId, long timestamp, long durationPlayed, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE tracks SET skipCount = skipCount + 1 WHERE id = :trackId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object incrementTrackSkipCount(long trackId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertSkipEvent(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.SkipHistoryEntity skip, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "\n        SELECT t.* FROM tracks t\n        JOIN track_tags tt ON t.id = tt.trackId\n        WHERE tt.tagId IN (SELECT tagId FROM track_tags WHERE trackId = :trackId)\n        AND t.id != :trackId\n        GROUP BY t.id\n        ORDER BY COUNT(tt.tagId) DESC\n        LIMIT :limit\n    ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSimilarTracks(long trackId, int limit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.freqvox.data.local.entities.TrackEntity>> $completion);
    
    @androidx.room.Query(value = "\n        SELECT * FROM tracks \n        WHERE id IN (\n            SELECT trackId FROM skip_history \n            WHERE skippedAt > :recentThreshold \n            GROUP BY trackId \n            HAVING COUNT(*) >= 3\n        )\n    ")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> getHiddenTracks(long recentThreshold);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}