package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.ModelSwitchLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelSwitchLogDao {

    @Query("SELECT * FROM model_switch_logs ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 40): Flow<List<ModelSwitchLogEntity>>

    @Insert
    suspend fun insert(log: ModelSwitchLogEntity)
}
