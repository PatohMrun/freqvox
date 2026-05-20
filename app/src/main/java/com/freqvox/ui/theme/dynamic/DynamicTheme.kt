package com.freqvox.ui.theme.dynamic

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

val LocalGradientPrimary   = compositionLocalOf { Color(0xFF0A0A0F) }
val LocalGradientSecondary = compositionLocalOf { Color(0xFF0A0A0F) }
val LocalGradientTertiary  = compositionLocalOf { Color(0xFF0A0A0F) }

private val DEEP_DARK = Color(0xFF06060C)

// ── Color Generation ──────────────────────────────────────────────────────────

/**
 * Hashes a string into multiple independent 32-bit integers using
 * different prime multipliers, giving us independent "axes" of randomness
 * without a simple modulo pattern.
 */
private fun multiHash(s: String): Triple<Int, Int, Int> {
    var h1 = 0x811c9dc5.toInt()
    var h2 = 0x01000193.toInt()
    var h3 = 0x04c11db7.toInt()
    for (c in s) {
        h1 = (h1 xor c.code) * 0x01000193
        h2 = (h2 xor c.code) * 0x27d4eb2f
        h3 = (h3 xor c.code) * 0x16777619.toInt()
    }
    return Triple(abs(h1), abs(h2), abs(h3))
}

/**
 * Generates a primary color from the full 360° HSV wheel.
 * Keeps saturation 0.65–1.0 and value 0.5–0.85 so it's always vivid.
 */
private fun generatePrimaryFromTitle(title: String): Color {
    val (h1, h2, h3) = multiHash(title)
    val hue        = (h1 % 360).toFloat()
    val saturation = 0.65f + (h2 % 36) / 100f   // 0.65 – 1.00
    val value      = 0.50f + (h3 % 36) / 100f   // 0.50 – 0.85
    val hsv = floatArrayOf(hue, saturation, value)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Derives a harmonically valid secondary color.
 * Picks among 8 classic relationships, selecting based on h2 so it's
 * independent of the primary hue selection.
 */
private fun generateSecondaryColor(primary: Color, title: String): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)

    val (_, h2, h3) = multiHash(title)

    // 8 harmony types — all musically valid, never clash
    val hueShift = when (h2 % 8) {
        0 -> 30f          // Near-analogous
        1 -> -30f
        2 -> 60f          // Wide analogous
        3 -> -60f
        4 -> 120f         // Triadic
        5 -> -120f
        6 -> 150f         // Split-complementary
        else -> 180f      // Full complementary
    }

    // Slightly vary saturation & value so the two colors have visible contrast
    val satOffset = ((h3 % 20) - 10) / 100f   // ±0.10
    val valOffset = ((h3 % 16) - 8)  / 100f   // ±0.08

    hsv[0] = (hsv[0] + hueShift + 360f) % 360f
    hsv[1] = (hsv[1] + satOffset).coerceIn(0.55f, 1.0f)
    hsv[2] = (hsv[2] + valOffset).coerceIn(0.45f, 0.85f)

    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Tertiary — a third accent chosen from a different harmony angle,
 * so we have 3 genuinely distinct colors for the 3-stop gradient.
 */
private fun generateTertiaryColor(primary: Color, title: String): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsv)

    val (h1, _, h3) = multiHash(title + "T") // small salt shift

    val hueShift = when (h1 % 6) {
        0 -> 90f
        1 -> -90f
        2 -> 210f
        3 -> -210f
        4 -> 270f
        else -> -270f
    }
    val satOffset = ((h3 % 20) - 10) / 100f
    val valOffset = ((h3 % 12) - 6) / 100f

    hsv[0] = (hsv[0] + hueShift + 360f) % 360f
    hsv[1] = (hsv[1] + satOffset).coerceIn(0.5f, 1.0f)
    hsv[2] = (hsv[2] + valOffset).coerceIn(0.4f, 0.80f)

    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Crushes a vibrant color down to a near-black while preserving its hue. */
private fun processBackgroundColor(color: Color): Color {
    if (color == DEEP_DARK) return DEEP_DARK
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = hsv[1].coerceIn(0.25f, 0.50f)
    hsv[2] = 0.07f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// ── Composable ────────────────────────────────────────────────────────────────

@Composable
fun DynamicTheme(
    artworkPath: String?,
    trackTitle: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view    = LocalView.current

    // Safe default when nothing is playing
    val idle = DEEP_DARK

    var seedColor      by remember { mutableStateOf(idle) }
    var secondaryColor by remember { mutableStateOf(idle) }
    var tertiaryColor  by remember { mutableStateOf(idle) }

    LaunchedEffect(artworkPath, trackTitle) {
        if (trackTitle.isBlank() || trackTitle == "No track selected") {
            seedColor      = idle
            secondaryColor = idle
            tertiaryColor  = idle
            return@LaunchedEffect
        }

        // Try album art first; fall back to title-derived colour
        val primary = if (artworkPath != null && File(artworkPath).exists()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bmp = BitmapFactory.decodeFile(artworkPath)
                    if (bmp != null) {
                        val palette = Palette.from(bmp).generate()
                        val vibrant = palette.getVibrantColor(0)
                        if (vibrant != 0) Color(vibrant)
                        else Color(palette.getDominantColor(0))
                    } else null
                }.getOrNull()
            } ?: generatePrimaryFromTitle(trackTitle)
        } else {
            generatePrimaryFromTitle(trackTitle)
        }

        seedColor      = primary
        secondaryColor = generateSecondaryColor(primary, trackTitle)
        tertiaryColor  = generateTertiaryColor(primary, trackTitle)
    }

    // Animate all three independently with slightly staggered speeds
    val animBackground by animateColorAsState(processBackgroundColor(seedColor),      tween(900),  label = "bg")
    val animPrimary    by animateColorAsState(seedColor,                              tween(800),  label = "p1")
    val animSecondary  by animateColorAsState(secondaryColor,                         tween(1000), label = "p2")
    val animTertiary   by animateColorAsState(tertiaryColor,                          tween(1100), label = "p3")

    // Paint status & nav bar
    LaunchedEffect(animBackground) {
        if (!view.isInEditMode) {
            val window = (context as Activity).window
            window.statusBarColor    = animBackground.toArgb()
            window.navigationBarColor = animBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars    = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    val colorScheme = darkColorScheme(
        primary    = animPrimary,
        secondary  = animSecondary,
        tertiary   = animTertiary,
        background = animBackground,
        surface    = animBackground.copy(alpha = 0.85f)
    )

    CompositionLocalProvider(
        LocalGradientPrimary   provides animPrimary,
        LocalGradientSecondary provides animSecondary,
        LocalGradientTertiary  provides animTertiary
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
