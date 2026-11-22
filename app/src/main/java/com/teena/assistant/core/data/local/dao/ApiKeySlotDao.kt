package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.ApiKeySlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeySlotDao {

    @Query("SELECT * FROM api_key_slots ORDER BY provider ASC, createdAtEpochMs ASC")
    fun observeAll(): Flow<List<ApiKeySlotEntity>>

    @Query("SELECT * FROM api_key_slots ORDER BY provider ASC, createdAtEpochMs ASC")
    suspend fun getAll(): List<ApiKeySlotEntity>

    @Query("SELECT * FROM api_key_slots WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ApiKeySlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slot: ApiKeySlotEntity)

    @Query("UPDATE api_key_slots SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query(
        "UPDATE api_key_slots SET cooldownUntilEpochMs = :cooldownUntil, consecutiveFailures = :failures, lastTestMessage = :message WHERE id = :id"
    )
    suspend fun updateCooldown(id: String, cooldownUntil: Long, failures: Int, message: String)

    @Query(
        "UPDATE api_key_slots SET isValid = :isValid, lastTestMessage = :message, lastLatencyMs = :latencyMs, cooldownUntilEpochMs = 0, consecutiveFailures = 0 WHERE id = :id"
    )
    suspend fun updateTestResult(id: String, isValid: Boolean, message: String, latencyMs: Long?)

    @Query("DELETE FROM api_key_slots WHERE id = :id")
    suspend fun deleteById(id: String)
}
