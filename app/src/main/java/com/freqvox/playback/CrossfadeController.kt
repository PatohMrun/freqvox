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

    // The equal-power ramp gain (what the crossfade wants each player at) is kept separate
    // from the duck gain (audio-focus ducking). The actual player volume is always
    // rampX * duckFactor — see applyVolumes() — so a duck landing mid-crossfade lowers both
    // tracks instead of fighting the ramp, and un-ducking returns to the ramp's live gain
    // (1f at rest) rather than a stale captured value that used to strand playback quiet.
    private var rampMain = 1f
    private var rampTail = 0f
    @Volatile private var duckFactor = 1f

    private val POLL_MS = 20L
    private val RAMP_STEP_MS = 50L
    private val DUCK_LEVEL = 0.25f
    // Idle cadence while crossfade is OFF (the default). We can't fully stop the poller —
    // it's also what notices the user turning the feature ON — but at the off setting there
    // is nothing to do every second, so we coast at 5s. Worst case it takes 5s after enabling
    // for the poller to start tracking a transition, which is well before any track ends.
    private val OFF_POLL_MS = 5000L
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
        if (windowMs <= 0L) { resetArming(); return OFF_POLL_MS } // OFF — coast, near-zero wakeups
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

        // Incoming track starts silent, outgoing tail at full — through the duck gain so an
        // interruption already in progress keeps both quiet.
        rampMain = 0f
        rampTail = 1f

        // Start tailPlayer exactly at the handoff point to maintain perfect beat-sync.
        // While there is a microscopic hardware gap as AudioTrack wakes up, attempting
        // to stagger this causes overlapping echoes because ExoPlayer decoder warmup
        // times are unpredictable across devices.
        tailPlayer.volume = rampTail * duckFactor
        tailPlayer.play()

        // Incoming track: jump the session player forward NOW.
        // Mute BEFORE the seek: seekToNextMediaItem() on the live renderer forces
        // DefaultAudioSink to flush + reconfigure the AudioTrack, and that flush is the
        // source of the audible crossfade click. Setting volume to 0 first (rampMain is 0f
        // at fade start anyway) makes the flush silent instead of popping at full gain.
        selfTriggeredJump = true
        mainPlayer.volume = rampMain * duckFactor
        mainPlayer.seekToNextMediaItem()
        mainPlayer.play()

        ramp(windowMs)
    }

    private fun ramp(windowMs: Long) {
        val steps = (windowMs / RAMP_STEP_MS).coerceAtLeast(1)
        var i = 0L
        val stepper = object : Runnable {
            override fun run() {
                if (!crossfading) return                // cancelled
                i++
                val t = (i.toFloat() / steps).coerceIn(0f, 1f)
                
                // Equal-Power Crossfade: Human hearing perceives audio power logarithmically.
                // A linear crossfade (t and 1-t) causes acoustic power to drop by 50% in the middle 
                // of the fade, creating an audible "volume dip".
                // Using sine/cosine curves maintains a constant 100% power output throughout the entire transition.
                rampMain = kotlin.math.sin(t * (Math.PI / 2)).toFloat()
                rampTail = kotlin.math.cos(t * (Math.PI / 2)).toFloat()
                applyVolumes()

                if (t >= 1f) finishCrossfade() else handler.postDelayed(this, RAMP_STEP_MS)
            }
        }
        handler.postDelayed(stepper, RAMP_STEP_MS)
    }

    private fun finishCrossfade() {
        rampMain = 1f
        rampTail = 0f
        mainPlayer.volume = rampMain * duckFactor
        stopTail()
        crossfading = false
    }

    /** Abort an in-flight crossfade (e.g. user skipped): snap the live player back to its
     *  resting gain (full, or ducked if an interruption is active) and silence the tail. */
    private fun cancelCrossfade() {
        rampMain = 1f
        rampTail = 0f
        mainPlayer.volume = rampMain * duckFactor
        stopTail()
        crossfading = false
        preArmed = false
    }

    /** Player volume = equal-power ramp gain × audio-focus duck gain. Single writer for both
     *  players' volume during a crossfade so the two concerns compose instead of clobbering. */
    private fun applyVolumes() {
        mainPlayer.volume = rampMain * duckFactor
        tailPlayer.volume = rampTail * duckFactor
    }

    /**
     * Audio-focus ducking hook (wired from [PlaybackAudioFocusHelper]). Lowers/raises a gain
     * that multiplies into whatever the crossfade ramp is currently doing, so:
     *  - a duck mid-crossfade quiets BOTH the incoming and outgoing tracks (not just one);
     *  - un-ducking restores to the ramp's live gain (1f at rest), never a stale captured
     *    fraction — the old bug that could leave playback permanently quiet.
     */
    fun setDucked(ducked: Boolean) {
        val f = if (ducked) DUCK_LEVEL else 1f
        if (f == duckFactor) return
        duckFactor = f
        applyVolumes()
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
