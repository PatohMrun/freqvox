package com.freqvox.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.main.MainViewModel
import java.io.File

enum class LibraryTab {
    Songs, Albums, Artists, Folders, Playlists
}

@Composable
fun LibraryScreen(
    onTrackClick: (TrackEntity) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val tracks by viewModel.allTracks.collectAsState()
    var selectedTab by remember { mutableStateOf(LibraryTab.Songs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Collection",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sub-Tab Filter Row
        LazyRow(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryTab.values()) { tab ->
                val isSelected = selectedTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    label = { Text(tab.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1A1A26),
                        labelColor = Color.Gray,
                        selectedContainerColor = Color(0xFF7C4DFF),
                        selectedLabelColor = Color.White
                    ),
                    border = null,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
            }
        }

        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No music found in your library", color = Color.Gray)
            }
        } else {
            when (selectedTab) {
                LibraryTab.Songs -> {
                    LazyColumn {
                        items(tracks) { track ->
                            TrackItem(track = track, onClick = { onTrackClick(track) })
                        }
                    }
                }
                LibraryTab.Albums -> {
                    val albums = tracks.groupBy { it.album }
                    LazyColumn {
                        items(albums.keys.toList()) { albumName ->
                            val albumTracks = albums[albumName]!!
                            AlbumItem(albumName, albumTracks.first().artist, albumTracks.size)
                        }
                    }
                }
                LibraryTab.Artists -> {
                    val artists = tracks.groupBy { it.artist }
                    LazyColumn {
                        items(artists.keys.toList()) { artistName ->
                            ArtistItem(artistName, artists[artistName]!!.size)
                        }
                    }
                }
                LibraryTab.Folders -> {
                    val folders = tracks.groupBy { 
                        File(it.path).parentFile?.name ?: "Root"
                    }
                    LazyColumn {
                        items(folders.keys.toList()) { folderName ->
                            FolderItem(folderName, folders[folderName]!!.size)
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Coming soon: ${selectedTab.name}", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItem(track: TrackEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color(0xFF1A1A26)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = track.title.take(1).uppercase(),
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = track.artist,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AlbumItem(name: String, artist: String, trackCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color(0xFF1A1A26)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color.Gray, fontSize = 24.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("$artist • $trackCount tracks", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun ArtistItem(name: String, trackCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color(0xFF1A1A26)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color.Gray, fontSize = 24.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("$trackCount tracks", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun FolderItem(name: String, trackCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color(0xFF1A1A26)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("$trackCount tracks", color = Color.Gray, fontSize = 14.sp)
        }
    }
}
