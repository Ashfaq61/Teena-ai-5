package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_records")
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val slotId: String,
    val provider: String,
    val modelId: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val success: Boolean,
    val httpCode: Int?,
    val latencyMs: Long,
    val timestampEpochMs: Long
)
