package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
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
import java.util.Locale

/** Standalone sleep-timer picker so the side panel can set it without opening the player. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    isActive: Boolean,
    timeLeftMs: Long,
    stopAfterTrack: Boolean,
    stopAfterAlbum: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onEndOfAlbum: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = Fvox.accent
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Fvox.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sleep Timer", color = Fvox.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            when {
                isActive -> Text("Fades out, then stops in ${formatTimer(timeLeftMs)}", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                stopAfterTrack -> Text("Stops at the end of this track", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                stopAfterAlbum -> Text("Stops at the end of this album", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (stopAfterTrack) accent.copy(alpha = 0.22f) else Fvox.onSurface.copy(alpha = 0.07f))
                    .border(
                        1.dp,
                        if (stopAfterTrack) accent.copy(alpha = 0.6f) else Fvox.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onEndOfTrack(); onDismiss() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = if (stopAfterTrack) accent else Fvox.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End of current track", color = if (stopAfterTrack) Fvox.onSurface else Fvox.onSurface.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (stopAfterAlbum) accent.copy(alpha = 0.22f) else Fvox.onSurface.copy(alpha = 0.07f))
                    .border(
                        1.dp,
                        if (stopAfterAlbum) accent.copy(alpha = 0.6f) else Fvox.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onEndOfAlbum(); onDismiss() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Album, contentDescription = null, tint = if (stopAfterAlbum) accent else Fvox.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End of this album", color = if (stopAfterAlbum) Fvox.onSurface else Fvox.onSurface.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            listOf(5, 10, 15, 30, 45, 60).chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { minutes ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Fvox.onSurface.copy(alpha = 0.07f))
                                .border(1.dp, Fvox.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { onSelectMinutes(minutes); onDismiss() }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$minutes", color = Fvox.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("min", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (isActive || stopAfterTrack || stopAfterAlbum) {
                TextButton(onClick = { onCancel(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel Timer", color = Fvox.accentAlt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1000
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
