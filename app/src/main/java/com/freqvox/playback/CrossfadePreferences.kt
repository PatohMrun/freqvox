package com.freqvox.playback

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crossfade duration, in seconds. 0 (the default) means OFF — tracks butt-join gaplessly,
 * exactly as before. Any positive value overlaps the end of one track with the start of the
 * next for that many seconds. Read live by [CrossfadeController] so a settings change takes
 * effect on the next transition without restarting playback.
 */
@Singleton
class CrossfadePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_playback", Context.MODE_PRIVATE)

    var durationSeconds: Int
        get() = prefs.getInt(KEY_CROSSFADE_SECONDS, 0)
        set(value) = prefs.edit().putInt(KEY_CROSSFADE_SECONDS, value.coerceIn(0, MAX_SECONDS)).apply()

    /**
     * Album mode: when on, consecutive tracks from the SAME album butt-join gaplessly instead
     * of crossfading — so an album mastered to flow track-to-track (segues, live sets, DJ mixes)
     * isn't smeared by the fade. Different-album transitions still crossfade normally. No-op when
     * [durationSeconds] is 0. Read live by [CrossfadeController]. Default on — it's the behaviour
     * most people expect once they notice a segue getting blurred.
     */
    var albumMode: Boolean
        get() = prefs.getBoolean(KEY_ALBUM_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_ALBUM_MODE, value).apply()

    /**
     * Volume normalization (ReplayGain). When on, [CrossfadeController] reads each track's
     * REPLAYGAIN_TRACK_GAIN tag (parsed by ExoPlayer from Vorbis comments / ID3 TXXX) and turns
     * loud tracks *down* toward the reference level so shuffle stops jumping in loudness. It is
     * attenuate-only (gain clamped to ≤1.0), so it can never boost past unity and therefore can
     * never clip; a track with no tag plays unchanged. Off by default — opt-in. Read live.
     */
    var volumeNormalization: Boolean
        get() = prefs.getBoolean(KEY_VOLUME_NORM, false)
        set(value) = prefs.edit().putBoolean(KEY_VOLUME_NORM, value).apply()

    companion object {
        const val MAX_SECONDS = 12
        /** Offered choices in the UI (0 = off). */
        val CHOICES = listOf(0, 3, 5, 8, 10, 12)
        private const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
        private const val KEY_ALBUM_MODE = "crossfade_album_mode"
        private const val KEY_VOLUME_NORM = "volume_normalization"
    }
}
