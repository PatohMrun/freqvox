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
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
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

    // After release we keep displaying `scrubProgress` until the upstream
    // `progress()` actually catches up to it. Otherwise the bar would briefly
    // render the stale interpolated position (one or two frames) before
    // Media3 reports the seek result — that's the post-release bounce.
    var pendingProgress by remember { mutableFloatStateOf(-1f) }

    val upstream = progress().coerceIn(0f, 1f)
    val displayedProgress = when {
        isScrubbing             -> scrubProgress
        pendingProgress >= 0f   -> pendingProgress
        else                    -> upstream
    }

    // Clear the pending freeze once upstream converges (or after a safety timeout
    // so we never get stuck if a seek silently fails).
    LaunchedEffect(upstream, pendingProgress) {
        if (pendingProgress < 0f) return@LaunchedEffect
        if (abs(upstream - pendingProgress) < 0.02f) {
            delay(60L) // let interpolator's snapTo settle before swapping
            pendingProgress = -1f
        } else {
            delay(1200L) // safety timeout
            pendingProgress = -1f
        }
    }

    // Keep the latest onSeek without restarting the pointerInput modifier.
    val onSeekState = rememberUpdatedState(onSeek)

    // Captured for the (non-composable) draw lambda below.
    val waveColor = com.freqvox.ui.theme.LocalFreqvoxColors.current.onSurface
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
                                    // Final commit on release. Freeze the bar at
                                    // the released position until upstream catches up.
                                    onSeekState.value(scrubProgress)
                                    pendingProgress = scrubProgress
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

            val activeBrush = Brush.verticalGradient(
                colors = listOf(accentPrimary, accentSecondary),
                startY = 0f,
                endY   = canvasHeight
            )
            val inactiveBrush = Brush.verticalGradient(
                colors = listOf(waveColor.copy(alpha = 0.12f), waveColor.copy(alpha = 0.06f)),
                startY = 0f,
                endY   = canvasHeight
            )

            heights.forEachIndexed { index, heightFactor ->
                val barProgress = index.toFloat() / barCount
                val isPlayed    = barProgress < p
                val barHeight   = canvasHeight * heightFactor
                val x           = index * barWidth

                val brush = if (isPlayed) activeBrush else inactiveBrush

                drawRect(
                    brush   = brush,
                    topLeft = Offset(x + gap / 2, (canvasHeight - barHeight) / 2),
                    size    = Size(barWidth - gap, barHeight)
                )
            }

            val playheadX = canvasWidth * p
            drawLine(
                color       = waveColor.copy(alpha = 0.9f),
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
            Text(currentPosStr, color = Fvox.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(durationStr,   color = Fvox.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
fun rememberWaveformHeights(count: Int): List<Float> {
    return remember {
        List(count) { Random.nextFloat().coerceIn(0.15f, 1.0f) }
    }
}
