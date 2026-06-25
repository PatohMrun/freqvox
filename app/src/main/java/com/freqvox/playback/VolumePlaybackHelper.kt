package com.freqvox.playback

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Pauses playback when the media volume is turned all the way down to 0, and resumes
 * it when the volume is raised again — but ONLY if it was this that paused the music.
 *
 * The "only if volume paused it" rule is the careful part: a manual pause must never be
 * undone by someone nudging the volume, and starting playback yourself must clear the
 * flag so a later volume bump doesn't double-trigger. The state is a single
 * [pausedByVolume] latch:
 *  - volume hits 0 while playing  → pause, latch = true
 *  - volume rises while latched   → resume, latch = false
 *  - anything that makes us *play* (auto-resume OR a manual play) clears the latch
 *
 * Watches the system STREAM_MUSIC level (distinct from ExoPlayer's own [ExoPlayer.volume],
 * which the audio-focus ducking uses), so the two never interfere.
 */
class VolumePlaybackHelper(
    private val context: Context,
    private val player: ExoPlayer
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private var pausedByVolume = false
    private var registered = false

    // Settings.System changes fire for many keys; we just re-read the music volume and
    // act idempotently, so spurious callbacks are harmless.
    //
    // We gate on player.playWhenReady (the stable "user wants playback" intent), NOT
    // player.isPlaying. isPlaying flips to false during buffering/seeking, so checking it
    // here made the latch mis-fire after the first cycle ("worked once"): a volume change
    // landing during a buffering blip wouldn't latch, and the resume check could be skipped.
    private val volumeObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            val musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            when {
                musicVolume == 0 && player.playWhenReady -> {
                    pausedByVolume = true
                    player.pause()
                }
                musicVolume > 0 && pausedByVolume -> {
                    pausedByVolume = false
                    player.play()
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // Whenever playback is (re)started, it's no longer "paused by volume", so drop
            // the latch. Our own auto-resume already cleared it first; this also covers a
            // MANUAL play, so a later volume bump can't wrongly resume a manual pause.
            if (playWhenReady) pausedByVolume = false
        }
    }

    fun start() {
        if (registered) return
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )
        player.addListener(playerListener)
        registered = true
    }

    fun stop() {
        if (!registered) return
        context.contentResolver.unregisterContentObserver(volumeObserver)
        player.removeListener(playerListener)
        registered = false
    }
}
