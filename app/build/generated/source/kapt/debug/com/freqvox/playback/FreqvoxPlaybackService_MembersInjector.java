package com.freqvox.playback;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class FreqvoxPlaybackService_MembersInjector implements MembersInjector<FreqvoxPlaybackService> {
  private final Provider<FreqvoxEqualizerManager> equalizerManagerProvider;

  public FreqvoxPlaybackService_MembersInjector(
      Provider<FreqvoxEqualizerManager> equalizerManagerProvider) {
    this.equalizerManagerProvider = equalizerManagerProvider;
  }

  public static MembersInjector<FreqvoxPlaybackService> create(
      Provider<FreqvoxEqualizerManager> equalizerManagerProvider) {
    return new FreqvoxPlaybackService_MembersInjector(equalizerManagerProvider);
  }

  @Override
  public void injectMembers(FreqvoxPlaybackService instance) {
    injectEqualizerManager(instance, equalizerManagerProvider.get());
  }

  @InjectedFieldSignature("com.freqvox.playback.FreqvoxPlaybackService.equalizerManager")
  public static void injectEqualizerManager(FreqvoxPlaybackService instance,
      FreqvoxEqualizerManager equalizerManager) {
    instance.equalizerManager = equalizerManager;
  }
}
