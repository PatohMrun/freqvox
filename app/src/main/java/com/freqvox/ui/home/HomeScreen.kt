@file:OptIn(ExperimentalFoundationApi::class)

package com.freqvox.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.PlaybackController
import com.freqvox.data.prefs.ThemeMode
import com.freqvox.ui.main.MainViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import com.freqvox.ui.components.TagPickerSheet
import com.freqvox.ui.components.TrackActionsDialogs
import com.freqvox.ui.components.TrackOptionsSheet
import com.freqvox.ui.components.rememberTrackActionsState
import com.freqvox.ui.search.SearchSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playbackController: PlaybackController,
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onSearchClick: () -> Unit,
    onOpenPanel: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val activeTracks by viewModel.activeTracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val totalSongs by viewModel.totalSongs.collectAsStateWithLifecycle()
    val totalDuration by viewModel.totalDuration.collectAsStateWithLifecycle()
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val playHistory by viewModel.playHistory.collectAsStateWithLifecycle()
    val taggedTrackIds by viewModel.taggedTrackIds.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val taggingTrackId by viewModel.taggingTrackId.collectAsStateWithLifecycle()
    val taggingTrackTags by viewModel.taggingTrackTags.collectAsStateWithLifecycle()
    val dislikedTrackIds by viewModel.dislikedTrackIds.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val greeting = rememberGreeting()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val trackActions = rememberTrackActionsState()
    var optionsTrack by remember { mutableStateOf<TrackEntity?>(null) }
    // Long-press now opens a richer options menu (Tag is one entry inside) instead of
    // jumping straight to the tag sheet.
    val onTagLongPress: (TrackEntity) -> Unit = { optionsTrack = it }

    var refreshTrigger by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showSearchSheet) {
        SearchSheet(
            playbackController = playbackController,
            onDismiss = { showSearchSheet = false },
            viewModel = viewModel
        )
    }

    if (taggingTrackId != null) {
        val targetId = taggingTrackId
        TagPickerSheet(
            tags = allTags,
            selectedTagIds = taggingTrackTags.map { it.id },
            onTagClick = { viewModel.toggleTagForTaggingTrack(it.id) },
            onCreateCustomTag = { viewModel.createCustomTag(it) },
            onDismiss = { viewModel.closeTagPicker() },
            isDisliked = targetId != null && targetId in dislikedTrackIds,
            onToggleDislike = targetId?.let { id -> { viewModel.toggleDislikeForTrack(id) } }
        )
    }

    // Rich long-press menu for a Home track, plus the shared add-to-playlist / delete dialogs.
    optionsTrack?.let { track ->
        TrackOptionsSheet(
            track = track,
            onTag = { viewModel.openTagPicker(track.id) },
            onAddToPlaylist = { trackActions.openAddToPlaylist(track) },
            onPlayNext = { playbackController.playTrackNext(track) },
            onDelete = { trackActions.openDeleteFlow(track) },
            onDismiss = { optionsTrack = null }
        )
    }
    TrackActionsDialogs(state = trackActions, viewModel = viewModel, playlists = allPlaylists)

    // Endless feed — every page is a fresh remix of the same library with a
    // different per-page seed. Pulling to refresh resets back to page 1 AND
    // wipes the cross-page artist/album exclusion memory so all artists become
    // eligible again. Capped to keep memory bounded; once reached, the footer
    // tells the user to pull-to-refresh for entirely new content.
    var pageCount by remember(activeTracks, refreshTrigger) { mutableIntStateOf(1) }
    val maxPages = 20

    val feedItems: List<PagedFeedItem> = remember(
        activeTracks, playHistory, taggedTrackIds, refreshTrigger, pageCount
    ) {
        val ctx = FeedContext(playHistory, taggedTrackIds)
        var session = FeedSessionState()
        (1..pageCount).flatMap { page ->
            val pageRandom = Random(refreshTrigger * 1_000_003L + page * 31L)
            val result = FeedGenerator.generateFeed(activeTracks, ctx, pageRandom, session)
            session = result.session
            result.items.map { PagedFeedItem(page, it) }
        }
    }
    val reachedEnd = pageCount >= maxPages

    val heroTrack = currentTrack
        ?: activeTracks.find { it.id == playHistory.firstOrNull()?.trackId }
        ?: activeTracks.firstOrNull()

    val listState = rememberLazyListState()

    // Auto-paginate when the user nears the end of the rendered feed (until the cap).
    LaunchedEffect(listState, activeTracks.size) {
        if (activeTracks.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (total <= 0) -1 else (total - lastVisible)
        }
            .distinctUntilChanged()
            .collect { distance ->
                if (distance in 1..6 && pageCount < maxPages) pageCount++
            }
    }

    // Back-to-top FAB visibility — only show after the user has scrolled meaningfully.
    val showBackToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 8 }
    }

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

        Column(modifier = Modifier.fillMaxSize()) {
        // Fixed header — stays locked in place while the feed scrolls beneath it.
        Header(
            greeting = greeting,
            totalSongs = totalSongs,
            totalDurationMs = totalDuration,
            onSearchClick = { showSearchSheet = true },
            themeMode = themeMode,
            onCycleTheme = { viewModel.cycleTheme() },
            onOpenPanel = onOpenPanel,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp) // Space for bottom nav/player
        ) {

        // Empty library → make it obvious the app is working (scanning) or that no
        // music was found, instead of leaving a blank feed that reads as "broken".
        if (totalSongs == 0) {
            item {
                LibraryStatusState(
                    isScanning = isScanning,
                    onRescan = { viewModel.scanLibrary() },
                    modifier = Modifier
                        .fillParentMaxHeight(0.7f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
        }

        // 2. The Hero "Jump Back In" Card (Always shows last played or first available)
        item {
            if (heroTrack != null) {
                HeroFeedCard(
                    track = heroTrack,
                    onClick = { onTrackClick(heroTrack, activeTracks) },
                    onLongPress = { onTagLongPress(heroTrack) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Iterate through the dynamically generated endless feed blocks
        items(
            items = feedItems,
            key = { "${it.page}:${it.item.stableId()}" },
            contentType = { it.item::class }
        ) { paged ->
            val item = paged.item
            when (item) {
                is FeedItem.Spotlight -> {
                    SpotlightFeedBlock(
                        track = item.track,
                        onClick = { onTrackClick(item.track, activeTracks) },
                        onLongPress = { onTagLongPress(item.track) },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                is FeedItem.FeaturedBanner -> {
                    FeaturedBannerBlock(
                        track = item.track,
                        onClick = { onTrackClick(item.track, activeTracks) },
                        onLongPress = { onTagLongPress(item.track) },
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
                                onClick = { onTrackClick(track, item.tracks) },
                                onLongPress = { onTagLongPress(track) }
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
                        onTrackClick = onTrackClick,
                        onTagLongPress = onTagLongPress
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
                        onTrackClick = onTrackClick,
                        onTagLongPress = onTagLongPress
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickPicksRow(
                        tracks = item.tracks.drop(half),
                        queueContext = item.tracks,
                        onTrackClick = onTrackClick,
                        onTagLongPress = onTagLongPress
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                is FeedItem.Grid -> {
                    SectionTitle(item.title, icon = item.icon, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    GridFeedBlock(
                        tracks = item.tracks,
                        onClick = { track -> onTrackClick(track, item.tracks) },
                        onLongPress = onTagLongPress,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            } // Close when

        if (reachedEnd) {
            item(key = "feed-end-footer") {
                EndOfFeedFooter(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
                )
            }
        }
    } // Close LazyColumn
    } // Close fixed-header Column

        // Back-to-top pill — compact and unobtrusive, sits just above the mini-player.
        AnimatedVisibility(
            visible = showBackToTop,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 78.dp)
        ) {
            Surface(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                shape = CircleShape,
                color = Color(0xFF7C4DFF).copy(alpha = 0.88f),
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Back to top",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } // Close Box
    } // Close PullToRefreshBox
} // Close HomeScreen

/** Wraps a [FeedItem] with the page index it was generated for, so [LazyColumn]
 *  keys remain unique across endless-scroll pages. */
data class PagedFeedItem(val page: Int, val item: FeedItem)

@Composable
fun EndOfFeedFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "You've explored a lot",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pull down to refresh for a completely new mix",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun GridFeedBlock(
    tracks: List<TrackEntity>,
    onClick: (TrackEntity) -> Unit,
    onLongPress: (TrackEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                            .combinedClickable(
                                onClick = { onClick(track) },
                                onLongClick = { onLongPress(track) }
                            )
                            .padding(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            TrackArtwork(track = track, modifier = Modifier.fillMaxSize())
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
fun SpotlightFeedBlock(
    track: TrackEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // huge square
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        val artPath = track.artworkPath
        if (artPath != null) {
            // Album art fills the square; signature scrim overlaid for legibility.
            AsyncImage(
                model = java.io.File(artPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF7C4DFF).copy(alpha = 0.25f),
                                Color(0xFF12121A).copy(alpha = 0.80f)
                            )
                        )
                    )
            )
        } else {
            // No art → original solid background + static "pulse" spotlight design.
            Box(modifier = Modifier.matchParentSize().background(Color(0xFF1A1A26)))
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
            
            if (artPath == null) {
                Text(
                    text = track.title.take(1).uppercase(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Header(greeting: String, totalSongs: Int, totalDurationMs: Long, onSearchClick: () -> Unit, themeMode: ThemeMode = ThemeMode.SYSTEM, onCycleTheme: () -> Unit = {}, onOpenPanel: () -> Unit = {}, modifier: Modifier = Modifier) {
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
            IconButton(onClick = onCycleTheme) {
                Icon(
                    imageVector = when (themeMode) {
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    },
                    contentDescription = "Theme: ${themeMode.name.lowercase()}",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onOpenPanel) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Logo → opens the right-side "more features" panel
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onOpenPanel() },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Image(
                    painter = painterResource(id = com.freqvox.R.drawable.brand_icon),
                    contentDescription = "Menu",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun HeroFeedCard(
    track: TrackEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        val artPath = track.artworkPath
        if (artPath != null) {
            // Album art fills the card; the card's signature purple→dark gradient is then
            // overlaid as a translucent scrim so the art reads through while the title/
            // artist at the bottom stay legible.
            AsyncImage(
                model = java.io.File(artPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF7C4DFF).copy(alpha = 0.25f),
                                Color(0xFF12121A).copy(alpha = 0.80f)
                            )
                        )
                    )
            )
        } else {
            // No art → keep the original solid gradient + faint monogram look.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF7C4DFF).copy(alpha = 0.3f), Color(0xFF12121A))
                        )
                    )
            )
            Text(
                text = track.title.take(1).uppercase(),
                fontSize = 180.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.03f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 40.dp)
            )
        }

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
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    onLongPress: () -> Unit = {},
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
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            TrackArtwork(track = track, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrentTrack) activeAccent else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    onTrackClick: (TrackEntity, List<TrackEntity>) -> Unit,
    onTagLongPress: (TrackEntity) -> Unit = {}
) {
    val verticalScrollConsumer = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): Offset {
                return if (kotlin.math.abs(available.y) > kotlin.math.abs(available.x)) {
                    Offset(available.x, 0f)
                } else {
                    Offset.Zero
                }
            }
        }
    }

    LazyRow(
        modifier = Modifier.nestedScroll(verticalScrollConsumer),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .combinedClickable(
                        onClick = { onTrackClick(track, queueContext) },
                        onLongClick = { onTagLongPress(track) }
                    )
            ) {
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    TrackArtwork(track = track, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(10.dp))
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
    }
}

/**
 * Shown in place of the feed when the library has no tracks. While a scan is running
 * it reassures the user the app is actively looking for music; once the scan finishes
 * empty it explains why and offers a manual rescan. This is the fix for "the scanner is
 * silent, so users think the app can't detect their music."
 */
@Composable
private fun LibraryStatusState(
    isScanning: Boolean,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                color = Color(0xFF7C4DFF),
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Scanning your library…",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Looking for music on your device",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No music found",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Make sure you have audio files on this device and that Freqvox has " +
                    "permission to access them.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onRescan,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C4DFF))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Rescan")
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

/**
 * Shows the track's real embedded cover art when it exists, falling back to the
 * generated [ProceduralArtwork] otherwise. The procedural pattern is drawn as the
 * base layer so it also shows through if the cached art file was evicted from the
 * cache dir (in which case AsyncImage simply fails to load over it).
 */
@Composable
fun TrackArtwork(track: TrackEntity, modifier: Modifier = Modifier) {
    Box(modifier) {
        ProceduralArtwork(track = track, modifier = Modifier.matchParentSize())
        val path = track.artworkPath
        if (path != null) {
            // Crop = zoom-to-fill. Off-aspect art (VidMate's 16:9 thumbnails) is scaled up
            // to cover the square and the slight overflow is cropped — fills cleanly with no
            // black letterbox bars. Square online covers are unaffected.
            AsyncImage(
                model = java.io.File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * Initials for the generated monogram fallback. Prefers the artist (the more
 * recognizable identity in a library view), falling back to the title, and to a
 * music note when neither yields a letter/digit (e.g. an all-symbol filename).
 * Returns at most two characters so it stays legible even in a 56dp thumbnail.
 */
fun monogramInitials(track: TrackEntity): String {
    val source = track.artist
        .takeIf { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
        ?: track.title
    val words = source.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "♪" // ♪
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/** Memoized per-track procedural parameters (seeded by title → stable per track). */
private class ProcParams(hash: Int) {
    val shapeType: Int
    val shapeAlpha: Float
    val scale: Float
    val offsetX: Float
    val offsetY: Float
    init {
        val r = java.util.Random(hash.toLong())
        shapeType = r.nextInt(3)
        shapeAlpha = 0.18f + r.nextFloat() * 0.16f
        scale = 0.5f + r.nextFloat() * 1.3f
        offsetX = -0.5f + r.nextFloat()
        offsetY = -0.5f + r.nextFloat()
    }
}

/**
 * Generated fallback art (gradient + texture + centered monogram), drawn entirely in a
 * single [Canvas] draw pass. Everything per-track is memoized and the monogram is rendered
 * via the native canvas — so there's NO subcomposition (no BoxWithConstraints) and no Text
 * layout node. That matters because this is the base layer of every thumbnail in the lists.
 */
@Composable
fun ProceduralArtwork(track: TrackEntity, modifier: Modifier = Modifier) {
    val params = remember(track.title) { ProcParams(track.title.hashCode()) }
    val gradient = remember(track.title) { getGradientForTrack(track.title) }
    val initials = remember(track.artist, track.title) { monogramInitials(track) }
    val monogramPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            alpha = 235 // ~0.92
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
            )
            setShadowLayer(8f, 0f, 2f, 0x59000000) // ~0.35 black
        }
    }

    Canvas(modifier = modifier.clipToBounds()) {
        drawRect(brush = gradient)

        val centerPoint = Offset(
            size.width / 2 + size.width * params.offsetX,
            size.height / 2 + size.height * params.offsetY
        )
        val radius = size.minDimension * params.scale
        when (params.shapeType) {
            0 -> {
                drawCircle(color = Color.White.copy(alpha = params.shapeAlpha), radius = radius, center = centerPoint)
                drawCircle(color = Color.Black.copy(alpha = params.shapeAlpha * 0.5f), radius = radius * 0.6f, center = Offset(centerPoint.x + radius * 0.3f, centerPoint.y - radius * 0.3f))
            }
            1 -> {
                val strokeWidth = size.minDimension * 0.4f
                drawLine(color = Color.Black.copy(alpha = params.shapeAlpha), start = Offset(-size.width * params.scale, size.height + size.height * params.scale), end = Offset(size.width + size.width * params.scale, -size.height * params.scale), strokeWidth = strokeWidth)
            }
            2 -> {
                drawCircle(
                    brush = Brush.radialGradient(colors = listOf(Color.White.copy(alpha = params.shapeAlpha), Color.Transparent), center = centerPoint, radius = radius),
                    radius = radius,
                    center = centerPoint
                )
            }
        }

        // Light scrim so the gradient stays muted behind the monogram.
        drawRect(color = Color(0xFF1A1A26).copy(alpha = 0.28f))

        // Centered monogram, drawn natively — scales with the tile via size.minDimension.
        monogramPaint.textSize = size.minDimension * 0.42f
        val fm = monogramPaint.fontMetrics
        drawContext.canvas.nativeCanvas.drawText(
            initials,
            size.width / 2f,
            size.height / 2f - (fm.ascent + fm.descent) / 2f,
            monogramPaint
        )
    }
}

@Composable
fun FeaturedBannerBlock(
    track: TrackEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        val artPath = track.artworkPath
        if (artPath != null) {
            // Album art fills the banner; horizontal scrim keeps the left-aligned text
            // legible (dark on the left) while the art shows through on the right.
            AsyncImage(
                model = java.io.File(artPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1A1A26).copy(alpha = 0.82f),
                                Color(0xFF6B3FA0).copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        } else {
            // No art → original solid gradient + faded typography bleeding off the edge.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6B3FA0), Color(0xFF1A1A26))
                        )
                    )
            )
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
        }

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
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
