package com.freqvox.data.artwork

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Returns JPEG/PNG bytes for a confident match, or null if nothing matched. Sources are
     *  tried in order: iTunes → Deezer (both nice square covers), then MusicBrainz + Cover
     *  Art Archive — keyless open data that fills real albums iTunes/Deezer miss. */
    suspend fun fetchCover(artist: String, title: String): ByteArray? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null
        val imageUrl = fetchFromItunes(artist, title) ?: fetchFromDeezer(artist, title)
        if (imageUrl != null) {
            val bytes = downloadImage(imageUrl)
            if (bytes != null) return@withContext bytes
        }
        // Fallback: returns the image bytes directly because a match may have to probe a few
        // releases (not every release in the Archive has a front cover).
        fetchFromCoverArtArchive(artist, title)
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

    /**
     * MusicBrainz match → Cover Art Archive front image. Two-hop and keyless:
     *  1. search MusicBrainz recordings for the song (rate-limited to ≤1 req/s, as they
     *     require, with a descriptive User-Agent or they reject the request);
     *  2. for each confidently-matched recording, walk its releases and pull the front
     *     cover from the Cover Art Archive — the first release that actually has one wins.
     * Returns image bytes (not a URL) because step 2 may probe several releases.
     */
    private suspend fun fetchFromCoverArtArchive(artist: String, title: String): ByteArray? {
        val query = buildString {
            append("recording:\"").append(escapeLucene(title)).append('"')
            if (artist.isNotBlank()) append(" AND artist:\"").append(escapeLucene(artist)).append('"')
        }
        val url = "https://musicbrainz.org/ws/2/recording/?query=" +
            URLEncoder.encode(query, "UTF-8") + "&fmt=json&limit=5"

        throttleMusicBrainz()
        val body = httpGet(url, MUSICBRAINZ_USER_AGENT) ?: return null
        val recordings = runCatching { JSONObject(body).optJSONArray("recordings") }
            .getOrNull() ?: return null

        // Collect release MBIDs from confidently-matched recordings, bounded so a single
        // lookup never fans out into many Archive requests.
        val releaseIds = LinkedHashSet<String>()
        outer@ for (i in 0 until recordings.length()) {
            val rec = recordings.optJSONObject(i) ?: continue
            val recArtist = rec.optJSONArray("artist-credit")
                ?.optJSONObject(0)?.optString("name").orEmpty()
            if (!isConfidentMatch(artist, title, recArtist, rec.optString("title"))) continue
            val releases = rec.optJSONArray("releases") ?: continue
            for (j in 0 until releases.length()) {
                releases.optJSONObject(j)?.optString("id")
                    ?.takeIf { it.isNotBlank() }?.let { releaseIds.add(it) }
                if (releaseIds.size >= MAX_CAA_RELEASES) break@outer
            }
        }

        for (id in releaseIds) {
            // CAA front endpoint 307-redirects to archive.org (OkHttp follows by default);
            // a release with no cover 404s, so we just move to the next candidate.
            val bytes = downloadImage("https://coverartarchive.org/release/$id/front-500")
            if (bytes != null) return bytes
        }
        return null
    }

    // MusicBrainz asks for ≤1 request/second; serialize search calls and space them out.
    private val musicBrainzMutex = Mutex()
    private var lastMusicBrainzRequestMs = 0L
    private suspend fun throttleMusicBrainz() {
        musicBrainzMutex.withLock {
            val wait = MUSICBRAINZ_MIN_INTERVAL_MS - (System.currentTimeMillis() - lastMusicBrainzRequestMs)
            if (wait > 0) delay(wait)
            lastMusicBrainzRequestMs = System.currentTimeMillis()
        }
    }

    /** Escape the Lucene phrase specials so a title with quotes/backslashes can't break the
     *  query (or inject operators). */
    private fun escapeLucene(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun httpGet(url: String, userAgent: String = "Freqvox/1.0"): String? = try {
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
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

    private companion object {
        // MusicBrainz requires a descriptive User-Agent with a contact (a generic/default one
        // is rejected). The repo URL is the contact.
        const val MUSICBRAINZ_USER_AGENT = "Freqvox/2.20 ( https://github.com/PatohMrun/freqvox )"
        // ≥1s between MusicBrainz searches; 1100ms gives headroom against their limiter.
        const val MUSICBRAINZ_MIN_INTERVAL_MS = 1100L
        // Cap Archive cover probes per lookup so one match can't fan out into many requests.
        const val MAX_CAA_RELEASES = 4
    }
}
