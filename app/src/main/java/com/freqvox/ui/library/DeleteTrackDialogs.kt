package com.freqvox.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TrackEntity

@Composable
fun DeleteTrackStepOneDialog(
    track: TrackEntity,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surfaceVariant,
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4D7C))
        },
        title = {
            Text("Remove song?", color = Fvox.onSurface, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                "\"${track.title}\" will be removed from Freqvox. On the next step you'll confirm permanent deletion from your device.",
                color = Fvox.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("Continue", color = Color(0xFFFF4D7C), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Fvox.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun DeleteTrackStepTwoDialog(
    track: TrackEntity,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surfaceVariant,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252))
        },
        title = {
            Text("Delete from device?", color = Fvox.onSurface, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                "This permanently deletes \"${track.title}\" from your phone storage. This cannot be undone.",
                color = Fvox.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text("Delete permanently", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Fvox.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}
