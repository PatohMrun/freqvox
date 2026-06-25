package com.freqvox.data.artwork

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.content.ContextCompat
import com.freqvox.data.local.entities.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves album art for a track. Local sources are offline and free, so they run
 * during the library scan; the online source (added separately) is gated and runs
 * on demand. The first source to yield art wins, and the bitmap is cached as JPEG so
 * subsequent loads are cheap. When nothing yields art the UI falls back to the
 * generated monogram (see ProceduralArtwork).
 */
@Singleton
class ArtworkResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: ArtworkPreferences,
    private val networkPolicy: NetworkPolicy,
    private val remoteClient: RemoteArtworkClient,
    private val vidmateSource: VidmateThumbnailSource
) {
    // Local art is cached here; cacheDir is fine because every local source is
    // re-derivable for free if the OS evicts it.
    private val artworkDir = File(context.cacheDir, "artworks")

    // Online art is expensive to refetch, so it lives in filesDir, which the OS does
    // not evict under cache pressure.
    private val remoteArtworkDir = File(context.filesDir, "artworks_remote")

    /**
     * Resolves art from local sources only, in order of fidelity:
     * embedded picture → MediaStore thumbnail → sidecar image in the track's folder.
     * Returns the cached file path, or null when no local source yields art.
     */
    suspend fun resolveLocalArtwork(track: TrackEntity): String? = withContext(Dispatchers.IO) {
        val cacheFile = cacheFileFor(track.id)
        if (cacheFile.exists()) return@withContext cacheFile.absolutePath

        val bitmap = extractEmbedded(track.id)
            ?: loadMediaStoreThumbnail(track.id)
            ?: loadFolderSidecar(track)
            ?: return@withContext null

        saveJpeg(bitmap, cacheFile)
        cacheFile.absolutePath
    }

    /** Global gate — true only when the user has consented AND the current connection
     *  is permitted. Callers check this to decide whether to even start a backfill. */
    fun canFetchOnline(): Boolean = prefs.consentShown && networkPolicy.canFetch()

    /** Whether we've already tried this track online and found nothing (negative cache). */
    fun alreadyTriedOnline(trackId: Long): Boolean = prefs.isLookupFailed(trackId)

    /**
     * Resolves art from the online source, fully self-gated:
     *   - Gate A: nothing happens until the consent dialog has been acknowledged;
     *   - negative cache: tracks already looked up and missed are skipped (no network);
     *   - network policy: re-checked live, honoring the per-transport toggles.
     * On a miss the track is recorded in the negative cache (unless [markFailures] is
     * false, in which case the caller batches that — see the backfill). Returns the
     * persisted file path on success, or null otherwise.
     */
    suspend fun resolveOnlineArtwork(
        track: TrackEntity,
        markFailures: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        if (!prefs.consentShown) return@withContext null
        if (prefs.isLookupFailed(track.id)) return@withContext null
        if (!networkPolicy.canFetch()) return@withContext null

        val file = remoteFileFor(track.id)
        if (file.exists()) return@withContext file.absolutePath

        // "Unknown Artist" carries no signal — pass it as blank so the client's
        // confidence gate relies on the title alone instead of a bogus artist.
        val artist = track.artist.takeIf {
            it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true)
        } ?: ""

        // Try proper album art first (nicer square covers), then fall back to the EXACT
        // thumbnail this track was downloaded with (Vidmate) — which fills the regional /
        // catalogue-gap songs that iTunes/Deezer don't have.
        var bytes = remoteClient.fetchCover(artist, track.title)
        if (bytes == null) {
            val url = fileNameFor(track.id)?.let { vidmateSource.lookup(it) }
            if (url != null) bytes = remoteClient.fetchFromUrl(url)
        }
        if (bytes == null) {
            if (markFailures) prefs.markLookupFailed(track.id)
            return@withContext null
        }
        file.writeBytes(bytes)
        file.absolutePath
    }

    /** Batched negative-cache mark, for callers that collect misses and record them once. */
    fun markLookupFailed(ids: Collection<Long>) = prefs.markLookupFailed(ids)

    /**
     * After a VidMate folder is linked, give another online attempt to ONLY the
     * tracks that now have a known thumbnail and still lack art — rather than
     * wiping the whole negative cache and re-probing the entire library on every
     * link (the old behaviour, which is what made linking grind).
     *
     * Matching is done with a single MediaStore query (id → file name) intersected
     * against [VidmateThumbnailSource.knownFileNames], so it costs one query, not
     * one per track. Returns how many tracks were re-queued.
     */
    suspend fun requeueVidmateLinkable(missingArtIds: Set<Long>): Int = withContext(Dispatchers.IO) {
        if (missingArtIds.isEmpty()) return@withContext 0
        val known = vidmateSource.knownFileNames()
        if (known.isEmpty()) return@withContext 0
        val linkable = fileNamesByTrackId(missingArtIds)
            .filterValues { it in known }
            .keys
        prefs.clearFailedLookups(linkable)
        linkable.size
    }

    /** trackId → on-disk file name for the given ids, via ONE MediaStore query. */
    private fun fileNamesByTrackId(ids: Set<Long>): Map<Long, String> {
        val out = HashMap<Long, String>(ids.size)
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    if (id !in ids) continue
                    c.getString(nameCol)?.let { out[id] = it }
                }
            }
        }
        return out
    }

    /** The on-disk file name for a track, used to match Vidmate's download records. */
    private fun fileNameFor(trackId: Long): String? = try {
        context.contentResolver.query(
            audioUri(trackId),
            arrayOf(MediaStore.Audio.Media.DISPLAY_NAME),
            null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    private fun remoteFileFor(trackId: Long): File {
        if (!remoteArtworkDir.exists()) remoteArtworkDir.mkdirs()
        return File(remoteArtworkDir, "$trackId.jpg")
    }

    /** Removes any cached/persisted art for a track (used when it leaves the library). */
    fun deleteArtwork(trackId: Long) {
        File(artworkDir, "$trackId.jpg").takeIf { it.exists() }?.delete()
        File(remoteArtworkDir, "$trackId.jpg").takeIf { it.exists() }?.delete()
    }

    private fun cacheFileFor(trackId: Long): File {
        if (!artworkDir.exists()) artworkDir.mkdirs()
        return File(artworkDir, "$trackId.jpg")
    }

    private fun audioUri(trackId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

    /** Source 1: picture embedded in the audio file's tags (highest fidelity). */
    private fun extractEmbedded(trackId: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri(trackId))
            retriever.embeddedPicture?.let { bytes ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                
                var inSampleSize = 1
                val reqSize = 512
                if (options.outHeight > reqSize || options.outWidth > reqSize) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= reqSize && halfWidth / inSampleSize >= reqSize) {
                        inSampleSize *= 2
                    }
                }
                
                val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, finalOptions)
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Source 2: the thumbnail MediaStore already maintains for the track, which can
     * surface album art derived elsewhere even when this file has no embedded picture.
     * loadThumbnail is API 29+; on older devices this source is simply skipped.
     */
    private fun loadMediaStoreThumbnail(trackId: Long): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            context.contentResolver.loadThumbnail(audioUri(trackId), Size(512, 512), null)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Source 3: a cover image sitting next to the track (cover.jpg, folder.jpg, …),
     * common for ripped albums. Reading image files in shared storage requires the
     * images permission (READ_MEDIA_IMAGES on 33+, READ_EXTERNAL_STORAGE below); if
     * the user hasn't granted it this source is skipped so the rest of the chain
     * still runs.
     */
    private fun loadFolderSidecar(track: TrackEntity): Bitmap? {
        if (!hasImagesPermission()) return null
        val folder = track.folderPath
        if (folder.isBlank() || folder == "Unknown") return null

        val candidates = setOf("cover", "folder", "album", "albumart", "front")
        return try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            // Images located directly inside the track's folder.
            val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            val args = arrayOf("$folder/%")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, args, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val base = name.substringBeforeLast('.').lowercase()
                    if (base in candidates) {
                        val imageUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol)
                        )
                        
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            BitmapFactory.decodeStream(input, null, options)
                        }
                        
                        var inSampleSize = 1
                        val reqSize = 512
                        if (options.outHeight > reqSize || options.outWidth > reqSize) {
                            val halfHeight = options.outHeight / 2
                            val halfWidth = options.outWidth / 2
                            while (halfHeight / inSampleSize >= reqSize && halfWidth / inSampleSize >= reqSize) {
                                inSampleSize *= 2
                            }
                        }
                        
                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                            return BitmapFactory.decodeStream(input, null, finalOptions)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun hasImagesPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun saveJpeg(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
    }
}
