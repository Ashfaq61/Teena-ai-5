package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_models")
data class DiscoveredModelEntity(
    @PrimaryKey
    val compositeId: String,
    val provider: String,
    val modelId: String,
    val displayName: String,
    val capabilitiesCsv: String,
    val isFree: Boolean,
    val isPreview: Boolean,
    val contextWindow: Int,
    val pricingSummary: String,
    val probeStatus: String,
    val probeLatencyMs: Long?,
    val probeError: String?,
    val discoveredAtEpochMs: Long,
    val probedAtEpochMs: Long?
)
