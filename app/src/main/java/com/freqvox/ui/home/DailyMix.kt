package com.freqvox.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.tag.formatTagDisplayName
import kotlin.random.Random

data class DailyMix(val title: String, val subtitle: String, val tracks: List<TrackEntity>)

/**
 * Builds a small set of themed mixes that stay identical for the whole calendar day: the RNG is
 * seeded by the epoch-day, so the same themes and order come back on every feed regeneration and
 * app restart, then roll over to a fresh set tomorrow. Themes are drawn from tags with enough
 * songs and from prolific artists; falls back to plain shuffles for a sparsely-tagged library.
 */
fun generateDailyMixes(
    tracks: List<TrackEntity>,
    taggedTrackIds: Map<String, Set<Long>>,
    epochDay: Long,
    max: Int = 4
): List<DailyMix> {
    if (tracks.size < 12) return emptyList()
    val random = Random(epochDay)
    val byId = tracks.associateBy { it.id }

    data class Theme(val label: String, val pool: List<TrackEntity>)
    val themes = mutableListOf<Theme>()
    taggedTrackIds.forEach { (name, ids) ->
        if (name.equals("disliked", ignoreCase = true)) return@forEach
        val pool = ids.mapNotNull { byId[it] }
        if (pool.size >= 8) themes += Theme(formatTagDisplayName(name), pool)
    }
    tracks.groupBy { it.artist }
        .filter { it.value.size >= 8 && it.key.isNotBlank() }
        .forEach { (artist, pool) -> themes += Theme(artist, pool) }

    if (themes.isEmpty()) {
        return List(minOf(max, 2)) { i ->
            DailyMix("Daily Mix ${i + 1}", "A shuffle of your library",
                tracks.shuffled(Random(epochDay + i)).take(25))
        }
    }

    return themes.shuffled(random).take(max).mapIndexed { i, theme ->
        DailyMix(
            title = "Daily Mix ${i + 1}",
            subtitle = theme.label,
            tracks = theme.pool.shuffled(Random(epochDay + i)).take(30)
        )
    }
}

/** Horizontal row of tappable Daily Mix cover cards. Tapping plays the whole mix. */
@Composable
fun DailyMixesRow(
    mixes: List<DailyMix>,
    onPlayMix: (DailyMix) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionTitle("Daily Mixes", icon = Icons.Default.AutoAwesome, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            mixes.forEach { mix ->
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPlayMix(mix) }
                ) {
                    mix.tracks.firstOrNull()?.let { TrackArtwork(track = it, modifier = Modifier.fillMaxSize()) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                        Text(mix.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            mix.subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
