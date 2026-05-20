package com.freqvox.di

import android.content.Context
import com.freqvox.data.local.dao.TrackDao
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
    fun providePlaybackController(
        @ApplicationContext context: Context,
        trackDao: TrackDao,
        sleepTimerManager: SleepTimerManager
    ): PlaybackController {
        return PlaybackController(context, trackDao, sleepTimerManager).apply {
            initializeController()
        }
    }
}
