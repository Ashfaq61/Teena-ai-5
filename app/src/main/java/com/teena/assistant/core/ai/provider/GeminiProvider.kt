package com.teena.assistant.core.ai.provider

import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.ChatStreamEvent
import com.teena.assistant.core.ai.model.ChatTurn
import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ProbeOutcome
import com.teena.assistant.core.ai.model.ProbeStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : AiProvider {

    var baseUrl: String = "https://generativelanguage.googleapis.com"
    var liveWsUrl: String =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    override val providerType: AiProviderType = AiProviderType.GEMINI

    override suspend fun testConnection(apiKey: String): ProbeOutcome = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models?pageSize=1")
            .header("x-goog-api-key", apiKey)
            .get()
            .build()
        executeChecked(request, start) { ProbeOutcome(ProbeStatus.WORKING, it) }
    }

    override suspend fun discoverModels(apiKey: String): List<DiscoveredModel> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DiscoveredModel>()
        var pageToken: String? = null
        var guard = 0
        val now = System.currentTimeMillis()

        do {
            guard++
            val url = buildString {
                append("$baseUrl/v1beta/models?pageSize=100")
                if (!pageToken.isNullOrBlank()) {
                    append("&pageToken=").append(pageToken)
                }
            }
            val request = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .get()
                .build()

            val bodyString = okHttpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw toApiException(response.code, raw)
                }
                raw
            }

            val root = json.parseToJsonElement(bodyString).jsonObject
            val modelsArray = root["models"]?.jsonArray ?: JsonArray(emptyList())
            for (elem in modelsArray) {
                val obj = elem.jsonObject
                val fullName = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val modelId = fullName.removePrefix("models/")
                if (modelId.isBlank()) continue

                val displayName = obj["displayName"]?.jsonPrimitive?.contentOrNull ?: modelId
                val inputLimit = obj["inputTokenLimit"]?.jsonPrimitive?.intOrNull ?: 32768
                val methods = obj["supportedGenerationMethods"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?.toSet()
                    ?: emptySet()

                val capabilities = classifyCapabilities(modelId, methods)
                if (capabilities.isEmpty()) continue

                val isPreview = modelId.contains("preview", ignoreCase = true) ||
                    modelId.contains("exp", ignoreCase = true)

                discovered += DiscoveredModel(
                    compositeId = "${providerType.name}:$modelId",
                    provider = providerType,
                    modelId = modelId,
                    displayName = displayName,
                    capabilities = capabilities,
                    isFree = true,
                    isPreview = isPreview,
                    contextWindow = inputLimit,
                    pricingSummary = "Free / AI Studio quota",
                    probeStatus = ProbeStatus.UNPROBED,
                    probeLatencyMs = null,
                    probeError = null,
                    discoveredAtEpochMs = now,
                    probedAtEpochMs = null
                )
            }

            pageToken = root["nextPageToken"]?.jsonPrimitive?.contentOrNull
        } while (!pageToken.isNullOrBlank() && guard < 10)

        discovered
    }

    private fun classifyCapabilities(modelId: String, methods: Set<String>): Set<ModelCapability> {
        val caps = mutableSetOf<ModelCapability>()
        if ("bidiGenerateContent" in methods) {
            caps += ModelCapability.LIVE_AUDIO
        }
        if ("generateContent" in methods) {
            caps += ModelCapability.CHAT
            caps += ModelCapability.TOOLS
            val lower = modelId.lowercase()
            if (lower.contains("gemini") && !lower.contains("tts") && !lower.contains("imagen")) {
                caps += ModelCapability.VISION
            }
        }
        if ("embedContent" in methods || "batchEmbedContents" in methods) {
            caps += ModelCapability.EMBEDDINGS
        }
        return caps
    }

    override suspend fun probeModel(
        apiKey: String,
        modelId: String,
        capability: ModelCapability
    ): ProbeOutcome = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        when (capability) {
            ModelCapability.LIVE_AUDIO -> probeLiveHandshake(apiKey, modelId, start)
            ModelCapability.EMBEDDINGS -> {
                val payload = buildJsonObject {
                    put("content", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", "ping") })
                        })
                    })
                }.toString()
                val req = Request.Builder()
                    .url("$baseUrl/v1beta/models/$modelId:embedContent")
                    .header("x-goog-api-key", apiKey)
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                executeChecked(req, start) { ProbeOutcome(ProbeStatus.WORKING, it) }
            }
            else -> {
                val payload = buildJsonObject {
                    put("contents", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("parts", buildJsonArray {
                                add(buildJsonObject { put("text", "OK") })
                            })
                        })
                    })
                    put("generationConfig", buildJsonObject {
                        put("maxOutputTokens", 8)
                    })
                }.toString()
                val req = Request.Builder()
                    .url("$baseUrl/v1beta/models/$modelId:generateContent")
                    .header("x-goog-api-key", apiKey)
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                executeChecked(req, start) { ProbeOutcome(ProbeStatus.WORKING, it) }
            }
        }
    }

    private suspend fun probeLiveHandshake(
        apiKey: String,
        modelId: String,
        startMs: Long
    ): ProbeOutcome {
        val result = CompletableDeferred<ProbeOutcome>()
        val request = Request.Builder()
            .url(liveWsUrl)
            .header("x-goog-api-key", apiKey)
            .build()

        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val setupFrame = buildJsonObject {
                    put("setup", buildJsonObject {
                        put("model", "models/$modelId")
                        put("generationConfig", buildJsonObject {
                            put("responseModalities", buildJsonArray { add(JsonPrimitive("AUDIO")) })
                        })
                    })
                }.toString()
                webSocket.send(setupFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("setupComplete")) {
                    result.complete(ProbeOutcome(ProbeStatus.WORKING, System.currentTimeMillis() - startMs))
                    webSocket.close(1000, "probe_done")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (!result.isCompleted) {
                    val status = if (reason.contains("quota", true) || code == 1008) {
                        ProbeStatus.RATE_LIMITED
                    } else {
                        ProbeStatus.FAILED
                    }
                    result.complete(
                        ProbeOutcome(
                            status = status,
                            latencyMs = System.currentTimeMillis() - startMs,
                            errorMessage = "WS closed ($code): $reason"
                        )
                    )
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!result.isCompleted) {
                    val code = response?.code
                    val status = if (code == 429) ProbeStatus.RATE_LIMITED else ProbeStatus.FAILED
                    result.complete(
                        ProbeOutcome(
                            status = status,
                            latencyMs = System.currentTimeMillis() - startMs,
                            errorMessage = "WS failure (${code ?: "net"}): ${t.message}"
                        )
                    )
                }
            }
        })

        return try {
            withTimeout(8_000L) { result.await() }
        } catch (t: Throwable) {
            ws.cancel()
            ProbeOutcome(
                status = ProbeStatus.FAILED,
                latencyMs = System.currentTimeMillis() - startMs,
                errorMessage = "Live probe timeout: ${t.message}"
            )
        }
    }

    override fun streamChat(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<ChatTurn>
    ): Flow<ChatStreamEvent> = callbackFlow {
        val contentsArray = buildJsonArray {
            history.forEach { turn ->
                val geminiRole = if (turn.role == "assistant" || turn.role == "model") "model" else "user"
                add(buildJsonObject {
                    put("role", geminiRole)
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", turn.text) })
                    })
                })
            }
        }
        val bodyObj = buildJsonObject {
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", systemPrompt) })
                    })
                })
            }
            put("contents", contentsArray)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", apiKey)
            .header("Accept", "text/event-stream")
            .post(bodyObj.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = okHttpClient.newCall(request)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty()
                    throw toApiException(response.code, err)
                }
                val source = response.body?.source()
                    ?: throw ProviderApiException(response.code, "Empty response body")

                val fullBuilder = StringBuilder()
                var inputTokens = 0
                var outputTokens = 0

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty() || data == "[DONE]") continue

                    val chunk = json.parseToJsonElement(data).jsonObject
                    val candidates = chunk["candidates"]?.jsonArray
                    val parts = candidates?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray
                    parts?.forEach { part ->
                        val text = part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                        if (!text.isNullOrEmpty()) {
                            fullBuilder.append(text)
                            trySend(ChatStreamEvent.Delta(text))
                        }
                    }
                    val usage = chunk["usageMetadata"]?.jsonObject
                    if (usage != null) {
                        inputTokens = usage["promptTokenCount"]?.jsonPrimitive?.intOrNull ?: inputTokens
                        outputTokens = usage["candidatesTokenCount"]?.jsonPrimitive?.intOrNull ?: outputTokens
                    }
                }

                trySend(
                    ChatStreamEvent.Completed(
                        fullText = fullBuilder.toString(),
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
    ): FloatArray? = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("content", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", text) })
                })
            })
        }.toString()

        val req = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:embedContent")
            .header("x-goog-api-key", apiKey)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        okHttpClient.newCall(req).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw toApiException(response.code, raw)
            }
            val values = json.parseToJsonElement(raw).jsonObject["embedding"]
                ?.jsonObject?.get("values")?.jsonArray
                ?: return@withContext null
            values.mapNotNull { it.jsonPrimitive.floatOrNull }.toFloatArray()
        }
    }

    private inline fun executeChecked(
        request: Request,
        startMs: Long,
        onSuccess: (latencyMs: Long) -> ProbeOutcome
    ): ProbeOutcome {
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startMs
                val raw = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> onSuccess(latency)
                    response.code == 429 -> ProbeOutcome(
                        ProbeStatus.RATE_LIMITED,
                        latency,
                        extractErrorSummary(429, raw)
                    )
                    else -> ProbeOutcome(
                        ProbeStatus.FAILED,
                        latency,
                        extractErrorSummary(response.code, raw)
                    )
                }
            }
        } catch (io: IOException) {
            ProbeOutcome(
                ProbeStatus.FAILED,
                System.currentTimeMillis() - startMs,
                io.message ?: "Network error"
            )
        }
    }

    private fun toApiException(code: Int, rawBody: String): ProviderApiException =
        ProviderApiException(
            httpCode = code,
            message = extractErrorSummary(code, rawBody)
        )

    private fun extractErrorSummary(code: Int, rawBody: String): String {
        val parsedMsg = runCatching {
            json.parseToJsonElement(rawBody).jsonObject["error"]
                ?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        return "HTTP $code: ${(parsedMsg ?: rawBody).take(160)}"
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
