package com.freqvox.playback;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0013\n\u0002\u0010\b\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010,\u001a\u00020-J\b\u0010.\u001a\u00020-H\u0002J\u0010\u0010/\u001a\u00020-2\u0006\u00100\u001a\u00020(H\u0002J2\u00101\u001a\u00020-2\u0006\u00102\u001a\u00020\u000b2\u0006\u00103\u001a\u00020\u000b2\u0006\u00104\u001a\u00020\u000b2\u0006\u00105\u001a\u00020\u000b2\n\b\u0002\u00106\u001a\u0004\u0018\u00010\u000bJ\u0006\u00107\u001a\u00020-J\u0006\u00108\u001a\u00020-J\u0006\u00109\u001a\u00020-J\u000e\u0010:\u001a\u00020-2\u0006\u0010;\u001a\u00020\u000fJ\b\u0010<\u001a\u00020-H\u0002J\u0006\u0010=\u001a\u00020-J\b\u0010>\u001a\u00020-H\u0002J\u000e\u0010?\u001a\u00020-2\u0006\u0010@\u001a\u00020AJ\u0006\u0010B\u001a\u00020-J\u0006\u0010C\u001a\u00020-J\u0012\u0010D\u001a\u00020-2\b\u00100\u001a\u0004\u0018\u00010(H\u0002R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0014\u001a\n\u0012\u0004\u0012\u00020\u0016\u0018\u00010\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0019\u0010\u001b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001aR\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001aR\u0017\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001aR\u0017\u0010!\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001aR\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00130\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u001aR\u0012\u0010$\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010%R\u0010\u0010&\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\'\u001a\u0004\u0018\u00010(X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010)\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010*\u001a\u00020+X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006E"}, d2 = {"Lcom/freqvox/playback/PlaybackController;", "", "context", "Landroid/content/Context;", "trackDao", "Lcom/freqvox/data/local/dao/TrackDao;", "sleepTimerManager", "Lcom/freqvox/playback/SleepTimerManager;", "(Landroid/content/Context;Lcom/freqvox/data/local/dao/TrackDao;Lcom/freqvox/playback/SleepTimerManager;)V", "_currentArtist", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_currentArtworkUri", "Landroid/net/Uri;", "_currentPosition", "", "_currentTrackTitle", "_duration", "_isPlaying", "", "controllerFuture", "Lcom/google/common/util/concurrent/ListenableFuture;", "Landroidx/media3/session/MediaController;", "currentArtist", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentArtist", "()Lkotlinx/coroutines/flow/StateFlow;", "currentArtworkUri", "getCurrentArtworkUri", "currentPosition", "getCurrentPosition", "currentTrackTitle", "getCurrentTrackTitle", "duration", "getDuration", "isPlaying", "lastTrackId", "Ljava/lang/Long;", "mediaController", "pendingMediaItem", "Landroidx/media3/common/MediaItem;", "playStartTime", "scope", "Lkotlinx/coroutines/CoroutineScope;", "initializeController", "", "logPlayTime", "playMediaItem", "mediaItem", "playMediaUri", "uri", "title", "artist", "trackId", "artworkPath", "playNext", "playPrevious", "release", "seekTo", "position", "setupControllerListener", "skipNext", "startPositionPolling", "startSleepTimer", "minutes", "", "stopSleepTimer", "togglePlayPause", "updateMetadata", "app_debug"})
public final class PlaybackController {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.TrackDao trackDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.playback.SleepTimerManager sleepTimerManager = null;
    @org.jetbrains.annotations.Nullable()
    private com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController> controllerFuture;
    @org.jetbrains.annotations.Nullable()
    private androidx.media3.session.MediaController mediaController;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isPlaying = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _currentTrackTitle = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> currentTrackTitle = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _currentArtist = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> currentArtist = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<android.net.Uri> _currentArtworkUri = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<android.net.Uri> currentArtworkUri = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _currentPosition = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> currentPosition = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _duration = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> duration = null;
    @org.jetbrains.annotations.Nullable()
    private androidx.media3.common.MediaItem pendingMediaItem;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Long lastTrackId;
    private long playStartTime = 0L;
    
    public PlaybackController(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.TrackDao trackDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.playback.SleepTimerManager sleepTimerManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getCurrentTrackTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getCurrentArtist() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<android.net.Uri> getCurrentArtworkUri() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getCurrentPosition() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getDuration() {
        return null;
    }
    
    public final void initializeController() {
    }
    
    private final void setupControllerListener() {
    }
    
    private final void updateMetadata(androidx.media3.common.MediaItem mediaItem) {
    }
    
    private final void logPlayTime() {
    }
    
    private final void startPositionPolling() {
    }
    
    public final void playMediaUri(@org.jetbrains.annotations.NotNull()
    java.lang.String uri, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, @org.jetbrains.annotations.NotNull()
    java.lang.String trackId, @org.jetbrains.annotations.Nullable()
    java.lang.String artworkPath) {
    }
    
    private final void playMediaItem(androidx.media3.common.MediaItem mediaItem) {
    }
    
    public final void seekTo(long position) {
    }
    
    public final void togglePlayPause() {
    }
    
    public final void playNext() {
    }
    
    public final void skipNext() {
    }
    
    public final void playPrevious() {
    }
    
    public final void startSleepTimer(int minutes) {
    }
    
    public final void stopSleepTimer() {
    }
    
    public final void release() {
    }
}