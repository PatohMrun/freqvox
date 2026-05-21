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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.data.local.entities.TagEntity

private val tagIcons: Map<String, ImageVector> = mapOf(
    "liked"      to Icons.Default.Favorite,
    "chill"      to Icons.Default.AcUnit,
    "energetic"  to Icons.Default.Bolt,
    "focus"      to Icons.Default.School,
    "hype"       to Icons.Default.Whatshot,
    "melancholy" to Icons.Default.Cloud,
    "late night" to Icons.Default.Bedtime,
    "workout"    to Icons.Default.FitnessCenter,
    "drive"      to Icons.Default.DirectionsCar,
    "sleep"      to Icons.Default.Hotel,
    "party"      to Icons.Default.Celebration,
    "morning"    to Icons.Default.WbSunny,
    "throwback"  to Icons.Default.History,
    "underrated" to Icons.Default.Diamond,
    "sad"        to Icons.Default.SentimentDissatisfied,
    "vibes"      to Icons.Default.AutoAwesome
)

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
            .fillMaxHeight(0.55f),
        color = Color(0xFF12121A),
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
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Tap to toggle · builds smart playlists",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tags.distinctBy { it.name.lowercase() }) { tag ->
                    TagChip(
                        tag = tag,
                        isSelected = selectedTagIds.contains(tag.id),
                        icon = tagIcons[tag.name.lowercase()],
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
                else Color.White.copy(alpha = 0.05f)
            )
            .then(
                if (isSelected)
                    Modifier.border(1.5.dp, tagColor.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
                else
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
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
                    tint = if (isSelected) tagColor else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = tag.name.replaceFirstChar { it.uppercase() },
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}
