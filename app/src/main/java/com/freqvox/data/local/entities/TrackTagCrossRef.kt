package com.freqvox.data.local.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "track_tags",
    primaryKeys = ["trackId", "tagId"],
    indices = [Index("tagId")]
)
data class TrackTagCrossRef(
    val trackId: Long,
    val tagId: Long,
    val assignedAt: Long = System.currentTimeMillis()
)
