package com.freqvox.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FreqvoxPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var equalizerManager: FreqvoxEqualizerManager

    override fun onCreate() {
        super.onCreate()
        
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            
        // Setup MediaSession with Activity deep link
        val intent = Intent(this, Class.forName("com.freqvox.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        // Initialize Equalizer with the player's audio session ID
        equalizerManager.init(player.audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        equalizerManager.release()
        super.onDestroy()
    }
}
