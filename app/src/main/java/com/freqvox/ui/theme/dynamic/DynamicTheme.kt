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

val LocalGradientPrimary = compositionLocalOf { Color(0xFF0A0A0F) }
val LocalGradientSecondary = compositionLocalOf { Color(0xFF0A0A0F) }

@Composable
fun DynamicTheme(
    artworkPath: String?,
    trackTitle: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    
    val fallbackColors = listOf(
        Color(0xFF6B3FA0), // Deep Purple
        Color(0xFFFF4D7C), // Terracotta
        Color(0xFF4DFFB4), // Neon Green
        Color(0xFF00E5FF), // Cyan
        Color(0xFF2962FF)  // Deep Blue
    )

    val defaultFallback = remember(trackTitle) {
        if (trackTitle.isBlank() || trackTitle == "No track selected") {
            Color(0xFF0A0A0F)
        } else {
            val hash = abs(trackTitle.hashCode())
            fallbackColors[hash % fallbackColors.size]
        }
    }
    
    var seedColor by remember { mutableStateOf(defaultFallback) }
    var secondaryColor by remember { mutableStateOf(generateSecondaryColor(defaultFallback, trackTitle)) }
    
    LaunchedEffect(artworkPath, trackTitle) {
        if (artworkPath != null && File(artworkPath).exists()) {
            val color = withContext(Dispatchers.IO) {
                try {
                    val bitmap = BitmapFactory.decodeFile(artworkPath)
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val vibrant = palette.getVibrantColor(defaultFallback.toArgb())
                        val dominant = palette.getDominantColor(vibrant)
                        Color(dominant)
                    } else {
                        defaultFallback
                    }
                } catch (e: Exception) {
                    defaultFallback
                }
            }
            seedColor = color
            secondaryColor = generateSecondaryColor(color, trackTitle)
        } else {
            seedColor = defaultFallback
            secondaryColor = generateSecondaryColor(defaultFallback, trackTitle)
        }
    }

    val animatedBackground by animateColorAsState(
        targetValue = processBackgroundColor(seedColor),
        animationSpec = tween(1000),
        label = "themeBackground"
    )
    
    val animatedPrimary by animateColorAsState(
        targetValue = seedColor,
        animationSpec = tween(1000),
        label = "primaryColor"
    )
    
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(1000),
        label = "secondaryColor"
    )

    LaunchedEffect(animatedBackground) {
        if (!view.isInEditMode) {
            val window = (context as Activity).window
            window.statusBarColor = animatedBackground.toArgb()
            window.navigationBarColor = animatedBackground.toArgb()
            // Force light icons because background is always dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    
    val colorScheme = darkColorScheme(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        background = animatedBackground,
        surface = animatedBackground.copy(alpha = 0.8f)
    )

    CompositionLocalProvider(
        LocalGradientPrimary provides animatedPrimary,
        LocalGradientSecondary provides animatedSecondary
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

private fun processBackgroundColor(color: Color): Color {
    if (color == Color(0xFF0A0A0F)) return color
    
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsl)
    
    // Clamp saturation to be visible but not overwhelming
    hsl[1] = hsl[1].coerceIn(0.2f, 0.4f)
    // Darken significantly for the background
    hsl[2] = 0.08f 
    
    return Color(android.graphics.Color.HSVToColor(hsl))
}

private fun generateSecondaryColor(primary: Color, salt: String): Color {
    if (primary == Color(0xFF0A0A0F)) return primary
    
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(primary.toArgb(), hsl)
    
    val hash = abs(salt.hashCode())
    val hueShift = when (hash % 5) {
        0 -> 40f    // Analogous right
        1 -> -40f   // Analogous left
        2 -> 60f    // Triadic right
        3 -> -60f   // Triadic left
        else -> 180f // Complementary
    }
    
    hsl[0] = (hsl[0] + hueShift + 360f) % 360f
    
    // Ensure it's slightly darker or muted so it blends well as a secondary color
    hsl[1] = hsl[1].coerceIn(0.4f, 0.9f) // Saturation
    hsl[2] = hsl[2].coerceIn(0.3f, 0.6f) // Value
    
    return Color(android.graphics.Color.HSVToColor(hsl))
}
