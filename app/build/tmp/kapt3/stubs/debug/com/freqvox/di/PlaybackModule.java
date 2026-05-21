package com.freqvox.di;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007J2\u0010\u0007\u001a\u00020\b2\b\b\u0001\u0010\u0005\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0004H\u0007\u00a8\u0006\u0010"}, d2 = {"Lcom/freqvox/di/PlaybackModule;", "", "()V", "provideEqualizerManager", "Lcom/freqvox/playback/FreqvoxEqualizerManager;", "context", "Landroid/content/Context;", "providePlaybackController", "Lcom/freqvox/playback/PlaybackController;", "trackDao", "Lcom/freqvox/data/local/dao/TrackDao;", "playHistoryDao", "Lcom/freqvox/data/local/dao/PlayHistoryDao;", "sleepTimerManager", "Lcom/freqvox/playback/SleepTimerManager;", "equalizerManager", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class PlaybackModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.freqvox.di.PlaybackModule INSTANCE = null;
    
    private PlaybackModule() {
        super();
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.freqvox.playback.FreqvoxEqualizerManager provideEqualizerManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.freqvox.playback.PlaybackController providePlaybackController(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.TrackDao trackDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.data.local.dao.PlayHistoryDao playHistoryDao, @org.jetbrains.annotations.NotNull()
    com.freqvox.playback.SleepTimerManager sleepTimerManager, @org.jetbrains.annotations.NotNull()
    com.freqvox.playback.FreqvoxEqualizerManager equalizerManager) {
        return null;
    }
}