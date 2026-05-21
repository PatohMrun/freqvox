package com.freqvox.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long, // MediaStore ID
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val dateAdded: Long,
    val path: String, // content URI for playback
    val folderPath: String, // actual filesystem path for grouping
    val artworkPath: String? = null,
    val playCount: Int = 0,
    val lastPlayed: Long? = null,
    val totalPlayTime: Long = 0,
    val skipCount: Int = 0
)
