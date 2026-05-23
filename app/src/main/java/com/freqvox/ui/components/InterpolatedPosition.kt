package com.freqvox.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
fun rememberInterpolatedPositionMs(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    tickMs: Long = 1000L
): State<Long> {
    val animated = remember { Animatable(positionMs.toFloat()) }

    LaunchedEffect(positionMs, isPlaying, durationMs) {
        animated.snapTo(positionMs.toFloat())
        if (isPlaying && durationMs > 0 && positionMs < durationMs) {
            val window = tickMs + 200L
            val target = (positionMs + window).coerceAtMost(durationMs).toFloat()
            animated.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = window.toInt(), easing = LinearEasing)
            )
        }
    }

    return remember(animated) { derivedStateOf { animated.value.toLong() } }
}
