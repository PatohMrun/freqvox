package com.freqvox.ui.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FreqvoxVisualizer(
    isPlaying: Boolean,
    trackTitle: String,
    accentPrimary: Color,
    accentSecondary: Color,
    modifier: Modifier = Modifier
) {
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Time driver for procedural noise
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withFrameNanos { frameTimeNanos ->
                time = frameTimeNanos / 100_000_000f // Scale time for nice speed
            }
        }
    }

    val sweepBrush = remember(accentPrimary, accentSecondary) {
        Brush.sweepGradient(
            colors = listOf(
                accentPrimary,
                accentSecondary,
                accentPrimary
            )
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension * 0.28f * (if (isPlaying) pulseScale else 1f)
        val maxBarHeight = size.minDimension * 0.18f
        
        val barCount = 48
        val angleStep = (2 * Math.PI) / barCount

        for (i in 0 until barCount) {
            val angle = i * angleStep
            
            // Generate a procedural height based on index and time
            val noise = if (isPlaying) {
                // A mix of sines creates a chaotic but smooth wave
                val n1 = sin(i * 0.8f + time * 0.8f)
                val n2 = cos(i * 1.5f - time * 0.5f)
                val n3 = sin(i * 0.3f + time * 1.2f)
                ((n1 + n2 + n3 + 3f) / 6f).coerceIn(0.1f, 1f)
            } else {
                0.1f // Flat when stopped
            }
            
            val barHeight = noise * maxBarHeight
            
            val innerRadius = baseRadius
            val outerRadius = baseRadius + barHeight
            
            val startX = center.x + innerRadius * cos(angle).toFloat()
            val startY = center.y + innerRadius * sin(angle).toFloat()
            
            val endX = center.x + outerRadius * cos(angle).toFloat()
            val endY = center.y + outerRadius * sin(angle).toFloat()

            drawLine(
                brush = sweepBrush,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )
        }
        
        // Inner solid circle
        drawCircle(
            color = Color(0xFF1A1A26),
            radius = baseRadius - 8f,
            center = center
        )
        // Inner glass ring
        drawCircle(
            brush = sweepBrush,
            radius = baseRadius - 8f,
            center = center,
            style = Stroke(width = 4f),
            alpha = 0.5f
        )
    }

    if (trackTitle.isNotEmpty()) {
        Text(
            text = trackTitle.take(1).uppercase(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold
        )
    }
    }
}
