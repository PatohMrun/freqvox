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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
    modifier: Modifier = Modifier
) {
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()
    val trackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val artist by playbackController.currentArtist.collectAsStateWithLifecycle()
    val artworkUri by playbackController.currentArtworkUri.collectAsStateWithLifecycle()
    val currentPosition by playbackController.currentPosition.collectAsStateWithLifecycle()
    val duration by playbackController.duration.collectAsStateWithLifecycle()

    // Swipe-to-skip state (on the content row, not the seek bar)
    var offsetX by remember { mutableStateOf(0f) }

    // Seek bar state
    var barWidth by remember { mutableIntStateOf(1) }
    var isDragging by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    // Holds the last seeked fraction until currentPosition catches up (prevents bounce)
    var pendingSeekFraction by remember { mutableFloatStateOf(-1f) }
    val barHeight by animateDpAsState(if (isDragging) 4.dp else 2.dp, label = "barH")

    // Clear pending seek once currentPosition reflects the seek target (within 3%)
    if (pendingSeekFraction >= 0f && duration > 0) {
        val positionFraction = currentPosition.toFloat() / duration
        if (kotlin.math.abs(positionFraction - pendingSeekFraction) < 0.03f) {
            pendingSeekFraction = -1f
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
            // ── Content row — swipe left/right to skip ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta -> offsetX += delta },
                        onDragStopped = {
                            if (offsetX > 150) playbackController.playPrevious()
                            else if (offsetX < -150) playbackController.playNext()
                            offsetX = 0f
                        }
                    )
                    .clickable(onClick = onMiniPlayerClick)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork thumbnail
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

                Row(verticalAlignment = Alignment.CenterVertically) {
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

            // ── Seek bar — 24dp touch target overlay at the top ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.TopCenter)
                    .onSizeChanged { barWidth = it.width }
                    .pointerInput(barWidth, duration) {
                        awaitEachGesture {
                            // Wait for finger down
                            val firstEvent = awaitPointerEvent()
                            val firstChange = firstEvent.changes.firstOrNull()
                                ?: return@awaitEachGesture
                            if (!firstChange.pressed) return@awaitEachGesture
                            firstChange.consume()

                            isDragging = true
                            seekFraction = (firstChange.position.x / barWidth).coerceIn(0f, 1f)

                            // Track drag
                            var event = firstEvent
                            while (event.changes.any { it.pressed }) {
                                event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    seekFraction = (change.position.x / barWidth).coerceIn(0f, 1f)
                                    change.consume()
                                }
                            }

                            // Seek on release — freeze display at seekFraction until position converges
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
                        .height(24.dp) // Use full touch target height so thumb isn't clipped
                ) {
                    val trackHeight = barHeight.toPx()
                    val y = trackHeight / 2f

                    // Resolve progress inside the draw scope so per-frame Animatable ticks
                    // invalidate only this draw, not the surrounding composition.
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

                    val thumbRadius = if (isDragging) 6.dp.toPx() else 4.dp.toPx()
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius,
                        center = androidx.compose.ui.geometry.Offset(progressWidth, y)
                    )
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
