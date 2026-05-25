package com.freqvox.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freqvox.playback.PlaybackController
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    playbackController: PlaybackController,
    onMiniPlayerClick: () -> Unit,
    onSwipeUpToExpand: () -> Unit = onMiniPlayerClick,
    modifier: Modifier = Modifier
) {
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()
    val trackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val artist by playbackController.currentArtist.collectAsStateWithLifecycle()
    val artworkUri by playbackController.currentArtworkUri.collectAsStateWithLifecycle()
    val currentPosition by playbackController.currentPosition.collectAsStateWithLifecycle()
    val duration by playbackController.duration.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val expandThresholdPx = with(density) { 56.dp.toPx() }

    // Body-region gesture state. The buttons row sits OUTSIDE this gesture handler
    // so taps reach the IconButtons directly instead of being intercepted here.
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Seek bar state
    var barWidth by remember { mutableIntStateOf(1) }
    var isDragging by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    // Holds the last seeked fraction until currentPosition catches up (prevents bounce)
    var pendingSeekFraction by remember { mutableFloatStateOf(-1f) }
    val barHeight by animateDpAsState(if (isDragging) 4.dp else 2.dp, label = "barH")

    // Clear pending seek once currentPosition reflects the seek target (within 3%).
    LaunchedEffect(currentPosition, duration, pendingSeekFraction) {
        if (pendingSeekFraction >= 0f && duration > 0) {
            val positionFraction = currentPosition.toFloat() / duration
            if (kotlin.math.abs(positionFraction - pendingSeekFraction) < 0.03f) {
                kotlinx.coroutines.delay(60L)
                pendingSeekFraction = -1f
            }
        }
    }

    val interpolatedPos = rememberInterpolatedPositionMs(isPlaying, currentPosition, duration)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(sharedBoundsModifier("player.container"))
            .shadow(8.dp, clip = false)
            .background(Color(0xFF1A1A26))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // ── Content row ────────────────────────────────────────────────────
            // Body (artwork + title) holds the swipe-to-expand gesture.
            // Buttons are siblings, so their tap targets are independent.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(top = 6.dp), // leave the top strip for the seek bar
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val firstEvent = awaitPointerEvent()
                                val firstChange = firstEvent.changes.firstOrNull()
                                    ?: return@awaitEachGesture
                                if (!firstChange.pressed) return@awaitEachGesture

                                var totalX = 0f
                                var totalY = 0f
                                var gesture: String? = null // "h", "v", or null until decided
                                var event = firstEvent
                                while (event.changes.any { it.pressed }) {
                                    event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                    // positionChange() returns Offset.Zero when another
                                    // pointer-input consumed the change (e.g. the seek
                                    // bar). That keeps us from fighting it.
                                    val delta = change.positionChange()
                                    totalX += delta.x
                                    totalY += delta.y
                                    if (gesture == null && (kotlin.math.abs(totalX) > 12f || kotlin.math.abs(totalY) > 12f)) {
                                        gesture = if (kotlin.math.abs(totalY) > kotlin.math.abs(totalX)) "v" else "h"
                                    }
                                    when (gesture) {
                                        "h" -> {
                                            offsetX += delta.x
                                            change.consume()
                                        }
                                        "v" -> {
                                            offsetY = (offsetY + delta.y).coerceIn(-expandThresholdPx * 1.4f, 0f)
                                            change.consume()
                                        }
                                    }
                                }

                                when {
                                    gesture == "v" && offsetY < -expandThresholdPx -> onSwipeUpToExpand()
                                    gesture == "h" && offsetX > 150f -> playbackController.playPrevious()
                                    gesture == "h" && offsetX < -150f -> playbackController.playNext()
                                    gesture == null && kotlin.math.abs(totalX) < 12f && kotlin.math.abs(totalY) < 12f ->
                                        onMiniPlayerClick()
                                }
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = Color(0xFF12121A)
                    ) {
                        if (artworkUri != null) {
                            AsyncImage(
                                model = artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    trackTitle.take(1).uppercase(),
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier
                                .then(sharedElementModifier("player.title"))
                                .basicMarquee()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = artist.ifBlank { "Unknown Artist" },
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(sharedElementModifier("player.artist"))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Controls — siblings of the gesture region. Taps land on IconButtons.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    IconButton(onClick = { playbackController.playPrevious() }) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { playbackController.togglePlayPause() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (scaleIn(animationSpec = tween(280, easing = FastOutSlowInEasing), initialScale = 0.6f) +
                                        fadeIn(tween(280))) togetherWith
                                        (scaleOut(animationSpec = tween(220, easing = FastOutSlowInEasing), targetScale = 0.6f) +
                                                fadeOut(tween(220)))
                            },
                            label = "miniPlayPause"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    IconButton(onClick = { playbackController.playNext() }) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Seek bar — slim strip at the top, drag-only ────────────────────
            // Touch slop must be crossed horizontally before scrubbing activates.
            // A plain tap (or vertical swipe) passes through to the row's gesture
            // handler, so the bar can never be moved by accident.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .align(Alignment.TopCenter)
                    .onSizeChanged { barWidth = it.width }
                    .pointerInput(barWidth, duration) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val firstDrag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                            } ?: return@awaitEachGesture

                            isDragging = true
                            seekFraction = (firstDrag.position.x / barWidth).coerceIn(0f, 1f)
                            firstDrag.consume()

                            horizontalDrag(firstDrag.id) { change ->
                                seekFraction = (change.position.x / barWidth).coerceIn(0f, 1f)
                                change.consume()
                            }

                            if (duration > 0) {
                                pendingSeekFraction = seekFraction
                                playbackController.seekTo((seekFraction * duration).toLong())
                            }
                            isDragging = false
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                ) {
                    val trackHeight = barHeight.toPx()
                    val y = trackHeight / 2f

                    val displayProgress = when {
                        isDragging -> seekFraction
                        pendingSeekFraction >= 0f -> pendingSeekFraction
                        duration > 0 -> interpolatedPos.value.toFloat() / duration
                        else -> 0f
                    }

                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = trackHeight,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    val progressWidth = size.width * displayProgress
                    if (progressWidth > 0) {
                        drawLine(
                            color = Color(0xFF7C4DFF),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(progressWidth, y),
                            strokeWidth = trackHeight,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }

                    // Hide the thumb when idle — it's a status bar, not a slider,
                    // until the user actively scrubs.
                    if (isDragging) {
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(progressWidth, y)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
