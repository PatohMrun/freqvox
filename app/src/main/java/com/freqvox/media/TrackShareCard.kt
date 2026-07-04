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
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import com.freqvox.data.local.entities.TrackEntity
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a shareable "now playing" style poster card for a track (album art over a gradient
 * derived from the cover, with title/artist and the Freqvox wordmark) and fires the Android
 * share sheet. The PNG is written to cacheDir/shared and exposed through the app's FileProvider.
 */
object TrackShareCard {

    fun shareTrackCard(context: Context, track: TrackEntity) {
        val bitmap = render(track)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "freqvox_share.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share track").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun render(track: TrackEntity): Bitmap {
        val w = 1080
        val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val art = track.artworkPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
        val seed = art?.let {
            val vibrant = Palette.from(it).generate().getVibrantColor(0)
            if (vibrant != 0) vibrant else null
        } ?: titleColor(track.title)

        val hsv = FloatArray(3)
        Color.colorToHSV(seed, hsv)
        val mid = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceIn(0.4f, 0.9f), 0.42f))
        val dark = Color.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.9f).coerceIn(0f, 1f), 0.10f))
        paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(mid, dark), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Album art (or a solid tile) as a rounded square.
        val artSize = 720f
        val artLeft = (w - artSize) / 2f
        val artTop = 180f
        val rect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
        if (art != null) {
            val path = Path().apply { addRoundRect(rect, 36f, 36f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(art, Rect(0, 0, art.width, art.height), rect, paint)
            canvas.restore()
        } else {
            paint.color = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 0.5f))
            canvas.drawRoundRect(rect, 36f, 36f, paint)
        }

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.isFakeBoldText = true
        paint.textSize = 66f
        canvas.drawText(ellipsize(track.title, paint, w - 160f), w / 2f, artTop + artSize + 130f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 46f
        paint.color = Color.argb(220, 255, 255, 255)
        canvas.drawText(ellipsize(track.artist, paint, w - 160f), w / 2f, artTop + artSize + 200f, paint)

        paint.isFakeBoldText = true
        paint.textSize = 36f
        paint.color = Color.argb(170, 255, 255, 255)
        canvas.drawText("F R E Q V O X", w / 2f, h - 90f, paint)

        return bmp
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) t = t.dropLast(1)
        return "$t…"
    }

    private fun titleColor(title: String): Int {
        val hue = ((title.hashCode() % 360) + 360) % 360
        return Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.7f, 0.6f))
    }
}
