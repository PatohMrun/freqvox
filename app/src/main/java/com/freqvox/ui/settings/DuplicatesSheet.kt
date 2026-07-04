package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.theme.Fvox
import java.util.Locale

/**
 * Lists likely-duplicate track groups (same title + artist ignoring case/punctuation) so the
 * user can review and delete the extras. Deletion is routed through the standard two-step
 * delete flow via [onDelete].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesSheet(
    groups: List<List<TrackEntity>>,
    onDelete: (TrackEntity) -> Unit,
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
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        ) {
            Text(
                "Duplicates",
                color = Fvox.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Text(
                if (groups.isEmpty()) "No duplicates found."
                else "${groups.size} group${if (groups.size == 1) "" else "s"} of songs that share a title and artist. Keep one, delete the rest.",
                color = Fvox.onSurface.copy(alpha = 0.45f), fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(groups.size) { gi ->
                    val group = groups[gi]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Fvox.onSurface.copy(alpha = 0.04f))
                            .padding(12.dp)
                    ) {
                        Text(
                            group.first().title,
                            color = Fvox.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            group.first().artist,
                            color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        group.forEachIndexed { i, track ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val detail = buildString {
                                        append(formatDuration(track.duration))
                                        if (track.album.isNotBlank()) { append(" · "); append(track.album) }
                                        if (track.playCount > 0) { append(" · "); append("${track.playCount} plays") }
                                    }
                                    Text(
                                        track.folderPath.substringAfterLast('/').ifBlank { "Track" },
                                        color = Fvox.onSurface.copy(alpha = 0.8f), fontSize = 13.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        detail + if (i == 0) "  · kept" else "",
                                        color = if (i == 0) Fvox.accent.copy(alpha = 0.8f) else Fvox.onSurface.copy(alpha = 0.4f),
                                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onDelete(track) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete this copy",
                                        tint = Fvox.accentAlt
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
