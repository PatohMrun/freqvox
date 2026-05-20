package com.freqvox.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    progress: Float, // 0.0f to 1.0f
    durationStr: String,
    currentPosStr: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // We'll generate a static-ish waveform for now. 
    // In a real app, you'd pre-calculate this from the audio file.
    val barCount = 60
    val heights = rememberWaveformHeights(barCount)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onSeek(offset.x / size.width)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / barCount
            val gap = 2.dp.toPx()

            heights.forEachIndexed { index, heightFactor ->
                val barProgress = index.toFloat() / barCount
                val isPlayed = barProgress < progress
                
                val barHeight = canvasHeight * heightFactor
                val x = index * barWidth
                
                val color = if (isPlayed) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF7C4DFF), Color(0xFFFF4D7C))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1A26), Color(0xFF1A1A26))
                    )
                }

                drawRect(
                    brush = color,
                    topLeft = Offset(x + gap / 2, (canvasHeight - barHeight) / 2),
                    size = Size(barWidth - gap, barHeight)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentPosStr,
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = MaterialTheme.typography.labelSmall.fontFamily // Ideally JetBrains Mono
            )
            Text(
                text = durationStr,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun rememberWaveformHeights(count: Int): List<Float> {
    // Seed with a fixed value so it doesn't jump around on recomposition
    // In a real implementation, this would be tied to the track ID.
    return androidx.compose.runtime.remember {
        List(count) { Random.nextFloat().coerceIn(0.2f, 1.0f) }
    }
}
