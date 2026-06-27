package com.freqvox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6B3FA0),
    secondary = Color(0xFF7C4DFF),
    tertiary = Color(0xFFFF4D7C),
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF12121A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B3FA0),
    secondary = Color(0xFF7C4DFF),
    tertiary = Color(0xFFFF4D7C),
    background = Color(0xFFF4F4F7),
    surface = Color(0xFFFFFFFF)
)

/**
 * Static app theme (the launch shell). The dynamic, album-art-driven theme lives in
 * [com.freqvox.ui.theme.dynamic.DynamicTheme], which takes over once content composes; this
 * just makes the brief pre-content surface honor the chosen mode.
 */
@Composable
fun FreqvoxTheme(isLight: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFreqvoxColors provides if (isLight) FreqvoxLightColors else FreqvoxDarkColors
    ) {
        MaterialTheme(
            colorScheme = if (isLight) LightColorScheme else DarkColorScheme,
            content = content
        )
    }
}
