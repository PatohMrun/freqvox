package com.freqvox.data.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0007\u0018\u00002\u00020\u0001B)\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0018H\u0002J\u000e\u0010\u001a\u001a\u00020\u001bH\u0082@\u00a2\u0006\u0002\u0010\u001cJ\u001a\u0010\u001d\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00150\r0\f2\u0006\u0010\u001e\u001a\u00020\u001fJ\u001a\u0010 \u001a\u0004\u0018\u00010\u00182\u0006\u0010!\u001a\u00020\u001f2\u0006\u0010\"\u001a\u00020\u0018H\u0002J\u0012\u0010#\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020$0\r0\fJ\u001c\u0010%\u001a\b\u0012\u0004\u0012\u00020\u00150\r2\u0006\u0010!\u001a\u00020\u001fH\u0086@\u00a2\u0006\u0002\u0010&J\u001a\u0010\'\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\f2\u0006\u0010!\u001a\u00020\u001fJ\u001c\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00150\r2\u0006\u0010)\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010*J\u000e\u0010+\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0002\u0010\u001cJ\u001e\u0010,\u001a\u00020\u001b2\u0006\u0010!\u001a\u00020\u001f2\u0006\u0010-\u001a\u00020\u001fH\u0086@\u00a2\u0006\u0002\u0010.R\u001d\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u001d\u0010\u0011\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\r0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0010R\u001d\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00150\r0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0010R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006/"}, d2 = {"Lcom/freqvox/data/repository/TrackRepository;", "", "context", "Landroid/content/Context;", "trackDao", "Lcom/freqvox/data/local/dao/TrackDao;", "tagDao", "Lcom/freqvox/data/local/dao/TagDao;", "playHistoryDao", "Lcom/freqvox/data/local/dao/PlayHistoryDao;", "(Landroid/content/Context;Lcom/freqvox/data/local/dao/TrackDao;Lcom/freqvox/data/local/dao/TagDao;Lcom/freqvox/data/local/dao/PlayHistoryDao;)V", "allTags", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/freqvox/data/local/entities/TagEntity;", "getAllTags", "()Lkotlinx/coroutines/flow/Flow;", "allTrackTagCrossRefs", "Lcom/freqvox/data/local/entities/TrackTagCrossRef;", "getAllTrackTagCrossRefs", "allTracks", "Lcom/freqvox/data/local/entities/TrackEntity;", "getAllTracks", "cleanMetadata", "", "title", "ensureDefaultTags", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getActiveTracks", "recentThreshold", "", "getOrExtractArtwork", "trackId", "audioPath", "getRecentPlayHistory", "Lcom/freqvox/data/local/entities/PlayHistoryEntity;", "getSimilarTracks", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTagsForTrack", "getTracksByFolder", "folderPath", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "scanLibrary", "toggleTagForTrack", "tagId", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class TrackRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.TrackDao trackDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.TagDao tagDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.PlayHistoryDao playHistoryDao = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> allTracks = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TagEntity>> allTags = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackTagCrossRef>> allTrackTagCrossRefs = null;
    
    @javax.inject.Inject()
    public TrackRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.TrackDao trackDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.TagDao tagDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.PlayHistoryDao playHistoryDao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> getAllTracks() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TagEntity>> getAllTags() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackTagCrossRef>> getAllTrackTagCrossRefs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.PlayHistoryEntity>> getRecentPlayHistory() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TrackEntity>> getActiveTracks(long recentThreshold) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object scanLibrary(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object ensureDefaultTags(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.String getOrExtractArtwork(long trackId, java.lang.String audioPath) {
        return null;
    }
    
    private final java.lang.String cleanMetadata(java.lang.String title) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.TagEntity>> getTagsForTrack(long trackId) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object toggleTagForTrack(long trackId, long tagId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getSimilarTracks(long trackId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.freqvox.data.local.entities.TrackEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getTracksByFolder(@org.jetbrains.annotations.NotNull()
    java.lang.String folderPath, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.freqvox.data.local.entities.TrackEntity>> $completion) {
        return null;
    }
}