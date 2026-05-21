package com.freqvox.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TagEntity
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
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    val tabOrder = remember { mutableStateListOf(*LibraryTab.values()) }
    var selectedTab by remember { mutableStateOf(LibraryTab.Songs) }
    var sortOrder by remember { mutableStateOf(SortOrder.Default) }
    var trackToAdd by remember { mutableStateOf<TrackEntity?>(null) }
    
    var expandedAlbumName by remember { mutableStateOf<String?>(null) }
    var expandedArtistName by remember { mutableStateOf<String?>(null) }
    var expandedFolderPath by remember { mutableStateOf<String?>(null) }

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
                Image(
                    painter = painterResource(id = com.freqvox.R.drawable.splash_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
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
                    itemsIndexed(sortedTracks, key = { _, track -> track.id }) { index, track ->
                        LibraryTrackItem(
                            track = track,
                            isCurrentTrack = currentTrackTitle == track.title,
                            isPlaying = isPlaying,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (currentTrackTitle == track.title) {
                                    playbackController.togglePlayPause()
                                } else {
                                    playbackController.playQueue(sortedTracks, index)
                                }
                            },
                            onDelete = { viewModel.deleteTrack(track.id) },
                            onAddToPlaylist = { trackToAdd = track }
                        )
                    }
                }

                LibraryTab.Albums -> {
                    val albums = remember(sortedTracks) { sortedTracks.groupBy { it.album } }
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
                                representativeTrack = albumTracks.first(),
                                isExpanded = expandedAlbumName == albumName,
                                expandedTracks = if (expandedAlbumName == albumName) albumTracks else emptyList(),
                                currentTrackTitle = currentTrackTitle,
                                onExpand = { expandedAlbumName = if (expandedAlbumName == albumName) null else albumName },
                                onPlay = { tracks, startIndex -> playbackController.playQueue(tracks, startIndex) }
                            )
                        }
                    }
                }

                LibraryTab.Artists -> {
                    val artists = remember(sortedTracks) { sortedTracks.groupBy { it.artist } }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                    ) {
                        items(artists.keys.toList()) { artistName ->
                            val artistTracks = artists[artistName]!!
                            ArtistItem(
                                name = artistName,
                                trackCount = artistTracks.size,
                                representativeTrack = artistTracks.first(),
                                isExpanded = expandedArtistName == artistName,
                                expandedTracks = if (expandedArtistName == artistName) artistTracks else emptyList(),
                                currentTrackTitle = currentTrackTitle,
                                onExpand = { expandedArtistName = if (expandedArtistName == artistName) null else artistName },
                                onPlay = { tracks, startIndex -> playbackController.playQueue(tracks, startIndex) }
                            )
                        }
                    }
                }

                LibraryTab.Folders -> {
                    val folders = remember(sortedTracks) {
                        sortedTracks.groupBy { track ->
                            track.folderPath
                                .replace("/storage/emulated/0/", "Internal Storage/")
                                .replace("/storage/emulated/1/", "SD Card/")
                                .replace("/storage/emulated/", "Storage ")
                        }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                    ) {
                        items(folders.keys.toList()) { folderName ->
                            val folderTracks = folders[folderName]!!
                            FolderItem(
                                path = folderName,
                                trackCount = folderTracks.size,
                                isExpanded = expandedFolderPath == folderName,
                                expandedTracks = if (expandedFolderPath == folderName) folderTracks else emptyList(),
                                currentTrackTitle = currentTrackTitle,
                                onExpand = { expandedFolderPath = if (expandedFolderPath == folderName) null else folderName },
                                onPlay = { tracks, startIndex -> playbackController.playQueue(tracks, startIndex) }
                            )
                        }
                    }
                }

                LibraryTab.Playlists -> PlaylistsTab(
                    playlists = allPlaylists,
                    allTags = allTags,
                    viewModel = viewModel,
                    playbackController = playbackController
                )
            }
        }
    }

    trackToAdd?.let { track ->
        SelectPlaylistDialog(
            playlists = allPlaylists.filter { it.type == "manual" },
            onSelect = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, track.id)
                trackToAdd = null
            },
            onDismiss = { trackToAdd = null }
        )
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null
) {
    val accent = Color(0xFF7C4DFF)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1E1E2E),
            title = { Text("Remove song", color = Color.White, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Remove \"${track.title}\" from your library?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete?.invoke()
                }) {
                    Text("Remove", color = Color(0xFFFF4D7C), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4D7C))
            }
        )
    }

    Row(
        modifier = modifier
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { if (onDelete != null) showDeleteDialog = true }
                )
            }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isCurrentTrack) accent else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(24.dp)
            )
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E2E))
                ) {
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Color.White) },
                            onClick = { 
                                showMenu = false
                                onAddToPlaylist.invoke()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.White)
                            }
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from Library", color = Color(0xFFFF4D7C)) },
                            onClick = { 
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4D7C))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumItem(
    name: String, 
    artist: String, 
    trackCount: Int, 
    representativeTrack: TrackEntity, 
    isExpanded: Boolean,
    expandedTracks: List<TrackEntity>,
    currentTrackTitle: String = "",
    onExpand: () -> Unit,
    onPlay: (List<TrackEntity>, Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onExpand),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                if (isExpanded) {
                    IconButton(
                        onClick = { onPlay(expandedTracks, 0) },
                        enabled = expandedTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) Color(0xFF7C4DFF) else Color.Gray)
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
            if (isExpanded) {
                ExpandedTrackList(tracks = expandedTracks, onPlay = onPlay, currentTrackTitle = currentTrackTitle)
            }
        }
    }
}

