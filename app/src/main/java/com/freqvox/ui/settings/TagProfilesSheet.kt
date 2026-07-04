package com.freqvox.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.ui.tag.formatTagDisplayName
import com.freqvox.ui.theme.Fvox

/**
 * Assigns an equalizer preset to each tag ("playback profile"). When a track carrying that tag
 * plays, the preset is applied automatically. Preset names come from the device equalizer, so
 * the list is empty until the equalizer has initialized (i.e. something has played).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagProfilesSheet(
    tags: List<TagEntity>,
    presets: List<String>,
    profiles: Map<String, Int>,
    onSet: (String, Int) -> Unit,
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
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        ) {
            Text("Playback profiles", color = Fvox.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Text(
                "Give a tag an EQ preset — it's applied automatically whenever a track with that tag plays.",
                color = Fvox.onSurface.copy(alpha = 0.45f), fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
            )

            if (presets.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text("Play a track first so the equalizer loads its presets.", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 13.sp)
                }
            } else if (tags.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text("Create some tags first.", color = Fvox.onSurface.copy(alpha = 0.5f), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(tags.size) { i ->
                        val tag = tags[i]
                        val current = profiles[tag.name.lowercase()]
                        ProfileRow(
                            label = formatTagDisplayName(tag.name),
                            currentPresetName = current?.let { presets.getOrNull(it) } ?: "None",
                            presets = presets,
                            onPick = { idx -> onSet(tag.name, idx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    label: String,
    currentPresetName: String,
    presets: List<String>,
    onPick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Fvox.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Fvox.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(currentPresetName, color = Fvox.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Fvox.accent)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Fvox.surfaceVariant) {
                DropdownMenuItem(text = { Text("None", color = Fvox.onSurface) }, onClick = { onPick(-1); expanded = false })
                presets.forEachIndexed { idx, name ->
                    DropdownMenuItem(text = { Text(name, color = Fvox.onSurface) }, onClick = { onPick(idx); expanded = false })
                }
            }
        }
    }
}
