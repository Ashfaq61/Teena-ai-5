package com.teena.assistant.core.ai.provider

import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.ChatStreamEvent
import com.teena.assistant.core.ai.model.ChatTurn
import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ProbeOutcome
import com.teena.assistant.core.ai.model.ProbeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

abstract class OpenAiCompatibleProvider(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : AiProvider {

    abstract var baseUrl: String
    protected abstract val modelsPath: String
    protected abstract val chatCompletionsPath: String

    protected open fun extraHeaders(builder: Request.Builder): Request.Builder = builder

    override suspend fun testConnection(apiKey: String): ProbeOutcome = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val req = extraHeaders(
            Request.Builder()
                .url("$baseUrl$modelsPath")
                .header("Authorization", "Bearer $apiKey")
                .get()
        ).build()

        try {
            okHttpClient.newCall(req).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val raw = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> ProbeOutcome(ProbeStatus.WORKING, latency)
                    response.code == 429 -> ProbeOutcome(ProbeStatus.RATE_LIMITED, latency, extractError(429, raw))
                    else -> ProbeOutcome(ProbeStatus.FAILED, latency, extractError(response.code, raw))
                }
            }
        } catch (io: IOException) {
            ProbeOutcome(ProbeStatus.FAILED, System.currentTimeMillis() - start, io.message ?: "Network error")
        }
    }

    override suspend fun discoverModels(apiKey: String): List<DiscoveredModel> = withContext(Dispatchers.IO) {
        val req = extraHeaders(
            Request.Builder()
                .url("$baseUrl$modelsPath")
                .header("Authorization", "Bearer $apiKey")
                .get()
        ).build()

        val raw = okHttpClient.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ProviderApiException(response.code, extractError(response.code, body))
            }
            body
        }

        val root = json.parseToJsonElement(raw).jsonObject
        val data = root["data"]?.jsonArray ?: JsonArray(emptyList())
        val now = System.currentTimeMillis()
        data.mapNotNull { elem -> parseModel(elem.jsonObject, now) }
    }

    protected abstract fun parseModel(obj: JsonObject, nowMs: Long): DiscoveredModel?

    override suspend fun probeModel(
        apiKey: String,
        modelId: String,
        capability: ModelCapability
    ): ProbeOutcome = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (capability == ModelCapability.SPEECH) {
            return@withContext ProbeOutcome(
                ProbeStatus.UNPROBED,
                0L,
                "Speech transcription endpoint requires audio upload; skipped text probe."
            )
        }
        val payload = buildJsonObject {
            put("model", modelId)
            put("max_tokens", 8)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", "OK")
                })
            })
        }.toString()

        val req = extraHeaders(
            Request.Builder()
                .url("$baseUrl$chatCompletionsPath")
                .header("Authorization", "Bearer $apiKey")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
        ).build()

        try {
            okHttpClient.newCall(req).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val raw = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> ProbeOutcome(ProbeStatus.WORKING, latency)
                    response.code == 429 -> ProbeOutcome(ProbeStatus.RATE_LIMITED, latency, extractError(429, raw))
                    else -> ProbeOutcome(ProbeStatus.FAILED, latency, extractError(response.code, raw))
                }
            }
        } catch (io: IOException) {
            ProbeOutcome(ProbeStatus.FAILED, System.currentTimeMillis() - start, io.message ?: "Network error")
        }
    }

    override fun streamChat(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<ChatTurn>
    ): Flow<ChatStreamEvent> = callbackFlow {
        val payload = buildJsonObject {
            put("model", modelId)
            put("stream", true)
            put("messages", buildJsonArray {
                if (systemPrompt.isNotBlank()) {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                }
                history.forEach { turn ->
                    add(buildJsonObject {
                        put("role", turn.role)
                        put("content", turn.text)
                    })
                }
            })
        }.toString()

        val req = extraHeaders(
            Request.Builder()
                .url("$baseUrl$chatCompletionsPath")
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "text/event-stream")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
        ).build()

        val call = okHttpClient.newCall(req)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty()
                    throw ProviderApiException(response.code, extractError(response.code, err))
                }
                val source = response.body?.source()
                    ?: throw ProviderApiException(response.code, "Empty response body")

                val fullText = StringBuilder()
                var inputTokens = 0
                var outputTokens = 0

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty() || data == "[DONE]") continue

                    val obj = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull() ?: continue
                    val delta = obj["choices"]?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("delta")?.jsonObject
                        ?.get("content")?.jsonPrimitive?.contentOrNull
                    if (!delta.isNullOrEmpty()) {
                        fullText.append(delta)
                        trySend(ChatStreamEvent.Delta(delta))
                    }
                    val usage = obj["usage"]?.jsonObject
                    if (usage != null) {
                        inputTokens = usage["prompt_tokens"]?.jsonPrimitive?.intOrNull ?: inputTokens
                        outputTokens = usage["completion_tokens"]?.jsonPrimitive?.intOrNull ?: outputTokens
                    }
                }

                trySend(
                    ChatStreamEvent.Completed(
                        fullText = fullText.toString(),
                        inputTokens = inputTokens,
                        outputTokens = outputTokens
                    )
                )
            }
            close()
        } catch (t: Throwable) {
            close(t)
        }

        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    override suspend fun embedText(
        apiKey: String,
        modelId: String,
        text: String
    ): FloatArray? = null

    protected fun extractError(code: Int, raw: String): String {
        val msg = runCatching {
            json.parseToJsonElement(raw).jsonObject["error"]
                ?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        return "HTTP $code: ${(msg ?: raw).take(160)}"
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@Singleton
class GroqProvider @Inject constructor(
    okHttpClient: OkHttpClient,
    json: Json
) : OpenAiCompatibleProvider(okHttpClient, json) {

    override var baseUrl: String = "https://api.groq.com"
    override val modelsPath: String = "/openai/v1/models"
    override val chatCompletionsPath: String = "/openai/v1/chat/completions"
    override val providerType: AiProviderType = AiProviderType.GROQ

    override fun parseModel(obj: JsonObject, nowMs: Long): DiscoveredModel? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val active = obj["active"]?.jsonPrimitive?.booleanOrNull ?: true
        if (!active) return null

        val lower = id.lowercase()
        val capabilities = mutableSetOf<ModelCapability>()
        when {
            lower.contains("whisper") -> capabilities += ModelCapability.SPEECH
            lower.contains("guard") || lower.contains("tts") -> return null
            else -> {
                capabilities += ModelCapability.CHAT
                capabilities += ModelCapability.TOOLS
                if (lower.contains("vision") || lower.contains("scout") || lower.contains("maverick")) {
                    capabilities += ModelCapability.VISION
                }
            }
        }
        val contextWindow = obj["context_window"]?.jsonPrimitive?.intOrNull ?: 8192
        val isPreview = lower.contains("preview") || lower.contains("exp")

        return DiscoveredModel(
            compositeId = "${providerType.name}:$id",
            provider = providerType,
            modelId = id,
            displayName = id,
            capabilities = capabilities,
            isFree = true,
            isPreview = isPreview,
            contextWindow = contextWindow,
            pricingSummary = "Groq Free Tier / Rate-limited",
            probeStatus = ProbeStatus.UNPROBED,
            probeLatencyMs = null,
            probeError = null,
            discoveredAtEpochMs = nowMs,
            probedAtEpochMs = null
        )
    }
}

@Singleton
class OpenRouterProvider @Inject constructor(
    okHttpClient: OkHttpClient,
    json: Json
) : OpenAiCompatibleProvider(okHttpClient, json) {

    override var baseUrl: String = "https://openrouter.ai"
    override val modelsPath: String = "/api/v1/models"
    override val chatCompletionsPath: String = "/api/v1/chat/completions"
    override val providerType: AiProviderType = AiProviderType.OPENROUTER

    override fun extraHeaders(builder: Request.Builder): Request.Builder =
        builder
            .header("HTTP-Referer", "https://teena.assistant.local")
            .header("X-Title", "TEENA AI")

    override fun parseModel(obj: JsonObject, nowMs: Long): DiscoveredModel? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id
        val contextLength = obj["context_length"]?.jsonPrimitive?.intOrNull ?: 8192

        val pricing = obj["pricing"]?.jsonObject
        val promptPrice = pricing?.get("prompt")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val completionPrice = pricing?.get("completion")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        val isFree = (promptPrice == 0.0 && completionPrice == 0.0) || id.endsWith(":free")

        val pricingSummary = if (isFree) {
            "Free ($0 / $0)"
        } else {
            val p = pricing?.get("prompt")?.jsonPrimitive?.contentOrNull ?: "?"
            val c = pricing?.get("completion")?.jsonPrimitive?.contentOrNull ?: "?"
            "Paid (in:$p / out:$c)"
        }

        val capabilities = mutableSetOf(ModelCapability.CHAT)
        val supportedParams = obj["supported_parameters"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.toSet()
            ?: emptySet()
        if ("tools" in supportedParams) {
            capabilities += ModelCapability.TOOLS
        }
        val modality = obj["architecture"]?.jsonObject?.get("modality")?.jsonPrimitive?.contentOrNull.orEmpty()
        if (modality.contains("image", ignoreCase = true)) {
            capabilities += ModelCapability.VISION
        }

        val isPreview = id.contains("preview", ignoreCase = true) || id.contains("exp", ignoreCase = true)

        return DiscoveredModel(
            compositeId = "${providerType.name}:$id",
            provider = providerType,
            modelId = id,
            displayName = name,
            capabilities = capabilities,
            isFree = isFree,
            isPreview = isPreview,
            contextWindow = contextLength,
            pricingSummary = pricingSummary,
            probeStatus = ProbeStatus.UNPROBED,
            probeLatencyMs = null,
            probeError = null,
            discoveredAtEpochMs = nowMs,
            probedAtEpochMs = null
        )
    }
}
