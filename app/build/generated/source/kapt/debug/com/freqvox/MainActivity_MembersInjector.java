package com.freqvox;

import com.freqvox.playback.PlaybackController;
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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<PlaybackController> playbackControllerProvider;

  public MainActivity_MembersInjector(Provider<PlaybackController> playbackControllerProvider) {
    this.playbackControllerProvider = playbackControllerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<PlaybackController> playbackControllerProvider) {
    return new MainActivity_MembersInjector(playbackControllerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPlaybackController(instance, playbackControllerProvider.get());
  }

  @InjectedFieldSignature("com.freqvox.MainActivity.playbackController")
  public static void injectPlaybackController(MainActivity instance,
      PlaybackController playbackController) {
    instance.playbackController = playbackController;
  }
}
