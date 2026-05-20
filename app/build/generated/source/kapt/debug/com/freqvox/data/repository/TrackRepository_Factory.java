package com.freqvox.data.repository;

import android.content.Context;
import com.freqvox.data.local.dao.TagDao;
import com.freqvox.data.local.dao.TrackDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class TrackRepository_Factory implements Factory<TrackRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<TrackDao> trackDaoProvider;

  private final Provider<TagDao> tagDaoProvider;

  public TrackRepository_Factory(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider, Provider<TagDao> tagDaoProvider) {
    this.contextProvider = contextProvider;
    this.trackDaoProvider = trackDaoProvider;
    this.tagDaoProvider = tagDaoProvider;
  }

  @Override
  public TrackRepository get() {
    return newInstance(contextProvider.get(), trackDaoProvider.get(), tagDaoProvider.get());
  }

  public static TrackRepository_Factory create(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider, Provider<TagDao> tagDaoProvider) {
    return new TrackRepository_Factory(contextProvider, trackDaoProvider, tagDaoProvider);
  }

  public static TrackRepository newInstance(Context context, TrackDao trackDao, TagDao tagDao) {
    return new TrackRepository(context, trackDao, tagDao);
  }
}
