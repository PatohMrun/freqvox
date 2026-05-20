package com.freqvox.ui.main;

import com.freqvox.data.repository.PlaylistRepository;
import com.freqvox.data.repository.TrackRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<TrackRepository> trackRepositoryProvider;

  private final Provider<PlaylistRepository> playlistRepositoryProvider;

  public MainViewModel_Factory(Provider<TrackRepository> trackRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider) {
    this.trackRepositoryProvider = trackRepositoryProvider;
    this.playlistRepositoryProvider = playlistRepositoryProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(trackRepositoryProvider.get(), playlistRepositoryProvider.get());
  }

  public static MainViewModel_Factory create(Provider<TrackRepository> trackRepositoryProvider,
      Provider<PlaylistRepository> playlistRepositoryProvider) {
    return new MainViewModel_Factory(trackRepositoryProvider, playlistRepositoryProvider);
  }

  public static MainViewModel newInstance(TrackRepository trackRepository,
      PlaylistRepository playlistRepository) {
    return new MainViewModel(trackRepository, playlistRepository);
  }
}
