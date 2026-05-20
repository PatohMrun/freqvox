package com.freqvox.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freqvox.ui.player.components.WaveformScrubber
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
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val currentPosStr = formatTime(currentPosition)
    val durationStr = formatTime(duration)

    // Gesture state for artwork
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Handle back in MainScreen */ }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
            }
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                letterSpacing = 2.sp
            )
            IconButton(onClick = { /* Menu */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Album Artwork with Gestures
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> offsetX += delta },
                    onDragStopped = {
                        if (offsetX < -150f) onNextClick()
                        offsetX = 0f
                    }
                )
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> offsetY += delta },
                    onDragStopped = {
                        if (offsetY < -150f) onTagSwipeUp()
                        offsetY = 0f
                    }
                )
                .shadow(24.dp, RoundedCornerShape(20.dp), ambientColor = Color.Magenta, spotColor = Color.Magenta)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E28)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (isPlaying) {
                EqualizerAnimation()
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Track Info & Like Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trackTitle,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = artist,
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isLiked) Color(0xFFFF4D7C) else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Waveform Scrubber
        WaveformScrubber(
            progress = progress,
            durationStr = durationStr,
            currentPosStr = currentPosStr,
            onSeek = onSeek
        )

        Spacer(modifier = Modifier.weight(1f))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Shuffle */ }) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.Gray)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Surface(
                    onClick = onPlayPauseClick,
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    modifier = Modifier.size(72.dp).shadow(12.dp, RoundedCornerShape(50), spotColor = Color(0xFF7C4DFF))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                IconButton(onClick = onNextClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            
            IconButton(onClick = { /* Repeat */ }) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Volume Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Slider(
                value = 0.7f,
                onValueChange = {},
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF4DFFB4),
                    inactiveTrackColor = Color(0xFF1A1A26)
                )
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun EqualizerAnimation() {
    Row(
        modifier = Modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(7) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "eq")
            val height by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + (index * 100), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eqBar"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(height)
                    .background(Color(0xFF4DFFB4), RoundedCornerShape(2.dp))
            )
        }
    }
}
