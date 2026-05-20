package com.freqvox.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skip_history")
data class SkipHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val skippedAt: Long = System.currentTimeMillis()
)
