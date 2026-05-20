package com.freqvox.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.freqvox.data.local.entities.TrackEntity

@Composable
fun TrackMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onTag: () -> Unit,
    onPlayMoreLikeThis: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A26)
    ) {
        DropdownMenuItem(
            text = { Text("Play Next", color = Color.White) },
            onClick = {
                onPlayNext()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Gray) }
        )
        DropdownMenuItem(
            text = { Text("Add to Playlist", color = Color.White) },
            onClick = {
                onAddToPlaylist()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Gray) }
        )
        DropdownMenuItem(
            text = { Text("Tag Track", color = Color.White) },
            onClick = {
                onTag()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = Color.Gray) }
        )
        DropdownMenuItem(
            text = { Text("Play More Like This", color = Color.White) },
            onClick = {
                onPlayMoreLikeThis()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C4DFF)) }
        )
    }
}
