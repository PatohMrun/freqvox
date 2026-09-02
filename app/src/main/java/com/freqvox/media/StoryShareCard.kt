package com.freqvox.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import com.freqvox.ui.story.IllustrationKind
import com.freqvox.ui.story.SegmentVisual
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the weekly recap as a shareable poster image (minutes, a sparkline of the week, top
 * artist / top track, streak) and opens the Android share sheet via the app's FileProvider.
 */
object StoryShareCard {

    fun shareRecap(
        context: Context,
        weekMinutes: Long,
        dailyMinutes: List<Float>,
        topArtist: String?,
        topTrack: String?,
        streakDays: Int,
        artPath: String?
    ) {
        val bmp = render(weekMinutes, dailyMinutes, topArtist, topTrack, streakDays, artPath)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "freqvox_week.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My week in Freqvox")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share your week").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Shares any single story card (tip / milestone / algorithm explainer / recap sub-card) as
     * a poster: accent-derived gradient, the card's ACTUAL visual redrawn as its hero (not a
     * substitute emoji), then title + body. The weekly recap's cover card keeps its richer
     * dedicated poster ([shareRecap]); every other card — including the recap's own sub-cards
     * (top artist, streak, minutes sparkline, …) — is rendered here from [visual] directly, so
     * the shared image actually looks like the card that was on screen. Illustration-kind cards
     * previously got an emoji swapped in for their vector art (looked "off" against the card the
     * user actually saw); Sparkline/Bars/Histogram/StreakCalendar cards previously lost their
     * chart entirely. Both now render for real.
     */
    fun shareSegment(
        context: Context,
        title: String,
        body: String,
        visual: SegmentVisual,
        accentArgb: Int
    ) {
        val w = 1080; val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Same gradient recipe as the recap poster, seeded by the card's accent.
        val hsv = FloatArray(3); Color.colorToHSV(accentArgb, hsv)
        val mid = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceIn(0.4f, 0.9f), 0.42f))
        val dark = Color.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.9f).coerceIn(0f, 1f), 0.09f))
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(mid, dark), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        val textTop = drawSegmentHero(c, p, visual, accentArgb, w)

        // Title (wrapped) + body (wrapped, dimmer).
        p.textAlign = Paint.Align.CENTER
        p.color = Color.WHITE
        p.isFakeBoldText = true
        p.textSize = 74f
        var y = textTop
        wrapText(title, p, w - 160f).forEach { line -> c.drawText(line, w / 2f, y, p); y += 88f }
        y += 24f
        p.isFakeBoldText = false
        p.color = Color.argb(210, 255, 255, 255)
        p.textSize = 44f
        wrapText(body, p, w - 200f).forEach { line -> c.drawText(line, w / 2f, y, p); y += 60f }

        p.color = Color.argb(170, 255, 255, 255); p.isFakeBoldText = true; p.textSize = 34f
        c.drawText("F R E Q V O X", w / 2f, h - 80f, p)

        shareBitmap(context, bmp, "freqvox_card.png", title)
    }

    /** Renders [visual] as the card's hero and returns the y-coordinate the title should start
     *  at. One case per [SegmentVisual] subtype — mirrors what SegmentVisualView draws on screen
     *  so the shared poster matches the card the user actually saw. */
    private fun drawSegmentHero(c: Canvas, p: Paint, visual: SegmentVisual, accentArgb: Int, w: Int): Float =
        when (visual) {
            is SegmentVisual.Emoji -> drawEmojiHero(c, p, visual.emoji)
            is SegmentVisual.AlbumArt -> drawAlbumArtHero(c, p, visual.track.artworkPath, w)
            is SegmentVisual.ArtCollage -> drawCollageHero(c, p, visual.tracks.mapNotNull { it.artworkPath }, w)
            is SegmentVisual.Illustration -> drawIllustrationHero(c, p, visual.kind, accentArgb, w)
            is SegmentVisual.Sparkline -> drawSparklineHero(c, p, visual.points, w)
            is SegmentVisual.Bars -> drawBarsHero(c, p, visual.items, accentArgb, w)
            is SegmentVisual.Histogram -> drawHistogramHero(c, p, visual.buckets, accentArgb, w)
            is SegmentVisual.StreakCalendar -> drawStreakHero(c, p, visual.activeDays, accentArgb, w)
        }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    private fun drawEmojiHero(c: Canvas, p: Paint, emoji: String): Float {
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.textSize = 300f
        c.drawText(emoji.ifBlank { "🎵" }, c.width / 2f, 500f, p)
        return 680f
    }

    private fun drawAlbumArtHero(c: Canvas, p: Paint, artPath: String?, w: Int): Float {
        val art = artPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
            ?: return drawEmojiHero(c, p, "🎵")
        val size = 480
        val left = (w - size) / 2f; val top = 180f
        val scaled = Bitmap.createScaledBitmap(art, size, size, true)
        val path = Path().apply { addRoundRect(left, top, left + size, top + size, 48f, 48f, Path.Direction.CW) }
        p.style = Paint.Style.FILL
        c.save(); c.clipPath(path); c.drawBitmap(scaled, left, top, p); c.restore()
        return top + size + 130f
    }

    private fun drawCollageHero(c: Canvas, p: Paint, artPaths: List<String>, w: Int): Float {
        val arts = artPaths.take(4).mapNotNull { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        if (arts.isEmpty()) return drawEmojiHero(c, p, "🎶")
        val size = 480
        val left = (w - size) / 2f; val top = 180f
        val path = Path().apply { addRoundRect(left, top, left + size, top + size, 48f, 48f, Path.Direction.CW) }
        p.style = Paint.Style.FILL
        c.save(); c.clipPath(path)
        p.color = Color.rgb(30, 30, 40); c.drawRect(left, top, left + size, top + size, p)
        val cell = size / 2
        arts.forEachIndexed { i, bmp ->
            val cx = left + (i % 2) * cell; val cy = top + (i / 2) * cell
            c.drawBitmap(Bitmap.createScaledBitmap(bmp, cell, cell, true), cx, cy, p)
        }
        c.restore()
        return top + size + 130f
    }

    private fun drawSparklineHero(c: Canvas, p: Paint, points: List<Float>, w: Int): Float {
        val left = 140f; val right = w - 140f; val top = 260f; val bottom = 560f
        if (points.size >= 2) {
            fun x(i: Int) = left + (right - left) * i / (points.size - 1)
            fun y(v: Float) = bottom - v.coerceIn(0f, 1f) * (bottom - top)
            val line = Path().apply {
                moveTo(x(0), y(points[0]))
                points.forEachIndexed { i, v -> if (i > 0) lineTo(x(i), y(v)) }
            }
            val fill = Path().apply { addPath(line); lineTo(right, bottom); lineTo(left, bottom); close() }
            p.style = Paint.Style.FILL; p.color = withAlpha(Color.WHITE, 0.15f); c.drawPath(fill, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 8f; p.color = Color.WHITE; c.drawPath(line, p)
            p.style = Paint.Style.FILL
            points.forEachIndexed { i, v -> c.drawCircle(x(i), y(v), 9f, p) }
        }
        return bottom + 130f
    }

    private fun drawBarsHero(c: Canvas, p: Paint, items: List<Pair<String, Int>>, accent: Int, w: Int): Float {
        var y = 240f
        val max = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        val left = 140f; val right = w - 140f; val barLeft = left + 280f
        items.take(5).forEach { (label, v) ->
            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.LEFT
            p.color = Color.WHITE; p.textSize = 34f; p.isFakeBoldText = true
            c.drawText(label.take(16), left, y + 28f, p)
            p.isFakeBoldText = false
            p.color = withAlpha(Color.WHITE, 0.12f)
            c.drawRoundRect(RectF(barLeft, y, right, y + 40f), 20f, 20f, p)
            val barW = (right - barLeft) * (v.toFloat() / max)
            p.color = accent
            c.drawRoundRect(RectF(barLeft, y, barLeft + barW, y + 40f), 20f, 20f, p)
            y += 74f
        }
        return y + 60f
    }

    private fun drawHistogramHero(c: Canvas, p: Paint, buckets: List<Int>, accent: Int, w: Int): Float {
        val top = 260f; val bottom = 560f
        val max = (buckets.maxOrNull() ?: 1).coerceAtLeast(1)
        val left = 140f; val right = w - 140f
        val n = buckets.size.coerceAtLeast(1)
        val barW = (right - left) / n
        p.style = Paint.Style.FILL
        buckets.forEachIndexed { i, count ->
            val f = count.toFloat() / max
            val barH = (bottom - top) * f.coerceAtLeast(0.02f)
            p.color = if (count > 0) withAlpha(accent, 0.4f + 0.5f * f) else withAlpha(Color.WHITE, 0.08f)
            c.drawRect(left + i * barW, bottom - barH, left + (i + 1) * barW - 2f, bottom, p)
        }
        return bottom + 130f
    }

    private fun drawStreakHero(c: Canvas, p: Paint, activeDays: List<Boolean>, accent: Int, w: Int): Float {
        val days = activeDays.takeLast(14)
        val top = 340f
        val dot = 34f; val spacing = 22f
        val totalWidth = days.size * dot + (days.size - 1).coerceAtLeast(0) * spacing
        var x = (w - totalWidth) / 2f
        p.style = Paint.Style.FILL
        days.forEach { active ->
            p.color = if (active) accent else withAlpha(Color.WHITE, 0.14f)
            c.drawCircle(x + dot / 2, top, dot / 2, p)
            x += dot + spacing
        }
        return top + 160f
    }

    /** Illustration hero: a canvas port of SegmentVisualView's drawIllustration, one case per
     *  [IllustrationKind], sized to the same 480×480 hero box every other visual uses. */
    private fun drawIllustrationHero(c: Canvas, p: Paint, kind: IllustrationKind, accent: Int, w: Int): Float {
        val size = 480f
        val ox = (w - size) / 2f; val oy = 180f
        val onDark = Color.WHITE

        fun rr(color: Int, l: Float, t: Float, r: Float, b: Float, rad: Float) {
            p.style = Paint.Style.FILL; p.color = color
            c.drawRoundRect(RectF(ox + l, oy + t, ox + r, oy + b), rad, rad, p)
        }
        fun circ(color: Int, cx: Float, cy: Float, radius: Float, stroke: Float? = null) {
            p.color = color
            if (stroke != null) { p.style = Paint.Style.STROKE; p.strokeWidth = stroke } else p.style = Paint.Style.FILL
            c.drawCircle(ox + cx, oy + cy, radius, p)
        }
        fun line(color: Int, x1: Float, y1: Float, x2: Float, y2: Float, strokeW: Float) {
            p.style = Paint.Style.STROKE; p.strokeWidth = strokeW; p.color = color
            c.drawLine(ox + x1, oy + y1, ox + x2, oy + y2, p)
        }
        fun tri(color: Int, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            p.style = Paint.Style.FILL; p.color = color
            val path = Path().apply { moveTo(ox + x1, oy + y1); lineTo(ox + x2, oy + y2); lineTo(ox + x3, oy + y3); close() }
            c.drawPath(path, p)
        }
        fun arc(color: Int, cx: Float, cy: Float, r: Float, startAngle: Float, sweepAngle: Float, strokeW: Float) {
            p.style = Paint.Style.STROKE; p.strokeWidth = strokeW; p.color = color
            c.drawArc(RectF(ox + cx - r, oy + cy - r, ox + cx + r, oy + cy + r), startAngle, sweepAngle, false, p)
        }
        fun rectFill(color: Int, l: Float, t: Float, r: Float, b: Float) {
            p.style = Paint.Style.FILL; p.color = color
            c.drawRect(ox + l, oy + t, ox + r, oy + b, p)
        }

        val w0 = size; val h0 = size
        when (kind) {
            IllustrationKind.FEED_FLOW -> repeat(4) { i ->
                val t = h0 * (0.12f + i * 0.16f)
                val inset = w0 * (0.14f + i * 0.03f)
                rr(if (i == 0) accent else withAlpha(onDark, 0.35f - i * 0.06f), inset, t, w0 - inset, t + h0 * 0.11f, 14f)
            }
            IllustrationKind.TAGS -> {
                val chips = listOf(0.2f to 0.3f, 0.55f to 0.3f, 0.3f to 0.52f, 0.62f to 0.55f, 0.25f to 0.72f)
                chips.forEachIndexed { i, (x, y) ->
                    rr(if (i % 2 == 0) accent else withAlpha(onDark, 0.4f), w0 * x, h0 * y, w0 * x + w0 * 0.22f, h0 * y + h0 * 0.11f, 30f)
                }
            }
            IllustrationKind.CROSSFADE -> {
                val path1 = Path(); val path2 = Path()
                val mid = h0 / 2
                for (i in 0..40) {
                    val x = w0 * i / 40f
                    val y1 = mid + kotlin.math.sin(i / 40f * 6.28f * 2).toFloat() * h0 * 0.18f
                    val y2 = mid + kotlin.math.cos(i / 40f * 6.28f * 2).toFloat() * h0 * 0.18f
                    if (i == 0) { path1.moveTo(ox + x, oy + y1); path2.moveTo(ox + x, oy + y2) }
                    else { path1.lineTo(ox + x, oy + y1); path2.lineTo(ox + x, oy + y2) }
                }
                p.style = Paint.Style.STROKE; p.strokeWidth = 6f
                p.color = accent; c.drawPath(path1, p)
                p.color = withAlpha(onDark, 0.55f); c.drawPath(path2, p)
            }
            IllustrationKind.SHUFFLE -> {
                circ(withAlpha(onDark, 0.12f), w0 / 2, h0 / 2, w0 * 0.32f)
                listOf(-1f, 1f).forEach { s ->
                    val path = Path().apply {
                        moveTo(ox + w0 * 0.28f, oy + h0 * (0.5f + 0.16f * s))
                        cubicTo(
                            ox + w0 * 0.45f, oy + h0 * (0.5f + 0.16f * s),
                            ox + w0 * 0.55f, oy + h0 * (0.5f - 0.16f * s),
                            ox + w0 * 0.72f, oy + h0 * (0.5f - 0.16f * s)
                        )
                    }
                    p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = accent
                    c.drawPath(path, p)
                }
            }
            IllustrationKind.STATS -> {
                val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f)
                heights.forEachIndexed { i, hf ->
                    val left = w0 * (0.2f + i * 0.13f)
                    rr(if (i == 3) accent else withAlpha(onDark, 0.35f), left, h0 * (0.85f - hf * 0.6f), left + w0 * 0.09f, h0 * (0.85f - hf * 0.6f) + h0 * hf * 0.6f, 8f)
                }
            }
            IllustrationKind.GENERIC -> {
                circ(withAlpha(accent, 0.3f), w0 / 2, h0 / 2, w0 * 0.28f)
                circ(onDark, w0 / 2, h0 / 2, w0 * 0.06f)
            }
            IllustrationKind.GESTURE -> {
                circ(withAlpha(onDark, 0.10f), w0 * 0.5f, h0 * 0.5f, w0 * 0.34f)
                circ(withAlpha(accent, 0.30f), w0 * 0.5f, h0 * 0.5f, w0 * 0.24f, stroke = 5f)
                circ(withAlpha(accent, 0.55f), w0 * 0.5f, h0 * 0.5f, w0 * 0.15f, stroke = 5f)
                circ(accent, w0 * 0.5f, h0 * 0.5f, w0 * 0.07f)
            }
            IllustrationKind.TIMER -> {
                circ(withAlpha(onDark, 0.12f), w0 / 2, h0 * 0.55f, w0 * 0.30f)
                circ(accent, w0 / 2, h0 * 0.55f, w0 * 0.30f, stroke = 6f)
                line(onDark, w0 / 2, h0 * 0.55f, w0 / 2, h0 * 0.55f - w0 * 0.18f, 6f)
                line(onDark, w0 / 2, h0 * 0.55f, w0 / 2 + w0 * 0.12f, h0 * 0.55f, 6f)
                line(accent, w0 / 2, h0 * 0.18f, w0 / 2, h0 * 0.24f, 6f)
            }
            IllustrationKind.LYRICS -> {
                val widths = listOf(0.5f, 0.7f, 0.55f, 0.65f)
                widths.forEachIndexed { i, wf ->
                    val highlighted = i == 1
                    rr(
                        if (highlighted) accent else withAlpha(onDark, 0.28f),
                        w0 * 0.5f - (w0 * wf) / 2, h0 * (0.28f + i * 0.14f),
                        w0 * 0.5f + (w0 * wf) / 2, h0 * (0.28f + i * 0.14f) + h0 * 0.06f, 20f
                    )
                }
            }
            IllustrationKind.PLAYLIST -> {
                repeat(3) { i -> rr(withAlpha(onDark, 0.4f), w0 * 0.2f, h0 * (0.3f + i * 0.13f), w0 * 0.2f + w0 * 0.4f, h0 * (0.3f + i * 0.13f) + h0 * 0.06f, 12f) }
                val path = Path().apply {
                    moveTo(ox + w0 * 0.68f, oy + h0 * 0.32f); lineTo(ox + w0 * 0.68f, oy + h0 * 0.56f); lineTo(ox + w0 * 0.84f, oy + h0 * 0.44f); close()
                }
                p.style = Paint.Style.FILL; p.color = accent
                c.drawPath(path, p)
            }
            IllustrationKind.SHARE -> {
                val ax = w0 * 0.68f; val ay = h0 * 0.28f
                val bx = w0 * 0.68f; val by = h0 * 0.72f
                val sx = w0 * 0.32f; val sy = h0 * 0.5f
                line(withAlpha(onDark, 0.5f), sx, sy, ax, ay, 5f)
                line(withAlpha(onDark, 0.5f), sx, sy, bx, by, 5f)
                circ(accent, ax, ay, w0 * 0.07f)
                circ(accent, bx, by, w0 * 0.07f)
                circ(onDark, sx, sy, w0 * 0.08f)
            }
            IllustrationKind.SEARCH -> {
                circ(accent, w0 * 0.44f, h0 * 0.44f, w0 * 0.20f, stroke = 7f)
                line(onDark, w0 * 0.44f + w0 * 0.15f, h0 * 0.44f + h0 * 0.15f, w0 * 0.72f, h0 * 0.72f, 9f)
            }
            IllustrationKind.EQUALIZER -> {
                val heights = listOf(0.35f, 0.55f, 0.3f, 0.6f)
                val xs = listOf(0.28f, 0.43f, 0.58f, 0.73f)
                xs.forEachIndexed { i, x ->
                    line(withAlpha(onDark, 0.25f), w0 * x, h0 * 0.2f, w0 * x, h0 * 0.8f, 4f)
                    circ(accent, w0 * x, h0 * heights[i], w0 * 0.035f)
                }
            }
            IllustrationKind.VOLUME -> {
                val cx = w0 * 0.5f; val cy = h0 * 0.5f
                rr(withAlpha(onDark, 0.5f), w0 * 0.22f, h0 * 0.42f, w0 * 0.22f + w0 * 0.12f, h0 * 0.42f + h0 * 0.16f, 4f)
                tri(withAlpha(onDark, 0.5f), w0 * 0.34f, h0 * 0.36f, w0 * 0.34f, h0 * 0.64f, w0 * 0.5f, h0 * 0.5f)
                listOf(0.10f, 0.17f, 0.24f).forEach { rf -> arc(withAlpha(accent, 0.8f), cx, cy, w0 * rf, -35f, 70f, 4f) }
            }
            IllustrationKind.PALETTE -> {
                circ(withAlpha(onDark, 0.10f), w0 / 2, h0 / 2, w0 * 0.30f)
                listOf(
                    Triple(0.38f, 0.4f, 0.9f), Triple(0.62f, 0.38f, 0.6f),
                    Triple(0.4f, 0.62f, 0.7f), Triple(0.64f, 0.6f, 0.45f)
                ).forEach { (x, y, a) -> circ(withAlpha(accent, a), w0 * x, h0 * y, w0 * 0.06f) }
            }
            IllustrationKind.THEME -> {
                val sunCx = w0 * 0.32f; val sunCy = h0 * 0.5f; val sunR = w0 * 0.10f
                circ(accent, sunCx, sunCy, sunR, stroke = 4f)
                listOf(0.0, 60.0, 300.0).forEach { deg ->
                    val rad = Math.toRadians(deg)
                    val inner = sunR + w0 * 0.02f; val outer = sunR + w0 * 0.06f
                    line(
                        accent,
                        sunCx + (inner * kotlin.math.cos(rad)).toFloat(), sunCy + (inner * kotlin.math.sin(rad)).toFloat(),
                        sunCx + (outer * kotlin.math.cos(rad)).toFloat(), sunCy + (outer * kotlin.math.sin(rad)).toFloat(),
                        4f
                    )
                }
                circ(onDark, w0 * 0.66f, h0 * 0.5f, w0 * 0.12f)
            }
            IllustrationKind.CLEANUP -> {
                rr(withAlpha(onDark, 0.25f), w0 * 0.26f, h0 * 0.26f, w0 * 0.26f + w0 * 0.36f, h0 * 0.26f + h0 * 0.36f, 14f)
                rr(accent, w0 * 0.38f, h0 * 0.38f, w0 * 0.38f + w0 * 0.36f, h0 * 0.38f + h0 * 0.36f, 14f)
                circ(onDark, w0 * 0.74f, h0 * 0.74f, w0 * 0.07f)
                line(accent, w0 * 0.705f, h0 * 0.74f, w0 * 0.735f, h0 * 0.77f, 5f)
                line(accent, w0 * 0.735f, h0 * 0.77f, w0 * 0.78f, h0 * 0.71f, 5f)
            }
            IllustrationKind.SAVE -> {
                rr(withAlpha(onDark, 0.35f), w0 * 0.26f, h0 * 0.26f, w0 * 0.26f + w0 * 0.48f, h0 * 0.26f + h0 * 0.48f, 16f)
                rectFill(accent, w0 * 0.36f, h0 * 0.26f, w0 * 0.36f + w0 * 0.22f, h0 * 0.26f + h0 * 0.14f)
                line(onDark, w0 * 0.32f, h0 * 0.62f, w0 * 0.68f, h0 * 0.62f, 5f)
            }
            IllustrationKind.FOLDER -> {
                rr(accent, w0 * 0.22f, h0 * 0.34f, w0 * 0.22f + w0 * 0.22f, h0 * 0.34f + h0 * 0.10f, 6f)
                rr(withAlpha(onDark, 0.3f), w0 * 0.22f, h0 * 0.40f, w0 * 0.22f + w0 * 0.56f, h0 * 0.40f + h0 * 0.30f, 8f)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = withAlpha(accent, 0.5f)
                c.drawRoundRect(RectF(ox + w0 * 0.22f, oy + h0 * 0.40f, ox + w0 * 0.22f + w0 * 0.56f, oy + h0 * 0.40f + h0 * 0.30f), 8f, 8f, p)
            }
            IllustrationKind.ARTWORK -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = withAlpha(onDark, 0.5f)
                c.drawRoundRect(RectF(ox + w0 * 0.24f, oy + h0 * 0.30f, ox + w0 * 0.24f + w0 * 0.52f, oy + h0 * 0.30f + h0 * 0.42f), 10f, 10f, p)
                circ(accent, w0 * 0.36f, h0 * 0.42f, w0 * 0.045f)
                val mtn = Path().apply {
                    moveTo(ox + w0 * 0.28f, oy + h0 * 0.68f); lineTo(ox + w0 * 0.42f, oy + h0 * 0.46f); lineTo(ox + w0 * 0.50f, oy + h0 * 0.56f)
                    lineTo(ox + w0 * 0.60f, oy + h0 * 0.42f); lineTo(ox + w0 * 0.72f, oy + h0 * 0.68f); close()
                }
                p.style = Paint.Style.FILL; p.color = withAlpha(onDark, 0.4f)
                c.drawPath(mtn, p)
            }
            IllustrationKind.RADIO -> {
                line(withAlpha(onDark, 0.6f), w0 * 0.36f, h0 * 0.28f, w0 * 0.36f, h0 * 0.5f, 4f)
                circ(accent, w0 * 0.36f, h0 * 0.26f, w0 * 0.03f)
                rr(withAlpha(onDark, 0.35f), w0 * 0.26f, h0 * 0.5f, w0 * 0.26f + w0 * 0.28f, h0 * 0.5f + h0 * 0.18f, 8f)
                listOf(0.10f, 0.18f, 0.26f).forEach { rf -> arc(withAlpha(accent, 0.75f), w0 * 0.36f, h0 * 0.26f, w0 * rf, -55f, 40f, 4f) }
            }
            IllustrationKind.EDIT -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = 14f; p.strokeCap = Paint.Cap.ROUND; p.color = onDark
                c.drawLine(ox + w0 * 0.28f, oy + h0 * 0.72f, ox + w0 * 0.62f, oy + h0 * 0.38f, p)
                p.strokeCap = Paint.Cap.BUTT
                tri(accent, w0 * 0.62f, h0 * 0.38f, w0 * 0.70f, h0 * 0.30f, w0 * 0.72f, h0 * 0.40f)
            }
            IllustrationKind.SETTINGS -> {
                for (i in 0 until 8) {
                    val ang = Math.toRadians((i * 45).toDouble())
                    val r = w0 * 0.30f
                    circ(withAlpha(onDark, 0.35f), w0 / 2 + (r * kotlin.math.cos(ang)).toFloat(), h0 / 2 + (r * kotlin.math.sin(ang)).toFloat(), w0 * 0.035f)
                }
                circ(accent, w0 / 2, h0 / 2, w0 * 0.20f)
                circ(withAlpha(onDark, 0.5f), w0 / 2, h0 / 2, w0 * 0.08f)
            }
            IllustrationKind.SPEED -> {
                listOf(0f, 0.16f).forEach { off ->
                    tri(accent, w0 * (0.32f + off), h0 * 0.32f, w0 * (0.32f + off), h0 * 0.68f, w0 * (0.50f + off), h0 * 0.5f)
                }
            }
            IllustrationKind.REWIND -> {
                listOf(0f, 0.16f).forEach { off ->
                    tri(accent, w0 * (0.68f - off), h0 * 0.32f, w0 * (0.68f - off), h0 * 0.68f, w0 * (0.50f - off), h0 * 0.5f)
                }
            }
            IllustrationKind.STOP -> {
                circ(withAlpha(onDark, 0.14f), w0 / 2, h0 / 2, w0 * 0.28f)
                rr(accent, w0 * 0.38f, h0 * 0.38f, w0 * 0.38f + w0 * 0.24f, h0 * 0.38f + h0 * 0.24f, 6f)
            }
            IllustrationKind.CAR -> {
                rr(accent, w0 * 0.35f, h0 * 0.38f, w0 * 0.35f + w0 * 0.30f, h0 * 0.38f + h0 * 0.14f, 10f)
                rr(accent, w0 * 0.20f, h0 * 0.48f, w0 * 0.20f + w0 * 0.60f, h0 * 0.48f + h0 * 0.16f, 10f)
                circ(onDark, w0 * 0.32f, h0 * 0.66f, w0 * 0.05f)
                circ(onDark, w0 * 0.68f, h0 * 0.66f, w0 * 0.05f)
            }
            IllustrationKind.FLAME -> {
                val flame = Path().apply {
                    moveTo(ox + w0 * 0.5f, oy + h0 * 0.22f)
                    cubicTo(ox + w0 * 0.72f, oy + h0 * 0.42f, ox + w0 * 0.68f, oy + h0 * 0.68f, ox + w0 * 0.5f, oy + h0 * 0.78f)
                    cubicTo(ox + w0 * 0.32f, oy + h0 * 0.68f, ox + w0 * 0.28f, oy + h0 * 0.42f, ox + w0 * 0.5f, oy + h0 * 0.22f)
                    close()
                }
                p.style = Paint.Style.FILL; p.color = accent
                c.drawPath(flame, p)
                val inner = Path().apply {
                    moveTo(ox + w0 * 0.5f, oy + h0 * 0.42f)
                    cubicTo(ox + w0 * 0.60f, oy + h0 * 0.52f, ox + w0 * 0.58f, oy + h0 * 0.64f, ox + w0 * 0.5f, oy + h0 * 0.70f)
                    cubicTo(ox + w0 * 0.42f, oy + h0 * 0.64f, ox + w0 * 0.42f, oy + h0 * 0.52f, ox + w0 * 0.5f, oy + h0 * 0.42f)
                    close()
                }
                p.color = withAlpha(onDark, 0.35f)
                c.drawPath(inner, p)
            }
            IllustrationKind.TROPHY -> {
                val cup = Path().apply {
                    moveTo(ox + w0 * 0.34f, oy + h0 * 0.28f); lineTo(ox + w0 * 0.66f, oy + h0 * 0.28f)
                    lineTo(ox + w0 * 0.58f, oy + h0 * 0.52f); lineTo(ox + w0 * 0.42f, oy + h0 * 0.52f); close()
                }
                p.style = Paint.Style.FILL; p.color = accent
                c.drawPath(cup, p)
                arc(accent, w0 * 0.27f, h0 * 0.35f, w0 * 0.09f, 100f, 160f, 4f)
                arc(accent, w0 * 0.73f, h0 * 0.35f, w0 * 0.09f, -80f, 160f, 4f)
                rectFill(withAlpha(onDark, 0.5f), w0 * 0.47f, h0 * 0.52f, w0 * 0.47f + w0 * 0.06f, h0 * 0.52f + h0 * 0.10f)
                rr(withAlpha(onDark, 0.5f), w0 * 0.36f, h0 * 0.62f, w0 * 0.36f + w0 * 0.28f, h0 * 0.62f + h0 * 0.06f, 4f)
            }
            IllustrationKind.STAR -> {
                val outerR = w0 * 0.30f; val innerR = w0 * 0.13f
                val path = Path()
                for (i in 0 until 10) {
                    val ang = Math.toRadians((-90 + i * 36).toDouble())
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = ox + w0 / 2 + (r * kotlin.math.cos(ang)).toFloat()
                    val y = oy + h0 / 2 + (r * kotlin.math.sin(ang)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                p.style = Paint.Style.FILL; p.color = accent
                c.drawPath(path, p)
            }
            IllustrationKind.GEM -> {
                val gem = Path().apply {
                    moveTo(ox + w0 * 0.32f, oy + h0 * 0.34f)
                    lineTo(ox + w0 * 0.68f, oy + h0 * 0.34f)
                    lineTo(ox + w0 * 0.80f, oy + h0 * 0.46f)
                    lineTo(ox + w0 * 0.5f, oy + h0 * 0.76f)
                    lineTo(ox + w0 * 0.20f, oy + h0 * 0.46f)
                    close()
                }
                p.style = Paint.Style.FILL; p.color = accent
                c.drawPath(gem, p)
                line(withAlpha(onDark, 0.35f), w0 * 0.32f, h0 * 0.34f, w0 * 0.5f, h0 * 0.46f, 3f)
                line(withAlpha(onDark, 0.35f), w0 * 0.68f, h0 * 0.34f, w0 * 0.5f, h0 * 0.46f, 3f)
                line(withAlpha(onDark, 0.35f), w0 * 0.5f, h0 * 0.46f, w0 * 0.5f, h0 * 0.76f, 3f)
            }
        }
        return oy + size + 130f
    }

    /** Greedy word wrap against the paint's current text size. */
    private fun wrapText(text: String, p: Paint, maxWidth: Float): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (p.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun shareBitmap(context: Context, bmp: Bitmap, fileName: String, text: String) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun render(
        weekMinutes: Long, dailyMinutes: List<Float>, topArtist: String?, topTrack: String?, streakDays: Int, artPath: String?
    ): Bitmap {
        val w = 1080; val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        val seed = artPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }?.let {
            val v = Palette.from(it).generate().getVibrantColor(0); if (v != 0) v else null
        } ?: 0xFF7C4DFF.toInt()
        val hsv = FloatArray(3); Color.colorToHSV(seed, hsv)
        val mid = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceIn(0.4f, 0.9f), 0.42f))
        val dark = Color.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.9f).coerceIn(0f, 1f), 0.09f))
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(mid, dark), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        p.color = Color.WHITE
        p.textAlign = Paint.Align.LEFT
        p.isFakeBoldText = true
        p.textSize = 58f
        c.drawText("Your week", 80f, 200f, p)
        p.isFakeBoldText = false
        p.color = Color.argb(200, 255, 255, 255)
        p.textSize = 40f
        c.drawText("in Freqvox", 80f, 255f, p)

        // Big minutes
        p.color = Color.WHITE
        p.isFakeBoldText = true
        p.textSize = 200f
        c.drawText("$weekMinutes", 76f, 520f, p)
        p.isFakeBoldText = false
        p.textSize = 44f
        p.color = Color.argb(210, 255, 255, 255)
        c.drawText("minutes this week", 80f, 585f, p)

        // Sparkline band
        if (dailyMinutes.size >= 2) {
            val left = 80f; val right = w - 80f; val top = 660f; val bottom = 860f
            fun x(i: Int) = left + (right - left) * i / (dailyMinutes.size - 1)
            fun y(v: Float) = bottom - v.coerceIn(0f, 1f) * (bottom - top)
            val line = Path().apply {
                moveTo(x(0), y(dailyMinutes[0]))
                dailyMinutes.forEachIndexed { i, v -> if (i > 0) lineTo(x(i), y(v)) }
            }
            val fill = Path().apply { addPath(line); lineTo(right, bottom); lineTo(left, bottom); close() }
            p.color = Color.argb(60, 255, 255, 255); p.style = Paint.Style.FILL; c.drawPath(fill, p)
            p.color = Color.WHITE; p.style = Paint.Style.STROKE; p.strokeWidth = 8f; c.drawPath(line, p)
            p.style = Paint.Style.FILL
            dailyMinutes.forEachIndexed { i, v -> c.drawCircle(x(i), y(v), 9f, p) }
        }

        // Rows
        var ry = 1010f
        fun row(label: String, value: String?) {
            if (value.isNullOrBlank()) return
            p.color = Color.argb(150, 255, 255, 255); p.textSize = 28f; p.isFakeBoldText = true
            c.drawText(label.uppercase(), 80f, ry, p)
            p.color = Color.WHITE; p.textSize = 46f; p.isFakeBoldText = false
            c.drawText(value.take(28), 80f, ry + 56f, p)
            ry += 130f
        }
        row("Top artist", topArtist)
        row("On repeat", topTrack)
        if (streakDays >= 2) row("Streak", "$streakDays days")

        p.color = Color.argb(170, 255, 255, 255); p.isFakeBoldText = true; p.textSize = 34f
        p.textAlign = Paint.Align.CENTER
        c.drawText("F R E Q V O X", w / 2f, h - 80f, p)

        return bmp
    }
}
