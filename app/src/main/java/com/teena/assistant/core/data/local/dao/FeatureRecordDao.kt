package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.FeatureRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeatureRecordDao {

    @Query("SELECT * FROM feature_records ORDER BY phase ASC, title ASC")
    fun observeAll(): Flow<List<FeatureRecordEntity>>

    @Query("SELECT * FROM feature_records WHERE sectionRoute = :route ORDER BY phase ASC")
    fun observeByRoute(route: String): Flow<List<FeatureRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<FeatureRecordEntity>)

    @Query("SELECT COUNT(*) FROM feature_records")
    suspend fun count(): Int
}
