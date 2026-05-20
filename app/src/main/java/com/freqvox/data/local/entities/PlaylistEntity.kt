package com.freqvox.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "manual" or "query"
    val queryJson: String? = null, // Store tag filter logic for dynamic playlists
    val createdAt: Long = System.currentTimeMillis()
)
