package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Fixed accent" appearance option. By default Freqvox derives the background-gradient palette
 * from each track's album art (the dynamic signature). When [enabled], the whole gradient is
 * pinned to a single user-chosen [color] instead, for people who prefer a consistent look.
 * The static UI accent token (Fvox.accent) is unaffected either way.
 */
@Singleton
class AccentPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_accent", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Chosen accent as an ARGB int. Kept even while [enabled] is false so the picker remembers it. */
    var color: Int
        get() = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
        set(value) = prefs.edit().putInt(KEY_COLOR, value).apply()

    companion object {
        const val DEFAULT_COLOR = 0xFF7C4DFF.toInt()
        /** Swatches offered in the picker (ARGB). */
        val SWATCHES = listOf(
            0xFF7C4DFF.toInt(), // violet
            0xFF2196F3.toInt(), // blue
            0xFF00BCD4.toInt(), // cyan
            0xFF2ECC71.toInt(), // green
            0xFFF1C40F.toInt(), // amber
            0xFFFF7043.toInt(), // orange
            0xFFE91E63.toInt(), // pink
            0xFFEF5350.toInt()  // red
        )
        private const val KEY_ENABLED = "fixed_accent_enabled"
        private const val KEY_COLOR = "fixed_accent_color"
    }
}
