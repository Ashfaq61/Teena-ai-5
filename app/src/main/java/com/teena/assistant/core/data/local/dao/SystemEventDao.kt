package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.SystemEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemEventDao {

    @Query("SELECT * FROM system_events ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<SystemEventEntity>>

    @Insert
    suspend fun insert(event: SystemEventEntity)
}
