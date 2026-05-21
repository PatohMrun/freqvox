package com.freqvox.data.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.freqvox.data.local.dao.PlayHistoryDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.PlayHistoryEntity
import com.freqvox.data.local.entities.TagEntity
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.data.local.entities.TrackTagCrossRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val tagDao: TagDao,
    private val playHistoryDao: PlayHistoryDao
) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()
    val allTrackTagCrossRefs: Flow<List<TrackTagCrossRef>> = tagDao.getAllTrackTagCrossRefs()

    fun getRecentPlayHistory(): Flow<List<PlayHistoryEntity>> {
        val since = System.currentTimeMillis() - 90L * 86_400_000
        return playHistoryDao.getHistorySinceFlow(since)
    }

    fun getActiveTracks(recentThreshold: Long): Flow<List<TrackEntity>> = 
        trackDao.getActiveTracks(recentThreshold)

    suspend fun scanLibrary() = withContext(Dispatchers.IO) {
        val tracksInMediaStore = mutableListOf<TrackEntity>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val rawTitle = cursor.getString(titleColumn) ?: continue
                if (rawTitle.isBlank()) continue
                val rawArtist = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn) ?: ""
                val duration = cursor.getLong(durationColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                val path = android.content.ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                val cleanedTitle = cleanMetadata(rawTitle)
                val cleanedArtist = when {
                    rawArtist.isNullOrBlank() || rawArtist == "<unknown>" -> "Unknown Artist"
                    else -> rawArtist
                }

                // Do NOT extract artwork here — it reads every audio file and takes O(n*fileIO) time.
                // Preserve any artwork already cached from a previous scan; fill in null for new tracks.
                val existingArtwork = trackDao.getTrackById(id)?.artworkPath

                tracksInMediaStore.add(
                    TrackEntity(
                        id = id,
                        title = cleanedTitle,
                        artist = cleanedArtist,
                        album = album,
                        duration = duration,
                        dateAdded = dateAdded,
                        path = path,
                        artworkPath = existingArtwork
                    )
                )
            }
        }

        if (tracksInMediaStore.isNotEmpty()) {
            trackDao.insertTracks(tracksInMediaStore)
            trackDao.deleteRemovedTracks(tracksInMediaStore.map { it.id })
        }

        ensureDefaultTags()

        // Extract artwork for tracks that don't have it yet — runs after tracks are visible in UI
        tracksInMediaStore.filter { it.artworkPath == null }.forEach { track ->
            val artworkPath = getOrExtractArtwork(track.id)
            if (artworkPath != null) {
                trackDao.updateTrack(track.copy(artworkPath = artworkPath))
            }
        }
    }

    private suspend fun ensureDefaultTags() {
        val defaultTags = listOf(
            "liked"       to 0xFFFF4D7C.toInt(),
            "chill"       to 0xFF42A5F5.toInt(),
            "energetic"   to 0xFFFF6D00.toInt(),
            "focus"       to 0xFF4DFFB4.toInt(),
            "hype"        to 0xFFFFA726.toInt(),
            "melancholy"  to 0xFF6B3FA0.toInt(),
            "late night"  to 0xFF7C4DFF.toInt(),
            "workout"     to 0xFFFF4D7C.toInt(),
            "drive"       to 0xFF00BCD4.toInt(),
            "sleep"       to 0xFF3F51B5.toInt(),
            "party"       to 0xFFE91E63.toInt(),
            "morning"     to 0xFFFFEB3B.toInt(),
            "throwback"   to 0xFF795548.toInt(),
            "underrated"  to 0xFF4CAF50.toInt(),
            "sad"         to 0xFF607D8B.toInt(),
            "vibes"       to 0xFF9C27B0.toInt()
        )
        val existingNames = tagDao.getAllTags().first().map { it.name.lowercase() }.toSet()
        defaultTags.forEach { (name, color) ->
            if (name !in existingNames) {
                tagDao.insertTag(TagEntity(name = name, color = color))
            }
        }
    }

    private fun getOrExtractArtwork(trackId: Long): String? {
        val artworkDir = File(context.cacheDir, "artworks")
        if (!artworkDir.exists()) artworkDir.mkdirs()

        val artworkFile = File(artworkDir, "$trackId.jpg")
        if (artworkFile.exists()) return artworkFile.absolutePath

        return try {
            val retriever = MediaMetadataRetriever()
            val contentUri = android.content.ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId
            )
            retriever.setDataSource(context, contentUri)
            val art = retriever.embeddedPicture
            retriever.release()

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                FileOutputStream(artworkFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                artworkFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanMetadata(title: String): String {
        return title
            .replace(Regex("\\(Official Video\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Official Audio\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(4K\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[HD\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Lyrics\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Official Music Video\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[Official Video\\]", RegexOption.IGNORE_CASE), "")
            .trim()
            .removeSuffix("-")
            .trim()
    }

    fun getTagsForTrack(trackId: Long): Flow<List<TagEntity>> = tagDao.getTagsForTrack(trackId)
    
    suspend fun toggleTagForTrack(trackId: Long, tagId: Long) {
        val currentTags = tagDao.getTagIdsForTrack(trackId)
        if (currentTags.contains(tagId)) {
            tagDao.deleteTrackTagCrossRef(TrackTagCrossRef(trackId, tagId))
        } else {
            tagDao.insertTrackTagCrossRef(TrackTagCrossRef(trackId, tagId))
        }
    }

    suspend fun getSimilarTracks(trackId: Long): List<TrackEntity> {
        return trackDao.getSimilarTracks(trackId)
    }

    suspend fun deleteTrack(trackId: Long) = withContext(Dispatchers.IO) {
        trackDao.deleteTrackById(trackId)
    }

    suspend fun getTracksByFolder(folderPath: String): List<TrackEntity> {
        return allTracks.first().filter { it.path.startsWith(folderPath) }
    }
}
