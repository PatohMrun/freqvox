package com.freqvox.di;

import android.content.Context;
import com.freqvox.data.local.dao.PlayHistoryDao;
import com.freqvox.data.local.dao.TrackDao;
import com.freqvox.playback.PlaybackController;
import com.freqvox.playback.SleepTimerManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class PlaybackModule_ProvidePlaybackControllerFactory implements Factory<PlaybackController> {
  private final Provider<Context> contextProvider;

  private final Provider<TrackDao> trackDaoProvider;

  private final Provider<PlayHistoryDao> playHistoryDaoProvider;

  private final Provider<SleepTimerManager> sleepTimerManagerProvider;

  public PlaybackModule_ProvidePlaybackControllerFactory(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider, Provider<PlayHistoryDao> playHistoryDaoProvider,
      Provider<SleepTimerManager> sleepTimerManagerProvider) {
    this.contextProvider = contextProvider;
    this.trackDaoProvider = trackDaoProvider;
    this.playHistoryDaoProvider = playHistoryDaoProvider;
    this.sleepTimerManagerProvider = sleepTimerManagerProvider;
  }

  @Override
  public PlaybackController get() {
    return providePlaybackController(contextProvider.get(), trackDaoProvider.get(), playHistoryDaoProvider.get(), sleepTimerManagerProvider.get());
  }

  public static PlaybackModule_ProvidePlaybackControllerFactory create(
      Provider<Context> contextProvider, Provider<TrackDao> trackDaoProvider,
      Provider<PlayHistoryDao> playHistoryDaoProvider,
      Provider<SleepTimerManager> sleepTimerManagerProvider) {
    return new PlaybackModule_ProvidePlaybackControllerFactory(contextProvider, trackDaoProvider, playHistoryDaoProvider, sleepTimerManagerProvider);
  }

  public static PlaybackController providePlaybackController(Context context, TrackDao trackDao,
      PlayHistoryDao playHistoryDao, SleepTimerManager sleepTimerManager) {
    return Preconditions.checkNotNullFromProvides(PlaybackModule.INSTANCE.providePlaybackController(context, trackDao, playHistoryDao, sleepTimerManager));
  }
}
