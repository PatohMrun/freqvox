package com.freqvox.data.artwork

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks up cover art from free, key-less metadata APIs (iTunes Search, then Deezer)
 * and downloads the chosen image. Because side-loaded files often carry junk
 * metadata, every candidate passes a confidence gate before its art is accepted — a
 * wrong cover is worse than the generated monogram.
 */
@Singleton
class RemoteArtworkClient @Inject constructor(
    private val client: OkHttpClient
) {
    /** Downloads the image at a known exact URL (e.g. a Vidmate thumbnail). No matching. */
    suspend fun fetchFromUrl(url: String): ByteArray? = withContext(Dispatchers.IO) {
        downloadImage(url)
    }

    /** Returns JPEG/PNG bytes for a confident match, or null if nothing matched. */
    suspend fun fetchCover(artist: String, title: String): ByteArray? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null
        val imageUrl = fetchFromItunes(artist, title)
            ?: fetchFromDeezer(artist, title)
            ?: return@withContext null
        downloadImage(imageUrl)
    }

    // iTunes returns artworkUrl100 (100px); swapping the size token yields a larger image.
    private fun fetchFromItunes(artist: String, title: String): String? {
        val term = encode(listOf(artist, title))
        val body = httpGet("https://itunes.apple.com/search?term=$term&entity=song&media=music&limit=5")
            ?: return null
        val results = runCatching { JSONObject(body).optJSONArray("results") }.getOrNull() ?: return null
        for (i in 0 until results.length()) {
            val r = results.optJSONObject(i) ?: continue
            if (isConfidentMatch(artist, title, r.optString("artistName"), r.optString("trackName"))) {
                val art = r.optString("artworkUrl100")
                if (art.isNotBlank()) return art.replace("100x100bb", "600x600bb")
            }
        }
        return null
    }

    private fun fetchFromDeezer(artist: String, title: String): String? {
        val q = encode(listOf(artist, title))
        val body = httpGet("https://api.deezer.com/search?q=$q&limit=5") ?: return null
        val data = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return null
        for (i in 0 until data.length()) {
            val r = data.optJSONObject(i) ?: continue
            val foundArtist = r.optJSONObject("artist")?.optString("name").orEmpty()
            if (isConfidentMatch(artist, title, foundArtist, r.optString("title"))) {
                val album = r.optJSONObject("album")
                val cover = album?.optString("cover_xl").orEmpty()
                    .ifBlank { album?.optString("cover_big").orEmpty() }
                if (cover.isNotBlank()) return cover
            }
        }
        return null
    }

    private fun httpGet(url: String): String? = try {
        val request = Request.Builder().url(url).header("User-Agent", "Freqvox/1.0").build()
        client.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (e: Exception) {
        null
    }

    private fun downloadImage(url: String): ByteArray? = try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.bytes() else null
        }
    } catch (e: Exception) {
        null
    }

    private fun encode(parts: List<String>): String =
        URLEncoder.encode(parts.filter { it.isNotBlank() }.joinToString(" "), "UTF-8")

    // ── Confidence gate ──────────────────────────────────────────────────────
    // The title is the strong signal; the artist is only checked when we actually
    // have a real one (not "Unknown Artist", which the resolver passes as blank).
    private fun isConfidentMatch(qArtist: String, qTitle: String, fArtist: String, fTitle: String): Boolean {
        if (!tokensMatch(qTitle, fTitle, 0.6)) return false
        if (qArtist.isBlank()) return true
        return tokensMatch(qArtist, fArtist, 0.5)
    }

    private fun tokensMatch(a: String, b: String, threshold: Double): Boolean {
        val ta = tokenize(a)
        val tb = tokenize(b)
        if (ta.isEmpty() || tb.isEmpty()) return false
        // Containment (one is a subset of the other) catches "Song" vs "Song (Remastered)".
        if (ta.containsAll(tb) || tb.containsAll(ta)) return true
        val overlap = ta.intersect(tb).size.toDouble()
        return overlap / minOf(ta.size, tb.size) >= threshold
    }

    private fun tokenize(s: String): Set<String> =
        s.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
}
