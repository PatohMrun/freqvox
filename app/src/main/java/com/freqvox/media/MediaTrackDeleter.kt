package com.freqvox.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest

object MediaTrackDeleter {

    fun mediaStoreUri(trackId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

    /** Android 11+ — launches the system "allow delete?" sheet. */
    fun createDeleteRequest(context: Context, trackId: Long): IntentSenderRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val pending = MediaStore.createDeleteRequest(
                context.contentResolver,
                listOf(mediaStoreUri(trackId))
            )
            IntentSenderRequest.Builder(pending.intentSender).build()
        } catch (_: Exception) {
            null
        }
    }

    /** Pre-Android 11 fallback — may fail on scoped storage without legacy write access. */
    fun tryDeleteViaResolver(context: Context, trackId: Long): Boolean =
        try {
            context.contentResolver.delete(mediaStoreUri(trackId), null, null) > 0
        } catch (_: Exception) {
            false
        }
}
