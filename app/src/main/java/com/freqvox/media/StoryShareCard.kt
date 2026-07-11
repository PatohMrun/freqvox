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
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
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
     * Shares any single story card (tip / milestone / algorithm explainer) as a poster:
     * accent-derived gradient, the card's emoji or album art as the hero, then title + body.
     * The weekly recap keeps its richer dedicated poster ([shareRecap]); this covers the rest,
     * so the share button always reflects the card actually on screen.
     */
    fun shareSegment(
        context: Context,
        title: String,
        body: String,
        emoji: String?,
        artPath: String?,
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

        // Hero: album art (rounded square) when it decodes, else an emoji — ALWAYS one of
        // the two. Cards whose art file is missing/unreadable used to render a blank hole
        // where the cover should be; now they fall back to a music-note badge.
        val art = artPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        var textTop = 640f
        if (art != null) {
            val size = 480
            val left = (w - size) / 2f; val top = 180f
            val scaled = Bitmap.createScaledBitmap(art, size, size, true)
            val path = Path().apply {
                addRoundRect(left, top, left + size, top + size, 48f, 48f, Path.Direction.CW)
            }
            c.save(); c.clipPath(path)
            c.drawBitmap(scaled, left, top, p)
            c.restore()
            textTop = top + size + 130f
        } else {
            val hero = emoji?.takeIf { it.isNotBlank() } ?: "🎵"
            p.color = Color.WHITE
            p.textAlign = Paint.Align.CENTER
            p.textSize = 300f
            c.drawText(hero, w / 2f, 500f, p)
            textTop = 680f
        }

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
