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
import androidx.compose.ui.graphics.StrokeCap
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
        IllustrationKind.GESTURE -> {
            // A long-press: ripple rings around a finger dot.
            val c = Offset(w * 0.5f, h * 0.5f)
            drawCircle(onDark.copy(alpha = 0.10f), radius = w * 0.34f, center = c)
            drawCircle(accent.copy(alpha = 0.30f), radius = w * 0.24f, center = c, style = Stroke(width = 5f))
            drawCircle(accent.copy(alpha = 0.55f), radius = w * 0.15f, center = c, style = Stroke(width = 5f))
            drawCircle(accent, radius = w * 0.07f, center = c)
        }
        IllustrationKind.TIMER -> {
            val c = Offset(w / 2, h * 0.55f)
            drawCircle(onDark.copy(alpha = 0.12f), radius = w * 0.30f, center = c)
            drawCircle(accent, radius = w * 0.30f, center = c, style = Stroke(width = 6f))
            drawLine(onDark, c, Offset(c.x, c.y - w * 0.18f), strokeWidth = 6f) // minute hand
            drawLine(onDark, c, Offset(c.x + w * 0.12f, c.y), strokeWidth = 6f)   // hour hand
            drawLine(accent, Offset(c.x, h * 0.18f), Offset(c.x, h * 0.24f), strokeWidth = 6f) // top nub
        }
        IllustrationKind.LYRICS -> {
            val widths = listOf(0.5f, 0.7f, 0.55f, 0.65f)
            widths.forEachIndexed { i, wf ->
                val highlighted = i == 1
                drawRoundRect(
                    color = if (highlighted) accent else onDark.copy(alpha = 0.28f),
                    topLeft = Offset(w * 0.5f - (w * wf) / 2, h * (0.28f + i * 0.14f)),
                    size = Size(w * wf, h * 0.06f),
                    cornerRadius = CornerRadius(20f, 20f)
                )
            }
        }
        IllustrationKind.PLAYLIST -> {
            repeat(3) { i ->
                drawRoundRect(onDark.copy(alpha = 0.4f), Offset(w * 0.2f, h * (0.3f + i * 0.13f)), Size(w * 0.4f, h * 0.06f), CornerRadius(12f, 12f))
            }
            // play triangle
            val path = Path().apply {
                moveTo(w * 0.68f, h * 0.32f); lineTo(w * 0.68f, h * 0.56f); lineTo(w * 0.84f, h * 0.44f); close()
            }
            drawPath(path, accent)
        }
        IllustrationKind.SHARE -> {
            val a = Offset(w * 0.68f, h * 0.28f)
            val b = Offset(w * 0.68f, h * 0.72f)
            val src = Offset(w * 0.32f, h * 0.5f)
            drawLine(onDark.copy(alpha = 0.5f), src, a, strokeWidth = 5f)
            drawLine(onDark.copy(alpha = 0.5f), src, b, strokeWidth = 5f)
            drawCircle(accent, radius = w * 0.07f, center = a)
            drawCircle(accent, radius = w * 0.07f, center = b)
            drawCircle(onDark, radius = w * 0.08f, center = src)
        }
        IllustrationKind.SEARCH -> {
            val c = Offset(w * 0.44f, h * 0.44f)
            drawCircle(accent, radius = w * 0.20f, center = c, style = Stroke(width = 7f))
            drawLine(onDark, Offset(c.x + w * 0.15f, c.y + h * 0.15f), Offset(w * 0.72f, h * 0.72f), strokeWidth = 9f)
        }
        IllustrationKind.EQUALIZER -> {
            val heights = listOf(0.35f, 0.55f, 0.3f, 0.6f)
            val xs = listOf(0.28f, 0.43f, 0.58f, 0.73f)
            xs.forEachIndexed { i, x ->
                drawLine(onDark.copy(alpha = 0.25f), Offset(w * x, h * 0.2f), Offset(w * x, h * 0.8f), strokeWidth = 4f)
                drawCircle(accent, radius = w * 0.035f, center = Offset(w * x, h * heights[i]))
            }
        }
        IllustrationKind.VOLUME -> {
            val cx = w * 0.5f; val cy = h * 0.5f
            drawRoundRect(onDark.copy(alpha = 0.5f), topLeft = Offset(w * 0.22f, h * 0.42f), size = Size(w * 0.12f, h * 0.16f), cornerRadius = CornerRadius(4f, 4f))
            val cone = Path().apply { moveTo(w * 0.34f, h * 0.36f); lineTo(w * 0.34f, h * 0.64f); lineTo(w * 0.5f, h * 0.5f); close() }
            drawPath(cone, onDark.copy(alpha = 0.5f))
            listOf(0.10f, 0.17f, 0.24f).forEach { rf ->
                val r = w * rf
                drawArc(accent.copy(alpha = 0.8f), startAngle = -35f, sweepAngle = 70f, useCenter = false,
                    topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2), style = Stroke(width = 4f))
            }
        }
        IllustrationKind.PALETTE -> {
            drawCircle(onDark.copy(alpha = 0.10f), radius = w * 0.30f, center = Offset(w / 2, h / 2))
            listOf(
                Triple(0.38f, 0.4f, 0.9f), Triple(0.62f, 0.38f, 0.6f),
                Triple(0.4f, 0.62f, 0.7f), Triple(0.64f, 0.6f, 0.45f)
            ).forEach { (x, y, a) -> drawCircle(accent.copy(alpha = a), radius = w * 0.06f, center = Offset(w * x, h * y)) }
        }
        IllustrationKind.THEME -> {
            val sunC = Offset(w * 0.32f, h * 0.5f); val sunR = w * 0.10f
            drawCircle(accent, radius = sunR, center = sunC, style = Stroke(width = 4f))
            listOf(0.0, 60.0, 300.0).forEach { deg ->
                val rad = Math.toRadians(deg)
                val inner = sunR + w * 0.02f; val outer = sunR + w * 0.06f
                drawLine(
                    accent,
                    Offset(sunC.x + (inner * kotlin.math.cos(rad)).toFloat(), sunC.y + (inner * kotlin.math.sin(rad)).toFloat()),
                    Offset(sunC.x + (outer * kotlin.math.cos(rad)).toFloat(), sunC.y + (outer * kotlin.math.sin(rad)).toFloat()),
                    strokeWidth = 4f
                )
            }
            drawCircle(onDark, radius = w * 0.12f, center = Offset(w * 0.66f, h * 0.5f))
        }
        IllustrationKind.CLEANUP -> {
            drawRoundRect(onDark.copy(alpha = 0.25f), topLeft = Offset(w * 0.26f, h * 0.26f), size = Size(w * 0.36f, h * 0.36f), cornerRadius = CornerRadius(14f, 14f))
            drawRoundRect(accent, topLeft = Offset(w * 0.38f, h * 0.38f), size = Size(w * 0.36f, h * 0.36f), cornerRadius = CornerRadius(14f, 14f))
            drawCircle(onDark, radius = w * 0.07f, center = Offset(w * 0.74f, h * 0.74f))
            drawLine(accent, Offset(w * 0.705f, h * 0.74f), Offset(w * 0.735f, h * 0.77f), strokeWidth = 5f, cap = StrokeCap.Round)
            drawLine(accent, Offset(w * 0.735f, h * 0.77f), Offset(w * 0.78f, h * 0.71f), strokeWidth = 5f, cap = StrokeCap.Round)
        }
        IllustrationKind.SAVE -> {
            drawRoundRect(onDark.copy(alpha = 0.35f), topLeft = Offset(w * 0.26f, h * 0.26f), size = Size(w * 0.48f, h * 0.48f), cornerRadius = CornerRadius(16f, 16f))
            drawRect(accent, topLeft = Offset(w * 0.36f, h * 0.26f), size = Size(w * 0.22f, h * 0.14f))
            drawLine(onDark, Offset(w * 0.32f, h * 0.62f), Offset(w * 0.68f, h * 0.62f), strokeWidth = 5f)
        }
        IllustrationKind.FOLDER -> {
            drawRoundRect(accent, topLeft = Offset(w * 0.22f, h * 0.34f), size = Size(w * 0.22f, h * 0.10f), cornerRadius = CornerRadius(6f, 6f))
            drawRoundRect(onDark.copy(alpha = 0.3f), topLeft = Offset(w * 0.22f, h * 0.40f), size = Size(w * 0.56f, h * 0.30f), cornerRadius = CornerRadius(8f, 8f))
            drawRoundRect(accent.copy(alpha = 0.5f), topLeft = Offset(w * 0.22f, h * 0.40f), size = Size(w * 0.56f, h * 0.30f), cornerRadius = CornerRadius(8f, 8f), style = Stroke(width = 3f))
        }
        IllustrationKind.ARTWORK -> {
            drawRoundRect(onDark.copy(alpha = 0.5f), topLeft = Offset(w * 0.24f, h * 0.30f), size = Size(w * 0.52f, h * 0.42f), cornerRadius = CornerRadius(10f, 10f), style = Stroke(width = 4f))
            drawCircle(accent, radius = w * 0.045f, center = Offset(w * 0.36f, h * 0.42f))
            val mtn = Path().apply {
                moveTo(w * 0.28f, h * 0.68f); lineTo(w * 0.42f, h * 0.46f); lineTo(w * 0.50f, h * 0.56f); lineTo(w * 0.60f, h * 0.42f); lineTo(w * 0.72f, h * 0.68f); close()
            }
            drawPath(mtn, onDark.copy(alpha = 0.4f))
        }
        IllustrationKind.RADIO -> {
            drawLine(onDark.copy(alpha = 0.6f), Offset(w * 0.36f, h * 0.28f), Offset(w * 0.36f, h * 0.5f), strokeWidth = 4f)
            drawCircle(accent, radius = w * 0.03f, center = Offset(w * 0.36f, h * 0.26f))
            drawRoundRect(onDark.copy(alpha = 0.35f), topLeft = Offset(w * 0.26f, h * 0.5f), size = Size(w * 0.28f, h * 0.18f), cornerRadius = CornerRadius(8f, 8f))
            listOf(0.10f, 0.18f, 0.26f).forEach { rf ->
                val r = w * rf
                drawArc(
                    accent.copy(alpha = 0.75f), startAngle = -55f, sweepAngle = 40f, useCenter = false,
                    topLeft = Offset(w * 0.36f - r, h * 0.26f - r), size = Size(r * 2, r * 2), style = Stroke(width = 4f)
                )
            }
        }
        IllustrationKind.EDIT -> {
            drawLine(onDark, Offset(w * 0.28f, h * 0.72f), Offset(w * 0.62f, h * 0.38f), strokeWidth = 14f, cap = StrokeCap.Round)
            val tip = Path().apply { moveTo(w * 0.62f, h * 0.38f); lineTo(w * 0.70f, h * 0.30f); lineTo(w * 0.72f, h * 0.40f); close() }
            drawPath(tip, accent)
        }
        IllustrationKind.SETTINGS -> {
            for (i in 0 until 8) {
                val ang = Math.toRadians((i * 45).toDouble())
                val r = w * 0.30f
                drawCircle(
                    onDark.copy(alpha = 0.35f), radius = w * 0.035f,
                    center = Offset(w / 2 + (r * kotlin.math.cos(ang)).toFloat(), h / 2 + (r * kotlin.math.sin(ang)).toFloat())
                )
            }
            drawCircle(accent, radius = w * 0.20f, center = Offset(w / 2, h / 2))
            drawCircle(onDark.copy(alpha = 0.5f), radius = w * 0.08f, center = Offset(w / 2, h / 2))
        }
        IllustrationKind.SPEED -> {
            listOf(0f, 0.16f).forEach { off ->
                val tri = Path().apply {
                    moveTo(w * (0.32f + off), h * 0.32f); lineTo(w * (0.32f + off), h * 0.68f); lineTo(w * (0.50f + off), h * 0.5f); close()
                }
                drawPath(tri, accent)
            }
        }
        IllustrationKind.REWIND -> {
            listOf(0f, 0.16f).forEach { off ->
                val tri = Path().apply {
                    moveTo(w * (0.68f - off), h * 0.32f); lineTo(w * (0.68f - off), h * 0.68f); lineTo(w * (0.50f - off), h * 0.5f); close()
                }
                drawPath(tri, accent)
            }
        }
        IllustrationKind.STOP -> {
            drawCircle(onDark.copy(alpha = 0.14f), radius = w * 0.28f, center = Offset(w / 2, h / 2))
            drawRoundRect(accent, topLeft = Offset(w * 0.38f, h * 0.38f), size = Size(w * 0.24f, h * 0.24f), cornerRadius = CornerRadius(6f, 6f))
        }
        IllustrationKind.CAR -> {
            drawRoundRect(accent, topLeft = Offset(w * 0.35f, h * 0.38f), size = Size(w * 0.30f, h * 0.14f), cornerRadius = CornerRadius(10f, 10f))
            drawRoundRect(accent, topLeft = Offset(w * 0.20f, h * 0.48f), size = Size(w * 0.60f, h * 0.16f), cornerRadius = CornerRadius(10f, 10f))
            drawCircle(onDark, radius = w * 0.05f, center = Offset(w * 0.32f, h * 0.66f))
            drawCircle(onDark, radius = w * 0.05f, center = Offset(w * 0.68f, h * 0.66f))
        }
        IllustrationKind.FLAME -> {
            val flame = Path().apply {
                moveTo(w * 0.5f, h * 0.22f)
                cubicTo(w * 0.72f, h * 0.42f, w * 0.68f, h * 0.68f, w * 0.5f, h * 0.78f)
                cubicTo(w * 0.32f, h * 0.68f, w * 0.28f, h * 0.42f, w * 0.5f, h * 0.22f)
                close()
            }
            drawPath(flame, accent)
            val inner = Path().apply {
                moveTo(w * 0.5f, h * 0.42f)
                cubicTo(w * 0.60f, h * 0.52f, w * 0.58f, h * 0.64f, w * 0.5f, h * 0.70f)
                cubicTo(w * 0.42f, h * 0.64f, w * 0.42f, h * 0.52f, w * 0.5f, h * 0.42f)
                close()
            }
            drawPath(inner, onDark.copy(alpha = 0.35f))
        }
        IllustrationKind.TROPHY -> {
            val cup = Path().apply {
                moveTo(w * 0.34f, h * 0.28f); lineTo(w * 0.66f, h * 0.28f); lineTo(w * 0.58f, h * 0.52f); lineTo(w * 0.42f, h * 0.52f); close()
            }
            drawPath(cup, accent)
            drawArc(
                accent, startAngle = 100f, sweepAngle = 160f, useCenter = false,
                topLeft = Offset(w * 0.18f, h * 0.26f), size = Size(w * 0.18f, h * 0.18f), style = Stroke(width = 4f)
            )
            drawArc(
                accent, startAngle = -80f, sweepAngle = 160f, useCenter = false,
                topLeft = Offset(w * 0.64f, h * 0.26f), size = Size(w * 0.18f, h * 0.18f), style = Stroke(width = 4f)
            )
            drawRect(onDark.copy(alpha = 0.5f), topLeft = Offset(w * 0.47f, h * 0.52f), size = Size(w * 0.06f, h * 0.10f))
            drawRoundRect(onDark.copy(alpha = 0.5f), topLeft = Offset(w * 0.36f, h * 0.62f), size = Size(w * 0.28f, h * 0.06f), cornerRadius = CornerRadius(4f, 4f))
        }
        IllustrationKind.STAR -> {
            val outerR = w * 0.30f; val innerR = w * 0.13f
            val path = Path()
            for (i in 0 until 10) {
                val ang = Math.toRadians((-90 + i * 36).toDouble())
                val r = if (i % 2 == 0) outerR else innerR
                val x = w / 2 + (r * kotlin.math.cos(ang)).toFloat()
                val y = h / 2 + (r * kotlin.math.sin(ang)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, accent)
        }
        IllustrationKind.GEM -> {
            val gem = Path().apply {
                moveTo(w * 0.32f, h * 0.34f)
                lineTo(w * 0.68f, h * 0.34f)
                lineTo(w * 0.80f, h * 0.46f)
                lineTo(w * 0.5f, h * 0.76f)
                lineTo(w * 0.20f, h * 0.46f)
                close()
            }
            drawPath(gem, accent)
            drawLine(onDark.copy(alpha = 0.35f), Offset(w * 0.32f, h * 0.34f), Offset(w * 0.5f, h * 0.46f), strokeWidth = 3f)
            drawLine(onDark.copy(alpha = 0.35f), Offset(w * 0.68f, h * 0.34f), Offset(w * 0.5f, h * 0.46f), strokeWidth = 3f)
            drawLine(onDark.copy(alpha = 0.35f), Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.76f), strokeWidth = 3f)
        }
    }
}
