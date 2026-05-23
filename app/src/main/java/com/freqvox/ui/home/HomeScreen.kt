package com.freqvox.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.main.MainViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playbackController: PlaybackController,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val activeTracks by viewModel.activeTracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val totalSongs by viewModel.totalSongs.collectAsStateWithLifecycle()
    val totalDuration by viewModel.totalDuration.collectAsStateWithLifecycle()
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val playHistory by viewModel.playHistory.collectAsStateWithLifecycle()
    val taggedTrackIds by viewModel.taggedTrackIds.collectAsStateWithLifecycle()

    val greeting = rememberGreeting()

    var refreshTrigger by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val feedItems = remember(activeTracks, playHistory, taggedTrackIds, refreshTrigger) {
        FeedGenerator.generateFeed(activeTracks, FeedContext(playHistory, taggedTrackIds))
    }

    val heroTrack = currentTrack
        ?: activeTracks.find { it.id == playHistory.firstOrNull()?.trackId }
        ?: activeTracks.firstOrNull()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                delay(300) // Small delay for visual feedback
                refreshTrigger++
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        // Feed Illumination Layer: Massively blurred radial gradient from the Hero track
        if (heroTrack != null) {
            val colors = getColorsForTrack(heroTrack.title)
            val primaryColor = colors.first().copy(alpha = 0.10f) // Balanced ambient glow
            Canvas(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor, Color.Transparent),
                        center = Offset(size.width / 2, -100f),
                        radius = size.width
                    ),
                    center = Offset(size.width / 2, -100f),
                    radius = size.width
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp) // Space for bottom nav/player
        ) {
        // 1. Header with inline metrics
        item {
            Header(
                greeting = greeting,
                totalSongs = totalSongs,
                totalDurationMs = totalDuration,
                onSearchClick = onSearchClick,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 2. The Hero "Jump Back In" Card (Always shows last played or first available)
        item {
            if (heroTrack != null) {
                HeroFeedCard(
                    track = heroTrack,
                    onClick = { onTrackClick(heroTrack, activeTracks) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Iterate through the dynamically generated endless feed blocks
        items(
            items = feedItems,
            key = { it.stableId() },
            contentType = { it::class }
        ) { item ->
            when (item) {
                is FeedItem.Spotlight -> {
                    SpotlightFeedBlock(
                        track = item.track,
                        onClick = { onTrackClick(item.track, activeTracks) },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                is FeedItem.FeaturedBanner -> {
                    FeaturedBannerBlock(
                        track = item.track,
                        onClick = { onTrackClick(item.track, activeTracks) },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                is FeedItem.VerticalList -> {
                    if (item.title != null) {
                        SectionTitle(item.title, icon = item.icon, modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        item.tracks.forEach { track ->
                            FeedTrackCard(
                                track = track,
                                isCurrentTrack = currentTrackTitle == track.title,
                                isPlaying = isPlaying,
                                onClick = { onTrackClick(track, item.tracks) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                is FeedItem.HorizontalCarousel -> {
                    SectionTitle(item.title, icon = item.icon, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    QuickPicksRow(
                        tracks = item.tracks,
                        queueContext = item.tracks,
                        onTrackClick = onTrackClick
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                is FeedItem.DoubleHorizontalCarousel -> {
                    SectionTitle(item.title, icon = item.icon, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    val half = item.tracks.size / 2
                    QuickPicksRow(
                        tracks = item.tracks.take(half),
                        queueContext = item.tracks,
                        onTrackClick = onTrackClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickPicksRow(
                        tracks = item.tracks.drop(half),
                        queueContext = item.tracks,
                        onTrackClick = onTrackClick
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                is FeedItem.Grid -> {
                    SectionTitle(item.title, icon = item.icon, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    GridFeedBlock(
                        tracks = item.tracks,
                        onClick = { track -> onTrackClick(track, item.tracks) },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            } // Close when
    } // Close LazyColumn
    } // Close Box
    } // Close PullToRefreshBox
} // Close HomeScreen

@Composable
fun GridFeedBlock(tracks: List<TrackEntity>, onClick: (TrackEntity) -> Unit, modifier: Modifier = Modifier) {
    val chunked = tracks.chunked(2)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        chunked.forEach { rowTracks ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTracks.forEach { track ->
                    // Square grid card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .clickable { onClick(track) }
                            .padding(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            ProceduralArtwork(track = track, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = track.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // If odd number, pad with empty space
                if (rowTracks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SpotlightFeedBlock(track: TrackEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // huge square
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(Color(0xFF1A1A26))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Draw a static "pulse" spotlight design
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.35f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f,
                center = center
            )
            
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Spotlight",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            Text(
                text = track.title.take(1).uppercase(),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 80.sp,
                fontWeight = FontWeight.Black
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
fun Header(greeting: String, totalSongs: Int, totalDurationMs: Long, onSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    val hours = totalDurationMs / (1000 * 60 * 60)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalSongs Tracks • $hours Hours",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Profile Placeholder / Logo
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Image(
                    painter = painterResource(id = com.freqvox.R.drawable.splash_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun HeroFeedCard(track: TrackEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.3f), Color(0xFF12121A))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        // Abstract Monogram Background
        Text(
            text = track.title.take(1).uppercase(),
            fontSize = 180.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.03f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Jump Back In",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.Black,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Column {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
fun FeedTrackCard(
    track: TrackEntity,
    isCurrentTrack: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccent = Color(0xFF7C4DFF)
    val bgAlpha = if (isCurrentTrack) 0.10f else 0.04f
    val borderModifier = if (isCurrentTrack) {
        Modifier.border(1.dp, activeAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrentTrack) activeAccent.copy(alpha = bgAlpha) else Color.White.copy(alpha = bgAlpha))
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            ProceduralArtwork(track = track, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrentTrack) activeAccent else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
        Icon(
            imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isCurrentTrack && isPlaying) "Pause" else "Play",
            tint = if (isCurrentTrack) activeAccent else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF7C4DFF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun QuickPicksRow(
    tracks: List<TrackEntity>,
    queueContext: List<TrackEntity>,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onTrackClick(track, queueContext) }
            ) {
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    ProceduralArtwork(track = track, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
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

private val premiumGradients = listOf(
    listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), // Sunset
    listOf(Color(0xFF4776E6), Color(0xFF8E54E9)), // Purple Rain
    listOf(Color(0xFF00B4DB), Color(0xFF0083B0)), // Ocean
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)), // Gold
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)), // Emerald
    listOf(Color(0xFF8A2387), Color(0xFFE94057)), // Citrus
    listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)), // Velvet
    listOf(Color(0xFFED213A), Color(0xFF93291E)), // Blood
    listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)), // Mint
    listOf(Color(0xFFB20A2C), Color(0xFFFFFBD5))  // Cherry
)

fun getColorsForTrack(title: String): List<Color> {
    val index = kotlin.math.abs(title.hashCode()) % premiumGradients.size
    return premiumGradients[index]
}

fun getGradientForTrack(title: String): Brush {
    return Brush.linearGradient(getColorsForTrack(title))
}

@Composable
fun ProceduralArtwork(track: TrackEntity, modifier: Modifier = Modifier) {
    val hash = track.title.hashCode()
    val random = java.util.Random(hash.toLong())
    
    val shapeType = random.nextInt(3)
    val shapeAlpha = 0.20f + random.nextFloat() * 0.2f // Balanced shape visibility
    val scale = 0.5f + random.nextFloat() * 1.5f
    val offsetX = -0.5f + random.nextFloat()
    val offsetY = -0.5f + random.nextFloat()
    
    Box(
        modifier = modifier
            .background(getGradientForTrack(track.title))
            .clipToBounds()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPoint = Offset(
                size.width / 2 + size.width * offsetX,
                size.height / 2 + size.height * offsetY
            )
            val radius = size.minDimension * scale
            
            when (shapeType) {
                0 -> {
                    drawCircle(color = Color.White.copy(alpha = shapeAlpha), radius = radius, center = centerPoint)
                    drawCircle(color = Color.Black.copy(alpha = shapeAlpha * 0.5f), radius = radius * 0.6f, center = Offset(centerPoint.x + radius * 0.3f, centerPoint.y - radius * 0.3f))
                }
                1 -> {
                    val strokeWidth = size.minDimension * 0.4f
                    drawLine(color = Color.Black.copy(alpha = shapeAlpha), start = Offset(-size.width * scale, size.height + size.height * scale), end = Offset(size.width + size.width * scale, -size.height * scale), strokeWidth = strokeWidth)
                }
                2 -> {
                    drawCircle(
                        brush = Brush.radialGradient(colors = listOf(Color.White.copy(alpha = shapeAlpha), Color.Transparent), center = centerPoint, radius = radius),
                        radius = radius,
                        center = centerPoint
                    )
                }
            }
        }
        
        Text(
            text = track.title.take(3).uppercase(),
            color = Color.White.copy(alpha = 0.12f), // Balanced typography
            fontWeight = FontWeight.Black,
            fontSize = 80.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
                .graphicsLayer {
                    rotationZ = -15f
                    scaleX = 1.5f
                    scaleY = 1.5f
                }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A26).copy(alpha = 0.55f)) // Sweet spot: not too bright, not too dark
        )
    }
}

@Composable
fun FeaturedBannerBlock(track: TrackEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6B3FA0), Color(0xFF1A1A26))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        // Massive faded typography bleeding off the edge
        Text(
            text = track.title.uppercase(),
            fontSize = 80.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.05f),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Featured Track",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
