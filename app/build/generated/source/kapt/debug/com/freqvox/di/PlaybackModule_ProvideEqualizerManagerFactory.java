package com.freqvox.di;

import android.content.Context;
import com.freqvox.playback.FreqvoxEqualizerManager;
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
public final class PlaybackModule_ProvideEqualizerManagerFactory implements Factory<FreqvoxEqualizerManager> {
  private final Provider<Context> contextProvider;

  public PlaybackModule_ProvideEqualizerManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FreqvoxEqualizerManager get() {
    return provideEqualizerManager(contextProvider.get());
  }

  public static PlaybackModule_ProvideEqualizerManagerFactory create(
      Provider<Context> contextProvider) {
    return new PlaybackModule_ProvideEqualizerManagerFactory(contextProvider);
  }

  public static FreqvoxEqualizerManager provideEqualizerManager(Context context) {
    return Preconditions.checkNotNullFromProvides(PlaybackModule.INSTANCE.provideEqualizerManager(context));
  }
}
