package com.freqvox.data.artwork

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exact album-art source for tracks downloaded with Vidmate.
 *
 * Vidmate records, in a SQLite DB at `VidMate/config/task`, one row per download whose
 * `fp` column is the (custom-encoded) file path and whose `vf` JSON carries the source
 * thumbnail URL (`#picture_big`). Because that DB lives outside our media scope, the user
 * grants read access once via SAF (a folder pick); we then copy the DB out, parse it into a
 * `fileName → thumbnailUrl` map, and persist the map so we never re-parse on launch.
 *
 * The local `lm_thumbnail` PNG files are deliberately ignored — their ids map to neither
 * the row id nor the video id, so they're unaddressable; the DB's URL is the reliable link.
 */
@Singleton
class VidmateThumbnailSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mapFile = File(context.filesDir, "vidmate_art_map.json")

    @Volatile private var cache: Map<String, String>? = null

    /** Exact thumbnail URL for a track's file name, or null if not a known Vidmate download. */
    fun lookup(fileName: String): String? = loadMap()[fileName]

    /** How many songs are currently linked (drives the settings status line). */
    fun linkedCount(): Int = loadMap().size

    /** All file names we have a known thumbnail for. Lets the backfill re-queue
     *  exactly the tracks a fresh link can newly resolve, without wiping the whole
     *  negative cache. */
    fun knownFileNames(): Set<String> = loadMap().keys

    /**
     * Re-reads the Vidmate DB under the SAF-granted [treeUri], rebuilds and persists the map.
     * Returns the number of entries (0 if the folder couldn't be read — leaves any existing
     * map untouched so a wrong pick doesn't wipe a good one).
     */
    suspend fun rebuildFromTree(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val map = runCatching { parseTree(treeUri) }.getOrDefault(emptyMap())
        if (map.isNotEmpty()) {
            val obj = JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            mapFile.writeText(obj.toString())
            cache = map
        }
        map.size
    }

    private fun parseTree(treeUri: Uri): Map<String, String> {
        // Navigate VidMate/config/task within the granted tree.
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyMap()
        val taskDoc = tree.findFile("config")?.findFile("task") ?: return emptyMap()

        // SQLite can't open a SAF URI, so copy the DB to our cache and open it read-only.
        val tmp = File(context.cacheDir, "vidmate_task.db")
        context.contentResolver.openInputStream(taskDoc.uri)?.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        } ?: return emptyMap()

        val map = HashMap<String, String>()
        try {
            SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                for (table in listOf("vlDone", "vl")) {
                    runCatching {
                        db.rawQuery("SELECT fp, vf FROM $table", null)
                    }.getOrNull()?.use { c ->
                        val fpIdx = c.getColumnIndex("fp")
                        val vfIdx = c.getColumnIndex("vf")
                        while (c.moveToNext()) {
                            val fp = c.getString(fpIdx) ?: continue
                            val vf = c.getString(vfIdx) ?: continue
                            val fileName = decodeFp(fp).substringAfterLast('/')
                            if (fileName.isBlank()) continue
                            val url = extractPictureUrl(vf) ?: continue
                            map[fileName] = url
                        }
                    }
                }
            }
        } finally {
            tmp.delete()
        }
        return map
    }

    /** Decodes Vidmate's `fp` encoding: each char is `0xu` + two hex digits. Best-effort. */
    private fun decodeFp(s: String): String {
        val bytes = ArrayList<Byte>(s.length / 5)
        for (tok in s.split("0xu")) {
            if (tok.length >= 2) {
                tok.substring(0, 2).toIntOrNull(16)?.let { bytes.add(it.toByte()) }
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    private fun extractPictureUrl(vf: String): String? = try {
        val o = JSONObject(vf)
        o.optString("#picture_big").ifBlank { o.optString("#picture_default") }
            .takeIf { it.startsWith("http") }
    } catch (e: Exception) {
        null
    }

    private fun loadMap(): Map<String, String> {
        cache?.let { return it }
        val loaded = if (mapFile.exists()) {
            runCatching {
                val obj = JSONObject(mapFile.readText())
                buildMap<String, String> {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        put(k, obj.getString(k))
                    }
                }
            }.getOrDefault(emptyMap())
        } else emptyMap()
        cache = loaded
        return loaded
    }
}
