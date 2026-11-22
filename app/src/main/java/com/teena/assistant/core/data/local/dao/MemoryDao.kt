package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.MemoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memory_items ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<MemoryItemEntity>

    @Insert
    suspend fun insert(item: MemoryItemEntity): Long

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memory_items")
    suspend fun clearAll()
}
