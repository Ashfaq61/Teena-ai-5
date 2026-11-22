package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_key_slots")
data class ApiKeySlotEntity(
    @PrimaryKey
    val id: String,
    val provider: String,
    val label: String,
    val maskedKey: String,
    val enabled: Boolean,
    val isValid: Boolean,
    val lastTestMessage: String,
    val lastLatencyMs: Long?,
    val cooldownUntilEpochMs: Long,
    val consecutiveFailures: Int,
    val createdAtEpochMs: Long
)
