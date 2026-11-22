package com.teena.assistant.core.ai.model

enum class AiProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    GROQ("Groq"),
    OPENROUTER("OpenRouter")
}

enum class ModelCapability(val displayName: String) {
    CHAT("Chat"),
    LIVE_AUDIO("Live Voice"),
    VISION("Vision"),
    TOOLS("Tools"),
    EMBEDDINGS("Embeddings"),
    SPEECH("Speech")
}

enum class ProbeStatus(val label: String) {
    UNPROBED("Unprobed"),
    WORKING("Working"),
    RATE_LIMITED("Rate Limited"),
    FAILED("Failed")
}

data class ApiKeySlot(
    val id: String,
    val provider: AiProviderType,
    val label: String,
    val maskedKey: String,
    val enabled: Boolean,
    val isValid: Boolean,
    val lastTestMessage: String,
    val lastLatencyMs: Long?,
    val cooldownUntilEpochMs: Long,
    val createdAtEpochMs: Long
)

data class DiscoveredModel(
    val compositeId: String,
    val provider: AiProviderType,
    val modelId: String,
    val displayName: String,
    val capabilities: Set<ModelCapability>,
    val isFree: Boolean,
    val isPreview: Boolean,
    val contextWindow: Int,
    val pricingSummary: String,
    val probeStatus: ProbeStatus,
    val probeLatencyMs: Long?,
    val probeError: String?,
    val discoveredAtEpochMs: Long,
    val probedAtEpochMs: Long?
)

data class ModelSelection(
    val capability: ModelCapability,
    val selectedModel: DiscoveredModel?,
    val fallbackChain: List<DiscoveredModel>,
    val isUserOverride: Boolean,
    val selectionReason: String,
    val suggestedBetterModel: DiscoveredModel? = null
)

data class ChatTurn(
    val role: String,
    val text: String
)

sealed interface ChatStreamEvent {
    data class Delta(val text: String) : ChatStreamEvent
    data class Completed(
        val fullText: String,
        val inputTokens: Int,
        val outputTokens: Int
    ) : ChatStreamEvent
}

data class ProbeOutcome(
    val status: ProbeStatus,
    val latencyMs: Long,
    val errorMessage: String? = null
)

enum class LanguageHint(val displayName: String, val isRtl: Boolean) {
    ENGLISH("English", false),
    URDU("Urdu", true),
    ROMAN_URDU("Roman Urdu", false),
    HINGLISH("Hinglish", false)
}
