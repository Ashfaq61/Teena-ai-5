package com.teena.assistant

import com.google.common.truth.Truth.assertThat
import com.teena.assistant.core.ai.model.ChatStreamEvent
import com.teena.assistant.core.ai.model.ChatTurn
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ProbeStatus
import com.teena.assistant.core.ai.provider.GeminiProvider
import com.teena.assistant.core.ai.provider.GroqProvider
import com.teena.assistant.core.ai.provider.OpenRouterProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProviderDiscoveryTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun geminiDiscoversPaginatedModelsAndClassifiesLiveAndEmbeddings() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "models": [
                    {
                      "name": "models/gemini-2.5-flash",
                      "displayName": "Gemini 2.5 Flash",
                      "inputTokenLimit": 1048576,
                      "supportedGenerationMethods": ["generateContent", "bidiGenerateContent"]
                    }
                  ],
                  "nextPageToken": "page2"
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "models": [
                    {
                      "name": "models/text-embedding-004",
                      "displayName": "Text Embedding 004",
                      "inputTokenLimit": 2048,
                      "supportedGenerationMethods": ["embedContent"]
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val provider = GeminiProvider(client, json).apply {
            baseUrl = server.url("").toString().removeSuffix("/")
        }

        val models = provider.discoverModels("test-secret-key")
        assertThat(models).hasSize(2)

        val flash = models.first { it.modelId == "gemini-2.5-flash" }
        assertThat(flash.capabilities).containsAtLeast(
            ModelCapability.CHAT,
            ModelCapability.VISION,
            ModelCapability.TOOLS,
            ModelCapability.LIVE_AUDIO
        )

        val embed = models.first { it.modelId == "text-embedding-004" }
        assertThat(embed.capabilities).containsExactly(ModelCapability.EMBEDDINGS)

        val req1 = server.takeRequest()
        assertThat(req1.getHeader("x-goog-api-key")).isEqualTo("test-secret-key")
        assertThat(req1.path).doesNotContain("test-secret-key")
    }

    @Test
    fun geminiStreamsSseChunksAndReportsRateLimitOnProbe() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Assalam \"}]}}]}\n\n" +
                        "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Alaikum\"}]}}],\"usageMetadata\":{\"promptTokenCount\":5,\"candidatesTokenCount\":2}}\n\n"
                )
        )
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":{"message":"Quota exceeded"}}"""))

        val provider = GeminiProvider(client, json).apply {
            baseUrl = server.url("").toString().removeSuffix("/")
        }

        val events = provider.streamChat(
            apiKey = "secret",
            modelId = "gemini-2.5-flash",
            systemPrompt = "Be concise",
            history = listOf(ChatTurn("user", "Hi"))
        ).toList()

        val completed = events.filterIsInstance<ChatStreamEvent.Completed>().single()
        assertThat(completed.fullText).isEqualTo("Assalam Alaikum")
        assertThat(completed.inputTokens).isEqualTo(5)
        assertThat(completed.outputTokens).isEqualTo(2)

        val probeOutcome = provider.probeModel("secret", "gemini-2.5-flash", ModelCapability.CHAT)
        assertThat(probeOutcome.status).isEqualTo(ProbeStatus.RATE_LIMITED)
    }

    @Test
    fun openRouterAndGroqParsePricingAndCapabilities() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    {
                      "id": "meta-llama/llama-3.3-70b-instruct:free",
                      "name": "Llama 3.3 70B",
                      "context_length": 131072,
                      "pricing": { "prompt": "0", "completion": "0" },
                      "supported_parameters": ["tools"],
                      "architecture": { "modality": "text->text" }
                    },
                    {
                      "id": "openai/gpt-4o",
                      "name": "GPT-4o",
                      "context_length": 128000,
                      "pricing": { "prompt": "0.0000025", "completion": "0.00001" },
                      "supported_parameters": ["tools"],
                      "architecture": { "modality": "text+image->text" }
                    }
                  ]
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data": [
                    { "id": "llama-3.3-70b-versatile", "active": true, "context_window": 131072 },
                    { "id": "whisper-large-v3", "active": true, "context_window": 448 }
                  ]
                }
                """.trimIndent()
            )
        )

        val base = server.url("").toString().removeSuffix("/")
        val openRouter = OpenRouterProvider(client, json).apply { baseUrl = base }
        val groq = GroqProvider(client, json).apply { baseUrl = base }

        val orModels = openRouter.discoverModels("or-key")
        assertThat(orModels).hasSize(2)
        assertThat(orModels[0].isFree).isTrue()
        assertThat(orModels[1].isFree).isFalse()
        assertThat(orModels[1].capabilities).contains(ModelCapability.VISION)

        val groqModels = groq.discoverModels("groq-key")
        assertThat(groqModels).hasSize(2)
        assertThat(groqModels.first { it.modelId == "whisper-large-v3" }.capabilities)
            .containsExactly(ModelCapability.SPEECH)
    }
}
