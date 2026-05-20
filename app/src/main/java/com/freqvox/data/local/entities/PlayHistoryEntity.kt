package com.freqvox.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val durationPlayed: Long,
    val completed: Boolean
)
