package com.freqvox.data.local.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\r0\fH\'J\u001c\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\r2\u0006\u0010\u0010\u001a\u00020\u000fH\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u001c\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\r0\f2\u0006\u0010\u0010\u001a\u00020\u000fH\'J\u001c\u0010\u0013\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00140\r0\f2\u0006\u0010\u0015\u001a\u00020\u000fH\'J\u0016\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0017\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006\u0018"}, d2 = {"Lcom/freqvox/data/local/dao/TagDao;", "", "deleteTag", "", "tag", "Lcom/freqvox/data/local/entities/TagEntity;", "(Lcom/freqvox/data/local/entities/TagEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteTrackTagCrossRef", "crossRef", "Lcom/freqvox/data/local/entities/TrackTagCrossRef;", "(Lcom/freqvox/data/local/entities/TrackTagCrossRef;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllTags", "Lkotlinx/coroutines/flow/Flow;", "", "getTagIdsForTrack", "", "trackId", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTagsForTrack", "getTracksForTag", "Lcom/freqvox/data/local/entities/TrackEntity;", "tagId", "insertTag", "insertTrackTagCrossRef", "app_debug"})
@androidx.room.Dao()
public abstract interface TagDao {
    
    @androidx.room.Query(value = "SELECT * FROM tags ORDER BY name ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TagEntity>> getAllTags();
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertTag(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TagEntity tag, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteTag(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TagEntity tag, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertTrackTagCrossRef(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackTagCrossRef crossRef, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteTrackTagCrossRef(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackTagCrossRef crossRef, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Transaction()
    @androidx.room.Query(value = "\n        SELECT * FROM tracks \n        INNER JOIN track_tags ON tracks.id = track_tags.trackId \n        WHERE track_tags.tagId = :tagId\n    ")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> getTracksForTag(long tagId);
    
    @androidx.room.Query(value = "SELECT * FROM tags INNER JOIN track_tags ON tags.id = track_tags.tagId WHERE track_tags.trackId = :trackId")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TagEntity>> getTagsForTrack(long trackId);
    
    @androidx.room.Query(value = "SELECT tagId FROM track_tags WHERE trackId = :trackId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getTagIdsForTrack(long trackId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.Long>> $completion);
}