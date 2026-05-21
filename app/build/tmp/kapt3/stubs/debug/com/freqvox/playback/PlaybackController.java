package com.freqvox.playback;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0090\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0019\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b#\u0018\u00002\u00020\u0001B-\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\u000e\u0010L\u001a\u00020M2\u0006\u0010N\u001a\u00020OJ\u0006\u0010P\u001a\u00020MJ\u0006\u0010Q\u001a\u00020MJ\b\u0010R\u001a\u00020MH\u0002J\u000e\u0010S\u001a\u00020M2\u0006\u0010T\u001a\u00020\u0015J\b\u0010U\u001a\u00020MH\u0002J\u0010\u0010V\u001a\u00020M2\u0006\u0010W\u001a\u00020>H\u0002J2\u0010X\u001a\u00020M2\u0006\u0010Y\u001a\u00020\u000f2\u0006\u0010Z\u001a\u00020\u000f2\u0006\u0010[\u001a\u00020\u000f2\u0006\u0010\\\u001a\u00020\u000f2\n\b\u0002\u0010]\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010^\u001a\u00020MJ\u0006\u0010_\u001a\u00020MJ\u001e\u0010`\u001a\u00020M2\f\u0010a\u001a\b\u0012\u0004\u0012\u00020O0\u001c2\b\b\u0002\u0010b\u001a\u00020\u0015J\u0006\u0010c\u001a\u00020MJ\b\u0010d\u001a\u00020MH\u0002J\u0006\u0010e\u001a\u00020MJ\u000e\u0010f\u001a\u00020M2\u0006\u0010T\u001a\u00020\u0015J\u000e\u0010g\u001a\u00020M2\u0006\u0010h\u001a\u00020\u0013J\b\u0010i\u001a\u00020MH\u0002J\u0006\u0010j\u001a\u00020MJ\b\u0010k\u001a\u00020MH\u0002J\u000e\u0010l\u001a\u00020M2\u0006\u0010m\u001a\u00020\u0015J\u0006\u0010n\u001a\u00020MJ\u0006\u0010o\u001a\u00020MJ\u0006\u0010p\u001a\u00020MJ\u0012\u0010q\u001a\u00020M2\b\u0010W\u001a\u0004\u0018\u00010>H\u0002R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00110\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0016\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00130\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00130\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u001b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001d0\u001c0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010 \u001a\n\u0012\u0004\u0012\u00020\"\u0018\u00010!X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u000f0$\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010&R\u0019\u0010\'\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00110$\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010&R\u0017\u0010)\u001a\b\u0012\u0004\u0012\u00020\u00130$\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010&R\u0017\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00150$\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010&R\u0019\u0010-\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00130$\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010&R\u0017\u0010/\u001a\b\u0012\u0004\u0012\u00020\u000f0$\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010&R\u0017\u00101\u001a\b\u0012\u0004\u0012\u00020\u00130$\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u0010&R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u00104R\u000e\u00105\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00106\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0012\u00107\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u00108R\u0017\u00109\u001a\b\u0012\u0004\u0012\u00020\u001a0$\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010&R\u0017\u0010:\u001a\b\u0012\u0004\u0012\u00020\u001a0$\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010&R\u0012\u0010;\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u00108R\u0010\u0010<\u001a\u0004\u0018\u00010\"X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010=\u001a\u0004\u0018\u00010>X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010?\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001d\u0010@\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001d0\u001c0$\u00a2\u0006\b\n\u0000\u001a\u0004\bA\u0010&R\u0017\u0010B\u001a\b\u0012\u0004\u0012\u00020\u00150$\u00a2\u0006\b\n\u0000\u001a\u0004\bC\u0010&R\u000e\u0010D\u001a\u00020EX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010F\u001a\u0004\u0018\u00010GX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010H\u001a\b\u0012\u0004\u0012\u00020\u001a0$\u00a2\u0006\b\n\u0000\u001a\u0004\bI\u0010&R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010J\u001a\b\u0012\u0004\u0012\u00020\u00130$\u00a2\u0006\b\n\u0000\u001a\u0004\bK\u0010&R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006r"}, d2 = {"Lcom/freqvox/playback/PlaybackController;", "", "context", "Landroid/content/Context;", "trackDao", "Lcom/freqvox/data/local/dao/TrackDao;", "playHistoryDao", "Lcom/freqvox/data/local/dao/PlayHistoryDao;", "sleepTimerManager", "Lcom/freqvox/playback/SleepTimerManager;", "equalizerManager", "Lcom/freqvox/playback/FreqvoxEqualizerManager;", "(Landroid/content/Context;Lcom/freqvox/data/local/dao/TrackDao;Lcom/freqvox/data/local/dao/PlayHistoryDao;Lcom/freqvox/playback/SleepTimerManager;Lcom/freqvox/playback/FreqvoxEqualizerManager;)V", "_currentArtist", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_currentArtworkUri", "Landroid/net/Uri;", "_currentPosition", "", "_currentQueueIndex", "", "_currentTrackId", "_currentTrackTitle", "_duration", "_isPlaying", "", "_queue", "", "Lcom/freqvox/playback/QueueItem;", "_repeatMode", "_shuffleEnabled", "controllerFuture", "Lcom/google/common/util/concurrent/ListenableFuture;", "Landroidx/media3/session/MediaController;", "currentArtist", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentArtist", "()Lkotlinx/coroutines/flow/StateFlow;", "currentArtworkUri", "getCurrentArtworkUri", "currentPosition", "getCurrentPosition", "currentQueueIndex", "getCurrentQueueIndex", "currentTrackId", "getCurrentTrackId", "currentTrackTitle", "getCurrentTrackTitle", "duration", "getDuration", "getEqualizerManager", "()Lcom/freqvox/playback/FreqvoxEqualizerManager;", "historyAccumulated", "historySegmentStart", "historyTrackId", "Ljava/lang/Long;", "isPlaying", "isTimerRunning", "lastTrackId", "mediaController", "pendingMediaItem", "Landroidx/media3/common/MediaItem;", "playStartTime", "queue", "getQueue", "repeatMode", "getRepeatMode", "scope", "Lkotlinx/coroutines/CoroutineScope;", "seekDebounceJob", "Lkotlinx/coroutines/Job;", "shuffleEnabled", "getShuffleEnabled", "timerTimeLeft", "getTimerTimeLeft", "addToQueue", "", "track", "Lcom/freqvox/data/local/entities/TrackEntity;", "cycleRepeatMode", "initializeController", "insertHistoryRecord", "jumpToQueueItem", "index", "logPlayTime", "playMediaItem", "mediaItem", "playMediaUri", "uri", "title", "artist", "trackId", "artworkPath", "playNext", "playPrevious", "playQueue", "tracks", "startIndex", "reconnectIfNeeded", "refreshQueue", "release", "removeFromQueue", "seekTo", "position", "setupControllerListener", "skipNext", "startPositionPolling", "startSleepTimer", "minutes", "stopSleepTimer", "togglePlayPause", "toggleShuffle", "updateMetadata", "app_debug"})
public final class PlaybackController {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.TrackDao trackDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.data.local.dao.PlayHistoryDao playHistoryDao = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.playback.SleepTimerManager sleepTimerManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.freqvox.playback.FreqvoxEqualizerManager equalizerManager = null;
    @org.jetbrains.annotations.Nullable()
    private com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController> controllerFuture;
    @org.jetbrains.annotations.Nullable()
    private androidx.media3.session.MediaController mediaController;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _currentTrackId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> currentTrackId = null;
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
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _shuffleEnabled = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> shuffleEnabled = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _repeatMode = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> repeatMode = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isTimerRunning = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> timerTimeLeft = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.freqvox.playback.QueueItem>> _queue = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.freqvox.playback.QueueItem>> queue = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _currentQueueIndex = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> currentQueueIndex = null;
    @org.jetbrains.annotations.Nullable()
    private androidx.media3.common.MediaItem pendingMediaItem;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Long lastTrackId;
    private long playStartTime = 0L;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job seekDebounceJob;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Long historyTrackId;
    private long historyAccumulated = 0L;
    private long historySegmentStart = 0L;
    
