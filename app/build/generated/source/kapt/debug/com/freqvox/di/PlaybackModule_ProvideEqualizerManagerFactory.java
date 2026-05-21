package com.freqvox.di;

import com.freqvox.playback.FreqvoxEqualizerManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class PlaybackModule_ProvideEqualizerManagerFactory implements Factory<FreqvoxEqualizerManager> {
  @Override
  public FreqvoxEqualizerManager get() {
    return provideEqualizerManager();
  }

  public static PlaybackModule_ProvideEqualizerManagerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FreqvoxEqualizerManager provideEqualizerManager() {
    return Preconditions.checkNotNullFromProvides(PlaybackModule.INSTANCE.provideEqualizerManager());
  }

  private static final class InstanceHolder {
    private static final PlaybackModule_ProvideEqualizerManagerFactory INSTANCE = new PlaybackModule_ProvideEqualizerManagerFactory();
  }
}
