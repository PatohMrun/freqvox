package com.freqvox.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TagEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagOverlay(
    tags: List<TagEntity>,
    selectedTagIds: List<Long>,
    onTagClick: (TagEntity) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
        color = Color(0xFF12121A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAG TRACK",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tags) { tag ->
                    val isSelected = selectedTagIds.contains(tag.id)
                    TagChip(
                        tag = tag,
                        isSelected = isSelected,
                        onClick = { onTagClick(tag) }
                    )
                }
            }
        }
    }
}

@Composable
fun TagChip(
    tag: TagEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(25.dp),
        color = if (isSelected) Color(tag.color).copy(alpha = 0.3f) else Color(0xFF1A1A26),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(tag.color))) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = tag.name,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}
