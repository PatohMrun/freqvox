package com.freqvox.di

import android.content.Context
import androidx.room.Room
import com.freqvox.data.local.FreqvoxDatabase
import com.freqvox.data.local.dao.PlayHistoryDao
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
        // IMPORTANT: do NOT use .fallbackToDestructiveMigration() — it silently
        // WIPES the entire database (playlists, tags, play history, everything)
        // whenever the schema version changes without a migration. That was the
        // cause of "playlists occasionally reset" up to 1.16.
        //
        // .fallbackToDestructiveMigrationOnDowngrade() preserves user data on
        // forward upgrades; if you bump the schema version without writing a
        // Migration, Room will throw — that's intentional, treat it as a build
        // error and write the migration. Users keep their data.
        return Room.databaseBuilder(
            context,
            FreqvoxDatabase::class.java,
            "freqvox.db"
        )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideTrackDao(database: FreqvoxDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideTagDao(database: FreqvoxDatabase): TagDao = database.tagDao()

    @Provides
    fun providePlaylistDao(database: FreqvoxDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePlayHistoryDao(database: FreqvoxDatabase): PlayHistoryDao = database.playHistoryDao()
}
