package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onOpenArtist: (String) -> Unit = {},
    onOpenTag: (String) -> Unit = {},
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

            // ── Momentum: streak + weekly goal ─────────────────────────────────
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("MOMENTUM")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                MetricCard(
                    big = if (stats.currentStreakDays > 0) "${stats.currentStreakDays} 🔥" else "—",
                    label = if (stats.currentStreakDays == 1) "day streak" else "day streak",
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                WeeklyGoalCard(
                    weekMinutes = stats.weekMinutes,
                    goalMinutes = stats.weeklyGoalMinutes,
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
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

            // ── Top artists (tap to drill down) ────────────────────────────────
            if (stats.topArtists.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                SectionLabel("TOP ARTISTS")
                stats.topArtists.forEachIndexed { i, (artist, count) ->
                    TappableRankRow(
                        rank = i + 1,
                        title = artist,
                        trailing = "$count play${if (count == 1) "" else "s"}",
                        accent = accent,
                        onClick = { onOpenArtist(artist) }
                    )
                }
            }

            // ── Top tags (tap to drill down) ───────────────────────────────────
            if (stats.topTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                SectionLabel("TOP TAGS")
                stats.topTags.forEachIndexed { i, (tag, count) ->
                    TappableRankRow(
                        rank = i + 1,
                        title = com.freqvox.ui.tag.formatTagDisplayName(tag),
                        trailing = "$count play${if (count == 1) "" else "s"}",
                        accent = accent,
                        onClick = { onOpenTag(tag) }
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
private fun WeeklyGoalCard(weekMinutes: Long, goalMinutes: Int, accent: Color, modifier: Modifier = Modifier) {
    val fraction = if (goalMinutes > 0) (weekMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(16.dp)
    ) {
        val wkH = weekMinutes / 60
        val wkM = weekMinutes % 60
        Text(
            if (wkH > 0) "${wkH}h ${wkM}m" else "${wkM}m",
            color = Fvox.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Version-proof progress bar (avoids M3 LinearProgressIndicator API churn).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Fvox.onSurface.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "this week · goal ${goalMinutes / 60}h",
            color = Fvox.onSurface.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
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

@Composable
private fun TappableRankRow(rank: Int, title: String, trailing: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$rank", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        Text(title, color = Fvox.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(trailing, color = Fvox.onSurface.copy(alpha = 0.55f), fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text("›", color = Fvox.onSurface.copy(alpha = 0.4f), fontSize = 18.sp)
    }
}

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}
