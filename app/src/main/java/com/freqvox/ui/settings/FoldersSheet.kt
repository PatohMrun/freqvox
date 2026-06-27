package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersSheet(
    folders: List<String>,
    excluded: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = Fvox.accent
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
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Hidden Folders",
                color = Fvox.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Text(
                text = "Hidden folders are excluded from your library on the next scan.",
                color = Fvox.onSurface.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
            )

            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No folders found", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(folders, key = { it }) { folder ->
                        val isHidden = folder in excluded
                        val name = folder.substringAfterLast("/").ifEmpty { folder }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Fvox.onSurface.copy(alpha = 0.04f))
                                .clickable { onToggle(folder) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Default.FolderOff else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isHidden) Color(0xFF78909C) else accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    name,
                                    color = if (isHidden) Fvox.onSurface.copy(alpha = 0.45f) else Fvox.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    folder,
                                    color = Fvox.onSurface.copy(alpha = 0.3f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isHidden,
                                onCheckedChange = { onToggle(folder) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Fvox.onSurface,
                                    checkedTrackColor = Color(0xFF78909C),
                                    uncheckedThumbColor = Fvox.onSurface.copy(alpha = 0.6f),
                                    uncheckedTrackColor = Fvox.onSurface.copy(alpha = 0.08f),
                                    uncheckedBorderColor = Fvox.onSurface.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
