package com.freqvox.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the central graphic of a story segment. Emoji is complete; the data-visual kinds
 * (charts, collages, illustrations) are filled with real graphics in a later phase — for now
 * they're not produced by the content layer, so only Emoji actually appears.
 */
@Composable
fun SegmentVisualView(visual: SegmentVisual, accent: Color, modifier: Modifier = Modifier) {
    when (visual) {
        is SegmentVisual.Emoji -> Box(
            modifier = modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(visual.emoji, fontSize = 60.sp)
        }

        else -> Box(
            modifier = modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.25f))
        )
    }
}
