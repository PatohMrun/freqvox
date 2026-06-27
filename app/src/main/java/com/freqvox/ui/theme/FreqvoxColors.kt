package com.freqvox.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens the screens paint with, so the whole app flips between light and dark
 * from one place. The app historically used raw literals (Color.White, 0xFF12121A, …); these
 * tokens are the theme-aware replacements.
 *
 * Migration convention: a literal's ALPHA stays at the call site, only the base color comes
 * from a token. So `Color.White.copy(alpha = 0.5f)` → `colors.onSurface.copy(alpha = 0.5f)`,
 * which is white-on-dark in dark mode and (near-)black-on-light in light mode automatically.
 */
@Immutable
data class FreqvoxColors(
    val isLight: Boolean,
    /** Page background (the static fallback; DynamicTheme tints the live one from album art). */
    val background: Color,
    /** Panels, cards, sheets, the side drawer. */
    val surface: Color,
    /** Pills, chips, text inputs, subtle raised fills. */
    val surfaceVariant: Color,
    /** Primary text & icons. Replaces Color.White; keep `.copy(alpha=)` at call sites. */
    val onSurface: Color,
    /** Dark overlays used for legibility on top of album art. Replaces most Color.Black. */
    val scrim: Color,
    /** Brand accent fallback for the rare spot that isn't using the dynamic gradient. */
    val accent: Color
)

/** Dark palette — values equal the literals the app shipped with, so dark mode is unchanged. */
val FreqvoxDarkColors = FreqvoxColors(
    isLight = false,
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF12121A),
    surfaceVariant = Color(0xFF1A1A24),
    onSurface = Color(0xFFFFFFFF),
    scrim = Color(0xFF000000),
    accent = Color(0xFF7C4DFF)
)

/** Light palette — the mirror. Tuned further once we eyeball it on device. */
val FreqvoxLightColors = FreqvoxColors(
    isLight = true,
    background = Color(0xFFF4F4F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECECF1),
    onSurface = Color(0xFF0B0B12),
    // Scrims sit on colorful artwork, which is the same in both themes, so they stay dark.
    scrim = Color(0xFF000000),
    accent = Color(0xFF6B3FA0)
)

val LocalFreqvoxColors = staticCompositionLocalOf { FreqvoxDarkColors }

/**
 * Shorthand accessors for the active tokens, mirroring how `MaterialTheme.colorScheme` reads.
 * Lets a call site write `Fvox.onSurface` instead of pulling `LocalFreqvoxColors.current` into
 * a local. Only valid in @Composable scope — inside a Canvas DrawScope or a `remember{}` lambda
 * (non-composable), read `LocalFreqvoxColors.current` into a val first and use that.
 */
object Fvox {
    val background: Color @Composable @ReadOnlyComposable get() = LocalFreqvoxColors.current.background
    val surface: Color @Composable @ReadOnlyComposable get() = LocalFreqvoxColors.current.surface
    val surfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalFreqvoxColors.current.surfaceVariant
    val onSurface: Color @Composable @ReadOnlyComposable get() = LocalFreqvoxColors.current.onSurface
    val scrim: Color @Composable @ReadOnlyComposable get() = LocalFreqvoxColors.current.scrim
    val accent: Color @Composable @ReadOnlyComposable get() = LocalFreqvoxColors.current.accent
}
