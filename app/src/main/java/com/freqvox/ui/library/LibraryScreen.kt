package com.freqvox.ui.library

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDownOffAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.repository.AddTrackResult
import com.freqvox.media.MediaTrackDeleter
import com.freqvox.data.local.entities.PlaylistEntity
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.tag.formatTagDisplayName
import com.freqvox.ui.home.TrackArtwork
import com.freqvox.ui.main.MainViewModel
import com.freqvox.ui.search.SearchSheet
import kotlinx.coroutines.launch
import java.io.File

// NOTE: order here drives the initial pill order in the Library header.
// Playlists is intentionally second so it's one tap from the default (Songs).
// Users can still long-press + drag to reorder pills at runtime.
enum class LibraryTab { Songs, Playlists, Albums, Artists, Folders }

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
    onSearchClick: () -> Unit,
    onOpenPanel: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val tracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dislikedTrackIds by viewModel.dislikedTrackIds.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    // Pill order + selected pill are persisted (see LibraryUiPreferences) so they
    // survive leaving and re-entering the tab and an app restart. Unknown/missing
    // names fall back to the default order, and any tab absent from the saved order
    // (e.g. a newly added one) is appended so the row is always complete.
    val tabOrder = remember {
        val saved = viewModel.libraryTabOrder.mapNotNull { name ->
            LibraryTab.values().firstOrNull { it.name == name }
        }
        val complete = (saved + LibraryTab.values().filterNot { it in saved }).distinct()
        mutableStateListOf(*complete.toTypedArray())
    }
    var selectedTab by remember {
        mutableStateOf(
            viewModel.libraryLastTab
                ?.let { name -> LibraryTab.values().firstOrNull { it.name == name } }
                ?: LibraryTab.Songs
        )
    }
    var sortOrder by remember { mutableStateOf(SortOrder.Default) }

    // Side-panel "quick jump": when a playlist is tapped there, jump this screen to the
    // Playlists tab (the playlist itself is expanded via the ViewModel). Handles the case
    // where Library is already on screen; a fresh navigation reads the persisted tab.
    LaunchedEffect(Unit) {
        viewModel.openPlaylistTab.collect {
            selectedTab = LibraryTab.Playlists
            viewModel.setLibraryLastTab(LibraryTab.Playlists.name)
        }
    }

    var trackToAdd by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToDelete by remember { mutableStateOf<TrackEntity?>(null) }
    var deleteDialogStep by remember { mutableIntStateOf(0) }
    var pendingDeleteTrackId by remember { mutableStateOf<Long?>(null) }
    var showSearchSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteTrackId?.let { viewModel.completeTrackDeletion(it) }
            scope.launch {
                snackbarHostState.showSnackbar("Song deleted from device")
            }
        }
        pendingDeleteTrackId = null
    }

    var expandedAlbumName by remember { mutableStateOf<String?>(null) }
    var expandedArtistName by remember { mutableStateOf<String?>(null) }
    var expandedFolderPath by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    if (showSearchSheet) {
        SearchSheet(
            playbackController = playbackController,
            onDismiss = { showSearchSheet = false },
            viewModel = viewModel
        )
    }


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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Collection",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fvox.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tracks.size} Tracks",
                    fontSize = 14.sp,
                    color = Fvox.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSearchSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Fvox.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.cycleTheme() }) {
                    Icon(
                        imageVector = when (themeMode) {
                            com.freqvox.data.prefs.ThemeMode.LIGHT -> Icons.Default.LightMode
                            com.freqvox.data.prefs.ThemeMode.DARK -> Icons.Default.DarkMode
                            com.freqvox.data.prefs.ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        },
                        contentDescription = "Theme: ${themeMode.name.lowercase()}",
                        tint = Fvox.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onOpenPanel() },
                    shape = CircleShape,
                    color = Fvox.onSurface.copy(alpha = 0.1f)
                ) {
                    Image(
                        painter = painterResource(id = com.freqvox.R.drawable.brand_icon),
                        contentDescription = "Menu",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reorderable tab pills — full width, scrolls freely
        DraggableTabRow(
            tabOrder = tabOrder,
            selectedTab = selectedTab,
            onTabSelected = {
                selectedTab = it
                viewModel.setLibraryLastTab(it.name)
            },
            onOrderChanged = { order -> viewModel.setLibraryTabOrder(order.map { it.name }) },
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
                // Scan-aware: don't claim "no music" while a scan is still running.
                if (isScanning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Fvox.accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scanning your library…", color = Fvox.onSurface.copy(alpha = 0.7f))
                    }
                } else {
                    Text("No music found in your library", color = Fvox.onSurface.copy(alpha = 0.5f))
                }
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
                    SortOrder.LeastPlayed    -> tracks.sortedWith(compareBy<TrackEntity> { it.playCount }.thenBy { it.title })
                }
            }

            when (selectedTab) {
                LibraryTab.Songs -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                ) {
                    itemsIndexed(
                        sortedTracks,
                        key = { _, track -> track.id },
                        contentType = { _, _ -> "track" }
                    ) { index, track ->
                        LibraryTrackItem(
                            track = track,
                            isCurrentTrack = currentTrackTitle == track.title,
                            isPlaying = isPlaying,
                            isDisliked = track.id in dislikedTrackIds,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (currentTrackTitle == track.title) {
                                    playbackController.togglePlayPause()
                                } else {
                                    playbackController.playQueue(sortedTracks, index)
                                }
                            },
                            onRequestDelete = {
                                trackToDelete = track
                                deleteDialogStep = 1
                            },
                            onAddToPlaylist = { trackToAdd = track },
                            onToggleDislike = { viewModel.toggleDislikeForTrack(track.id) },
                            onPlayNext = {
                                playbackController.playTrackNext(track)
                                scope.launch { snackbarHostState.showSnackbar("Playing next") }
                            },
                            onAddToQueue = {
                                playbackController.addToQueue(track)
                                scope.launch { snackbarHostState.showSnackbar("Added to queue") }
                            }
                        )
                    }
                }

                LibraryTab.Albums -> {
                    val albums = remember(sortedTracks) { sortedTracks.groupBy { it.album } }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
                    ) {
                        items(albums.keys.toList(), key = { it }, contentType = { "album" }) { albumName ->
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
                        items(artists.keys.toList(), key = { it }, contentType = { "artist" }) { artistName ->
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
                        items(folders.keys.toList(), key = { it }, contentType = { "folder" }) { folderName ->
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
                scope.launch {
                    when (viewModel.addTrackToPlaylist(playlist.id, track.id)) {
                        AddTrackResult.Added ->
                            snackbarHostState.showSnackbar("Added to ${playlist.name}")
                        AddTrackResult.AlreadyInPlaylist ->
                            snackbarHostState.showSnackbar("Already in ${playlist.name}")
                    }
                    trackToAdd = null
                }
            },
            onCreateNew = { name ->
                viewModel.createPlaylistAndAddTrack(name, track.id)
                scope.launch { snackbarHostState.showSnackbar("Added to \"$name\"") }
                trackToAdd = null
            },
            onDismiss = { trackToAdd = null }
        )
    }

    trackToDelete?.let { track ->
        when (deleteDialogStep) {
            1 -> DeleteTrackStepOneDialog(
                track = track,
                onContinue = { deleteDialogStep = 2 },
                onDismiss = {
                    trackToDelete = null
                    deleteDialogStep = 0
                }
            )
            2 -> DeleteTrackStepTwoDialog(
                track = track,
                onConfirmDelete = {
                    pendingDeleteTrackId = track.id
                    trackToDelete = null
                    deleteDialogStep = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val request = MediaTrackDeleter.createDeleteRequest(context, track.id)
                        if (request != null) {
                            deleteLauncher.launch(request)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Could not delete file")
                            }
                            pendingDeleteTrackId = null
                        }
                    } else {
                        scope.launch {
                            if (MediaTrackDeleter.tryDeleteViaResolver(context, track.id)) {
                                viewModel.completeTrackDeletion(track.id)
                                snackbarHostState.showSnackbar("Song deleted from device")
                            } else {
                                snackbarHostState.showSnackbar("Could not delete file — check storage permission")
                            }
                            pendingDeleteTrackId = null
                        }
                    }
                },
                onDismiss = {
                    trackToDelete = null
                    deleteDialogStep = 0
                }
            )
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 100.dp)
    )
    }
}

