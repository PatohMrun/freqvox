package com.freqvox.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Custom accent" appearance option, two independently toggleable effects of one chosen [color]:
 *  - [enabled] — the UI accent token (Fvox.accent: switches, chips, rings, progress) follows
 *    the swatch, legibility-clamped per mode in DynamicTheme. accentAlt keeps its rose semantics.
 *  - [pinGradient] — ALSO pins the background gradient to the swatch. Off (the default) keeps
 *    the gradient dynamic (album-derived per track) while the controls stay custom-coloured.
 */
@Singleton
class AccentPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("freqvox_accent", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Only meaningful while [enabled]; kept separately so the choice is remembered across toggles. */
    var pinGradient: Boolean
        get() = prefs.getBoolean(KEY_PIN_GRADIENT, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_GRADIENT, value).apply()

    /** Chosen accent as an ARGB int. Kept even while [enabled] is false so the picker remembers it. */
    var color: Int
        get() = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
        set(value) = prefs.edit().putInt(KEY_COLOR, value).apply()

    companion object {
        const val DEFAULT_COLOR = 0xFF7C4DFF.toInt()
        /** Swatches offered in the picker (ARGB) — rendered 8 per row. */
        val SWATCHES = listOf(
            0xFF7C4DFF.toInt(), // violet
            0xFF2196F3.toInt(), // blue
            0xFF00BCD4.toInt(), // cyan
            0xFF2ECC71.toInt(), // green
            0xFFF1C40F.toInt(), // amber
            0xFFFF7043.toInt(), // orange
            0xFFE91E63.toInt(), // pink
            0xFFEF5350.toInt(), // red
            0xFF9575CD.toInt(), // lavender
            0xFF3D5AFE.toInt(), // indigo
            0xFF26A69A.toInt(), // teal
            0xFF9CCC65.toInt(), // lime
            0xFFFFA726.toInt(), // tangerine
            0xFFD81B60.toInt(), // magenta
            0xFF8D6E63.toInt(), // mocha
            0xFF78909C.toInt()  // steel
        )
        private const val KEY_ENABLED = "fixed_accent_enabled"
        private const val KEY_PIN_GRADIENT = "fixed_accent_pin_gradient"
        private const val KEY_COLOR = "fixed_accent_color"
    }
}
