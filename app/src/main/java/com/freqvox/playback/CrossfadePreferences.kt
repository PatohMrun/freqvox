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

    companion object {
        const val MAX_SECONDS = 12
        /** Offered choices in the UI (0 = off). */
        val CHOICES = listOf(0, 3, 5, 8, 10, 12)
        private const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
    }
}
