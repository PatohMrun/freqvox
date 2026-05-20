package com.freqvox.di;

import com.freqvox.data.local.FreqvoxDatabase;
import com.freqvox.data.local.dao.TagDao;
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
public final class DatabaseModule_ProvideTagDaoFactory implements Factory<TagDao> {
  private final Provider<FreqvoxDatabase> databaseProvider;

  public DatabaseModule_ProvideTagDaoFactory(Provider<FreqvoxDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TagDao get() {
    return provideTagDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideTagDaoFactory create(
      Provider<FreqvoxDatabase> databaseProvider) {
    return new DatabaseModule_ProvideTagDaoFactory(databaseProvider);
  }

  public static TagDao provideTagDao(FreqvoxDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTagDao(database));
  }
}