    public PlaybackController(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.TrackDao trackDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.PlayHistoryDao playHistoryDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.playback.SleepTimerManager sleepTimerManager, @org.jetbrains.annotations.NotNull()
    com.freqvox.playback.FreqvoxEqualizerManager equalizerManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.freqvox.playback.FreqvoxEqualizerManager getEqualizerManager() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getCurrentTrackId() {
        return null;
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
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getShuffleEnabled() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getRepeatMode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isTimerRunning() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getTimerTimeLeft() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.freqvox.playback.QueueItem>> getQueue() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getCurrentQueueIndex() {
        return null;
    }
    
    public final void initializeController() {
    }
    
    public final void reconnectIfNeeded() {
    }
    
    private final void setupControllerListener() {
    }
    
    private final void updateMetadata(androidx.media3.common.MediaItem mediaItem) {
    }
    
    private final void logPlayTime() {
    }
    
    private final void insertHistoryRecord() {
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
    
    public final void toggleShuffle() {
    }
    
    public final void cycleRepeatMode() {
    }
    
    private final void refreshQueue() {
    }
    
    public final void playQueue(@org.jetbrains.annotations.NotNull()
    java.util.List<com.freqvox.data.local.entities.TrackEntity> tracks, int startIndex) {
    }
    
    public final void addToQueue(@org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.entities.TrackEntity track) {
    }
    
    public final void removeFromQueue(int index) {
    }
    
    public final void jumpToQueueItem(int index) {
    }
    
    public final void startSleepTimer(int minutes) {
    }
    
    public final void stopSleepTimer() {
    }
    
    public final void release() {
    }
}