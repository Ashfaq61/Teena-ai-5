package com.teena.assistant.core.model

enum class FeatureStatus(val label: String) {
    ACTIVE("ACTIVE"),
    PARTIAL("PARTIAL"),
    BLOCKED("BLOCKED"),
    FAILED("FAILED"),
    NOT_IMPLEMENTED("NOT IMPLEMENTED"),
    IMPLEMENTED_NOT_DEVICE_VERIFIED("IMPLEMENTED, NOT DEVICE-VERIFIED")
}

data class FeatureSpec(
    val id: String,
    val title: String,
    val sectionRoute: String,
    val phase: Int,
    val status: FeatureStatus,
    val details: String
)
