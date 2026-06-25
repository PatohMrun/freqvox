package com.freqvox.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Manages audio focus with Freqvox-specific behavior:
 * - Duck (lower volume) on transient-can-duck instead of pausing
 * - Pause on phone-call style transient loss and permanent loss
 * - Auto-resume after transient loss only when we paused for that reason
 * - Headphone unplug ([AudioManager.ACTION_AUDIO_BECOMING_NOISY]): pause; user taps play to resume
 *
 * ExoPlayer's built-in focus handling is disabled; this helper owns focus requests.
 */
class PlaybackAudioFocusHelper(
    private val context: Context,
    private val player: ExoPlayer
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var hasFocus = false
    private var pausedForFocusLoss = false
    private var ducked = false
    private var volumeBeforeDuck = 1f

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> onFocusGain()
            AudioManager.AUDIOFOCUS_LOSS -> onFocusLoss(permanent = true)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onFocusLoss(permanent = false)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onFocusDuck()
        }
    }

    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .setAcceptsDelayedFocusGain(true)
            .build()
    } else {
        null
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            if (player.isPlaying) {
                player.pause()
                // Conservative: no auto-resume when headphones are plugged back in.
            }
        }
    }

    private var receiverRegistered = false

    fun start() {
        if (!receiverRegistered) {
            context.registerReceiver(
                becomingNoisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
            receiverRegistered = true
        }
    }

    fun stop() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(becomingNoisyReceiver)
            } catch (_: IllegalArgumentException) { /* already unregistered */ }
            receiverRegistered = false
        }
        abandonFocus()
    }

    /** Call when playback actually starts (isPlaying becomes true). */
    fun onPlaybackStarted() {
        requestFocus()
    }

    private fun requestFocus() {
        if (hasFocus) return
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        hasFocus = granted
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        hasFocus = false
        restoreVolumeAfterDuck()
        pausedForFocusLoss = false
    }

    private fun onFocusGain() {
        restoreVolumeAfterDuck()
        if (pausedForFocusLoss && player.playbackState != Player.STATE_ENDED) {
            player.play()
        }
        pausedForFocusLoss = false
    }

    private fun onFocusLoss(permanent: Boolean) {
        // Pause for a real focus loss (a call, another media app, etc.) — we should yield,
        // not keep playing quietly underneath. Short, duckable interruptions are handled
        // separately by onFocusDuck (AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK), which is the
        // "lower the volume instead of silence" case. On a transient loss we pause and
        // auto-resume on the matching focus gain; on a permanent loss we also abandon.
        restoreVolumeAfterDuck()
        if (player.isPlaying) {
            player.pause()
            pausedForFocusLoss = !permanent
        }
        if (permanent) {
            abandonFocus()
        }
    }

    private fun onFocusDuck() {
        if (!ducked) {
            volumeBeforeDuck = player.volume
            ducked = true
            player.volume = volumeBeforeDuck * 0.25f
        }
    }

    private fun restoreVolumeAfterDuck() {
        if (ducked) {
            player.volume = volumeBeforeDuck
            ducked = false
        }
    }
}