@Composable
fun ArtistItem(
    name: String, 
    trackCount: Int, 
    representativeTrack: TrackEntity, 
    isExpanded: Boolean,
    expandedTracks: List<TrackEntity>,
    currentTrackTitle: String = "",
    onExpand: () -> Unit,
    onPlay: (List<TrackEntity>, Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onExpand),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                if (isExpanded) {
                    IconButton(
                        onClick = { onPlay(expandedTracks, 0) },
                        enabled = expandedTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) Color(0xFF7C4DFF) else Color.Gray)
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
            if (isExpanded) {
                ExpandedTrackList(tracks = expandedTracks, onPlay = onPlay, currentTrackTitle = currentTrackTitle)
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<PlaylistEntity>,
    allTags: List<TagEntity>,
    viewModel: MainViewModel,
    playbackController: PlaybackController
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    var expandedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var expandedTracks by remember { mutableStateOf<List<TrackEntity>>(emptyList()) }
    
    // Virtual Playlists Data
    val allTracks by viewModel.allTracks.collectAsState()
    val taggedTrackIds by viewModel.taggedTrackIds.collectAsState()
    
    val likedTrackIds = taggedTrackIds["liked"] ?: emptySet()
    val likedTracks = remember(allTracks, likedTrackIds) { allTracks.filter { it.id in likedTrackIds } }
    val recentlyPlayed = remember(allTracks) { allTracks.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }.take(50) }
    val mostPlayed = remember(allTracks) { allTracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(50) }
    val leastPlayed = remember(allTracks) { allTracks.filter { it.playCount > 0 }.sortedBy { it.playCount }.take(50) }

    LaunchedEffect(expandedPlaylistId, playlists) {
        if (expandedPlaylistId == null || expandedPlaylistId!! < 0L) return@LaunchedEffect
        val playlist = playlists.find { it.id == expandedPlaylistId } ?: return@LaunchedEffect
        expandedTracks = viewModel.getTracksForPlaylist(playlist)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
        ) {
            // Virtual Playlists Section
            item {
                Text("Standard Collections", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp))
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -1, name = "Liked Songs", type = "virtual"),
                    isExpanded = expandedPlaylistId == -1L,
                    expandedTracks = if (expandedPlaylistId == -1L) likedTracks else emptyList(),
                    onExpand = { expandedPlaylistId = if (expandedPlaylistId == -1L) null else -1L },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -2, name = "Recently Played", type = "virtual"),
                    isExpanded = expandedPlaylistId == -2L,
                    expandedTracks = if (expandedPlaylistId == -2L) recentlyPlayed else emptyList(),
                    onExpand = { expandedPlaylistId = if (expandedPlaylistId == -2L) null else -2L },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -3, name = "Most Played", type = "virtual"),
                    isExpanded = expandedPlaylistId == -3L,
                    expandedTracks = if (expandedPlaylistId == -3L) mostPlayed else emptyList(),
                    onExpand = { expandedPlaylistId = if (expandedPlaylistId == -3L) null else -3L },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -4, name = "Least Played", type = "virtual"),
                    isExpanded = expandedPlaylistId == -4L,
                    expandedTracks = if (expandedPlaylistId == -4L) leastPlayed else emptyList(),
                    onExpand = { expandedPlaylistId = if (expandedPlaylistId == -4L) null else -4L },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }

            // Custom Playlists Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Playlists", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            }
            if (playlists.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No custom playlists yet", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        isExpanded = expandedPlaylistId == playlist.id,
                        expandedTracks = if (expandedPlaylistId == playlist.id) expandedTracks else emptyList(),
                        onExpand = {
                            expandedPlaylistId = if (expandedPlaylistId == playlist.id) null else playlist.id
                        },
                        onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                        onDelete = { playlistToDelete = playlist }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp),
            containerColor = Color(0xFF7C4DFF),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "New playlist")
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            allTags = allTags.distinctBy { it.name.lowercase() },
            onCreateManual = { name ->
                viewModel.createManualPlaylist(name)
                showCreateDialog = false
            },
            onCreateSmart = { name, tagIds ->
                viewModel.createDynamicPlaylist(name, tagIds)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = Color(0xFF1E1E2E),
            title = { Text("Delete playlist?", color = Color.White, fontWeight = FontWeight.SemiBold) },
            text = { Text("\"${playlist.name}\" will be permanently removed.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlaylist(playlist); playlistToDelete = null }) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
fun PlaylistItem(
    playlist: PlaylistEntity,
    isExpanded: Boolean,
    expandedTracks: List<TrackEntity>,
    currentTrackTitle: String = "",
    onExpand: () -> Unit,
    onPlay: (List<TrackEntity>, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val accent = Color(0xFF7C4DFF)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onExpand() }, onLongPress = { onDelete?.invoke() })
            },
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isExpanded && expandedTracks.isNotEmpty()) "${expandedTracks.size} tracks" else "Smart playlist",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
                if (isExpanded) {
                    IconButton(
                        onClick = { onPlay(expandedTracks, 0) },
                        enabled = expandedTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) accent else Color.Gray)
                    }
                }
                IconButton(onClick = { onExpand() }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            if (isExpanded) {
                ExpandedTrackList(tracks = expandedTracks, onPlay = { tracks, startIndex -> onPlay(tracks, startIndex) }, currentTrackTitle = currentTrackTitle)
            }
        }
    }
}


