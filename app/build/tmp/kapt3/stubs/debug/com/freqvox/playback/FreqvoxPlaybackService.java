package com.freqvox.playback;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u000b\u001a\u00020\fH\u0016J\b\u0010\r\u001a\u00020\fH\u0016J\u0012\u0010\u000e\u001a\u0004\u0018\u00010\n2\u0006\u0010\u000f\u001a\u00020\u0010H\u0016R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/freqvox/playback/FreqvoxPlaybackService;", "Landroidx/media3/session/MediaSessionService;", "()V", "equalizerManager", "Lcom/freqvox/playback/FreqvoxEqualizerManager;", "getEqualizerManager", "()Lcom/freqvox/playback/FreqvoxEqualizerManager;", "setEqualizerManager", "(Lcom/freqvox/playback/FreqvoxEqualizerManager;)V", "mediaSession", "Landroidx/media3/session/MediaSession;", "onCreate", "", "onDestroy", "onGetSession", "controllerInfo", "Landroidx/media3/session/MediaSession$ControllerInfo;", "app_debug"})
public final class FreqvoxPlaybackService extends androidx.media3.session.MediaSessionService {
    @org.jetbrains.annotations.Nullable()
    private androidx.media3.session.MediaSession mediaSession;
    @javax.inject.Inject()
    public com.freqvox.playback.FreqvoxEqualizerManager equalizerManager;
    
    public FreqvoxPlaybackService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.freqvox.playback.FreqvoxEqualizerManager getEqualizerManager() {
        return null;
    }
    
    public final void setEqualizerManager(@org.jetbrains.annotations.NotNull()
    com.freqvox.playback.FreqvoxEqualizerManager p0) {
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public androidx.media3.session.MediaSession onGetSession(@org.jetbrains.annotations.NotNull()
    androidx.media3.session.MediaSession.ControllerInfo controllerInfo) {
        return null;
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
}