package com.freqvox.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freqvox.ui.player.components.WaveformScrubber
import com.freqvox.ui.theme.dynamic.LocalGradientPrimary
import com.freqvox.ui.theme.dynamic.LocalGradientSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(
    trackTitle: String,
    artist: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    artworkUri: String?,
    isLiked: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onLikeClick: () -> Unit,
    onTagSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress       = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val currentPosStr  = formatTime(currentPosition)
    val durationStr    = formatTime(duration)
    val accentPrimary  = LocalGradientPrimary.current
    val accentSecondary = LocalGradientSecondary.current

    // Gesture state for swipe-to-skip / swipe-up-to-tag
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Like button pop animation
    var likeScale by remember { mutableStateOf(1f) }
    val animatedLikeScale by animateFloatAsState(
        targetValue = likeScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale",
        finishedListener = { likeScale = 1f }
    )

    // Artwork glow pulse when playing
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val currentGlowAlpha = if (isPlaying) glowAlpha else 0.20f

    // Full atmospheric — no scrim, let the gradient breathe
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Top Bar ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Back",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Artwork + Radial Glow ──────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp)
        ) {
            // Radial glow painted on Canvas behind the artwork
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension * 0.55f
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(
                            accentPrimary.copy(alpha = currentGlowAlpha),
                            accentSecondary.copy(alpha = currentGlowAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            // Album art card with swipe gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta -> offsetX += delta },
                        onDragStopped = {
                            if (offsetX < -150f) onNextClick()
                            else if (offsetX > 150f) onPreviousClick()
                            offsetX = 0f
                        }
                    )
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> offsetY += delta },
                        onDragStopped = {
                            if (offsetY < -120f) onTagSwipeUp()
                            offsetY = 0f
                        }
                    )
                    .shadow(
                        elevation   = 32.dp,
                        shape       = RoundedCornerShape(24.dp),
                        spotColor   = accentPrimary.copy(alpha = 0.6f),
                        ambientColor = accentSecondary.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1A26)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Subtle gradient overlay on artwork bottom so equalizer bars read well
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x88000000))
                            )
                        )
                )

                // Equalizer bars pinned at bottom inside artwork
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        EqualizerAnimation(accentColor = accentPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Track Info + Like ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trackTitle,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = {
                    likeScale = 1.4f
                    onLikeClick()
                }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFF4D7C) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(26.dp)
                        .scale(animatedLikeScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Waveform Scrubber ──────────────────────────────────────────────────
        WaveformScrubber(
            progress        = progress,
            durationStr     = durationStr,
            currentPosStr   = currentPosStr,
            onSeek          = onSeek,
            accentPrimary   = accentPrimary,
            accentSecondary = accentSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Playback Controls ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onPreviousClick, modifier = Modifier.size(52.dp)) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Gradient play/pause button with colored glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .shadow(
                        elevation    = 20.dp,
                        shape        = CircleShape,
                        spotColor    = accentPrimary.copy(alpha = 0.9f),
                        ambientColor = accentSecondary.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accentPrimary, accentSecondary)
                        )
                    )
            ) {
                IconButton(onClick = onPlayPauseClick, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            IconButton(onClick = onNextClick, modifier = Modifier.size(52.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Volume Row ─────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
            Slider(
                value = 0.7f,
                onValueChange = {},
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                colors = SliderDefaults.colors(
                    thumbColor        = Color.White,
                    activeTrackColor  = accentPrimary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Extra Controls Row ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExtraControlButton(icon = Icons.Default.Lyrics,      label = "Lyrics")
            ExtraControlButton(icon = Icons.AutoMirrored.Filled.QueueMusic, label = "Queue")
            ExtraControlButton(icon = Icons.Default.Equalizer,   label = "EQ")
            ExtraControlButton(icon = Icons.Default.Bedtime,     label = "Sleep")
            ExtraControlButton(icon = Icons.Default.Share,        label = "Share")
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ExtraControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = Color.White.copy(alpha = 0.65f),
                modifier           = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text     = label,
            color    = Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds  = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun EqualizerAnimation(accentColor: Color = Color(0xFF4DFFB4)) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        modifier = Modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(7) { index ->
            val height by transition.animateFloat(
                initialValue = 0.15f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(350 + (index * 80), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.9f), accentColor)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
