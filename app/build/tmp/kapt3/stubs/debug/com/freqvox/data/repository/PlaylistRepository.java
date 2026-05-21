package com.freqvox.data.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001:\u0001\u001fB\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ4\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u000b2\u000e\b\u0002\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00140\u000bH\u0086@\u00a2\u0006\u0002\u0010\u0016J\u0016\u0010\u0017\u001a\u00020\u00102\u0006\u0010\u0018\u001a\u00020\fH\u0086@\u00a2\u0006\u0002\u0010\u0019J\u001c\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001b0\u000b2\u0006\u0010\u0018\u001a\u00020\fH\u0086@\u00a2\u0006\u0002\u0010\u0019J\u0014\u0010\u001c\u001a\u0004\u0018\u00010\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\u0012H\u0002R\u001d\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/freqvox/data/repository/PlaylistRepository;", "", "playlistDao", "Lcom/freqvox/data/local/dao/PlaylistDao;", "tagDao", "Lcom/freqvox/data/local/dao/TagDao;", "trackRepository", "Lcom/freqvox/data/repository/TrackRepository;", "(Lcom/freqvox/data/local/dao/PlaylistDao;Lcom/freqvox/data/local/dao/TagDao;Lcom/freqvox/data/repository/TrackRepository;)V", "allPlaylists", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/freqvox/data/local/entities/PlaylistEntity;", "getAllPlaylists", "()Lkotlinx/coroutines/flow/Flow;", "createDynamicPlaylist", "", "name", "", "includedTagIds", "", "excludedTagIds", "(Ljava/lang/String;Ljava/util/List;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deletePlaylist", "playlist", "(Lcom/freqvox/data/local/entities/PlaylistEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTracksForPlaylist", "Lcom/freqvox/data/local/entities/TrackEntity;", "parseQuery", "Lcom/freqvox/data/repository/PlaylistRepository$TagQuery;", "json", "TagQuery", "app_debug"})
public final class PlaylistRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.PlaylistDao playlistDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.TagDao tagDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.repository.TrackRepository trackRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.PlaylistEntity>> allPlaylists = null;
    
    @javax.inject.Inject()
    public PlaylistRepository(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.PlaylistDao playlistDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.TagDao tagDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.repository.TrackRepository trackRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.freqvox.data.local.entities.PlaylistEntity>> getAllPlaylists() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deletePlaylist(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.PlaylistEntity playlist, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object createDynamicPlaylist(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Long> includedTagIds, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Long> excludedTagIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getTracksForPlaylist(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.PlaylistEntity playlist, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.freqvox.data.local.entities.TrackEntity>> $completion) {
        return null;
    }
    
    private final com.freqvox.data.repository.PlaylistRepository.TagQuery parseQuery(java.lang.String json) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0010\t\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0082\b\u0018\u00002\u00020\u0001B!\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\u0002\u0010\u0006J\u000f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J)\u0010\f\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0001J\u0013\u0010\r\u001a\u00020\u000e2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001J\t\u0010\u0012\u001a\u00020\u0013H\u00d6\u0001R\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\b\u00a8\u0006\u0014"}, d2 = {"Lcom/freqvox/data/repository/PlaylistRepository$TagQuery;", "", "include", "", "", "exclude", "(Ljava/util/List;Ljava/util/List;)V", "getExclude", "()Ljava/util/List;", "getInclude", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "", "app_debug"})
    static final class TagQuery {
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Long> include = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Long> exclude = null;
        
        public TagQuery(@org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Long> include, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Long> exclude) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Long> getInclude() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Long> getExclude() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Long> component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Long> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.freqvox.data.repository.PlaylistRepository.TagQuery copy(@org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Long> include, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Long> exclude) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}