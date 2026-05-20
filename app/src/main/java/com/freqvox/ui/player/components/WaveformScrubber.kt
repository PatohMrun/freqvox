package com.freqvox.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun WaveformScrubber(
    progress: Float,
    durationStr: String,
    currentPosStr: String,
    onSeek: (Float) -> Unit,
    accentPrimary: Color = Color(0xFF7C4DFF),
    accentSecondary: Color = Color(0xFFFF4D7C),
    modifier: Modifier = Modifier
) {
    val barCount = 72
    val heights = rememberWaveformHeights(barCount)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onSeek(offset.x / size.width)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
        ) {
            val canvasWidth  = size.width
            val canvasHeight = size.height
            val barWidth     = canvasWidth / barCount
            val gap          = 2.dp.toPx()

            heights.forEachIndexed { index, heightFactor ->
                val barProgress = index.toFloat() / barCount
                val isPlayed    = barProgress < progress
                val barHeight   = canvasHeight * heightFactor
                val x           = index * barWidth

                val brush = if (isPlayed) {
                    Brush.verticalGradient(
                        colors = listOf(accentPrimary, accentSecondary),
                        startY = (canvasHeight - barHeight) / 2,
                        endY   = (canvasHeight + barHeight) / 2
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))
                    )
                }

                drawRect(
                    brush    = brush,
                    topLeft  = Offset(x + gap / 2, (canvasHeight - barHeight) / 2),
                    size     = Size(barWidth - gap, barHeight)
                )
            }

            // Playhead line
            val playheadX = canvasWidth * progress
            drawLine(
                color       = Color.White.copy(alpha = 0.9f),
                start       = Offset(playheadX, 0f),
                end         = Offset(playheadX, canvasHeight),
                strokeWidth = 2.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(currentPosStr, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(durationStr,   color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
fun rememberWaveformHeights(count: Int): List<Float> {
    return remember {
        List(count) { Random.nextFloat().coerceIn(0.15f, 1.0f) }
    }
}
