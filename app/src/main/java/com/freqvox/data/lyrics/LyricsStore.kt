package com.freqvox.data.lyrics

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.freqvox.data.local.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Persistence + retrieval for lyrics. Fixes two long-standing holes in the sheet:
 *
 *  1. A user-picked .lrc used to be parsed straight into composable state and vanish the
 *     moment the sheet closed. Now it's COPIED into app storage (filesDir/lyrics) keyed by
 *     a stable per-track hash, so it survives sheet closes, app restarts and rescans.
 *  2. The "sidecar .lrc next to the song" lookup did `File(track.path)` — but track.path is
 *     a content:// URI, so it never resolved. The real filesystem path is queried from
 *     MediaStore's DATA column here instead.
 *
 * Lookup order: saved copy → sidecar file → (explicitly, via [fetchOnline]) LRCLIB.
 *
 * The stable key hashes the REAL file path (survives MediaStore id reissue, same rationale
 * as stats preservation); when that's unavailable it falls back to title|artist|duration.
 */
class LyricsStore(private val context: Context) {

    private val dir: File by lazy { File(context.filesDir, "lyrics").apply { mkdirs() } }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    sealed class OnlineResult {
        data class Found(val lines: List<String>, val synced: Boolean, val source: String) : OnlineResult()
        object NotFound : OnlineResult()
        data class Error(val message: String) : OnlineResult()
    }

