package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.DiscoveredModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveredModelDao {

    @Query("SELECT * FROM discovered_models ORDER BY provider ASC, displayName ASC")
    fun observeAll(): Flow<List<DiscoveredModelEntity>>

    @Query("SELECT * FROM discovered_models")
    suspend fun getAll(): List<DiscoveredModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(models: List<DiscoveredModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: DiscoveredModelEntity)

    @Query("DELETE FROM discovered_models WHERE provider = :provider")
    suspend fun deleteByProvider(provider: String)
}
