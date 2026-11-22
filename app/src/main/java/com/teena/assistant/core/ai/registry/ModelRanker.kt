package com.teena.assistant.core.ai.registry

import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ModelSelection
import com.teena.assistant.core.ai.model.ProbeStatus

object ModelRanker {

    fun rankCandidates(
        models: List<DiscoveredModel>,
        capability: ModelCapability
    ): List<DiscoveredModel> {
        val eligible = models.filter { capability in it.capabilities }
        return eligible.sortedWith(
            compareBy<DiscoveredModel>(
                { statusRank(it.probeStatus) },
                { if (it.isPreview) 1 else 0 },
                { if (it.isFree) 0 else 1 },
                { it.probeLatencyMs ?: Long.MAX_VALUE },
                { -it.contextWindow }
            )
        )
    }

    fun selectBest(
        models: List<DiscoveredModel>,
        capability: ModelCapability,
        pinnedCompositeId: String?
    ): ModelSelection {
        val ranked = rankCandidates(models, capability)
        if (ranked.isEmpty()) {
            return ModelSelection(
                capability = capability,
                selectedModel = null,
                fallbackChain = emptyList(),
                isUserOverride = false,
                selectionReason = "No ${capability.displayName}-capable model discovered yet. Add an API key and run Model Discovery."
            )
        }

        val pinned = pinnedCompositeId?.let { id -> ranked.firstOrNull { it.compositeId == id } }
        val topAuto = ranked.first()

        if (pinned != null) {
            val fallbacks = ranked.filter { it.compositeId != pinned.compositeId }
            val betterSuggestion = if (
                pinned.probeStatus != ProbeStatus.WORKING &&
                topAuto.probeStatus == ProbeStatus.WORKING
            ) {
                topAuto
            } else if (
                topAuto.compositeId != pinned.compositeId &&
                topAuto.probeStatus == ProbeStatus.WORKING &&
                !topAuto.isPreview &&
                (pinned.probeLatencyMs ?: Long.MAX_VALUE) > (topAuto.probeLatencyMs ?: Long.MAX_VALUE) + 400
            ) {
                topAuto
            } else {
                null
            }

            return ModelSelection(
                capability = capability,
                selectedModel = pinned,
                fallbackChain = fallbacks,
                isUserOverride = true,
                selectionReason = "User pinned override (${pinned.probeStatus.label}" +
                    (pinned.probeLatencyMs?.let { ", ${it}ms" } ?: "") + ")",
                suggestedBetterModel = betterSuggestion
            )
        }

        val fallbacks = ranked.drop(1)
        val reason = buildString {
            append("Auto-selected: ")
            append(topAuto.probeStatus.label.lowercase())
            if (!topAuto.isPreview) append(", stable") else append(", preview")
            if (topAuto.isFree) append(", free") else append(", paid")
            topAuto.probeLatencyMs?.let { append(", ${it}ms") }
            append(", ctx=${topAuto.contextWindow}")
        }

        return ModelSelection(
            capability = capability,
            selectedModel = topAuto,
            fallbackChain = fallbacks,
            isUserOverride = false,
            selectionReason = reason
        )
    }

    private fun statusRank(status: ProbeStatus): Int = when (status) {
        ProbeStatus.WORKING -> 0
        ProbeStatus.UNPROBED -> 1
        ProbeStatus.RATE_LIMITED -> 2
        ProbeStatus.FAILED -> 3
    }
}
