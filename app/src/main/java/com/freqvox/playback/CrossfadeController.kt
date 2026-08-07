package com.freqvox.playback

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment

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
    // rampX * duckFactor * prefs.sleepGain — see applyVolumes() — so a duck landing mid-crossfade lowers both
    // tracks instead of fighting the ramp, and un-ducking returns to the ramp's live gain
    // (1f at rest) rather than a stale captured value that used to strand playback quiet.
    private var rampMain = 1f
    private var rampTail = 0f
    @Volatile private var duckFactor = 1f

    // Volume-normalization gain (ReplayGain). A third factor multiplied into player volume
    // alongside the ramp and duck gains: volume = ramp × duck × norm. normMain tracks the
    // CURRENT (session) track; normTail is snapshotted from normMain at crossfade start so the
    // outgoing tail keeps the level it was playing at. Always 1f (no change) when the feature is
    // off or a track carries no ReplayGain tag — attenuate-only, so it can never clip.
    @Volatile private var normMain = 1f
    @Volatile private var normTail = 1f

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

        override fun onMetadata(metadata: Metadata) {
            // Read the current track's ReplayGain as it's decoded from the container and turn it
            // into a per-track gain. Only the MAIN (session) player's listener fires here, so this
            // always describes the current track. Applied immediately at rest; during a crossfade
            // applyVolumes() will pick it up as the incoming track ramps in.
            if (!prefs.volumeNormalization) return
            val db = parseReplayGainTrackGain(metadata) ?: return
            normMain = gainFactorFromDb(db)
            if (!crossfading) mainPlayer.volume = rampMain * duckFactor * prefs.sleepGain * normMain
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
                // Track changed otherwise — any pre-arm is for the old track now, drop it, and
                // reset normalization to unity until the new track's ReplayGain metadata arrives.
                else -> {
                    resetArming()
                    normMain = 1f
                    mainPlayer.volume = rampMain * duckFactor * prefs.sleepGain * normMain
                }
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
        // If normalization was just turned off, don't leave a track stuck attenuated. The poller
        // runs continuously (even with crossfade off, every OFF_POLL_MS), so this self-heals.
        if (!prefs.volumeNormalization && (normMain != 1f || normTail != 1f)) {
            normMain = 1f
            normTail = 1f
            if (!crossfading) mainPlayer.volume = rampMain * duckFactor * prefs.sleepGain
        }

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

        // Album mode — don't fade between two tracks of the same album; let them butt-join
        // gaplessly so an album that segues track-to-track plays as one continuous piece.
        // Checked only once we're near the window (cheap metadata reads, not every poll), and
        // we drop any pre-arm so the tail doesn't linger warmed for a transition we'll skip.
        if (prefs.albumMode && remaining <= windowMs + PREARM_LEAD_MS && nextIsSameAlbum()) {
            resetArming()
            return 1000L
        }

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

    /** True when the next queue item shares a (non-blank) album title with the current one.
     *  Uses the albumTitle metadata carried on each MediaItem — see PlaybackController. */
    private fun nextIsSameAlbum(): Boolean {
        val nextIndex = mainPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return false
        val current = mainPlayer.currentMediaItem?.mediaMetadata?.albumTitle?.toString()?.trim()
        if (current.isNullOrEmpty()) return false
        val next = mainPlayer.getMediaItemAt(nextIndex).mediaMetadata.albumTitle?.toString()?.trim()
        return current.equals(next, ignoreCase = true)
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

        // Carry the outgoing track's normalization onto the tail (it keeps playing that track),
        // and reset main to unity until the incoming track's ReplayGain metadata is read. The
        // incoming track is silent at ramp start, so an unknown gain for a moment is inaudible.
        normTail = normMain
        normMain = 1f

        // The fade timeline is the outgoing track's ACTUAL remaining audio, measured now.
        // If this trigger landed late (busy main thread), remaining is shorter than the
        // nominal window, and a ramp sized to the nominal window would spend its last
        // stretch fading against a tail that has already run out of file.
        val handoffPos = mainPlayer.currentPosition
        val duration = mainPlayer.duration
        val fadeMs =
            if (duration != C.TIME_UNSET) (duration - handoffPos).coerceIn(200L, windowMs)
            else windowMs

        // Correct the tail onto the true handoff point before starting it. Pre-arm seeked it
        // to the NOMINAL window start; if the trigger fired late the tail would replay audio
        // the listener already heard. The corrective seek lands inside already-buffered data
        // on a warm decoder, so it is cheap and keeps the handoff beat-synced.
        tailPlayer.seekTo(handoffPos)
        tailPlayer.volume = rampTail * duckFactor * prefs.sleepGain * normTail
        tailPlayer.play()

        // Incoming track: jump the session player forward NOW.
        // Mute BEFORE the seek: seekToNextMediaItem() on the live renderer forces
        // DefaultAudioSink to flush + reconfigure the AudioTrack, and that flush is the
        // source of the audible crossfade click. Setting volume to 0 first (rampMain is 0f
        // at fade start anyway) makes the flush silent instead of popping at full gain.
        selfTriggeredJump = true
        mainPlayer.volume = rampMain * duckFactor * prefs.sleepGain
        mainPlayer.seekToNextMediaItem()
        mainPlayer.play()

        ramp(fadeMs, tailFadeStart = handoffPos)
    }

    private fun ramp(fadeMs: Long, tailFadeStart: Long) {
        // Wall-clock safety budget, accumulated only while playback is intended (playWhenReady):
        // if a position stalls — incoming track stuck buffering, tail wedged — the fade still
        // ends instead of stranding crossfading=true forever. A user PAUSE doesn't accumulate,
        // so a paused fade waits indefinitely and resumes mid-fade.
        var activeMs = 0L
        val stepper = object : Runnable {
            override fun run() {
                if (!crossfading) return                // cancelled

                // Gains follow the players' MEDIA positions, not the wall clock. The incoming
                // track's position sits at 0 until its pipeline actually produces sound
                // (decoder + AudioTrack refill + Bluetooth latency, up to ~500ms), so it fades
                // in from true silence instead of popping in at whatever level a wall-clock
                // ramp had already reached — the old "volume dips, then jumps up" transition.
                // Positions also freeze on pause, so a mid-fade pause resumes mid-fade rather
                // than completing silently and snapping to full on resume.
                val tMain = (mainPlayer.currentPosition.toFloat() / fadeMs).coerceIn(0f, 1f)
                val tailDone = tailPlayer.playbackState == Player.STATE_ENDED ||
                    tailPlayer.playbackState == Player.STATE_IDLE
                val tTail =
                    if (tailDone) 1f
                    else ((tailPlayer.currentPosition - tailFadeStart).toFloat() / fadeMs)
                        .coerceIn(0f, 1f)

                // Equal-Power Crossfade: Human hearing perceives audio power logarithmically.
                // A linear crossfade (t and 1-t) causes acoustic power to drop by 50% in the middle
                // of the fade, creating an audible "volume dip".
                // Using sine/cosine curves maintains a constant 100% power output throughout the entire transition.
                rampMain = kotlin.math.sin(tMain * (Math.PI / 2)).toFloat()
                rampTail = kotlin.math.cos(tTail * (Math.PI / 2)).toFloat()
                applyVolumes()

                if (mainPlayer.playWhenReady) activeMs += RAMP_STEP_MS
                val timedOut = activeMs > fadeMs * 2 + 3000L

                if ((tMain >= 1f && tTail >= 1f) || timedOut) finishCrossfade()
                else handler.postDelayed(this, RAMP_STEP_MS)
            }
        }
        handler.post(stepper)
    }

    private fun finishCrossfade() {
        rampMain = 1f
        rampTail = 0f
        mainPlayer.volume = rampMain * duckFactor * prefs.sleepGain * normMain
        stopTail()
        crossfading = false
    }

    /** Abort an in-flight crossfade (e.g. user skipped): snap the live player back to its
     *  resting gain (full, or ducked if an interruption is active) and silence the tail. */
    private fun cancelCrossfade() {
        rampMain = 1f
        rampTail = 0f
        mainPlayer.volume = rampMain * duckFactor * prefs.sleepGain * normMain
        stopTail()
        crossfading = false
        preArmed = false
    }

    /** Player volume = equal-power ramp gain × audio-focus duck gain. Single writer for both
     *  players' volume during a crossfade so the two concerns compose instead of clobbering. */
    private fun applyVolumes() {
        mainPlayer.volume = rampMain * duckFactor * prefs.sleepGain * normMain
        tailPlayer.volume = rampTail * duckFactor * prefs.sleepGain * normTail
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

    /** Pull REPLAYGAIN_TRACK_GAIN (in dB) out of the track's parsed metadata, if present.
     *  Handles both Vorbis comments (FLAC/OGG/Opus) and ID3 TXXX frames (MP3). */
    private fun parseReplayGainTrackGain(metadata: Metadata): Float? {
        for (i in 0 until metadata.length()) {
            val db = when (val entry = metadata.get(i)) {
                is VorbisComment ->
                    if (entry.key.equals("REPLAYGAIN_TRACK_GAIN", ignoreCase = true)) parseDb(entry.value) else null
                is TextInformationFrame ->
                    if (entry.description?.equals("replaygain_track_gain", ignoreCase = true) == true)
                        parseDb(entry.values.firstOrNull()) else null
                else -> null
            }
            if (db != null) return db
        }
        return null
    }

    /** "-6.48 dB" → -6.48f. Tolerates missing unit / stray whitespace. */
    private fun parseDb(raw: String?): Float? =
        raw?.replace("dB", "", ignoreCase = true)?.trim()?.toFloatOrNull()

    /** dB → linear factor, clamped to [0, 1]: attenuate-only, so it can never boost past unity
     *  and therefore can never clip. Quiet tracks (positive gain) simply stay at unity. */
    private fun gainFactorFromDb(db: Float): Float =
        Math.pow(10.0, (db / 20.0)).toFloat().coerceIn(0f, 1f)
}
