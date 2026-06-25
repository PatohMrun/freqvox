package com.freqvox.playback

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Overlapping crossfade between consecutive tracks, on top of the normal gapless playlist.
 * ExoPlayer has no native crossfade (consecutive playlist items never overlap), so this uses
 * a SECOND player ([tailPlayer]) for the overlap.
 *
 * The trick that keeps the MediaSession / notification / queue correct: when the current track
 * is within the crossfade window of its end, the MAIN (session) player jumps straight to the
 * *next* track — so everything user-facing immediately reflects the new song — while the tail
 * player keeps playing the OUTGOING track's tail. The two are volume-ramped past each other.
 *
 * ── Why the two-phase PRE-ARM ──
 * Cold-starting the tail player (setMediaItem + prepare + seek to near the END of the file) is
 * expensive — and doing it AT the transition produced an audible hitch right before the fade.
 * So we split it: a couple seconds early (while the current track is still playing and masking
 * any work) we PRE-ARM — warm the tail decoder and seek it, paused, to the crossfade start
 * point. Then the actual trigger is just tailPlayer.play() + main.seekToNext + ramp, with no
 * decode/seek at the critical moment.
 *
 * Entirely inert when [CrossfadePreferences.durationSeconds] is 0 (the default).
 */
class CrossfadeController(
    private val mainPlayer: ExoPlayer,
    private val tailPlayer: ExoPlayer,
    private val prefs: CrossfadePreferences
) {
    private val handler = Handler(Looper.getMainLooper())

    private var crossfading = false
    private var preArmed = false
    // Set right before our own seekToNext so the transition listener can tell our jump
    // apart from a user pressing "next" mid-crossfade.
    private var selfTriggeredJump = false

    private val POLL_MS = 20L
    private val RAMP_STEP_MS = 50L
    // How early to warm the tail before the crossfade window opens.
    private val PREARM_LEAD_MS = 2000L

    private val poller = object : Runnable {
        override fun run() {
            val nextPollMs = maybeCrossfade()
            handler.postDelayed(this, nextPollMs)
        }
    }

    private val mainListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // Keep the outgoing tail in lockstep with the main player DURING a crossfade, so
            // anything that pauses playback mid-fade — a phone call grabbing audio focus, the
            // user hitting pause — silences the tail too instead of letting it keep singing.
            // It resumes with playback when focus/the user comes back. playWhenReady is the
            // stable intent signal (unlike isPlaying, which flickers on buffering), so this
            // can't misfire on the crossfade's own start. No-op outside a crossfade.
            if (!crossfading) return
            if (playWhenReady) tailPlayer.play() else tailPlayer.pause()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            handler.removeCallbacks(poller)
            if (isPlaying) handler.post(poller)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Consume OUR OWN jump's transition whenever it arrives (sync or async). Doing
            // this here — instead of resetting the flag right after seekToNextMediaItem() —
            // is robust to ExoPlayer dispatching the callback asynchronously, which would
            // otherwise look like a user skip and wrongly cancel the crossfade we just began.
            if (selfTriggeredJump) {
                selfTriggeredJump = false
                return
            }
            when {
                // User skipped/changed track mid-ramp — abort cleanly.
                crossfading -> cancelCrossfade()
                // Track changed otherwise — any pre-arm is for the old track now, drop it.
                else -> resetArming()
            }
        }
    }

    fun start() {
        mainPlayer.addListener(mainListener)
        if (mainPlayer.isPlaying) handler.post(poller)
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        mainPlayer.removeListener(mainListener)
    }

    private fun maybeCrossfade(): Long {
        val windowMs = prefs.durationSeconds * 1000L
        if (windowMs <= 0L) { resetArming(); return 1000L }      // OFF — inert
        if (crossfading) return 1000L
        if (!mainPlayer.isPlaying) return 1000L
        if (mainPlayer.repeatMode == Player.REPEAT_MODE_ONE) return 1000L
        if (!mainPlayer.hasNextMediaItem()) return 1000L         // last track — nothing to fade into

        val duration = mainPlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return 1000L
        // Don't crossfade tracks too short to give both a fade and some solo time.
        if (duration < windowMs * 2) return 1000L

        val remaining = duration - mainPlayer.currentPosition

        // Phase 1 — pre-arm the tail a bit before the window opens.
        if (!preArmed && remaining <= windowMs + PREARM_LEAD_MS) {
            preArm(tailStartPos = duration - windowMs)
        }
        // Phase 2 — trigger the actual crossfade.
        if (preArmed && remaining <= windowMs) {
            startCrossfade(windowMs)
            return 1000L // ramp takes over
        }
        
        // Dynamic polling: slow down the poll rate when far from the crossfade window.
        val timeUntilPrearm = remaining - (windowMs + PREARM_LEAD_MS)
        return when {
            timeUntilPrearm > 5000L -> 1000L // Far away: check once a second
            timeUntilPrearm > 0L -> 100L     // Getting closer: check 10 times a second
            else -> POLL_MS                  // Inside critical window: check 50 times a second
        }
    }

    /** Warm the tail decoder and seek it (paused) to where the crossfade will start. */
    private fun preArm(tailStartPos: Long) {
        val uri: Uri = mainPlayer.currentMediaItem?.localConfiguration?.uri ?: return
        tailPlayer.setMediaItem(MediaItem.fromUri(uri))
        tailPlayer.prepare()
        tailPlayer.seekTo(tailStartPos.coerceAtLeast(0L))
        tailPlayer.playWhenReady = false
        tailPlayer.volume = 1f
        preArmed = true
    }

    private fun startCrossfade(windowMs: Long) {
        crossfading = true
        preArmed = false   // consumed

        // The "Flicker" fix: 
        // When we call tailPlayer.play(), Android's AudioTrack takes ~50-150ms to 
        // initialize and start pushing sound. If we jump the mainPlayer immediately, 
        // the user hears a gap. 
        // Instead, we start the tailPlayer SILENTLY. Since it was pre-armed to the 
        // exact same position mainPlayer is currently at, they will play in perfect 
        // parallel sync.
        tailPlayer.volume = 0f
        tailPlayer.play()

        val staggerMs = 150L

        handler.postDelayed({
            // 150ms later, tailPlayer's AudioTrack is fully hot and perfectly aligned 
            // with where mainPlayer is. We snap it to full volume instantly.
            if (!crossfading) return@postDelayed
            tailPlayer.volume = 1f

            // Incoming track: jump the session player forward NOW
            selfTriggeredJump = true
            mainPlayer.seekToNextMediaItem()
            mainPlayer.volume = 0f
            mainPlayer.play()

            ramp(windowMs)
        }, staggerMs)
    }

    private fun ramp(windowMs: Long) {
        val steps = (windowMs / RAMP_STEP_MS).coerceAtLeast(1)
        var i = 0L
        val stepper = object : Runnable {
            override fun run() {
                if (!crossfading) return                // cancelled
                i++
                val t = (i.toFloat() / steps).coerceIn(0f, 1f)
                mainPlayer.volume = t
                tailPlayer.volume = 1f - t
                if (t >= 1f) finishCrossfade() else handler.postDelayed(this, RAMP_STEP_MS)
            }
        }
        handler.postDelayed(stepper, RAMP_STEP_MS)
    }

    private fun finishCrossfade() {
        mainPlayer.volume = 1f
        stopTail()
        crossfading = false
    }

    /** Abort an in-flight crossfade (e.g. user skipped): snap the live player back to full
     *  volume and silence the tail immediately. */
    private fun cancelCrossfade() {
        mainPlayer.volume = 1f
        stopTail()
        crossfading = false
        preArmed = false
    }

    /** Drop any pre-armed (but not yet started) tail. */
    private fun resetArming() {
        if (preArmed) {
            stopTail()
            preArmed = false
        }
    }

    private fun stopTail() {
        tailPlayer.stop()
        tailPlayer.clearMediaItems()
        tailPlayer.volume = 1f
    }
}
