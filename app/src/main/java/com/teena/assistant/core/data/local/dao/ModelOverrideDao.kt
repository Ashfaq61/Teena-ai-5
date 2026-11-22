package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.ModelOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelOverrideDao {

    @Query("SELECT * FROM model_overrides")
    fun observeAll(): Flow<List<ModelOverrideEntity>>

    @Query("SELECT * FROM model_overrides")
    suspend fun getAll(): List<ModelOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(overrideEntity: ModelOverrideEntity)

    @Query("DELETE FROM model_overrides WHERE capability = :capability")
    suspend fun clear(capability: String)
}
