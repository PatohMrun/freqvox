package com.freqvox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.theme.Fvox

/**
 * Edits a track's title / artist / album. Pure UI — the caller performs the actual MediaStore
 * write (with its consent flow) and the local DB update in [onSave].
 */
@Composable
fun EditMetadataDialog(
    track: TrackEntity,
    onSave: (title: String, artist: String, album: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Fvox.accent,
        unfocusedBorderColor = Fvox.onSurface.copy(alpha = 0.2f),
        focusedTextColor = Fvox.onSurface,
        unfocusedTextColor = Fvox.onSurface,
        focusedLabelColor = Fvox.accent,
        unfocusedLabelColor = Fvox.onSurface.copy(alpha = 0.5f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surfaceVariant,
        title = { Text("Edit info", color = Fvox.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onSave(title, artist, album) },
                enabled = title.isNotBlank()
            ) { Text("Save", color = Fvox.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Fvox.onSurface.copy(alpha = 0.6f)) } }
    )
}
