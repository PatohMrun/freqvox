package com.freqvox.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbDownOffAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.freqvox.ui.theme.Fvox
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.ui.tag.formatTagDisplayName
import com.freqvox.ui.tag.iconForTag

@Composable
fun TagOverlay(
    tags: List<TagEntity>,
    selectedTagIds: List<Long>,
    onTagClick: (TagEntity) -> Unit,
    onCreateCustomTag: (String) -> Unit,
    onClose: () -> Unit,
    isDisliked: Boolean = false,
    onToggleDislike: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visibleTags = remember(tags) { tags.filter { it.name.lowercase() != "disliked" } }

    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            containerColor = Color(0xFF1E1E2E),
            title = { Text("New Custom Tag", color = Fvox.onSurface) },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it.take(20) },
                    placeholder = { Text("Tag name (e.g. studying)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C4DFF),
                        unfocusedBorderColor = Fvox.onSurface.copy(alpha = 0.2f),
                        focusedTextColor = Fvox.onSurface,
                        unfocusedTextColor = Fvox.onSurface
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagName.isNotBlank()) {
                        onCreateCustomTag(newTagName.trim())
                    }
                    showNewTagDialog = false
                    newTagName = ""
                }) {
                    Text("Create", color = Color(0xFF7C4DFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
        color = Fvox.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 8.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TAG THIS TRACK",
                        style = MaterialTheme.typography.labelLarge,
                        color = Fvox.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Tap to toggle · builds smart playlists",
                        color = Fvox.onSurface.copy(alpha = 0.25f),
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Fvox.onSurface.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (onToggleDislike != null) {
                DislikeTrackRow(
                    isDisliked = isDisliked,
                    onClick = onToggleDislike
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            val distinctTags = remember(visibleTags) { visibleTags.distinctBy { it.name.lowercase() } }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(distinctTags, key = { it.id }) { tag ->
                    TagChip(
                        tag = tag,
                        isSelected = selectedTagIds.contains(tag.id),
                        icon = iconForTag(tag.name),
                        onClick = { onTagClick(tag) }
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Fvox.onSurface.copy(alpha = 0.05f))
                            .border(1.dp, Fvox.onSurface.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                            .clickable { showNewTagDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Custom Tag",
                                tint = Fvox.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Custom Tag",
                                color = Fvox.onSurface.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DislikeTrackRow(
    isDisliked: Boolean,
    onClick: () -> Unit
) {
    val dislikeColor = Color(0xFF78909C)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDisliked) dislikeColor.copy(alpha = 0.18f)
                else Fvox.onSurface.copy(alpha = 0.05f)
            )
            .border(
                width = if (isDisliked) 1.5.dp else 1.dp,
                color = if (isDisliked) dislikeColor.copy(alpha = 0.7f) else Fvox.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Outlined.ThumbDownOffAlt,
            contentDescription = null,
            tint = if (isDisliked) dislikeColor else Fvox.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isDisliked) "Disliked" else "Dislike this track",
                color = if (isDisliked) Fvox.onSurface else Fvox.onSurface.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = if (isDisliked) "Tap to remove" else "Hides it from your feed and autoplay",
                color = Fvox.onSurface.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }
        if (isDisliked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = dislikeColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TagChip(
    tag: TagEntity,
    isSelected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val tagColor = Color(tag.color)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (isSelected) tagColor.copy(alpha = 0.18f)
                else Fvox.onSurface.copy(alpha = 0.05f)
            )
            .then(
                if (isSelected)
                    Modifier.border(1.5.dp, tagColor.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
                else
                    Modifier.border(1.dp, Fvox.onSurface.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) tagColor else Fvox.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = formatTagDisplayName(tag.name),
                color = if (isSelected) Fvox.onSurface else Fvox.onSurface.copy(alpha = 0.55f),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}
