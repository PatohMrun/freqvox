package com.freqvox.di;

import com.freqvox.data.local.FreqvoxDatabase;
import com.freqvox.data.local.dao.PlayHistoryDao;
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
public final class DatabaseModule_ProvidePlayHistoryDaoFactory implements Factory<PlayHistoryDao> {
  private final Provider<FreqvoxDatabase> databaseProvider;

  public DatabaseModule_ProvidePlayHistoryDaoFactory(Provider<FreqvoxDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PlayHistoryDao get() {
    return providePlayHistoryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePlayHistoryDaoFactory create(
      Provider<FreqvoxDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePlayHistoryDaoFactory(databaseProvider);
  }

  public static PlayHistoryDao providePlayHistoryDao(FreqvoxDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePlayHistoryDao(database));
  }
}
