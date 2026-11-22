package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_overrides")
data class ModelOverrideEntity(
    @PrimaryKey
    val capability: String,
    val compositeModelId: String,
    val updatedAtEpochMs: Long
)
