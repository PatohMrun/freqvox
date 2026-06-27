package com.freqvox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TrackEntity

private val accent = Color(0xFF7C4DFF)

/**
 * Bottom-sheet "more options" for a single track — the richer long-press menu used on the
 * Home feed (where a feed of cards makes an anchored dropdown awkward). Tagging is one option
 * among several rather than the whole long-press action. Each row dismisses the sheet, then
 * runs its action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    track: TrackEntity,
    onTag: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                Text(track.title, color = Fvox.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OptionRow(Icons.Default.Sell, "Tag") { onDismiss(); onTag() }
            OptionRow(Icons.Default.PlaylistAdd, "Add to playlist") { onDismiss(); onAddToPlaylist() }
            OptionRow(Icons.Default.PlaylistPlay, "Play next") { onDismiss(); onPlayNext() }
            OptionRow(Icons.Default.Delete, "Delete from device", danger = true) { onDismiss(); onDelete() }
        }
    }
}

@Composable
private fun OptionRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val tint = if (danger) Color(0xFFFF5252) else accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Fvox.onSurface.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (danger) Color(0xFFFF5252) else Fvox.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
