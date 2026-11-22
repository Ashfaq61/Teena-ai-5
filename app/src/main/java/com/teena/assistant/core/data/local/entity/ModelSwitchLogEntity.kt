package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_switch_logs")
data class ModelSwitchLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val capability: String,
    val fromModelId: String?,
    val toModelId: String,
    val reason: String,
    val timestampEpochMs: Long
)