// Long-press any pill and drag left/right to reorder tabs. Built on the same
// reorderable library as the queue (sh.calvin.reorderable) so the non-dragged
// pills glide out of the way with a spring instead of snapping — the old
// hand-rolled threshold-swap version moved items in instant jumps.
// [onOrderChanged] fires on drop so the new order can be persisted.
@Composable
fun DraggableTabRow(
    tabOrder: MutableList<LibraryTab>,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onOrderChanged: (List<LibraryTab>) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Fvox.accent
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val f = from.index
        val t = to.index
        if (f in tabOrder.indices && t in tabOrder.indices) {
            tabOrder.add(t, tabOrder.removeAt(f))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // How pills that aren't being dragged slide aside. Mirrors the queue's feel.
    val placementSpec = remember {
        spring<IntOffset>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
    ) {
        items(tabOrder, key = { it.name }) { tab ->
            ReorderableItem(
                state = reorderState,
                key = tab.name,
                animateItemModifier = Modifier.animateItem(placementSpec = placementSpec)
            ) { isDragging ->
                val isSelected = selectedTab == tab
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.08f else 1f,
                    label = "pillScale"
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .zIndex(if (isDragging) 1f else 0f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDragging -> accent.copy(alpha = 0.5f)
                                isSelected -> accent
                                else       -> Fvox.onSurface.copy(alpha = 0.07f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isDragging -> accent.copy(alpha = 0.8f)
                                isSelected -> Color.Transparent
                                else       -> Fvox.onSurface.copy(alpha = 0.12f)
                            },
                            shape = CircleShape
                        )
                        .longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = { onOrderChanged(tabOrder.toList()) }
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = tab.name,
                        color = if (isSelected || isDragging) Fvox.onAccent
                                else Fvox.onSurface.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SortButton(sortOrder: SortOrder, onSortSelected: (SortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accent = Fvox.accent

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Fvox.onSurface.copy(alpha = 0.07f))
                .border(1.dp, Fvox.onSurface.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.SwapVert,
                contentDescription = "Sort",
                tint = Fvox.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Fvox.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Fvox.surfaceVariant)
        ) {
            SortOrder.values().forEach { sort ->
                val isActive = sort == sortOrder
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sort.label,
                            color = if (isActive) accent else Fvox.onSurface.copy(alpha = 0.85f),
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
                        textColor = Fvox.onSurface,
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
    isDisliked: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRequestDelete: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onToggleDislike: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null
) {
    val accent = Fvox.accent
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrentTrack) accent.copy(alpha = 0.10f)
                else Fvox.onSurface.copy(alpha = 0.04f)
            )
            .then(
                if (isCurrentTrack)
                    Modifier.border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .pointerInput(Unit) {
                // Long-press now opens the same "more" menu the trailing icon
                // shows (Add to Playlist / Delete from device). Jumping straight
                // into the delete confirmation made the gesture feel destructive
                // and removed any way to access the other action via long-press.
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
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
            TrackArtwork(track = track, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDisliked) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Disliked",
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = track.title,
                    color = when {
                        isCurrentTrack -> accent
                        isDisliked     -> Fvox.onSurface.copy(alpha = 0.45f)
                        else           -> Fvox.onSurface
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Fvox.onSurface.copy(alpha = if (isDisliked) 0.3f else 0.5f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isCurrentTrack) accent else Fvox.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.size(24.dp)
            )
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Fvox.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Fvox.surfaceVariant)
                ) {
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text("Play next", color = Fvox.onSurface) },
                            onClick = {
                                showMenu = false
                                onPlayNext.invoke()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Fvox.onSurface)
                            }
                        )
                    }
                    if (onAddToQueue != null) {
                        DropdownMenuItem(
                            text = { Text("Add to queue", color = Fvox.onSurface) },
                            onClick = {
                                showMenu = false
                                onAddToQueue.invoke()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Fvox.onSurface)
                            }
                        )
                    }
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Fvox.onSurface) },
                            onClick = { 
                                showMenu = false
                                onAddToPlaylist.invoke()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Fvox.onSurface)
                            }
                        )
                    }
                    if (onToggleDislike != null) {
                        val dislikeAccent = Color(0xFF78909C)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isDisliked) "Remove dislike" else "Dislike track",
                                    color = if (isDisliked) dislikeAccent else Fvox.onSurface
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleDislike.invoke()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Outlined.ThumbDownOffAlt,
                                    contentDescription = null,
                                    tint = if (isDisliked) dislikeAccent else Fvox.onSurface
                                )
                            }
                        )
                    }
                    if (onRequestDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete from device…", color = Color(0xFFFF4D7C)) },
                            onClick = {
                                showMenu = false
                                onRequestDelete.invoke()
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
        color = Fvox.onSurface.copy(alpha = 0.04f),
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
                    TrackArtwork(track = representativeTrack, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = Fvox.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$artist • $trackCount tracks",
                        color = Fvox.onSurface.copy(alpha = 0.5f),
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
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) Fvox.accent else Fvox.onSurface.copy(alpha = 0.5f))
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Fvox.onSurface.copy(alpha = 0.4f)
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
        color = Fvox.onSurface.copy(alpha = 0.04f),
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
            TrackArtwork(track = representativeTrack, modifier = Modifier.fillMaxSize())
        }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        color = Fvox.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$trackCount tracks",
                        color = Fvox.onSurface.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
                if (isExpanded) {
                    IconButton(
                        onClick = { onPlay(expandedTracks, 0) },
                        enabled = expandedTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) Fvox.accent else Fvox.onSurface.copy(alpha = 0.5f))
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Fvox.onSurface.copy(alpha = 0.4f)
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
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToAddSongs by remember { mutableStateOf<PlaylistEntity?>(null) }
    val expandedPlaylistId by viewModel.expandedPlaylistId.collectAsStateWithLifecycle()
    val expandedTracksFromVm by viewModel.expandedPlaylistTracks.collectAsStateWithLifecycle()
    val playlistCounts by viewModel.playlistTrackCounts.collectAsStateWithLifecycle()

    // Virtual Playlists Data
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val taggedTrackIds by viewModel.taggedTrackIds.collectAsStateWithLifecycle()

    val likedTrackIds = taggedTrackIds["liked"] ?: emptySet()
    val likedTracks = remember(allTracks, likedTrackIds) { allTracks.filter { it.id in likedTrackIds } }
    val recentlyPlayed = remember(allTracks) { allTracks.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed }.take(50) }
    val mostPlayed = remember(allTracks) { allTracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(50) }
    val leastPlayed = remember(allTracks) {
        allTracks.sortedWith(
            compareBy<TrackEntity> { it.playCount }  // 0-play songs first, then fewest plays
                .thenBy { it.dateAdded }               // tie-break: oldest added gets priority (true neglect)
        ).take(50)
    }

    // Reactive tracks for the currently expanded custom playlist (id > 0).
    // For virtual playlists (id < 0) we still compute locally above.
    val expandedTracks: List<TrackEntity> = if (expandedPlaylistId != null && expandedPlaylistId!! > 0L) {
        expandedTracksFromVm
    } else {
        emptyList()
    }

    val listState = rememberLazyListState()
    // Bring a just-opened custom playlist into view — e.g. tapped from the side panel, which
    // sets it expanded. It sits below the standard collections, so without this it expands
    // off-screen and looks like nothing happened. Virtual playlists (id < 0) are already up top.
    // Offset 6 = "Standard Collections" header + 4 virtual rows + "Your Playlists" header.
    LaunchedEffect(expandedPlaylistId, playlists) {
        val id = expandedPlaylistId ?: return@LaunchedEffect
        if (id <= 0L) return@LaunchedEffect
        val idx = playlists.indexOfFirst { it.id == id }
        if (idx >= 0) listState.animateScrollToItem(6 + idx)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp)
        ) {
            // Virtual Playlists Section
            item {
                Text("Standard Collections", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp))
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -1, name = "Liked Songs", type = "virtual"),
                    trackCount = likedTracks.size,
                    isExpanded = expandedPlaylistId == -1L,
                    expandedTracks = if (expandedPlaylistId == -1L) likedTracks else emptyList(),
                    onExpand = { viewModel.setExpandedPlaylist(if (expandedPlaylistId == -1L) null else -1L) },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -2, name = "Recently Played", type = "virtual"),
                    trackCount = recentlyPlayed.size,
                    isExpanded = expandedPlaylistId == -2L,
                    expandedTracks = if (expandedPlaylistId == -2L) recentlyPlayed else emptyList(),
                    onExpand = { viewModel.setExpandedPlaylist(if (expandedPlaylistId == -2L) null else -2L) },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -3, name = "Most Played", type = "virtual"),
                    trackCount = mostPlayed.size,
                    isExpanded = expandedPlaylistId == -3L,
                    expandedTracks = if (expandedPlaylistId == -3L) mostPlayed else emptyList(),
                    onExpand = { viewModel.setExpandedPlaylist(if (expandedPlaylistId == -3L) null else -3L) },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }
            item {
                PlaylistItem(
                    playlist = PlaylistEntity(id = -4, name = "Least Played", type = "virtual"),
                    trackCount = leastPlayed.size,
                    isExpanded = expandedPlaylistId == -4L,
                    expandedTracks = if (expandedPlaylistId == -4L) leastPlayed else emptyList(),
                    onExpand = { viewModel.setExpandedPlaylist(if (expandedPlaylistId == -4L) null else -4L) },
                    onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                    onDelete = null
                )
            }

            // Custom Playlists Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Playlists", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            }
            if (playlists.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No custom playlists yet", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    val isExpandedThis = expandedPlaylistId == playlist.id
                    // Prefer the live expanded tracks (immediately accurate) when
                    // this row is the expanded one; otherwise read the cached count
                    // from the playlistTrackCounts map so collapsed rows show
                    // their real size instead of "Tap to load". null = map not
                    // populated yet — keep -1 sentinel so PlaylistItem still
                    // shows "Tap to load" briefly during the first emission
                    // rather than a misleading "Empty".
                    val customTrackCount: Int = when {
                        isExpandedThis && expandedTracks.isNotEmpty() -> expandedTracks.size
                        else -> playlistCounts[playlist.id] ?: -1
                    }
                    PlaylistItem(
                        playlist = playlist,
                        trackCount = customTrackCount,
                        isExpanded = isExpandedThis,
                        expandedTracks = if (isExpandedThis) expandedTracks else emptyList(),
                        onExpand = {
                            viewModel.setExpandedPlaylist(
                                if (isExpandedThis) null else playlist.id
                            )
                        },
                        onPlay = { tracks, startIndex -> if (tracks.isNotEmpty()) playbackController.playQueue(tracks, startIndex) },
                        onDelete = { playlistToDelete = playlist },
                        onRename = { playlistToRename = playlist },
                        // Track-membership editing only applies to MANUAL playlists. Smart
                        // (tag-query) playlists derive their contents from tags, so adding/
                        // removing individual tracks would silently do nothing — don't offer it.
                        onRemoveTrack = if (playlist.type == "manual") {
                            { track -> viewModel.removeTrackFromPlaylist(playlist.id, track.id) }
                        } else null,
                        onAddSongs = if (playlist.type == "manual") {
                            { playlistToAddSongs = playlist }
                        } else null
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp),
            containerColor = Fvox.accent,
            contentColor = Fvox.onSurface
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
            containerColor = Fvox.surfaceVariant,
            title = { Text("Delete playlist?", color = Fvox.onSurface, fontWeight = FontWeight.SemiBold) },
            text = { Text("\"${playlist.name}\" will be permanently removed.", color = Fvox.onSurface.copy(alpha = 0.7f), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlaylist(playlist); playlistToDelete = null }) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = Fvox.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    playlistToRename?.let { playlist ->
        var newName by remember(playlist.id) { mutableStateOf(playlist.name) }
        val accent = Fvox.accent
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = Fvox.surfaceVariant,
            title = { Text("Rename playlist", color = Fvox.onSurface, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Fvox.onSurface.copy(alpha = 0.2f),
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Fvox.onSurface.copy(alpha = 0.5f),
                        focusedTextColor = Fvox.onSurface,
                        unfocusedTextColor = Fvox.onSurface,
                        cursorColor = accent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.renamePlaylist(playlist.id, newName.trim()); playlistToRename = null },
                    enabled = newName.isNotBlank()
                ) { Text("Save", color = if (newName.isNotBlank()) accent else Fvox.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel", color = Fvox.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    playlistToAddSongs?.let { playlist ->
        AddTracksToPlaylistDialog(
            playlistName = playlist.name,
            allTracks = allTracks,
            existingTrackIds = expandedTracks.map { it.id }.toSet(),
            onAdd = { track -> viewModel.addTrackToPlaylistAsync(playlist.id, track.id) },
            onDismiss = { playlistToAddSongs = null }
        )
    }
}

/**
 * In-playlist song picker. Lists library tracks with a search box; tapping a track adds
 * it to the playlist immediately (the caller persists it) and the row flips to a check.
 * Tracks already in the playlist are shown checked and non-tappable.
 */
@Composable
fun AddTracksToPlaylistDialog(
    playlistName: String,
    allTracks: List<TrackEntity>,
    existingTrackIds: Set<Long>,
    onAdd: (TrackEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = Fvox.accent
    var query by remember { mutableStateOf("") }
    val added = remember { mutableStateListOf<Long>() }
    val filtered = remember(query, allTracks) {
        if (query.isBlank()) allTracks
        else allTracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surfaceVariant,
        title = { Text("Add to $playlistName", color = Fvox.onSurface, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search songs") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Fvox.onSurface.copy(alpha = 0.2f),
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Fvox.onSurface.copy(alpha = 0.5f),
                        focusedTextColor = Fvox.onSurface,
                        unfocusedTextColor = Fvox.onSurface,
                        cursorColor = accent
                    )
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.id }) { track ->
                        val isIn = track.id in existingTrackIds || track.id in added
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isIn) { onAdd(track); added.add(track.id) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    color = Fvox.onSurface.copy(alpha = if (isIn) 0.4f else 0.9f),
                                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    track.artist,
                                    color = Fvox.onSurface.copy(alpha = 0.4f),
                                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                if (isIn) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = if (isIn) "Added" else "Add",
                                tint = if (isIn) Fvox.onSurface.copy(alpha = 0.4f) else accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = accent, fontWeight = FontWeight.SemiBold) }
        }
    )
}

