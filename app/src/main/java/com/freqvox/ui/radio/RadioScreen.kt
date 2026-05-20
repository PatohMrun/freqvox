package com.freqvox.ui.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RadioStation(
    val name: String,
    val url: String,
    val genre: String,
    val listeners: String
)

@Composable
fun RadioScreen(onStationClick: (RadioStation) -> Unit) {
    val stations = listOf(
        RadioStation("Nightride FM", "https://stream.nightride.fm/nightride.m4a", "Synthwave", "1.2k"),
        RadioStation("Lofi Girl", "https://bit.ly/lofi-girl-stream", "Lofi / Study", "45k"),
        RadioStation("SomaFM: Groove Salad", "https://ice1.somafm.com/groovesalad-128-mp3", "Ambient / Chill", "3.4k"),
        RadioStation("Deep Space One", "https://ice1.somafm.com/deepspaceone-128-mp3", "Deep Ambient", "800"),
        RadioStation("KEXP", "https://kexp-mp3-128.streamguys1.com/kexp128.mp3", "Indie / Alternative", "12k")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Radio",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Featured Station Hero Card
        FeaturedStationCard(stations[0], onStationClick)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "All Stations",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(stations) { station ->
                StationListItem(station, onStationClick)
            }
        }
    }
}

@Composable
fun FeaturedStationCard(station: RadioStation, onClick: (RadioStation) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick(station) },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A1A26)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Surface(
                        color = Color(0xFFFF4D7C),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(station.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(station.genre, fontSize = 14.sp, color = Color.Gray)
                }
                
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StationListItem(station: RadioStation, onClick: (RadioStation) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(station) }
            .background(Color(0xFF12121A), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1A1A26)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Radio, contentDescription = null, tint = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(station.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text(station.genre, color = Color.Gray, fontSize = 12.sp)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(station.listeners, color = Color(0xFF4DFFB4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("listeners", color = Color.Gray, fontSize = 10.sp)
        }
    }
}
