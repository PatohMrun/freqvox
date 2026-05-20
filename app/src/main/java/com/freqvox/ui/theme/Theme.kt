package com.freqvox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6B3FA0),
    secondary = Color(0xFF7C4DFF),
    tertiary = Color(0xFFFF4D7C),
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF12121A)
)

@Composable
fun FreqvoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
