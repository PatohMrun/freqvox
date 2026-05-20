package com.freqvox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freqvox.data.local.dao.PlayHistoryDao
import com.freqvox.data.local.dao.PlaylistDao
import com.freqvox.data.local.dao.TagDao
import com.freqvox.data.local.dao.TrackDao
import com.freqvox.data.local.entities.*

@Database(
    entities = [
        TrackEntity::class,
        TagEntity::class,
        TrackTagCrossRef::class,
        PlayHistoryEntity::class,
        SkipHistoryEntity::class,
        PlaylistEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FreqvoxDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun tagDao(): TagDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao
}
