package com.freqvox.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    track: TrackEntity?,
    currentPosition: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var rawLyrics by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(track) {
        if (track == null) return@LaunchedEffect
        isLoading = true
        withContext(Dispatchers.IO) {
            val trackFile = File(track.path)
            val lrcFile = File(trackFile.parent, trackFile.nameWithoutExtension + ".lrc")
            
            if (lrcFile.exists()) {
                val lines = lrcFile.readLines()
                lyrics = parseLrc(lines)
                rawLyrics = null
            } else {
                // Try embedded or just show no lyrics
                lyrics = emptyList()
                rawLyrics = null
            }
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A26),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Lyrics",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF7C4DFF))
                }
            } else if (lyrics.isEmpty() && rawLyrics == null) {
                LyricsInstructions()
            } else {
                // Display lyrics
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    lyrics.forEach { line ->
                        val isCurrent = currentPosition >= line.timestamp && 
                                       (lyrics.getOrNull(lyrics.indexOf(line) + 1)?.timestamp ?: Long.MAX_VALUE) > currentPosition
                        
                        Text(
                            text = line.content,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                            fontSize = if (isCurrent) 22.sp else 18.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsInstructions() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF7C4DFF).copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Local Lyrics Found",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "To see synced lyrics in Freqvox:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        InstructionStep(1, "Download a .lrc file for your song.")
        InstructionStep(2, "Name it exactly like the audio file.")
        InstructionStep(3, "Place it in the same folder as the song.")
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Freqvox prioritizes privacy and local ownership. We do not scrape lyrics from the web.",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun InstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF7C4DFF).copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$number", color = Color(0xFF7C4DFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

data class LyricLine(val timestamp: Long, val content: String)

fun parseLrc(lines: List<String>): List<LyricLine> {
    val lyricLines = mutableListOf<LyricLine>()
    val timeRegex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\]")
    
    lines.forEach { line ->
        val match = timeRegex.find(line)
        if (match != null) {
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val ms = match.groupValues[3].padEnd(3, '0').take(3).toLong()
            val timestamp = (min * 60 * 1000) + (sec * 1000) + ms
            val content = line.replace(timeRegex, "").trim()
            if (content.isNotEmpty()) {
                lyricLines.add(LyricLine(timestamp, content))
            }
        }
    }
    return lyricLines.sortedBy { it.timestamp }
}
