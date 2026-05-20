package com.freqvox.di

import android.content.Context
import androidx.room.Room
import com.freqvox.data.local.FreqvoxDatabase
import com.freqvox.data.local.dao.PlaylistDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FreqvoxDatabase {
        return Room.databaseBuilder(
            context,
            FreqvoxDatabase::class.java,
            "freqvox.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTrackDao(database: FreqvoxDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideTagDao(database: FreqvoxDatabase): TagDao = database.tagDao()

    @Provides
    fun providePlaylistDao(database: FreqvoxDatabase): PlaylistDao = database.playlistDao()
}
