package com.freqvox.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playbackController: PlaybackController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val currentTrackTitle by playbackController.currentTrackTitle.collectAsState()
    val currentArtist by playbackController.currentArtist.collectAsState()
    val isPlaying by playbackController.isPlaying.collectAsState()
    val currentArtworkUri by playbackController.currentArtworkUri.collectAsState()
    val currentPosition by playbackController.currentPosition.collectAsState()
    val duration by playbackController.duration.collectAsState()
    
    val isLiked by viewModel.isCurrentTrackLiked.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val currentTrackTags by viewModel.currentTrackTags.collectAsState()
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showTagSheet by remember { mutableStateOf(false) }

    // Sync current track path to ViewModel for tagging/liking
    LaunchedEffect(currentArtworkUri) {
        viewModel.updateCurrentTrack(currentArtworkUri?.path)
    }

    // Show mini player only if a track is selected and we are NOT on the full player screen
    val showMiniPlayer = currentTrackTitle != "No track selected" && 
            currentDestination?.route != Screen.Player.route

    DynamicTheme(artworkPath = currentArtworkUri?.path, trackTitle = currentTrackTitle) {
        val gradientPrimary   = com.freqvox.ui.theme.dynamic.LocalGradientPrimary.current
        val gradientSecondary = com.freqvox.ui.theme.dynamic.LocalGradientSecondary.current
        val gradientTertiary  = com.freqvox.ui.theme.dynamic.LocalGradientTertiary.current
        
        // 6-way direction from title hash — no simple repeating pattern
        val gradientAngle = remember(currentTrackTitle) {
            val hash = kotlin.math.abs(currentTrackTitle.hashCode())
            hash % 6
        }
        
        val gradientBrush = remember(gradientPrimary, gradientSecondary, gradientTertiary, gradientAngle) {
            val c1 = gradientPrimary.copy(alpha = 0.40f)
            val c2 = gradientSecondary.copy(alpha = 0.30f)
            val c3 = gradientTertiary.copy(alpha = 0.20f)
            when (gradientAngle) {
                // Vertical top → c1, mid → c2, bottom → dark
                0 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(c1, c2, c3, Color(0xFF06060C))
                )
                // Vertical inverted
                1 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(c3, Color(0xFF06060C), c1, c2)
                )
                // Diagonal top-left → bottom-right
                2 -> androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(c1, c2, c3),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end   = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
                // Diagonal top-right → bottom-left
                3 -> androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(c2, c1, c3),
                    start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                    end   = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                )
                // Stage light: vivid glow rises from the bottom
                4 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF06060C), c3, c2, c1)
                )
                // Split: c1 top, c3 bottom, c2 center fade
                else -> androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.0f to c1,
                    0.4f to c2,
                    0.7f to c3,
                    1.0f to Color(0xFF06060C)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientBrush)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
            bottomBar = {
                val isPlayerScreen = currentDestination?.route == Screen.Player.route
                AnimatedVisibility(
                    visible = !isPlayerScreen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                Column {
                    AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            playbackController = playbackController,
                            onMiniPlayerClick = {
                                navController.navigate(Screen.Player.route)
                            }
                        )
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
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
                } // end AnimatedVisibility
            }
        ) { innerPadding -> 
            val isPlayerScreen = currentDestination?.route == Screen.Player.route
            Box(modifier = if (isPlayerScreen) Modifier.fillMaxSize() else Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            playbackController = playbackController,
                            onTrackClick = { track ->
                                val currentTitle = playbackController.currentTrackTitle.value
                                if (currentTitle == track.title) {
                                    // Same track — toggle play/pause
                                    playbackController.togglePlayPause()
                                } else {
                                    playbackController.playMediaUri(track.path, track.title, track.artist, track.id.toString(), track.artworkPath)
                                }
                            }
                        )
                    }
                    composable(Screen.Search.route) { 
                        SearchScreen(onTrackClick = { track ->
                            playbackController.playMediaUri(track.path, track.title, track.artist, track.id.toString(), track.artworkPath)
                        }) 
                    }
                    composable(Screen.Library.route) { 
                        LibraryScreen(onTrackClick = { track ->
                            playbackController.playMediaUri(track.path, track.title, track.artist, track.id.toString(), track.artworkPath)
                        }) 
                    }
                    composable(Screen.Radio.route) { 
                        RadioScreen(onStationClick = { station ->
                            playbackController.playMediaUri(station.url, station.name, "Radio", station.url)
                        }) 
                    }
                    composable(Screen.Player.route) { 
                        PlayerScreen(
                            trackTitle = currentTrackTitle,
                            artist = currentArtist,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            artworkUri = currentArtworkUri?.toString(),
                            isLiked = isLiked,
                            onPlayPauseClick = { playbackController.togglePlayPause() },
                            onNextClick = { playbackController.playNext() },
                            onPreviousClick = { playbackController.playPrevious() },
                            onSeek = { playbackController.seekTo((it * duration.toFloat()).toLong()) },
                            onLikeClick = { viewModel.toggleLike() },
                            onTagSwipeUp = { showTagSheet = true }
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
        }
    }
}
