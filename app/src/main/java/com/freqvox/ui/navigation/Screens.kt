package com.freqvox.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Player : Screen("player", "Player", Icons.Default.PlayArrow)
    object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    object Radio : Screen("radio", "Radio", Icons.Default.Radio)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Library,
    Screen.Radio
)
