package com.freqvox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.playback.PlaybackController

@Composable
fun MiniPlayer(
    playbackController: PlaybackController,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by playbackController.isPlaying.collectAsState()
    val trackTitle by playbackController.currentTrackTitle.collectAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onMiniPlayerClick),
        color = Color(0xFF1A1A26),
        tonalElevation = 8.dp
    ) {
        Column {
            // Tiny progress bar at the bottom (placeholder)
            LinearProgressIndicator(
                progress = { 0.3f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Color(0xFF7C4DFF),
                trackColor = Color.Transparent,
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork thumbnail placeholder
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = Color(0xFF12121A)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(trackTitle.take(1).uppercase(), color = Color.Gray)
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
                        text = "Unknown Artist", // TODO: Get from controller
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { playbackController.togglePlayPause() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                IconButton(onClick = { playbackController.playNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}
