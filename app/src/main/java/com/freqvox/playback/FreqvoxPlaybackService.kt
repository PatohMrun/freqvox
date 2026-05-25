package com.freqvox.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.freqvox.R
import com.freqvox.data.repository.TrackRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class FreqvoxPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var audioFocusHelper: PlaybackAudioFocusHelper? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Inject
    lateinit var equalizerManager: FreqvoxEqualizerManager

    @Inject
    lateinit var trackRepository: TrackRepository

    override fun onCreate() {
        super.onCreate()

        val okHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Freqvox/1.0 (Android)")

        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(defaultDataSourceFactory))
            // We manage focus ourselves (duck vs pause, resume rules) — see PlaybackAudioFocusHelper.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)
            // Becoming-noisy is handled in PlaybackAudioFocusHelper (conservative: no auto-resume).
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        audioFocusHelper = PlaybackAudioFocusHelper(this, player).also { it.start() }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) audioFocusHelper?.onPlaybackStarted()
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                refreshLikeNotificationButton()
            }
        })

        val intent = Intent(this, Class.forName("com.freqvox.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == MediaSessionCommands.TOGGLE_LIKE) {
                    val trackId = session.player.currentMediaItem?.mediaId?.toLongOrNull()
                    if (trackId != null) {
                        serviceScope.launch {
                            trackRepository.toggleLikeForTrack(trackId)
                            refreshLikeNotificationButton()
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .build()

        mediaSession?.setCustomLayout(buildLikeCommandButtons(isLiked = false))

        equalizerManager.init(player.audioSessionId)
        refreshLikeNotificationButton()
    }

    private fun buildLikeCommandButtons(isLiked: Boolean): ImmutableList<CommandButton> {
        val button = CommandButton.Builder()
            .setDisplayName(if (isLiked) "Unlike" else "Like")
            .setIconResId(
                if (isLiked) R.drawable.ic_notif_favorite else R.drawable.ic_notif_favorite_border
            )
            .setSessionCommand(
                SessionCommand(MediaSessionCommands.TOGGLE_LIKE, Bundle.EMPTY)
            )
            .build()
        return ImmutableList.of(button)
    }

    private fun refreshLikeNotificationButton() {
        val session = mediaSession ?: return
        val trackId = session.player.currentMediaItem?.mediaId?.toLongOrNull()
        if (trackId == null) {
            session.setCustomLayout(buildLikeCommandButtons(isLiked = false))
            return
        }
        serviceScope.launch {
            val liked = trackRepository.isTrackLiked(trackId)
            session.setCustomLayout(buildLikeCommandButtons(isLiked = liked))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        audioFocusHelper?.stop()
        audioFocusHelper = null
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        equalizerManager.release()
        super.onDestroy()
    }
}
