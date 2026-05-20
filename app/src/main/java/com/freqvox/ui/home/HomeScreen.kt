package com.freqvox.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.main.MainViewModel
import java.util.*

@Composable
fun HomeScreen(
    onTrackClick: (TrackEntity) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val totalSongs by viewModel.totalSongs.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()

    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Header(greeting)
            Spacer(modifier = Modifier.height(24.dp))
            StatsSection(totalSongs, totalDuration)
            Spacer(modifier = Modifier.height(32.dp))
            SectionTitle("Recently Added")
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(recentlyAdded) { track ->
            RecentTrackItem(track = track, onClick = { onTrackClick(track) })
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
            SectionTitle("Quick Picks")
            Spacer(modifier = Modifier.height(16.dp))
            QuickPicksRow(recentlyAdded.shuffled().take(5), onTrackClick)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun Header(greeting: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = greeting,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        // Profile Placeholder
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF6B3FA0)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("P", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatsSection(totalSongs: Int, totalDurationMs: Long) {
    val hours = totalDurationMs / (1000 * 60 * 60)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Songs", totalSongs.toString(), Modifier.weight(1f))
        StatCard("Hours", hours.toString(), Modifier.weight(1f))
        StatCard("Tags", "12", Modifier.weight(1f)) // Hardcoded tags for now
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF12121A),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
fun RecentTrackItem(track: TrackEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1A1A26)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(track.title.take(1).uppercase(), color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(track.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(track.artist, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
fun QuickPicksRow(tracks: List<TrackEntity>, onTrackClick: (TrackEntity) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(tracks) { track ->
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .clickable { onTrackClick(track) }
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1A1A26)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(track.title.take(1).uppercase(), color = Color.Gray, fontSize = 32.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(track.title, color = Color.White, fontSize = 14.sp, maxLines = 1)
                Text(track.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
