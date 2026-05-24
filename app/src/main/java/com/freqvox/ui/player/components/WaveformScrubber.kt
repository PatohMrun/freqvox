package com.freqvox.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun WaveformScrubber(
    progress: () -> Float,
    durationStr: String,
    currentPosStr: String,
    onSeek: (Float) -> Unit,
    accentPrimary: Color = Color(0xFF7C4DFF),
    accentSecondary: Color = Color(0xFFFF4D7C),
    modifier: Modifier = Modifier
) {
    val barCount = 72
    val heights  = rememberWaveformHeights(barCount)

    // Local scrub state: while the user is touching the bar, we render this
    // value instead of `progress()`, so the bar tracks the finger 1:1 without
    // waiting for Media3 to confirm the seek and the polling loop to round-trip.
    var isScrubbing  by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    val displayedProgress = if (isScrubbing) scrubProgress else progress().coerceIn(0f, 1f)

    // Keep the latest onSeek without restarting the pointerInput modifier.
    val onSeekState = rememberUpdatedState(onSeek)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for first touch down on the bar.
                            val down = awaitPointerEvent(PointerEventPass.Main)
                                .changes.firstOrNull { it.pressed } ?: continue

                            val width = size.width.toFloat()
                            if (width <= 0f) continue

                            isScrubbing  = true
                            scrubProgress = (down.position.x / width).coerceIn(0f, 1f)
                            // Live audio update on initial touch so a single tap still seeks.
                            onSeekState.value(scrubProgress)
                            down.consume()

                            // Throttle live audio updates to avoid spamming Media3.
                            // ~50ms ≈ 20 fps which is plenty for "follow-along" audio scrubbing.
                            var lastLiveSeekNs = System.nanoTime()

                            // Follow until all pointers lift.
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Main)
                                val change = ev.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    // Final commit on release.
                                    onSeekState.value(scrubProgress)
                                    isScrubbing = false
                                    change.consume()
                                    break
                                }
                                scrubProgress = (change.position.x / width).coerceIn(0f, 1f)
                                change.consume()

                                val now = System.nanoTime()
                                if (now - lastLiveSeekNs > 50_000_000L) {
                                    onSeekState.value(scrubProgress)
                                    lastLiveSeekNs = now
                                }
                            }
                        }
                    }
                }
        ) {
            val canvasWidth  = size.width
            val canvasHeight = size.height
            val barWidth     = canvasWidth / barCount
            val gap          = 2.dp.toPx()
            val p            = displayedProgress

            heights.forEachIndexed { index, heightFactor ->
                val barProgress = index.toFloat() / barCount
                val isPlayed    = barProgress < p
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
                    brush   = brush,
                    topLeft = Offset(x + gap / 2, (canvasHeight - barHeight) / 2),
                    size    = Size(barWidth - gap, barHeight)
                )
            }

            val playheadX = canvasWidth * p
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
