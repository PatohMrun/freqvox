package com.freqvox.ui.story

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freqvox.ui.home.TrackArtwork

/**
 * Renders the central graphic of a story segment as real, drawn visuals (no placeholders):
 * album art, cover collages, a sparkline, bar/hour charts, a streak calendar, and simple
 * illustrations. Everything is tuned for the viewer's dark, accent-tinted background.
 */
@Composable
fun SegmentVisualView(visual: SegmentVisual, accent: Color, modifier: Modifier = Modifier) {
    val onDark = Color.White
    when (visual) {
        is SegmentVisual.Emoji -> Box(
            modifier = modifier.size(132.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Text(visual.emoji, fontSize = 60.sp) }

        is SegmentVisual.AlbumArt -> Box(
            modifier = modifier.size(220.dp).clip(RoundedCornerShape(22.dp))
        ) { TrackArtwork(track = visual.track, modifier = Modifier.fillMaxSize()) }

        is SegmentVisual.ArtCollage -> {
            val arts = visual.tracks.take(4)
            Box(modifier = modifier.size(220.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f))) {
                Column(Modifier.fillMaxSize()) {
                    listOf(arts.getOrNull(0) to arts.getOrNull(1), arts.getOrNull(2) to arts.getOrNull(3)).forEach { (a, b) ->
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            listOf(a, b).forEach { t ->
                                Box(Modifier.weight(1f).fillMaxHeight()) {
                                    if (t != null) TrackArtwork(track = t, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }

        is SegmentVisual.Sparkline -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            Canvas(Modifier.size(width = 260.dp, height = 110.dp)) {
                val pts = visual.points
                if (pts.size >= 2) {
                    val stepX = size.width / (pts.size - 1)
                    fun y(v: Float) = size.height - (v.coerceIn(0f, 1f) * size.height * 0.9f) - size.height * 0.05f
                    val line = Path().apply {
                        moveTo(0f, y(pts.first()))
                        pts.forEachIndexed { i, v -> if (i > 0) lineTo(i * stepX, y(v)) }
                    }
                    val fill = Path().apply {
                        addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                    }
                    drawPath(fill, accent.copy(alpha = 0.22f))
                    drawPath(line, onDark, style = Stroke(width = 5f))
                    pts.forEachIndexed { i, v -> drawCircle(onDark, radius = 5f, center = Offset(i * stepX, y(v))) }
                }
            }
            if (visual.caption.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(visual.caption, color = onDark.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }

        is SegmentVisual.Bars -> Column(modifier = modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val max = (visual.items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
            visual.items.take(5).forEach { (label, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = onDark, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(90.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.12f))) {
                        Box(Modifier.fillMaxWidth(v.toFloat() / max).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(accent))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("$v", color = onDark.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }

        is SegmentVisual.Histogram -> {
            val max = (visual.buckets.maxOrNull() ?: 1).coerceAtLeast(1)
            Row(modifier = modifier.width(260.dp).height(110.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                visual.buckets.forEach { c ->
                    val f = c.toFloat() / max
                    Box(
                        Modifier.weight(1f).fillMaxHeight(f.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(if (c > 0) accent.copy(alpha = 0.4f + 0.5f * f) else Color.White.copy(alpha = 0.08f))
                    )
                }
            }
        }

        is SegmentVisual.StreakCalendar -> Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            visual.activeDays.takeLast(14).forEach { active ->
                Box(
                    Modifier.size(16.dp).clip(CircleShape)
                        .background(if (active) accent else Color.White.copy(alpha = 0.14f))
                )
            }
        }

        is SegmentVisual.Illustration -> Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawIllustration(visual.kind, accent, onDark) }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIllustration(
    kind: IllustrationKind, accent: Color, onDark: Color
) {
    val w = size.width
    val h = size.height
    when (kind) {
        IllustrationKind.FEED_FLOW -> {
            // Stacked cards flowing downward = the regenerating feed.
            repeat(4) { i ->
                val top = h * (0.12f + i * 0.16f)
                val inset = w * (0.14f + i * 0.03f)
                drawRoundRect(
                    color = if (i == 0) accent else onDark.copy(alpha = 0.35f - i * 0.06f),
                    topLeft = Offset(inset, top),
                    size = Size(w - inset * 2, h * 0.11f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }
        IllustrationKind.TAGS -> {
            val chips = listOf(0.2f to 0.3f, 0.55f to 0.3f, 0.3f to 0.52f, 0.62f to 0.55f, 0.25f to 0.72f)
            chips.forEachIndexed { i, (x, y) ->
                drawRoundRect(
                    color = if (i % 2 == 0) accent else onDark.copy(alpha = 0.4f),
                    topLeft = Offset(w * x, h * y),
                    size = Size(w * 0.22f, h * 0.11f),
                    cornerRadius = CornerRadius(30f, 30f)
                )
            }
        }
        IllustrationKind.CROSSFADE -> {
            val path1 = Path(); val path2 = Path()
            val mid = h / 2
            for (i in 0..40) {
                val x = w * i / 40f
                val y1 = mid + kotlin.math.sin(i / 40f * 6.28f * 2).toFloat() * h * 0.18f
                val y2 = mid + kotlin.math.cos(i / 40f * 6.28f * 2).toFloat() * h * 0.18f
                if (i == 0) { path1.moveTo(x, y1); path2.moveTo(x, y2) } else { path1.lineTo(x, y1); path2.lineTo(x, y2) }
            }
            drawPath(path1, accent, style = Stroke(width = 6f))
            drawPath(path2, onDark.copy(alpha = 0.55f), style = Stroke(width = 6f))
        }
        IllustrationKind.SHUFFLE -> {
            drawCircle(onDark.copy(alpha = 0.12f), radius = w * 0.32f, center = Offset(w / 2, h / 2))
            listOf(-1f, 1f).forEach { s ->
                val p = Path().apply {
                    moveTo(w * 0.28f, h * (0.5f + 0.16f * s))
                    cubicTo(w * 0.45f, h * (0.5f + 0.16f * s), w * 0.55f, h * (0.5f - 0.16f * s), w * 0.72f, h * (0.5f - 0.16f * s))
                }
                drawPath(p, accent, style = Stroke(width = 6f))
            }
        }
        IllustrationKind.STATS -> {
            val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f)
            heights.forEachIndexed { i, hf ->
                drawRoundRect(
                    color = if (i == 3) accent else onDark.copy(alpha = 0.35f),
                    topLeft = Offset(w * (0.2f + i * 0.13f), h * (0.85f - hf * 0.6f)),
                    size = Size(w * 0.09f, h * hf * 0.6f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }
        IllustrationKind.GENERIC -> {
            drawCircle(accent.copy(alpha = 0.3f), radius = w * 0.28f, center = Offset(w / 2, h / 2))
            drawCircle(onDark, radius = w * 0.06f, center = Offset(w / 2, h / 2))
        }
    }
}
