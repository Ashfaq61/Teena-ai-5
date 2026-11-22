package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.UsageRecordEntity
import kotlinx.coroutines.flow.Flow

data class ProviderUsageSummary(
    val provider: String,
    val totalInputTokens: Long,
    val totalOutputTokens: Long,
    val requestCount: Long
)

@Dao
interface UsageRecordDao {

    @Query(
        "SELECT provider, SUM(inputTokens) AS totalInputTokens, SUM(outputTokens) AS totalOutputTokens, COUNT(*) AS requestCount " +
            "FROM usage_records GROUP BY provider ORDER BY provider ASC"
    )
    fun observeSummaryByProvider(): Flow<List<ProviderUsageSummary>>

    @Insert
    suspend fun insert(record: UsageRecordEntity)
}
