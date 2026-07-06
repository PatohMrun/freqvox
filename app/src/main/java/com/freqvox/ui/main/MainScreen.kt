package com.freqvox.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.components.LocalAnimatedVisibilityScope
import com.freqvox.ui.components.LocalSharedTransitionScope
import com.freqvox.ui.components.MiniPlayer
import com.freqvox.ui.home.HomeScreen
import com.freqvox.ui.library.LibraryScreen
import com.freqvox.ui.navigation.Screen
import com.freqvox.ui.navigation.bottomNavScreens
import com.freqvox.ui.player.PlayerScreen
import com.freqvox.ui.player.components.TagOverlay
import com.freqvox.ui.radio.RadioScreen
import com.freqvox.ui.search.SearchScreen
import com.freqvox.ui.theme.dynamic.DynamicTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    playbackController: PlaybackController,
    isLight: Boolean = false,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentTrackId by playbackController.currentTrackId.collectAsStateWithLifecycle()
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsStateWithLifecycle()
    val currentArtist by playbackController.currentArtist.collectAsStateWithLifecycle()
    val isPlaying by playbackController.isPlaying.collectAsStateWithLifecycle()
    val currentArtworkUri by playbackController.currentArtworkUri.collectAsStateWithLifecycle()
    val shuffleEnabled by playbackController.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by playbackController.repeatMode.collectAsStateWithLifecycle()
    val sleepTimerActive by playbackController.isTimerRunning.collectAsStateWithLifecycle()
    val sleepTimerTimeLeft by playbackController.timerTimeLeft.collectAsStateWithLifecycle()
    val stopAfterTrack by playbackController.stopAfterCurrentTrack.collectAsStateWithLifecycle()
    val stopAfterAlbum by playbackController.stopAfterAlbum.collectAsStateWithLifecycle()
    val queue by playbackController.queue.collectAsStateWithLifecycle()
    val currentQueueIndex by playbackController.currentQueueIndex.collectAsStateWithLifecycle()
    val playbackSpeed by playbackController.playbackSpeed.collectAsStateWithLifecycle()

    val isLiked by viewModel.isCurrentTrackLiked.collectAsStateWithLifecycle()
    val isDisliked by viewModel.isCurrentTrackDisliked.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val currentTrackTags by viewModel.currentTrackTags.collectAsStateWithLifecycle()

    // Data for the right-side panel + the sheets it opens.
    val totalSongs by viewModel.totalSongs.collectAsStateWithLifecycle()
    val totalDuration by viewModel.totalDuration.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val listeningStats by viewModel.listeningStats.collectAsStateWithLifecycle()

    // Header stories — hosted here (not in HomeScreen) so the ring works from every tab and
    // seen-state stays on ONE MainViewModel instance (each nav destination gets its own).
    val hasUnseenStory by viewModel.hasUnseenStory.collectAsStateWithLifecycle()
    val activeStoryDrop by viewModel.activeStoryDrop.collectAsStateWithLifecycle()
    val activeTracks by viewModel.activeTracks.collectAsStateWithLifecycle()
    val taggedTrackIds by viewModel.taggedTrackIds.collectAsStateWithLifecycle()
    val availableFolders by viewModel.availableFolders.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()

    // Online album-art consent + settings.
    val showArtworkConsent by viewModel.showArtworkConsent.collectAsStateWithLifecycle()
    val artworkRetrying by viewModel.artworkRetrying.collectAsStateWithLifecycle()
    val artworkOnlineEnabled by viewModel.artworkOnlineEnabled.collectAsStateWithLifecycle()
    val artworkAllowCellular by viewModel.artworkAllowCellular.collectAsStateWithLifecycle()
    val artworkAllowMeteredWifi by viewModel.artworkAllowMeteredWifi.collectAsStateWithLifecycle()
    val vidmateLinkedCount by viewModel.vidmateLinkedCount.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var showTagSheet by remember { mutableStateOf(false) }
    var searchFocusTrigger by remember { mutableStateOf(0L) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Right-side "more features" panel + the sheets it launches.
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showStatsSheet by remember { mutableStateOf(false) }
    var entityStats by remember {
        mutableStateOf<Triple<String, String, List<com.freqvox.data.local.entities.TrackEntity>>?>(null)
    }
    var showFoldersSheet by remember { mutableStateOf(false) }
    var showTagManagerSheet by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var showCrossfadeSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showArtworkSheet by remember { mutableStateOf(false) }
    var showAccentSheet by remember { mutableStateOf(false) }
    var showDuplicatesSheet by remember { mutableStateOf(false) }
    var showProfilesSheet by remember { mutableStateOf(false) }
    val openPanel: () -> Unit = { scope.launch { drawerState.open() } }

    var showStory by remember { mutableStateOf(false) }
    var previewStory by remember { mutableStateOf<com.freqvox.ui.story.StoryDrop?>(null) }
    val storyContext = androidx.compose.ui.platform.LocalContext.current
    // One handler for every story CTA, shared by the real viewer and the QA preview.
    val handleStoryAction: (com.freqvox.ui.story.StoryActionType) -> Unit = { action ->
        when (action) {
            com.freqvox.ui.story.StoryActionType.OPEN_CROSSFADE -> showCrossfadeSheet = true
            com.freqvox.ui.story.StoryActionType.OPEN_PROFILES -> showProfilesSheet = true
            com.freqvox.ui.story.StoryActionType.OPEN_ACCENT -> showAccentSheet = true
            com.freqvox.ui.story.StoryActionType.OPEN_STATS -> showStatsSheet = true
            com.freqvox.ui.story.StoryActionType.OPEN_ARTWORK -> showArtworkSheet = true
            com.freqvox.ui.story.StoryActionType.OPEN_TAGS -> showTagManagerSheet = true
            com.freqvox.ui.story.StoryActionType.OPEN_DUPLICATES -> showDuplicatesSheet = true
            com.freqvox.ui.story.StoryActionType.PLAY_DAILY_MIX -> {
                val ed = java.time.LocalDate.now().toEpochDay()
                com.freqvox.ui.home.generateDailyMixes(activeTracks, taggedTrackIds, ed)
                    .firstOrNull()?.takeIf { it.tracks.isNotEmpty() }
                    ?.let { playbackController.playQueue(it.tracks, 0) }
            }
            com.freqvox.ui.story.StoryActionType.OPEN_SEARCH -> {
                navController.navigate(Screen.Search.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            com.freqvox.ui.story.StoryActionType.NONE -> {}
        }
    }

    // Sync current track to ViewModel using the exposed ID for reliability
    LaunchedEffect(currentTrackId) {
        viewModel.updateCurrentTrackById(currentTrackId)
    }

    // Auto-collapse the player overlay if the track disappears
    LaunchedEffect(currentTrackTitle) {
        if (currentTrackTitle == "No track selected") showPlayer = false
    }

    // Dislike feedback: snackbar for every toggle + auto-skip when the user
    // dislikes the track that's currently playing. Without the skip, the song
    // they just rejected keeps playing and the button feels like it did
    // nothing. Snackbar message follows the resulting state.
    LaunchedEffect(viewModel, playbackController) {
        viewModel.dislikeEvents.collect { event ->
            if (event.isNowDisliked && event.isCurrentTrack) {
                playbackController.skipNext()
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            val message = if (event.isNowDisliked) {
                "Disliked. Track hidden from your feed."
            } else {
                "Dislike removed."
            }
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    // Generic snackbar channel — used by search surfaces (which have no host
    // of their own) for "Added to <playlist>" / "Song deleted" feedback.
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    // NOTE: BackHandler is intentionally NOT placed here. NavHost (composed inside
    // the Scaffold below) registers its own OnBackPressedCallback during composition,
    // and OnBackPressedDispatcher fires LIFO — last registered wins. If we add our
    // BackHandler here at MainScreen scope, NavHost's callback fires first and pops
    // the bottom-nav back stack (e.g. Library -> Home) before our handler is even
    // consulted, which is why dismissing the player used to land you on Home.
    // We register it INSIDE the player's AnimatedVisibility block instead.

    val showMiniPlayer = currentTrackTitle != "No track selected"

    val snackbarBottomPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = when {
            showPlayer       -> 36.dp
            showMiniPlayer   -> 160.dp
            else             -> 96.dp
        },
        animationSpec = tween(220),
        label = "snackbarBottomPadding"
    )

    val fixedAccentEnabled by viewModel.fixedAccentEnabled.collectAsStateWithLifecycle()
    val fixedAccentColor by viewModel.fixedAccentColor.collectAsStateWithLifecycle()
    val fixedAccent = if (fixedAccentEnabled) androidx.compose.ui.graphics.Color(fixedAccentColor) else null

    DynamicTheme(artworkPath = currentArtworkUri?.path, trackTitle = currentTrackTitle, isLight = isLight, fixedAccent = fixedAccent) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient background is its own composable so MainScreen scope doesn't
            // recompose when DynamicTheme animates colors mid-track-change.
            DynamicGradientBackground(
                trackTitle = currentTrackTitle,
                modifier = Modifier.fillMaxSize()
            )
            com.freqvox.ui.components.RightNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen || !showPlayer,
                drawerContent = {
                    com.freqvox.ui.components.AppSidePanel(
                        viewModel = viewModel,
                        totalSongs = totalSongs,
                        totalDurationMs = totalDuration,
                        playlists = allPlaylists,
                        tags = allTags,
                        onOpenStats = { scope.launch { drawerState.close() }; showStatsSheet = true },
                        onOpenEqualizer = { scope.launch { drawerState.close() }; showEqSheet = true },
                        onOpenCrossfade = { scope.launch { drawerState.close() }; showCrossfadeSheet = true },
                        onOpenSleep = { scope.launch { drawerState.close() }; showSleepSheet = true },
                        onOpenFolders = { scope.launch { drawerState.close() }; showFoldersSheet = true },
                        onOpenTags = { scope.launch { drawerState.close() }; showTagManagerSheet = true },
                        onOpenArtwork = { scope.launch { drawerState.close() }; showArtworkSheet = true },
                        onOpenAccent = { scope.launch { drawerState.close() }; showAccentSheet = true },
                        onOpenDuplicates = { scope.launch { drawerState.close() }; showDuplicatesSheet = true },
                        onOpenProfiles = { scope.launch { drawerState.close() }; showProfilesSheet = true },
                        onRescan = {
                            scope.launch { drawerState.close() }
                            viewModel.scanLibrary()
                            viewModel.postSnackbar("Rescanning library…")
                        },
                        onPlaylistClick = { pl ->
                            // Quick-jump: OPEN the playlist in the Library (Playlists tab,
                            // expanded) instead of immediately blasting playback. Lets the
                            // user see the tracks and decide, which a bare name-tap shouldn't
                            // pre-empt.
                            scope.launch { drawerState.close() }
                            viewModel.openPlaylistFromSidebar(pl.id)
                            viewModel.setLibraryLastTab(com.freqvox.ui.library.LibraryTab.Playlists.name)
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onTagClick = { tag ->
                            scope.launch {
                                drawerState.close()
                                val ids = viewModel.taggedTrackIds.value[tag.name.lowercase()] ?: emptySet()
                                val tracks = viewModel.allTracks.value.filter { it.id in ids }.shuffled()
                                if (tracks.isNotEmpty()) playbackController.playQueue(tracks, 0)
                            }
                        }
                    )
                }
            ) {
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                val sharedScope = this
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            Column {
                                AnimatedVisibility(
                                    visible = showMiniPlayer && !showPlayer,
                                    // Fade in/out matched to the 420ms shared-bounds morph so the
                                    // mini bar's exit and the player's enter run as one motion.
                                    enter = fadeIn(tween(420, easing = FastOutSlowInEasing)),
                                    exit = fadeOut(tween(420, easing = FastOutSlowInEasing))
                                ) {
                                    CompositionLocalProvider(
                                        LocalSharedTransitionScope provides sharedScope,
                                        LocalAnimatedVisibilityScope provides this@AnimatedVisibility
                                    ) {
                                        MiniPlayer(
                                            playbackController = playbackController,
                                            onMiniPlayerClick = { showPlayer = true },
                                            onSwipeUpToExpand = { showPlayer = true }
                                        )
                                    }
                                }

                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                                    contentColor = com.freqvox.ui.theme.Fvox.onSurface
                                ) {
                                    bottomNavScreens.forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon, contentDescription = null) },
                                            label = { Text(screen.title) },
                                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                            onClick = {
                                                val isCurrentlySelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                                if (isCurrentlySelected && screen.route == Screen.Search.route) {
                                                    searchFocusTrigger = System.currentTimeMillis()
                                                } else {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Fvox.accent,
                                                selectedTextColor = Fvox.accent,
                                                unselectedIconColor = Fvox.onSurface.copy(alpha = 0.7f),
                                                unselectedTextColor = Fvox.onSurface.copy(alpha = 0.7f),
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route,
                                enterTransition = { slideInHorizontally(animationSpec = tween(150), initialOffsetX = { 50 }) + fadeIn(animationSpec = tween(150)) },
                                exitTransition = { slideOutHorizontally(animationSpec = tween(150), targetOffsetX = { -50 }) + fadeOut(animationSpec = tween(150)) },
                                popEnterTransition = { slideInHorizontally(animationSpec = tween(150), initialOffsetX = { -50 }) + fadeIn(animationSpec = tween(150)) },
                                popExitTransition = { slideOutHorizontally(animationSpec = tween(150), targetOffsetX = { 50 }) + fadeOut(animationSpec = tween(150)) }
                            ) {
                                composable(Screen.Home.route) {
                                    HomeScreen(
                                        playbackController = playbackController,
                                        onOpenPanel = openPanel,
                                        onTrackClick = { track, contextTracks ->
                                            val currentTitle = playbackController.currentTrackTitle.value
                                            if (currentTitle == track.title) {
                                                playbackController.togglePlayPause()
                                            } else {
                                                val startIndex = contextTracks.indexOf(track).coerceAtLeast(0)
                                                playbackController.playQueue(contextTracks, startIndex)
                                            }
                                        },
                                        hasUnseenStory = hasUnseenStory,
                                        onOpenStory = { showStory = true },
                                        onLongPressStory = {
                                            previewStory = com.freqvox.ui.story.StoryContent.previewDrop(
                                                listeningStats, java.time.LocalDate.now().toEpochDay()
                                            )
                                        }
                                    )
                                }
                                composable(Screen.Search.route) {
                                    SearchScreen(
                                        playbackController = playbackController,
                                        focusTrigger = searchFocusTrigger,
                                        onFocusConsumed = { searchFocusTrigger = 0L }
                                    )
                                }
                                composable(Screen.Library.route) {
                                    LibraryScreen(
                                        playbackController = playbackController,
                                        onOpenPanel = openPanel,
                                        hasUnseenStory = hasUnseenStory,
                                        onOpenStory = { showStory = true },
                                        onTrackClick = { track ->
                                            val currentTitle = playbackController.currentTrackTitle.value
                                            if (currentTitle == track.title) {
                                                playbackController.togglePlayPause()
                                            } else {
                                                playbackController.playMediaUri(track.path, track.title, track.artist, track.id.toString(), track.artworkPath)
                                            }
                                        },
                                        onSearchClick = {
                                            navController.navigate(Screen.Search.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                                composable(Screen.Radio.route) {
                                    RadioScreen(
                                        playbackController = playbackController,
                                        onStationClick = { station ->
                                            playbackController.playMediaUri(station.url, station.name, "Radio", station.url)
                                        }
                                    )
                                }
                            }

                            if (showTagSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { showTagSheet = false },
                                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                    containerColor = com.freqvox.ui.theme.Fvox.surface,
                                    dragHandle = { BottomSheetDefaults.DragHandle() },
                                    contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
                                ) {
                                    TagOverlay(
                                        tags = allTags,
                                        selectedTagIds = currentTrackTags.map { it.id },
                                        onTagClick = { tag ->
                                            viewModel.toggleTagForCurrentTrack(tag.id)
                                        },
                                        onCreateCustomTag = { name ->
                                            viewModel.createCustomTag(name)
                                        },
                                        onClose = { showTagSheet = false },
                                        isDisliked = isDisliked,
                                        onToggleDislike = { viewModel.toggleDislike() },
                                        modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Closing the right-side panel takes priority over nav back. Registered
                    // after NavHost so the dispatcher (LIFO) routes Back here while the panel is
                    // open — closing it, instead of popping the nav stack (which finished the app).
                    BackHandler(enabled = drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    }

                    // Full-screen player overlay — appears on top of the Scaffold via SharedTransitionLayout.
                    // Fade duration is locked to the 420ms shared-bounds morph so the rectangle's
                    // expansion and the player's appearance hit "fully visible" on the same frame.
                    // Previously these were 280ms / 220ms, causing the player to look "done" while
                    // the bounds were still growing — that's what made it feel like two screens
                    // crossfading instead of one unified expansion.
                    AnimatedVisibility(
                        visible = showPlayer,
                        enter = fadeIn(tween(420, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(420, easing = FastOutSlowInEasing))
                    ) {
                        // Registered here so this callback is added to OnBackPressedDispatcher
                        // AFTER NavHost's callback (which composes earlier in the tree). Because
                        // the dispatcher iterates LIFO, ours fires first when the player is open,
                        // dismissing the player without ever touching the nav back stack.
                        BackHandler(enabled = showPlayer) { showPlayer = false }

                        CompositionLocalProvider(
                            LocalSharedTransitionScope provides sharedScope,
                            LocalAnimatedVisibilityScope provides this@AnimatedVisibility
                        ) {
                            // NO background here on purpose. PlayerScreen's root Column carries the
                            // dark background INSIDE the shared-bounds container, so the dark fill
                            // grows from the mini-player's rect to fullscreen along with the morph.
                            // A full-screen background here would fade in independently of the
                            // morph and look like a separate sheet appearing over the app.
                            Box(modifier = Modifier.fillMaxSize()) {
                            val playerPosition by playbackController.currentPosition.collectAsStateWithLifecycle()
                            val playerDuration by playbackController.duration.collectAsStateWithLifecycle()
                            PlayerScreen(
                                trackTitle = currentTrackTitle,
                                artist = currentArtist,
                                isPlaying = isPlaying,
                                currentPosition = playerPosition,
                                duration = playerDuration,
                                artworkUri = currentArtworkUri?.toString(),
                                isLiked = isLiked,
                                isDisliked = isDisliked,
                                shuffleEnabled = shuffleEnabled,
                                repeatMode = repeatMode,
                                sleepTimerActive = sleepTimerActive,
                                sleepTimerTimeLeft = sleepTimerTimeLeft,
                                stopAfterTrack = stopAfterTrack,
                                onStopAfterTrack = { playbackController.setStopAfterCurrentTrack(true) },
                                onBack = { showPlayer = false },
                                onPlayPauseClick = { playbackController.togglePlayPause() },
                                onNextClick = { playbackController.nextTrack() },
                                onPreviousClick = { playbackController.playPrevious() },
                                onSeek = { playbackController.seekTo((it * playerDuration.toFloat()).toLong()) },
                                onLikeClick = { viewModel.toggleLike() },
                                onDislikeClick = { viewModel.toggleDislike() },
                                onShuffleClick = { playbackController.toggleShuffle() },
                                onRepeatClick = { playbackController.cycleRepeatMode() },
                                onSleepTimer = { minutes -> playbackController.startSleepTimer(minutes) },
                                onStopSleepTimer = { playbackController.stopSleepTimer() },
                                queue = queue,
                                currentQueueIndex = currentQueueIndex,
                                onQueueItemClick = { index -> playbackController.jumpToQueueItem(index) },
                                onQueueItemRemove = { index -> playbackController.removeFromQueue(index) },
                                onQueueItemMove = { from, to -> playbackController.moveQueueItem(from, to) },
                                onSaveQueueAsPlaylist = { name -> viewModel.saveQueueAsPlaylist(name) },
                                onTagSwipeUp = { showTagSheet = true },
                                equalizerManager = playbackController.equalizerManager,
                                currentTrack = currentTrack,
                                playbackSpeed = playbackSpeed,
                                onSetSpeed = { playbackController.setPlaybackSpeed(it) }
                            )
                            }
                        }
                    }
                }
            }
            } // RightNavigationDrawer

            // Global snackbar host. Sits at the root so feedback (e.g. dislike
            // toggles fired from Home/Library/Search/Player) is visible from any
            // page. Bottom padding animates to clear the mini-player + nav bar
            // when present, or sit flush above nothing when the full-screen
            // player overlay is up.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = snackbarBottomPadding)
            )

            // ── Story viewer (hosted at root so the ring works from Home AND Library) ──
            if (showStory) {
                LaunchedEffect(Unit) { viewModel.markStorySeen() }
                com.freqvox.ui.story.StoryViewer(
                    drop = activeStoryDrop,
                    onAction = handleStoryAction,
                    onDismiss = { showStory = false },
                    onShare = {
                        com.freqvox.media.StoryShareCard.shareRecap(
                            context = storyContext,
                            weekMinutes = listeningStats.weekMinutes,
                            dailyMinutes = listeningStats.weekDailyMinutes,
                            topArtist = listeningStats.topArtistTrack?.artist,
                            topTrack = listeningStats.topTracks.firstOrNull()?.first?.title,
                            streakDays = listeningStats.currentStreakDays,
                            artPath = listeningStats.topArtistTrack?.artworkPath
                        )
                    }
                )
            }
            // QA-only preview gallery (long-press the Home ring); CTAs are live here too.
            previewStory?.let { drop ->
                com.freqvox.ui.story.StoryViewer(
                    drop = drop,
                    onAction = handleStoryAction,
                    onDismiss = { previewStory = null }
                )
            }

            // ── Panel-launched sheets (hosted at root so they overlay everything) ──
            if (showStatsSheet) {
                com.freqvox.ui.settings.StatsSheet(
                    stats = listeningStats,
                    onOpenArtist = { artist ->
                        val subset = viewModel.allTracks.value.filter { it.artist == artist }
                        entityStats = Triple(artist, "${subset.size} track${if (subset.size == 1) "" else "s"}", subset)
                    },
                    onOpenTag = { tag ->
                        val ids = viewModel.taggedTrackIds.value[tag.lowercase()] ?: emptySet()
                        val subset = viewModel.allTracks.value.filter { it.id in ids }
                        entityStats = Triple(
                            com.freqvox.ui.tag.formatTagDisplayName(tag),
                            "${subset.size} track${if (subset.size == 1) "" else "s"}",
                            subset
                        )
                    },
                    onDismiss = { showStatsSheet = false }
                )
            }
            entityStats?.let { (title, subtitle, tracks) ->
                com.freqvox.ui.settings.EntityStatsSheet(
                    title = title,
                    subtitle = subtitle,
                    tracks = tracks,
                    playHistory = viewModel.playHistory.value,
                    onDismiss = { entityStats = null }
                )
            }
            if (showProfilesSheet) {
                val tagProfiles by viewModel.tagProfiles.collectAsStateWithLifecycle()
                com.freqvox.ui.settings.TagProfilesSheet(
                    tags = allTags.distinctBy { it.name.lowercase() }.filter { it.name.lowercase() != "disliked" },
                    presets = viewModel.eqPresets,
                    profiles = tagProfiles,
                    onSet = { tag, preset -> viewModel.setTagProfile(tag, preset) },
                    onDismiss = { showProfilesSheet = false }
                )
            }
            if (showDuplicatesSheet) {
                val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
                val dupActions = com.freqvox.ui.components.rememberTrackActionsState()
                com.freqvox.ui.components.TrackActionsDialogs(
                    state = dupActions, viewModel = viewModel, playlists = allPlaylists
                )
                com.freqvox.ui.settings.DuplicatesSheet(
                    groups = duplicateGroups,
                    onDelete = { dupActions.openDeleteFlow(it) },
                    onDismiss = { showDuplicatesSheet = false }
                )
            }
            if (showFoldersSheet) {
                com.freqvox.ui.settings.FoldersSheet(
                    folders = availableFolders,
                    excluded = excludedFolders,
                    onToggle = { viewModel.toggleFolderExclusion(it) },
                    onDismiss = { showFoldersSheet = false }
                )
            }
            if (showTagManagerSheet) {
                com.freqvox.ui.settings.TagManagerSheet(
                    tags = allTags,
                    onSave = { tag, name, color -> viewModel.updateTag(tag, name, color) },
                    onDelete = { viewModel.deleteTag(it) },
                    onDismiss = { showTagManagerSheet = false }
                )
            }
            if (showEqSheet) {
                com.freqvox.ui.player.components.EqualizerSheet(
                    equalizerManager = playbackController.equalizerManager,
                    onDismiss = { showEqSheet = false }
                )
            }
            if (showCrossfadeSheet) {
                val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsStateWithLifecycle()
                val albumMode by viewModel.albumMode.collectAsStateWithLifecycle()
                com.freqvox.ui.settings.CrossfadeSheet(
                    selectedSeconds = crossfadeSeconds,
                    onSelect = { viewModel.setCrossfadeSeconds(it) },
                    albumMode = albumMode,
                    onAlbumModeChange = { viewModel.setAlbumMode(it) },
                    onDismiss = { showCrossfadeSheet = false }
                )
            }
            if (showAccentSheet) {
                com.freqvox.ui.settings.AccentSheet(
                    enabled = fixedAccentEnabled,
                    selectedColor = fixedAccentColor,
                    onToggle = { viewModel.setFixedAccentEnabled(it) },
                    onSelectColor = { viewModel.setFixedAccentColor(it) },
                    onDismiss = { showAccentSheet = false }
                )
            }
            if (showSleepSheet) {
                com.freqvox.ui.settings.SleepTimerSheet(
                    isActive = sleepTimerActive,
                    timeLeftMs = sleepTimerTimeLeft,
                    stopAfterTrack = stopAfterTrack,
                    stopAfterAlbum = stopAfterAlbum,
                    onSelectMinutes = { playbackController.startSleepTimer(it) },
                    onEndOfTrack = { playbackController.setStopAfterCurrentTrack(true) },
                    onEndOfAlbum = { playbackController.setStopAfterAlbum(true) },
                    onCancel = { playbackController.stopSleepTimer() },
                    onDismiss = { showSleepSheet = false }
                )
            }
            if (showArtworkSheet) {
                com.freqvox.ui.settings.ArtworkSheet(
                    onlineEnabled = artworkOnlineEnabled,
                    allowCellular = artworkAllowCellular,
                    allowMeteredWifi = artworkAllowMeteredWifi,
                    onToggleOnline = { viewModel.setArtworkOnlineEnabled(it) },
                    onToggleCellular = { viewModel.setArtworkAllowCellular(it) },
                    onToggleMeteredWifi = { viewModel.setArtworkAllowMeteredWifi(it) },
                    onRetryFailed = { viewModel.retryFailedArtwork() },
                    retrying = artworkRetrying,
                    onPhotoAccessGranted = { viewModel.scanLibrary() },
                    vidmateLinkedCount = vidmateLinkedCount,
                    onLinkVidmate = { viewModel.linkVidmateFolder(it) },
                    onDismiss = { showArtworkSheet = false }
                )
            }
            // One-time disclosure; blocks online fetching until acknowledged (Gate A).
            if (showArtworkConsent) {
                com.freqvox.ui.settings.ArtworkConsentDialog(
                    onManageSettings = {
                        viewModel.onArtworkConsentAcknowledged()
                        showArtworkSheet = true
                    },
                    onAcknowledge = { viewModel.onArtworkConsentAcknowledged() }
                )
            }
        }
    }
}

@Composable
private fun DynamicGradientBackground(
    trackTitle: String,
    modifier: Modifier = Modifier
) {
    val gradientPrimary   = com.freqvox.ui.theme.dynamic.LocalGradientPrimary.current
    val gradientSecondary = com.freqvox.ui.theme.dynamic.LocalGradientSecondary.current
    val gradientTertiary  = com.freqvox.ui.theme.dynamic.LocalGradientTertiary.current
    // The gradient fades into the page background; captured here so it anchors to near-black
    // in dark mode and near-white in light mode (the remember lambda below isn't composable).
    val gradientAnchor    = com.freqvox.ui.theme.LocalFreqvoxColors.current.background

    val gradientAngle = remember(trackTitle) {
        kotlin.math.abs(trackTitle.hashCode()) % 6
    }

    val gradientBrush = remember(gradientPrimary, gradientSecondary, gradientTertiary, gradientAngle, gradientAnchor) {
        val c1 = gradientPrimary.copy(alpha = 0.40f)
        val c2 = gradientSecondary.copy(alpha = 0.30f)
        val c3 = gradientTertiary.copy(alpha = 0.20f)
        when (gradientAngle) {
            0 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(c1, c2, c3, gradientAnchor)
            )
            1 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(c3, gradientAnchor, c1, c2)
            )
            2 -> androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(c1, c2, c3),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end   = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
            3 -> androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(c2, c1, c3),
                start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                end   = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
            )
            4 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(gradientAnchor, c3, c2, c1)
            )
            else -> androidx.compose.ui.graphics.Brush.verticalGradient(
                0.0f to c1,
                0.4f to c2,
                0.7f to c3,
                1.0f to gradientAnchor
            )
        }
    }

    Box(modifier = modifier.background(brush = gradientBrush))
}
