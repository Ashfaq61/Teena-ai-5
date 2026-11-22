package com.teena.assistant

import com.google.common.truth.Truth.assertThat
import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ProbeStatus
import com.teena.assistant.core.ai.registry.ModelRanker
import org.junit.Test

class ModelRankerTest {

    private fun makeModel(
        id: String,
        provider: AiProviderType = AiProviderType.GEMINI,
        caps: Set<ModelCapability> = setOf(ModelCapability.CHAT, ModelCapability.TOOLS),
        status: ProbeStatus = ProbeStatus.WORKING,
        isPreview: Boolean = false,
        isFree: Boolean = true,
        latencyMs: Long? = 250L,
        contextWindow: Int = 128_000
    ) = DiscoveredModel(
        compositeId = "${provider.name}:$id",
        provider = provider,
        modelId = id,
        displayName = id,
        capabilities = caps,
        isFree = isFree,
        isPreview = isPreview,
        contextWindow = contextWindow,
        pricingSummary = if (isFree) "Free" else "Paid",
        probeStatus = status,
        probeLatencyMs = latencyMs,
        probeError = null,
        discoveredAtEpochMs = 1000L,
        probedAtEpochMs = 1000L
    )

    @Test
    fun ranksWorkingStableFreeLowestLatencyFirst() {
        val failed = makeModel("broken-model", status = ProbeStatus.FAILED, latencyMs = 50L)
        val preview = makeModel("preview-model", isPreview = true, latencyMs = 80L)
        val paid = makeModel("paid-model", provider = AiProviderType.OPENROUTER, isFree = false, latencyMs = 90L)
        val slowStable = makeModel("stable-slow", latencyMs = 600L)
        val fastStable = makeModel("stable-fast", latencyMs = 180L)

        val selection = ModelRanker.selectBest(
            models = listOf(failed, preview, paid, slowStable, fastStable),
            capability = ModelCapability.CHAT,
            pinnedCompositeId = null
        )

        assertThat(selection.selectedModel?.modelId).isEqualTo("stable-fast")
        assertThat(selection.fallbackChain.map { it.modelId })
            .containsExactly("stable-slow", "paid-model", "preview-model", "broken-model")
            .inOrder()
    }

    @Test
    fun respectsUserOverrideAndSuggestsFasterWorkingAlternative() {
        val pinnedSlow = makeModel("pinned-model", latencyMs = 950L)
        val fastAuto = makeModel("fast-auto", latencyMs = 120L)

        val selection = ModelRanker.selectBest(
            models = listOf(pinnedSlow, fastAuto),
            capability = ModelCapability.CHAT,
            pinnedCompositeId = pinnedSlow.compositeId
        )

        assertThat(selection.isUserOverride).isTrue()
        assertThat(selection.selectedModel?.modelId).isEqualTo("pinned-model")
        assertThat(selection.suggestedBetterModel?.modelId).isEqualTo("fast-auto")
    }

    @Test
    fun filtersStrictlyByRequestedCapability() {
        val chatOnly = makeModel("llama-chat", provider = AiProviderType.GROQ, caps = setOf(ModelCapability.CHAT))
        val liveCapable = makeModel(
            "gemini-live",
            caps = setOf(ModelCapability.LIVE_AUDIO, ModelCapability.CHAT)
        )

        val liveSelection = ModelRanker.selectBest(
            models = listOf(chatOnly, liveCapable),
            capability = ModelCapability.LIVE_AUDIO,
            pinnedCompositeId = null
        )

        assertThat(liveSelection.selectedModel?.modelId).isEqualTo("gemini-live")
        assertThat(liveSelection.fallbackChain).isEmpty()
    }
}
