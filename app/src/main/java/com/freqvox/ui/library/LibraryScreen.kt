package com.freqvox.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.home.ProceduralArtwork
import com.freqvox.ui.main.MainViewModel
import java.io.File

enum class LibraryTab { Songs, Albums, Artists, Folders, Playlists }

enum class SortOrder(val label: String) {
    Default("Default"),
    TitleAsc("Title A → Z"),
    TitleDesc("Title Z → A"),
    Artist("Artist"),
    DurationAsc("Shortest First"),
    DurationDesc("Longest First"),
    RecentlyAdded("Recently Added"),
    RecentlyPlayed("Recently Played"),
    MostPlayed("Most Played"),
    LeastPlayed("Least Played")
}

@Composable
fun LibraryScreen(
    playbackController: PlaybackController,
    onTrackClick: (TrackEntity) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val tracks by viewModel.allTracks.collectAsState()
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsState()
    val isPlaying by playbackController.isPlaying.collectAsState()

    val tabOrder = remember { mutableStateListOf(*LibraryTab.values()) }
    var selectedTab by remember { mutableStateOf(LibraryTab.Songs) }
    var sortOrder by remember { mutableStateOf(SortOrder.Default) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Collection",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tracks.size} Tracks",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reorderable tab pills — full width, scrolls freely
        DraggableTabRow(
            tabOrder = tabOrder,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sort button — right-aligned on its own row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            SortButton(sortOrder = sortOrder, onSortSelected = { sortOrder = it })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No music found in your library", color = Color.Gray)
            }
        } else {
            val sortedTracks = remember(tracks, sortOrder) {
                when (sortOrder) {
                    SortOrder.Default        -> tracks
                    SortOrder.TitleAsc       -> tracks.sortedBy { it.title.lowercase() }
                    SortOrder.TitleDesc      -> tracks.sortedByDescending { it.title.lowercase() }
                    SortOrder.Artist         -> tracks.sortedBy { it.artist.lowercase() }
                    SortOrder.DurationAsc    -> tracks.sortedBy { it.duration }
                    SortOrder.DurationDesc   -> tracks.sortedByDescending { it.duration }
                    SortOrder.RecentlyAdded  -> tracks.sortedByDescending { it.dateAdded }
                    SortOrder.RecentlyPlayed -> tracks.sortedByDescending { it.lastPlayed ?: 0L }
                    SortOrder.MostPlayed     -> tracks.sortedByDescending { it.playCount }
                    SortOrder.LeastPlayed    -> tracks.sortedBy { it.playCount }
                }
            }

            when (selectedTab) {
                LibraryTab.Songs -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                ) {
                    items(sortedTracks, key = { it.id }) { track ->
                        LibraryTrackItem(
                            track = track,
                            isCurrentTrack = currentTrackTitle == track.title,
                            isPlaying = isPlaying,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }

                LibraryTab.Albums -> {
                    val albums = sortedTracks.groupBy { it.album }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                    ) {
                        items(albums.keys.toList()) { albumName ->
                            val albumTracks = albums[albumName]!!
                            AlbumItem(
                                name = albumName,
                                artist = albumTracks.first().artist,
                                trackCount = albumTracks.size,
                                representativeTrack = albumTracks.first()
                            )
                        }
                    }
                }

                LibraryTab.Artists -> {
                    val artists = sortedTracks.groupBy { it.artist }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                    ) {
                        items(artists.keys.toList()) { artistName ->
                            ArtistItem(
                                name = artistName,
                                trackCount = artists[artistName]!!.size,
                                representativeTrack = artists[artistName]!!.first()
                            )
                        }
                    }
                }

                LibraryTab.Folders -> {
                    val folders = sortedTracks.groupBy {
                        File(it.path).parentFile?.name ?: "Root"
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                    ) {
                        items(folders.keys.toList()) { folderName ->
                            FolderItem(folderName, folders[folderName]!!.size)
                        }
                    }
                }

                LibraryTab.Playlists -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Playlists coming soon", color = Color.Gray)
                }
            }
        }
    }
}

// Long-press any pill and drag left/right to reorder tabs.
@Composable
fun DraggableTabRow(
    tabOrder: MutableList<LibraryTab>,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingTab by remember { mutableStateOf<LibraryTab?>(null) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    val itemWidths = remember { mutableStateMapOf<LibraryTab, Float>() }
    val density = LocalDensity.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        tabOrder.forEach { tab ->
            val isSelected = selectedTab == tab
            val isDragging = draggingTab == tab

            Box(
                modifier = Modifier
                    .onSizeChanged { size -> itemWidths[tab] = size.width.toFloat() }
                    .graphicsLayer {
                        scaleX = if (isDragging) 1.08f else 1f
                        scaleY = if (isDragging) 1.08f else 1f
                    }
                    .pointerInput(tab) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingTab = tab
                                totalDragX = 0f
                            },
                            onDragEnd = { draggingTab = null; totalDragX = 0f },
                            onDragCancel = { draggingTab = null; totalDragX = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                val currentIdx = tabOrder.indexOf(tab)
                                val threshold = (itemWidths[tab]
                                    ?: with(density) { 80.dp.toPx() }) * 0.65f

                                when {
                                    totalDragX > threshold && currentIdx < tabOrder.size - 1 -> {
                                        tabOrder[currentIdx] = tabOrder[currentIdx + 1]
                                        tabOrder[currentIdx + 1] = tab
                                        totalDragX -= threshold
                                    }
                                    totalDragX < -threshold && currentIdx > 0 -> {
                                        tabOrder[currentIdx] = tabOrder[currentIdx - 1]
                                        tabOrder[currentIdx - 1] = tab
                                        totalDragX += threshold
                                    }
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when {
                                isDragging  -> Color(0xFF7C4DFF).copy(alpha = 0.5f)
                                isSelected  -> Color(0xFF7C4DFF)
                                else        -> Color.White.copy(alpha = 0.07f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isDragging -> Color(0xFF7C4DFF).copy(alpha = 0.8f)
                                isSelected -> Color.Transparent
                                else       -> Color.White.copy(alpha = 0.12f)
                            },
                            shape = CircleShape
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = tab.name,
                        color = if (isSelected || isDragging) Color.White
                                else Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun SortButton(sortOrder: SortOrder, onSortSelected: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accent = Color(0xFF7C4DFF)

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.SwapVert,
                contentDescription = "Sort",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1E1E2E))
        ) {
            SortOrder.values().forEach { sort ->
                val isActive = sort == sortOrder
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sort.label,
                            color = if (isActive) accent else Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = { onSortSelected(sort); expanded = false },
                    leadingIcon = {
                        if (isActive) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White,
                        leadingIconColor = accent
                    )
                )
            }
        }
    }
}

@Composable
fun LibraryTrackItem(
    track: TrackEntity,
    isCurrentTrack: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    val accent = Color(0xFF7C4DFF)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrentTrack) accent.copy(alpha = 0.10f)
                else Color.White.copy(alpha = 0.04f)
            )
            .then(
                if (isCurrentTrack)
                    Modifier.border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            ProceduralArtwork(track = track, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrentTrack) accent else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (isCurrentTrack) accent else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AlbumItem(name: String, artist: String, trackCount: Int, representativeTrack: TrackEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            ProceduralArtwork(track = representativeTrack, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$artist • $trackCount tracks",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArtistItem(name: String, trackCount: Int, representativeTrack: TrackEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            ProceduralArtwork(track = representativeTrack, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$trackCount tracks",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun FolderItem(name: String, trackCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.07f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$trackCount tracks",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}
