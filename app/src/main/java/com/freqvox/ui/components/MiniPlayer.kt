package com.freqvox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    val isPlaying by playbackController.isPlaying.collectAsState()
    val trackTitle by playbackController.currentTrackTitle.collectAsState()
    val artist by playbackController.currentArtist.collectAsState()
    val artworkUri by playbackController.currentArtworkUri.collectAsState()
    val currentPosition by playbackController.currentPosition.collectAsState()
    val duration by playbackController.duration.collectAsState()

    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    var offsetX by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX += delta
                },
                onDragStopped = {
                    if (offsetX > 150) {
                        playbackController.playPrevious()
                    } else if (offsetX < -150) {
                        playbackController.playNext()
                    }
                    offsetX = 0f
                }
            )
            .clickable(onClick = onMiniPlayerClick),
        color = Color(0xFF1A1A26),
        tonalElevation = 8.dp
    ) {
        Column {
            // Real-time progress bar at the bottom
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color(0xFF7C4DFF),
                trackColor = Color.White.copy(alpha = 0.05f),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork thumbnail
                Surface(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
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
                            Text(trackTitle.take(1).uppercase(), color = Color.Gray, fontWeight = FontWeight.Bold)
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist.ifBlank { "Unknown Artist" },
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { playbackController.playPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { playbackController.togglePlayPause() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = { playbackController.playNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
