package com.teena.assistant.core.ai.provider

import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.ChatStreamEvent
import com.teena.assistant.core.ai.model.ChatTurn
import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ProbeOutcome
import kotlinx.coroutines.flow.Flow

class ProviderApiException(
    val httpCode: Int?,
    override val message: String,
    val isAuthError: Boolean = httpCode == 401 || httpCode == 403,
    val isRateLimit: Boolean = httpCode == 429,
    val isModelNotFound: Boolean = httpCode == 404,
    val isServerError: Boolean = httpCode != null && httpCode >= 500
) : Exception(message)

interface AiProvider {
    val providerType: AiProviderType

    suspend fun testConnection(apiKey: String): ProbeOutcome

    suspend fun discoverModels(apiKey: String): List<DiscoveredModel>

    suspend fun probeModel(
        apiKey: String,
        modelId: String,
        capability: ModelCapability
    ): ProbeOutcome

    fun streamChat(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<ChatTurn>
    ): Flow<ChatStreamEvent>

    suspend fun embedText(
        apiKey: String,
        modelId: String,
        text: String
    ): FloatArray?
}
