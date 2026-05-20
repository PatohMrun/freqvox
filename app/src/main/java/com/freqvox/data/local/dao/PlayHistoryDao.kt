package com.freqvox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.freqvox.data.local.entities.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Insert
    suspend fun insert(history: PlayHistoryEntity)

    @Query("SELECT * FROM play_history WHERE playedAt > :since ORDER BY playedAt DESC")
    fun getHistorySinceFlow(since: Long): Flow<List<PlayHistoryEntity>>
}
