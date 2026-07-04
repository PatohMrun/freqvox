package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.playback.CrossfadePreferences

/**
 * Picks the crossfade length. 0s is "Off (gapless)" — tracks butt-join with no silence,
 * the default. Higher values overlap the end of one track with the start of the next.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossfadeSheet(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit,
    albumMode: Boolean,
    onAlbumModeChange: (Boolean) -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Crossfade",
                color = Fvox.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Text(
                text = "Overlap the end of one track with the start of the next. " +
                    "Off keeps tracks gapless.",
                color = Fvox.onSurface.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 16.dp)
            )

            CrossfadePreferences.CHOICES.forEach { seconds ->
                val selected = seconds == selectedSeconds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) Fvox.accent.copy(alpha = 0.18f)
                            else Fvox.onSurface.copy(alpha = 0.04f)
                        )
                        .clickable { onSelect(seconds) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (seconds == 0) "Off" else "$seconds seconds",
                        color = Fvox.onSurface,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Fvox.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Album mode toggle — only meaningful while crossfade is on, so dim + disable it
            // when Off. Rows above already carry their own bottom padding; add a small gap.
            val albumModeEnabled = selectedSeconds > 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Fvox.onSurface.copy(alpha = 0.04f))
                    .clickable(enabled = albumModeEnabled) { onAlbumModeChange(!albumMode) }
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val contentAlpha = if (albumModeEnabled) 1f else 0.4f
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Album mode",
                        color = Fvox.onSurface.copy(alpha = contentAlpha),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Skip the fade between songs from the same album, so segues stay seamless.",
                        color = Fvox.onSurface.copy(alpha = contentAlpha * 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                    )
                }
                Switch(
                    checked = albumMode,
                    onCheckedChange = onAlbumModeChange,
                    enabled = albumModeEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Fvox.onAccent,
                        checkedTrackColor = Fvox.accent
                    )
                )
            }
        }
    }
}
