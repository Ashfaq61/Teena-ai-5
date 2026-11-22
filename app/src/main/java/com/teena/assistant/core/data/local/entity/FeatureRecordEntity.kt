package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feature_records")
data class FeatureRecordEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val sectionRoute: String,
    val phase: Int,
    val status: String,
    val details: String,
    val updatedAtEpochMs: Long
)
