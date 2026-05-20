package com.freqvox.data.repository;

import android.content.Context;
import com.freqvox.data.local.dao.PlayHistoryDao;
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

  private final Provider<PlayHistoryDao> playHistoryDaoProvider;

  public TrackRepository_Factory(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider, Provider<TagDao> tagDaoProvider,
      Provider<PlayHistoryDao> playHistoryDaoProvider) {
    this.contextProvider = contextProvider;
    this.trackDaoProvider = trackDaoProvider;
    this.tagDaoProvider = tagDaoProvider;
    this.playHistoryDaoProvider = playHistoryDaoProvider;
  }

  @Override
  public TrackRepository get() {
    return newInstance(contextProvider.get(), trackDaoProvider.get(), tagDaoProvider.get(), playHistoryDaoProvider.get());
  }

  public static TrackRepository_Factory create(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider, Provider<TagDao> tagDaoProvider,
      Provider<PlayHistoryDao> playHistoryDaoProvider) {
    return new TrackRepository_Factory(contextProvider, trackDaoProvider, tagDaoProvider, playHistoryDaoProvider);
  }

  public static TrackRepository newInstance(Context context, TrackDao trackDao, TagDao tagDao,
      PlayHistoryDao playHistoryDao) {
    return new TrackRepository(context, trackDao, tagDao, playHistoryDao);
  }
}
