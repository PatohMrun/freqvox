package com.freqvox.ui.radio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.playback.PlaybackController

@Composable
fun RadioScreen(
    playbackController: PlaybackController,
    onStationClick: (RadioStation) -> Unit,
    viewModel: RadioViewModel = hiltViewModel()
) {
    val currentTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()

    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteStations by viewModel.favoriteStations.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val selectedCountryCode by viewModel.selectedCountryCode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playingStation by viewModel.playingStation.collectAsStateWithLifecycle()

    val currentStation = remember(currentTitle, playingStation) {
        if (playingStation != null && currentTitle == playingStation!!.name) playingStation else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Radio", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    when {
                        isLoading -> "Loading…"
                        stations.isEmpty() && !isLoading -> "No stations found"
                        selectedGenre == "Top" && selectedCountryCode == "KE" && searchQuery.isBlank() ->
                            "${stations.size} Kenyan stations · curated"
                        else -> "${stations.size} live streams"
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Search bar ────────────────────────────────────────────────────────
        item {
            RadioSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Country filter ────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Country",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 2.dp)
                )
                COUNTRY_OPTIONS.forEach { option ->
                    val code = option?.code
                    val label = option?.label ?: "World"
                    val isSelected = selectedCountryCode == code
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF2563EB) else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.10f),
                                CircleShape
                            )
                            .clickable { viewModel.selectCountry(code) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ── Genre filter ──────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ALL_GENRES.forEach { genre ->
                    val isSelected = selectedGenre == genre
                    val isFavoritesTab = genre == "Favorites"
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    if (isFavoritesTab) Color(0xFFFF4D7C) else Color(0xFF7C4DFF)
                                } else Color.White.copy(alpha = 0.07f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .clickable { viewModel.selectGenre(genre) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isFavoritesTab) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFFFF4D7C),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                genre,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Error ─────────────────────────────────────────────────────────────
        if (error != null) {
            item {
                Text(
                    error ?: "",
                    color = Color(0xFFFF4D7C),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── Loading ───────────────────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF7C4DFF), modifier = Modifier.size(28.dp))
                }
            }
        }

        // ── Now playing card ──────────────────────────────────────────────────
        if (currentStation != null) {
            item {
                NowPlayingRadioCard(
                    station = currentStation,
                    isPlaying = isPlaying,
                    onTogglePlayPause = { playbackController.togglePlayPause() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // ── Empty state ───────────────────────────────────────────────────────
        if (!isLoading && stations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No stations found", color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
                    if (selectedCountryCode != null && selectedGenre != "Favorites") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Try switching to World to search globally",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { viewModel.selectCountry(null) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Show World stations", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ── Station list ──────────────────────────────────────────────────────
        if (!isLoading && stations.isNotEmpty()) {
            val sectionLabel = when {
                searchQuery.isNotBlank() -> "Search Results"
                selectedGenre == "Top" && selectedCountryCode == "KE" -> "Kenya Featured"
                selectedGenre == "Top" -> "Top Stations"
                selectedGenre == "Favorites" -> "Your Favorites"
                else -> "$selectedGenre Stations"
            }
            item {
                Text(
                    sectionLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(stations, key = { it.id }) { station ->
                StationCard(
                    station = station,
                    isCurrentlyPlaying = currentTitle == station.name && isPlaying,
                    isFavorite = station.id in favorites,
                    onPlay = {
                        viewModel.setPlayingStation(station)
                        onStationClick(station)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(station) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RadioSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (focused) 0.10f else 0.07f))
            .border(
                1.dp,
                if (focused) Color(0xFF7C4DFF).copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF7C4DFF)),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            "Search stations…",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear",
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
fun NowPlayingRadioCard(
    station: RadioStation,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(station.accentColor)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LivePulse(color = Color(0xFFFF4D7C))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "NOW PLAYING",
                    fontSize = 10.sp,
                    color = Color(0xFFFF4D7C),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    station.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${station.genre} · ${station.description}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.3f))
                    .clickable { onTogglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun LivePulse(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(modifier = modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(color.copy(alpha = alpha * 0.35f)))
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
    }
}

@Composable
fun StationCard(
    station: RadioStation,
    isCurrentlyPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(station.accentColor)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrentlyPlaying) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .then(if (isCurrentlyPlaying) Modifier.border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp)) else Modifier)
            .clickable { onPlay() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.15f)))),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrentlyPlaying) LivePulse(color = Color.White)
            else Icon(Icons.Default.Radio, contentDescription = null, tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                station.name,
                color = if (isCurrentlyPlaying) accent else Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                station.description,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(station.genre, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                tint = if (isFavorite) Color(0xFFFF4D7C) else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