@Composable
fun CreatePlaylistDialog(
    allTags: List<TagEntity>,
    onCreateManual: (String) -> Unit,
    onCreateSmart: (String, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isSmartPlaylist by remember { mutableStateOf(false) }
    val selectedTagIds = remember { mutableStateListOf<Long>() }
    val accent = Color(0xFF7C4DFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2E),
        title = { Text("New Playlist", color = Color.White, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isSmartPlaylist,
                        onCheckedChange = { isSmartPlaylist = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Smart Playlist (Auto-updates via Tags)", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }

                if (isSmartPlaylist) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Include tracks tagged with:", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        gridItems(allTags) { tag ->
                            val isSelected = selectedTagIds.contains(tag.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedTagIds.remove(tag.id)
                                    else selectedTagIds.add(tag.id)
                                },
                                label = { Text(tag.name.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(tag.color).copy(alpha = 0.22f),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.06f),
                                    labelColor = Color.White.copy(alpha = 0.6f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = isSelected,
                                    selectedBorderColor = Color(tag.color).copy(alpha = 0.6f),
                                    borderColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank()) {
                        if (isSmartPlaylist) onCreateSmart(name.trim(), selectedTagIds.toList())
                        else onCreateManual(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create", color = if (name.isNotBlank()) accent else Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }
        }
    )
}

@Composable
fun FolderItem(
    path: String, 
    trackCount: Int, 
    isExpanded: Boolean,
    expandedTracks: List<TrackEntity>,
    currentTrackTitle: String = "",
    onExpand: () -> Unit,
    onPlay: (List<TrackEntity>, Int) -> Unit
) {
    val folderName = path.substringAfterLast("/").takeIf { it.isNotEmpty() } ?: path
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onExpand),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = folderName,
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
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isExpanded) {
                    IconButton(
                        onClick = { onPlay(expandedTracks, 0) },
                        enabled = expandedTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) Color(0xFF7C4DFF) else Color.Gray)
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
            if (isExpanded) {
                ExpandedTrackList(tracks = expandedTracks, onPlay = onPlay, currentTrackTitle = currentTrackTitle)
            }
        }
    }
}

@Composable
fun SelectPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onSelect: (PlaylistEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2E),
        title = { Text("Add to Playlist", color = Color.White, fontWeight = FontWeight.SemiBold) },
        text = {
            if (playlists.isEmpty()) {
                Text("You don't have any standard playlists yet.", color = Color.Gray, fontSize = 14.sp)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(playlist) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(playlist.name, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.White.copy(alpha = 0.6f)) }
        }
    )
}

@Composable
fun ExpandedTrackList(
    tracks: List<TrackEntity>,
    onPlay: (List<TrackEntity>, Int) -> Unit,
    currentTrackTitle: String = "",
    accent: Color = Color(0xFF7C4DFF)
) {
    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.07f), thickness = 1.dp)
    Spacer(modifier = Modifier.height(10.dp))
    if (tracks.isEmpty()) {
        Text(
            text = "No tracks inside.",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp
        )
    } else {
        tracks.take(100).forEachIndexed { index, track ->
            val isCurrent = track.title == currentTrackTitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlay(tracks, index) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.title,
                    color = if (isCurrent) accent else Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = track.artist,
                    color = if (isCurrent) accent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
            }
        }
        if (tracks.size > 100) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "+ ${tracks.size - 100} more (Play group to hear all)",
                color = accent.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
