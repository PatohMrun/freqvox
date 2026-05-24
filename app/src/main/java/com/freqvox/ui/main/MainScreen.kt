package com.freqvox.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val queue by playbackController.queue.collectAsStateWithLifecycle()
    val currentQueueIndex by playbackController.currentQueueIndex.collectAsStateWithLifecycle()

    val isLiked by viewModel.isCurrentTrackLiked.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val currentTrackTags by viewModel.currentTrackTags.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showTagSheet by remember { mutableStateOf(false) }
    var searchFocusTrigger by remember { mutableStateOf(0L) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    // Sync current track to ViewModel using the exposed ID for reliability
    LaunchedEffect(currentTrackId) {
        viewModel.updateCurrentTrackById(currentTrackId)
    }

    // Auto-collapse the player overlay if the track disappears
    LaunchedEffect(currentTrackTitle) {
        if (currentTrackTitle == "No track selected") showPlayer = false
    }

    // NOTE: BackHandler is intentionally NOT placed here. NavHost (composed inside
    // the Scaffold below) registers its own OnBackPressedCallback during composition,
    // and OnBackPressedDispatcher fires LIFO — last registered wins. If we add our
    // BackHandler here at MainScreen scope, NavHost's callback fires first and pops
    // the bottom-nav back stack (e.g. Library -> Home) before our handler is even
    // consulted, which is why dismissing the player used to land you on Home.
    // We register it INSIDE the player's AnimatedVisibility block instead.

    val showMiniPlayer = currentTrackTitle != "No track selected"

    DynamicTheme(artworkPath = currentArtworkUri?.path, trackTitle = currentTrackTitle) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient background is its own composable so MainScreen scope doesn't
            // recompose when DynamicTheme animates colors mid-track-change.
            DynamicGradientBackground(
                trackTitle = currentTrackTitle,
                modifier = Modifier.fillMaxSize()
            )
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                val sharedScope = this
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            Column {
                                AnimatedVisibility(
                                    visible = showMiniPlayer && !showPlayer,
                                    enter = fadeIn(tween(420)),
                                    exit = fadeOut(tween(420))
                                ) {
                                    CompositionLocalProvider(
                                        LocalSharedTransitionScope provides sharedScope,
                                        LocalAnimatedVisibilityScope provides this@AnimatedVisibility
                                    ) {
                                        MiniPlayer(
                                            playbackController = playbackController,
                                            onMiniPlayerClick = { showPlayer = true }
                                        )
                                    }
                                }

                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                                    contentColor = Color.White
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
                                                selectedIconColor = Color(0xFF7C4DFF),
                                                selectedTextColor = Color(0xFF7C4DFF),
                                                unselectedIconColor = Color.Gray,
                                                unselectedTextColor = Color.Gray,
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
                                        onTrackClick = { track, contextTracks ->
                                            val currentTitle = playbackController.currentTrackTitle.value
                                            if (currentTitle == track.title) {
                                                playbackController.togglePlayPause()
                                            } else {
                                                val startIndex = contextTracks.indexOf(track).coerceAtLeast(0)
                                                playbackController.playQueue(contextTracks, startIndex)
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
                                    sheetState = sheetState,
                                    containerColor = Color.Transparent,
                                    dragHandle = null
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
                                        onClose = {
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                showTagSheet = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Full-screen player overlay — appears on top of the Scaffold via SharedTransitionLayout
                    AnimatedVisibility(
                        visible = showPlayer,
                        enter = fadeIn(tween(420)),
                        exit = fadeOut(tween(420))
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
                                shuffleEnabled = shuffleEnabled,
                                repeatMode = repeatMode,
                                sleepTimerActive = sleepTimerActive,
                                sleepTimerTimeLeft = sleepTimerTimeLeft,
                                onBack = { showPlayer = false },
                                onPlayPauseClick = { playbackController.togglePlayPause() },
                                onNextClick = { playbackController.skipNext() },
                                onPreviousClick = { playbackController.playPrevious() },
                                onSeek = { playbackController.seekTo((it * playerDuration.toFloat()).toLong()) },
                                onLikeClick = { viewModel.toggleLike() },
                                onShuffleClick = { playbackController.toggleShuffle() },
                                onRepeatClick = { playbackController.cycleRepeatMode() },
                                onSleepTimer = { minutes -> playbackController.startSleepTimer(minutes) },
                                onStopSleepTimer = { playbackController.stopSleepTimer() },
                                queue = queue,
                                currentQueueIndex = currentQueueIndex,
                                onQueueItemClick = { index -> playbackController.jumpToQueueItem(index) },
                                onQueueItemRemove = { index -> playbackController.removeFromQueue(index) },
                                onQueueItemMove = { from, to -> playbackController.moveQueueItem(from, to) },
                                onTagSwipeUp = { showTagSheet = true },
                                equalizerManager = playbackController.equalizerManager,
                                currentTrack = currentTrack
                            )
                        }
                    }
                }
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

    val gradientAngle = remember(trackTitle) {
        kotlin.math.abs(trackTitle.hashCode()) % 6
    }

    val gradientBrush = remember(gradientPrimary, gradientSecondary, gradientTertiary, gradientAngle) {
        val c1 = gradientPrimary.copy(alpha = 0.40f)
        val c2 = gradientSecondary.copy(alpha = 0.30f)
        val c3 = gradientTertiary.copy(alpha = 0.20f)
        when (gradientAngle) {
            0 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(c1, c2, c3, Color(0xFF06060C))
            )
            1 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(c3, Color(0xFF06060C), c1, c2)
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
                colors = listOf(Color(0xFF06060C), c3, c2, c1)
            )
            else -> androidx.compose.ui.graphics.Brush.verticalGradient(
                0.0f to c1,
                0.4f to c2,
                0.7f to c3,
                1.0f to Color(0xFF06060C)
            )
        }
    }

    Box(modifier = modifier.background(brush = gradientBrush))
}
