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
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(
    stats: MainViewModel.ListeningStats,
    onDismiss: () -> Unit
) {
    val accent = Fvox.accent
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
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Your Listening",
                color = Fvox.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 16.dp)
            )

            val hours = stats.totalMinutes / 60
            val mins = stats.totalMinutes % 60

            // ── Headline metric cards ──────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    big = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                    label = "Total time",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    big = "${stats.totalPlays}",
                    label = "Total plays",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    big = "${stats.uniqueTracks}",
                    label = "Tracks played",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    big = "${stats.addedThisMonth}",
                    label = "Added this month",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.totalPlays == 0) {
                Spacer(modifier = Modifier.height(28.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Play some music to start building your stats.",
                        color = Fvox.onSurface.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
                return@Column
            }

            // ── Listening by hour ──────────────────────────────────────────────
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("BY HOUR OF DAY")
            stats.peakHour?.let { ph ->
                Text(
                    text = "Most active around ${formatHour(ph)}",
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )
            }
            HourHistogram(buckets = stats.hourBuckets, accent = accent)

            // ── Top tracks ─────────────────────────────────────────────────────
            if (stats.topTracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                SectionLabel("TOP TRACKS")
                stats.topTracks.forEachIndexed { i, (track, count) ->
                    RankRow(
                        rank = i + 1,
                        title = track.title,
                        subtitle = track.artist,
                        trailing = "$count play${if (count == 1) "" else "s"}",
                        accent = accent
                    )
                }
            }

            // ── Most skipped ───────────────────────────────────────────────────
            if (stats.mostSkipped.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                SectionLabel("MOST SKIPPED")
                stats.mostSkipped.forEachIndexed { i, (track, count) ->
                    RankRow(
                        rank = i + 1,
                        title = track.title,
                        subtitle = track.artist,
                        trailing = "$count skip${if (count == 1) "" else "s"}",
                        accent = Color(0xFF78909C)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(big: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(16.dp)
    ) {
        Text(big, color = Fvox.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Fvox.onSurface.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun HourHistogram(buckets: List<Int>, accent: Color) {
    val max = (buckets.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEachIndexed { hour, count ->
            val fraction = count.toFloat() / max
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            if (count > 0) accent.copy(alpha = 0.35f + 0.55f * fraction)
                            else Fvox.onSurface.copy(alpha = 0.05f)
                        )
                )
                // Label only every 6 hours to avoid clutter.
                if (hour % 6 == 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$hour", color = Fvox.onSurface.copy(alpha = 0.3f), fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun RankRow(rank: Int, title: String, subtitle: String, trailing: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$rank",
            color = accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Fvox.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Fvox.onSurface.copy(alpha = 0.45f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(trailing, color = Fvox.onSurface.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}