    /** The track's real filesystem path, via MediaStore (track.path is a content:// URI). */
    private fun realPathFor(track: TrackEntity): String? = try {
        context.contentResolver.query(
            Uri.parse(track.path),
            arrayOf(MediaStore.Audio.Media.DATA), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Exception) {
        null
    }

    private fun keyFor(track: TrackEntity): String {
        val seed = realPathFor(track) ?: "${track.title}|${track.artist}|${track.duration}"
        return MessageDigest.getInstance("MD5").digest(seed.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun savedFileFor(track: TrackEntity): File = File(dir, "${keyFor(track)}.lrc")

    // ── Per-track sync offset ───────────────────────────────────────────────────
    // Some .lrc timings are simply off against the user's rip of a song. The offset is
    // remembered per track (same stable key as the lyrics file): positive = lyrics fire
    // earlier (for lyrics that lag the voice), negative = later.

    private val offsetPrefs =
        context.getSharedPreferences("freqvox_lyrics_offsets", Context.MODE_PRIVATE)

    // Suspend: keyFor resolves the real path via a MediaStore query — not main-thread work.
    suspend fun offsetFor(track: TrackEntity): Long = withContext(Dispatchers.IO) {
        offsetPrefs.getLong(keyFor(track), 0L)
    }

    suspend fun setOffset(track: TrackEntity, offsetMs: Long) = withContext(Dispatchers.IO) {
        offsetPrefs.edit().apply {
            if (offsetMs == 0L) remove(keyFor(track)) else putLong(keyFor(track), offsetMs)
        }.apply()
    }

    /** Saved copy first, then the sidecar next to the audio file. Null = nothing stored. */
    suspend fun load(track: TrackEntity): List<String>? = withContext(Dispatchers.IO) {
        try {
            val saved = savedFileFor(track)
            if (saved.exists()) return@withContext saved.readLines()
            val real = realPathFor(track) ?: return@withContext null
            val rf = File(real)
            val sidecar = File(rf.parent ?: return@withContext null, rf.nameWithoutExtension + ".lrc")
            if (sidecar.exists()) sidecar.readLines() else null
        } catch (_: Exception) {
            null
        }
    }

    /** Copies a user-picked document into the store (making it permanent), returns its lines. */
    suspend fun importFrom(track: TrackEntity, uri: Uri): List<String>? =
        withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: return@withContext null
                if (text.isBlank()) return@withContext null
                savedFileFor(track).writeText(text)
                text.lines()
            } catch (_: Exception) {
                null
            }
        }

    /** Removes the saved copy (falls back to sidecar/online on next load). */
    suspend fun clear(track: TrackEntity) = withContext(Dispatchers.IO) {
        runCatching { savedFileFor(track).delete() }
    }

    /**
     * Online search, tried in order until one source has the song:
     *
     *  1. LRCLIB (lrclib.net) — free, keyless, purpose-built; exact signature match
     *     (artist + title + duration) then fuzzy search. Best for synced lyrics.
     *  2. NetEase Cloud Music — huge synced-lyrics DB behind a widely-used public
     *     endpoint. Result is duration-matched to avoid covers/remixes.
     *  3. Lyrics.ovh — plain-text last resort.
     *
     * Synced preferred, plain accepted; a hit from ANY source is persisted like an
     * imported file, so a given song touches the network at most once. NotFound is
     * only reported when every source cleanly answered "no such song" — a source that
     * ERRORED (down/unreachable) yields [OnlineResult.Error] instead, so the UI never
     * blames the user's connection for a genuine miss.
     */
    suspend fun fetchOnline(track: TrackEntity): OnlineResult = withContext(Dispatchers.IO) {
        var sawError = false
        for ((name, source) in listOf<Pair<String, (TrackEntity) -> Pair<String, Boolean>?>>(
            "LRCLIB" to ::fromLrclib,
            "NetEase" to ::fromNetease,
            "Lyrics.ovh" to ::fromLyricsOvh
        )) {
            try {
                source(track)?.let { (text, synced) ->
                    return@withContext persist(track, text, synced, name)
                }
            } catch (_: Exception) {
                sawError = true   // source unreachable — keep trying the rest
            }
        }
        if (sawError) OnlineResult.Error("Lyrics services couldn't be reached")
        else OnlineResult.NotFound
    }

    // ── Source 1: LRCLIB ────────────────────────────────────────────────────────

    private fun fromLrclib(track: TrackEntity): Pair<String, Boolean>? {
        // Exact match; duration is in seconds, ±2s tolerance applied server-side.
        val getUrl = "https://lrclib.net/api/get" +
            "?artist_name=${enc(track.artist)}" +
            "&track_name=${enc(track.title)}" +
            "&duration=${track.duration / 1000}"
        fetchJson(getUrl)?.let { o -> lyricsFrom(o)?.let { return it } }
        // Fuzzy search; first result that actually carries lyrics.
        val searchUrl = "https://lrclib.net/api/search" +
            "?track_name=${enc(track.title)}&artist_name=${enc(track.artist)}"
        val arr = fetchJsonArray(searchUrl) ?: return null
        for (i in 0 until arr.length()) {
            lyricsFrom(arr.optJSONObject(i) ?: continue)?.let { return it }
        }
        return null
    }

    // ── Source 2: NetEase Cloud Music ──────────────────────────────────────────

    private fun fromNetease(track: TrackEntity): Pair<String, Boolean>? {
        val q = enc("${track.artist} ${track.title}")
        val search = fetchJson("https://music.163.com/api/search/get?s=$q&type=1&limit=8")
            ?: return null
        val songs = search.optJSONObject("result")?.optJSONArray("songs") ?: return null
        if (songs.length() == 0) return null
        // Prefer the entry whose duration matches ours (±4s) — avoids remixes/covers;
        // fall back to the top result.
        var pick = songs.optJSONObject(0)
        for (i in 0 until songs.length()) {
            val s = songs.optJSONObject(i) ?: continue
            if (kotlin.math.abs(s.optLong("duration") - track.duration) <= 4000L) { pick = s; break }
        }
        val id = pick?.optLong("id", -1L) ?: -1L
        if (id <= 0) return null
        val lyricObj = fetchJson("https://music.163.com/api/song/lyric?id=$id&lv=1&kv=1&tv=-1")
            ?: return null
        val text = lyricObj.optJSONObject("lrc")?.optString("lyric")
            ?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        // NetEase's lrc payload is normally timestamped, but not always.
        val synced = Regex("\\[\\d+:\\d+").containsMatchIn(text)
        return text to synced
    }

    // ── Source 3: Lyrics.ovh (plain text) ──────────────────────────────────────

    private fun fromLyricsOvh(track: TrackEntity): Pair<String, Boolean>? {
        val o = fetchJson("https://api.lyrics.ovh/v1/${enc(track.artist)}/${enc(track.title)}")
            ?: return null
        val text = o.optString("lyrics").takeIf { it.isNotBlank() } ?: return null
        return text to false
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun persist(track: TrackEntity, text: String, synced: Boolean, source: String): OnlineResult {
        runCatching { savedFileFor(track).writeText(text) }
        return OnlineResult.Found(text.lines(), synced, source)
    }

    /** syncedLyrics preferred over plainLyrics; instrumental tracks report no lyrics. */
    private fun lyricsFrom(o: JSONObject): Pair<String, Boolean>? {
        if (o.optBoolean("instrumental", false)) return null
        o.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
            ?.let { return it to true }
        o.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
            ?.let { return it to false }
        return null
    }

    private fun fetchJson(url: String): JSONObject? = fetchBody(url)?.let {
        runCatching { JSONObject(it) }.getOrNull()
    }

    private fun fetchJsonArray(url: String): JSONArray? = fetchBody(url)?.let {
        runCatching { JSONArray(it) }.getOrNull()
    }

    private fun fetchBody(url: String): String? {
        val builder = Request.Builder().url(url)
            // LRCLIB asks clients to identify themselves.
            .header("User-Agent", "Freqvox Android (https://freqvox.app)")
        // NetEase's public endpoint expects a site Referer.
        if (url.contains("music.163.com")) builder.header("Referer", "https://music.163.com")
        val req = builder.build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }
}
