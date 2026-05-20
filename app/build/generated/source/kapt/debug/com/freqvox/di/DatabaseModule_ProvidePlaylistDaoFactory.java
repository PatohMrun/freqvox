package com.freqvox.di;

import com.freqvox.data.local.FreqvoxDatabase;
import com.freqvox.data.local.dao.PlaylistDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvidePlaylistDaoFactory implements Factory<PlaylistDao> {
  private final Provider<FreqvoxDatabase> databaseProvider;

  public DatabaseModule_ProvidePlaylistDaoFactory(Provider<FreqvoxDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PlaylistDao get() {
    return providePlaylistDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePlaylistDaoFactory create(
      Provider<FreqvoxDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePlaylistDaoFactory(databaseProvider);
  }

  public static PlaylistDao providePlaylistDao(FreqvoxDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePlaylistDao(database));
  }
}
