package com.freqvox.media

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest

/**
 * Edits a track's MediaStore metadata (title / artist / album). This updates what MediaStore —
 * and therefore Freqvox and other apps reading it — report; it does not rewrite the file's
 * embedded tags. On Android 11+ editing media the app doesn't own needs user consent, obtained
 * via [createWriteRequest] before [applyUpdate].
 */
object MediaMetadataWriter {

    private fun uriFor(trackId: Long) =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

    /** Android 11+ — the system "allow this app to modify?" consent sheet. */
    fun createWriteRequest(context: Context, trackId: Long): IntentSenderRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val pending = MediaStore.createWriteRequest(context.contentResolver, listOf(uriFor(trackId)))
            IntentSenderRequest.Builder(pending.intentSender).build()
        } catch (_: Exception) {
            null
        }
    }

    fun applyUpdate(context: Context, trackId: Long, title: String, artist: String, album: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.ALBUM, album)
        }
        return try {
            context.contentResolver.update(uriFor(trackId), values, null, null) > 0
        } catch (_: Exception) {
            false
        }
    }
}