@Composable
fun PlaylistItem(
    playlist: PlaylistEntity,
    trackCount: Int = -1,    // -1 = unknown (custom playlists before expansion)
    isExpanded: Boolean,
    expandedTracks: List<TrackEntity>,
    currentTrackTitle: String = "",
    onExpand: () -> Unit,
    onPlay: (List<TrackEntity>, Int) -> Unit,
    onDelete: (() -> Unit)? = null,
    // Editing affordances — only wired for custom (manual) playlists.
    onRename: (() -> Unit)? = null,
    onRemoveTrack: ((TrackEntity) -> Unit)? = null,
    onAddSongs: (() -> Unit)? = null
) {
    val accent = Fvox.accent

    // Resolved count: prefer live expanded count, fall back to provided trackCount
    val resolvedCount = if (isExpanded && expandedTracks.isNotEmpty()) expandedTracks.size else trackCount
    val countLabel = when {
        resolvedCount > 0 -> "${resolvedCount} song${if (resolvedCount == 1) "" else "s"}"
        resolvedCount == 0 -> "Empty"
        else -> "Tap to load"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onExpand() }, onLongPress = { onDelete?.invoke() })
            },
        color = Fvox.onSurface.copy(alpha = 0.04f),
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
                        color = Fvox.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countLabel,
                        color = Fvox.onSurface.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
                if (isExpanded && onRename != null) {
                    IconButton(onClick = { onRename() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename playlist", tint = Fvox.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
                if (isExpanded) {
                    IconButton(
                        onClick = { onPlay(expandedTracks, 0) },
                        enabled = expandedTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) accent else Fvox.onSurface.copy(alpha = 0.5f))
                    }
                }
                IconButton(onClick = { onExpand() }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Fvox.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            if (isExpanded) {
                ExpandedTrackList(
                    tracks = expandedTracks,
                    onPlay = { tracks, startIndex -> onPlay(tracks, startIndex) },
                    currentTrackTitle = currentTrackTitle,
                    onRemoveTrack = onRemoveTrack,
                    onAddSongs = onAddSongs
                )
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
    val accent = Fvox.accent

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surfaceVariant,
        title = { Text("New Playlist", color = Fvox.onSurface, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Fvox.onSurface.copy(alpha = 0.2f),
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Fvox.onSurface.copy(alpha = 0.5f),
                        focusedTextColor = Fvox.onSurface,
                        unfocusedTextColor = Fvox.onSurface,
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
                    Text("Smart Playlist (Auto-updates via Tags)", color = Fvox.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                }

                if (isSmartPlaylist) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Include tracks tagged with:", color = Fvox.onSurface.copy(alpha = 0.55f), fontSize = 12.sp)
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
                                label = { Text(formatTagDisplayName(tag.name), fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(tag.color).copy(alpha = 0.22f),
                                    selectedLabelColor = Fvox.onSurface,
                                    containerColor = Fvox.onSurface.copy(alpha = 0.06f),
                                    labelColor = Fvox.onSurface.copy(alpha = 0.6f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = isSelected,
                                    selectedBorderColor = Color(tag.color).copy(alpha = 0.6f),
                                    borderColor = Fvox.onSurface.copy(alpha = 0.1f)
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
                Text("Create", color = if (name.isNotBlank()) accent else Fvox.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Fvox.onSurface.copy(alpha = 0.6f)) }
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
        color = Fvox.onSurface.copy(alpha = 0.04f),
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
            color = Fvox.onSurface.copy(alpha = 0.07f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Fvox.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folderName,
                        color = Fvox.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$trackCount tracks",
                        color = Fvox.onSurface.copy(alpha = 0.5f),
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
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = if (expandedTracks.isNotEmpty()) Fvox.accent else Fvox.onSurface.copy(alpha = 0.5f))
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Fvox.onSurface.copy(alpha = 0.4f)
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
    onDismiss: () -> Unit,
    // When provided, the dialog can create a new playlist inline (and the caller adds
    // the track to it) — so "no playlists yet" isn't a dead end.
    onCreateNew: ((String) -> Unit)? = null
) {
    val accent = Fvox.accent
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surfaceVariant,
        title = { Text("Add to Playlist", color = Fvox.onSurface, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                if (onCreateNew != null) {
                    if (creating) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            label = { Text("New playlist name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = Fvox.onSurface.copy(alpha = 0.2f),
                                focusedLabelColor = accent,
                                unfocusedLabelColor = Fvox.onSurface.copy(alpha = 0.5f),
                                focusedTextColor = Fvox.onSurface,
                                unfocusedTextColor = Fvox.onSurface,
                                cursorColor = accent
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { creating = true }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = accent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Create new playlist", color = accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        if (playlists.isNotEmpty()) {
                            HorizontalDivider(color = Fvox.onSurface.copy(alpha = 0.08f))
                        }
                    }
                }
                if (!creating) {
                    if (playlists.isEmpty()) {
                        if (onCreateNew == null) {
                            Text("You don't have any standard playlists yet.", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(playlists, key = { it.id }) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(playlist) }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Fvox.onSurface.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(playlist.name, color = Fvox.onSurface, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    onClick = { onCreateNew?.invoke(newName.trim()) },
                    enabled = newName.isNotBlank()
                ) { Text("Create", color = if (newName.isNotBlank()) accent else Fvox.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold) }
            } else {
                TextButton(onClick = onDismiss) { Text("Close", color = Fvox.onSurface.copy(alpha = 0.6f)) }
            }
        }
    )
}

@Composable
fun ExpandedTrackList(
    tracks: List<TrackEntity>,
    onPlay: (List<TrackEntity>, Int) -> Unit,
    currentTrackTitle: String = "",
    accent: Color = Fvox.accent,
    // When set (custom playlists), each row gets a remove button and an "Add songs"
    // action appears at the bottom. Null for read-only groups (albums/artists/folders).
    onRemoveTrack: ((TrackEntity) -> Unit)? = null,
    onAddSongs: (() -> Unit)? = null
) {
    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = Fvox.onSurface.copy(alpha = 0.07f), thickness = 1.dp)
    Spacer(modifier = Modifier.height(10.dp))
    if (tracks.isEmpty()) {
        Text(
            text = "No tracks inside.",
            color = Fvox.onSurface.copy(alpha = 0.35f),
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
                    color = if (isCurrent) accent else Fvox.onSurface.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = track.artist,
                    color = if (isCurrent) accent.copy(alpha = 0.7f) else Fvox.onSurface.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                if (onRemoveTrack != null) {
                    IconButton(
                        onClick = { onRemoveTrack(track) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove from playlist",
                            tint = Fvox.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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
    if (onAddSongs != null) {
        Spacer(modifier = Modifier.height(10.dp))
        // Filled accent chip with white text — readable on the dark card (the plain
        // purple-on-dark text was too low-contrast).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.22f))
                .clickable { onAddSongs() }
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Fvox.onSurface, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add songs", color = Fvox.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
