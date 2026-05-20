package com.freqvox.data.repository;

import com.freqvox.data.local.dao.PlaylistDao;
import com.freqvox.data.local.dao.TagDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PlaylistRepository_Factory implements Factory<PlaylistRepository> {
  private final Provider<PlaylistDao> playlistDaoProvider;

  private final Provider<TagDao> tagDaoProvider;

  private final Provider<TrackRepository> trackRepositoryProvider;

  public PlaylistRepository_Factory(Provider<PlaylistDao> playlistDaoProvider,
      Provider<TagDao> tagDaoProvider, Provider<TrackRepository> trackRepositoryProvider) {
    this.playlistDaoProvider = playlistDaoProvider;
    this.tagDaoProvider = tagDaoProvider;
    this.trackRepositoryProvider = trackRepositoryProvider;
  }

  @Override
  public PlaylistRepository get() {
    return newInstance(playlistDaoProvider.get(), tagDaoProvider.get(), trackRepositoryProvider.get());
  }

  public static PlaylistRepository_Factory create(Provider<PlaylistDao> playlistDaoProvider,
      Provider<TagDao> tagDaoProvider, Provider<TrackRepository> trackRepositoryProvider) {
    return new PlaylistRepository_Factory(playlistDaoProvider, tagDaoProvider, trackRepositoryProvider);
  }

  public static PlaylistRepository newInstance(PlaylistDao playlistDao, TagDao tagDao,
      TrackRepository trackRepository) {
    return new PlaylistRepository(playlistDao, tagDao, trackRepository);
  }
}
