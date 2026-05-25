package com.freqvox.di

import android.content.Context
import com.freqvox.data.local.dao.PlayHistoryDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.playback.FreqvoxEqualizerManager
import com.freqvox.playback.PlaybackController
import com.freqvox.playback.SleepTimerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun provideEqualizerManager(@ApplicationContext context: Context): FreqvoxEqualizerManager {
        return FreqvoxEqualizerManager(context)
    }

    @Provides
    @Singleton
    fun providePlaybackController(
        @ApplicationContext context: Context,
        trackDao: TrackDao,
        tagDao: TagDao,
        playHistoryDao: PlayHistoryDao,
        sleepTimerManager: SleepTimerManager,
        equalizerManager: FreqvoxEqualizerManager
    ): PlaybackController {
        return PlaybackController(context, trackDao, tagDao, playHistoryDao, sleepTimerManager, equalizerManager).apply {
            initializeController()
        }
    }
}
