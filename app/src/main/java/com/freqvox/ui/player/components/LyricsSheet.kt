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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.data.lyrics.LyricsStore
import com.freqvox.ui.theme.Fvox
import kotlinx.coroutines.launch

/**
 * Lyrics for the current track. Sources, in order: the app's saved copy (imported or fetched
 * earlier — see [LyricsStore]), a sidecar .lrc next to the audio file, a user-picked document,
 * or an explicit online search (LRCLIB). Imports and online hits are PERSISTED, so lyrics
 * survive closing the sheet, app restarts and rescans — they used to live only in this
 * composable's memory and vanish on dismiss.
 *
 * Synced (.lrc-timestamped) lyrics get the karaoke view (highlight + tap-to-seek);
 * plain lyrics render as a static sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    track: TrackEntity?,
    currentPosition: Long,
    onDismiss: () -> Unit,
    onSeek: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val store = remember { LyricsStore(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isSynced by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    // Per-track timing nudge, persisted via LyricsStore. Positive = lines fire earlier.
    var offsetMs by remember { mutableLongStateOf(0L) }

    fun nudgeOffset(deltaMs: Long) {
        val t = track ?: return
        offsetMs = (offsetMs + deltaMs).coerceIn(-30_000L, 30_000L)
        val newOffset = offsetMs
        scope.launch { store.setOffset(t, newOffset) }
    }

    // Parses raw lines: timestamped → synced view; otherwise plain text view.
    fun applyLines(lines: List<String>) {
        val parsed = parseLrc(lines)
        if (parsed.isNotEmpty()) {
            lyrics = parsed; isSynced = true
        } else {
            lyrics = lines.filter { it.isNotBlank() }.map { LyricLine(0L, it.trim()) }
            isSynced = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            val t = track
            if (uri != null && t != null) {
                isLoading = true
                scope.launch {
                    val lines = store.importFrom(t, uri)   // persists the copy
                    if (lines != null) applyLines(lines)
                    else statusMessage = "Couldn't read that file"
                    isLoading = false
                }
            }
        }
    )

    fun searchOnline() {
        val t = track ?: return
        isLoading = true
        statusMessage = null
        scope.launch {
            when (val r = store.fetchOnline(t)) {
                is LyricsStore.OnlineResult.Found -> {
                    applyLines(r.lines)
                    statusMessage =
                        if (r.synced) "Synced lyrics from ${r.source}"
                        else "Lyrics from ${r.source} (not time-synced)"
                }
                LyricsStore.OnlineResult.NotFound ->
                    statusMessage = "No lyrics found online for this track"
                is LyricsStore.OnlineResult.Error ->
                    // Distinct from NotFound: a service genuinely didn't answer. Don't
                    // reflexively blame the user's connection — it's usually the service.
                    statusMessage = "Lyrics services unreachable — try again shortly"
            }
            isLoading = false
        }
    }

    LaunchedEffect(track) {
        if (track == null) { lyrics = emptyList(); return@LaunchedEffect }
        isLoading = true
        statusMessage = null
        offsetMs = store.offsetFor(track)
        val lines = store.load(track)
        if (lines != null) applyLines(lines) else lyrics = emptyList()
        isLoading = false
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
                    Row {
                        IconButton(onClick = { searchOnline() }, enabled = !isLoading) {
                            Icon(
                                Icons.Default.TravelExplore,
                                contentDescription = "Search online",
                                tint = Fvox.accent
                            )
                        }
                        IconButton(onClick = { launcher.launch(arrayOf("*/*")) }) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = "Change file",
                                tint = Fvox.accent
                            )
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Text(
                    msg,
                    color = Fvox.accent.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // ── Sync nudge (synced lyrics only) — remembered per track ──────────
            // + fires lines EARLIER (lyrics lagging the voice), − delays them.
            // Tap the readout to reset.
            if (!isLoading && lyrics.isNotEmpty() && isSynced) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sync",
                        color = Fvox.onSurface.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                    TextButton(onClick = { nudgeOffset(-500L) }) {
                        Text("−0.5s", color = Fvox.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = if (offsetMs == 0L) "0.0s"
                            else String.format("%+.1fs", offsetMs / 1000f),
                        color = if (offsetMs == 0L) Fvox.onSurface.copy(alpha = 0.5f) else Fvox.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val t = track
                            if (t != null) {
                                offsetMs = 0L
                                scope.launch { store.setOffset(t, 0L) }
                            }
                        }
                    )
                    TextButton(onClick = { nudgeOffset(+500L) }) {
                        Text("+0.5s", color = Fvox.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Fvox.accent)
                }
            } else if (lyrics.isEmpty()) {
                LyricsInstructions(
                    onSearchOnline = { searchOnline() },
                    onPickFile = { launcher.launch(arrayOf("*/*")) }
                )
            } else {
                // Synced view: the current line is the last one whose timestamp has passed. It's
                // auto-scrolled toward the middle as playback advances; tap a line to seek to it.
                // Plain (unsynced) lyrics render statically — no highlight, no seeking.
                val listState = rememberLazyListState()
                // The offset shifts the effective playhead: +offset makes lines fire earlier.
                val currentIndex = remember(lyrics, currentPosition, isSynced, offsetMs) {
                    if (isSynced) lyrics.indexOfLast { currentPosition + offsetMs >= it.timestamp } else -1
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
                        val isCurrent = isSynced && index == currentIndex
                        val seekable = isSynced && onSeek != null
                        Text(
                            text = line.content,
                            color = when {
                                isCurrent -> Fvox.onSurface
                                isSynced -> Fvox.onSurface.copy(alpha = 0.28f)
                                else -> Fvox.onSurface.copy(alpha = 0.75f)
                            },
                            fontSize = if (isCurrent) 24.sp else 18.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    // With an offset active, the moment this line is sung in the
                                    // AUDIO is timestamp − offset; seek there, not to the raw tag.
                                    if (seekable) Modifier.clickable {
                                        onSeek?.invoke((line.timestamp - offsetMs).coerceAtLeast(0L))
                                    }
                                    else Modifier
                                )
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsInstructions(onSearchOnline: () -> Unit, onPickFile: () -> Unit) {
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
            "Search the free LRCLIB database, or pick a .lrc file from your device. Either way, Freqvox remembers it for this song.",
            color = Fvox.onSurface.copy(alpha = 0.4f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onSearchOnline,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Fvox.accent,
                contentColor = Fvox.surface
            )
        ) {
            Icon(Icons.Default.TravelExplore, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Search online", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

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
