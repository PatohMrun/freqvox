package com.freqvox.ui.player.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    track: TrackEntity?,
    currentPosition: Long,
    onDismiss: () -> Unit,
    onSeek: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                isLoading = true
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        lyrics = parseLrc(reader.readLines())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    )

    LaunchedEffect(track) {
        if (track == null) { lyrics = emptyList(); return@LaunchedEffect }
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val trackFile = File(track.path)
                val lrcFile = File(trackFile.parent, trackFile.nameWithoutExtension + ".lrc")
                lyrics = if (lrcFile.exists()) parseLrc(lrcFile.readLines()) else emptyList()
            } catch (e: Exception) {
                lyrics = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Fvox.onSurface.copy(alpha = 0.2f)) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lyrics",
                    color = Fvox.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (lyrics.isNotEmpty()) {
                    IconButton(onClick = { launcher.launch(arrayOf("*/*")) }) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = "Change File",
                            tint = Fvox.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Fvox.accent)
                }
            } else if (lyrics.isEmpty()) {
                LyricsInstructions(onPickFile = { launcher.launch(arrayOf("*/*")) })
            } else {
                // Synced view: the current line is the last one whose timestamp has passed. It's
                // auto-scrolled toward the middle as playback advances; tap a line to seek to it.
                val listState = rememberLazyListState()
                val currentIndex = remember(lyrics, currentPosition) {
                    lyrics.indexOfLast { currentPosition >= it.timestamp }
                }
                LaunchedEffect(currentIndex) {
                    if (currentIndex >= 0) {
                        listState.animateScrollToItem(currentIndex, scrollOffset = -400)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 48.dp)
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isCurrent = index == currentIndex
                        Text(
                            text = line.content,
                            color = if (isCurrent) Fvox.onSurface else Fvox.onSurface.copy(alpha = 0.28f),
                            fontSize = if (isCurrent) 24.sp else 18.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (onSeek != null) Modifier.clickable { onSeek(line.timestamp) } else Modifier)
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsInstructions(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = Fvox.onSurface.copy(alpha = 0.15f),
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "No Lyrics Found",
            color = Fvox.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Freqvox reads .lrc files from your device to keep your library private.",
            color = Fvox.onSurface.copy(alpha = 0.4f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(36.dp))

        InstructionStep(1, "Download a .lrc file for this track.")
        InstructionStep(2, "Name it the same as the audio file.")
        InstructionStep(3, "Or pick it manually below.")

        Spacer(modifier = Modifier.height(44.dp))

        OutlinedButton(
            onClick = onPickFile,
            border = BorderStroke(1.5.dp, Fvox.accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Fvox.onSurface
            )
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Pick .lrc File", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .border(1.dp, Fvox.accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                color = Fvox.accent.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Fvox.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

data class LyricLine(val timestamp: Long, val content: String)

fun parseLrc(lines: List<String>): List<LyricLine> {
    val lyricLines = mutableListOf<LyricLine>()
    val timeRegex = Regex("\\[(\\d+):(\\d+)[\\.:](\\d+)\\]")

    lines.forEach { line ->
        val matches = timeRegex.findAll(line)
        val content = line.replace(timeRegex, "").trim()
        if (content.isNotEmpty()) {
            matches.forEach { match ->
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msString = match.groupValues[3]
                val ms = when (msString.length) {
                    1 -> msString.toLong() * 100
                    2 -> msString.toLong() * 10
                    else -> msString.take(3).toLong()
                }
                lyricLines.add(LyricLine((min * 60 * 1000) + (sec * 1000) + ms, content))
            }
        }
    }
    return lyricLines.sortedBy { it.timestamp }
}
