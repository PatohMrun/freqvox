package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.theme.Fvox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A focused stats drill-down for an arbitrary subset of tracks (one artist, or one tag). Reuses
 * the same visual language as the main stats sheet but scoped to [tracks].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityStatsSheet(
    title: String,
    subtitle: String,
    tracks: List<TrackEntity>,
    playHistory: List<PlayHistoryEntity>,
    onDismiss: () -> Unit
) {
    val accent = Fvox.accent
    val subsetIds = remember(tracks) { tracks.mapTo(HashSet()) { it.id } }
    val subsetHistory = remember(subsetIds, playHistory) { playHistory.filter { it.trackId in subsetIds } }

    val totalPlays = tracks.sumOf { it.playCount }
    val totalMinutes = tracks.sumOf { it.totalPlayTime } / 60_000
    val uniquePlayed = tracks.count { it.playCount > 0 }
    val firstPlayed = subsetHistory.minByOrNull { it.playedAt }?.playedAt
    val lastPlayed = tracks.mapNotNull { it.lastPlayed }.maxOrNull()
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(title, color = Fvox.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))

            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(if (hours > 0) "${hours}h ${mins}m" else "${mins}m", "Total time", accent, Modifier.weight(1f))
                MiniStat("$totalPlays", "Plays", accent, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("$uniquePlayed / ${tracks.size}", "Tracks played", accent, Modifier.weight(1f))
                MiniStat(
                    if (lastPlayed != null) dateFmt.format(Date(lastPlayed)) else "—",
                    "Last played", accent, Modifier.weight(1f)
                )
            }
            if (firstPlayed != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "First heard ${dateFmt.format(Date(firstPlayed))} (in recent history)",
                    color = Fvox.onSurface.copy(alpha = 0.45f), fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            val top = tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(5)
            if (top.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("MOST PLAYED", color = Fvox.onSurface.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                top.forEachIndexed { i, t ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, color = Fvox.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, color = Fvox.onSurface.copy(alpha = 0.45f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${t.playCount} plays", color = Fvox.onSurface.copy(alpha = 0.55f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(big: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(16.dp)
    ) {
        Text(big, color = Fvox.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
