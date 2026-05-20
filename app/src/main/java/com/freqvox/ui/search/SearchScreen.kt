package com.freqvox.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.library.TrackItem
import com.freqvox.ui.main.MainViewModel

@Composable
fun SearchScreen(
    onTrackClick: (TrackEntity) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Browse",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            placeholder = { Text("Search songs, artists, albums...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7C4DFF),
                unfocusedBorderColor = Color(0xFF1A1A26),
                focusedContainerColor = Color(0xFF12121A),
                unfocusedContainerColor = Color(0xFF12121A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (searchQuery.isBlank()) {
            Text(
                text = "Genres",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            GenreGrid()
        } else {
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found for \"$searchQuery\"", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(searchResults) { track ->
                        TrackItem(track = track, onClick = { onTrackClick(track) })
                    }
                }
            }
        }
    }
}

@Composable
fun GenreGrid() {
    val genres = listOf(
        "Electronic" to Color(0xFF6B3FA0),
        "Ambient" to Color(0xFF4DFFB4),
        "Hip-Hop" to Color(0xFFFF4D7C),
        "R&B" to Color(0xFF7C4DFF),
        "Rock" to Color(0xFFFFA726),
        "Classical" to Color(0xFF42A5F5),
        "Afrobeats" to Color(0xFF66BB6A),
        "Jazz" to Color(0xFFEF5350)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in genres.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GenreCard(genres[i].first, genres[i].second, Modifier.weight(1f))
                if (i + 1 < genres.size) {
                    GenreCard(genres[i + 1].first, genres[i + 1].second, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GenreCard(name: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.2f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
