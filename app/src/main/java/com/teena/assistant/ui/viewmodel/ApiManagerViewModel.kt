package com.teena.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.ApiKeySlot
import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ModelSelection
import com.teena.assistant.core.ai.registry.ApiKeyManager
import com.teena.assistant.core.ai.registry.ModelRegistry
import com.teena.assistant.core.data.local.dao.ProviderUsageSummary
import com.teena.assistant.core.data.local.dao.UsageRecordDao
import com.teena.assistant.core.data.local.entity.ModelSwitchLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApiManagerUiState(
    val keySlots: List<ApiKeySlot> = emptyList(),
    val discoveredModels: List<DiscoveredModel> = emptyList(),
    val selections: Map<ModelCapability, ModelSelection> = emptyMap(),
    val switchLogs: List<ModelSwitchLogEntity> = emptyList(),
    val usageByProvider: List<ProviderUsageSummary> = emptyList(),
    val isBusy: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class ApiManagerViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val modelRegistry: ModelRegistry,
    usageRecordDao: UsageRecordDao
) : ViewModel() {

    private val busyState = MutableStateFlow(false to (null as String?))

    private val selectionsFlow = combine(
        modelRegistry.observeSelection(ModelCapability.CHAT),
        modelRegistry.observeSelection(ModelCapability.LIVE_AUDIO),
        modelRegistry.observeSelection(ModelCapability.VISION),
        modelRegistry.observeSelection(ModelCapability.EMBEDDINGS)
    ) { chat, live, vision, embed ->
        mapOf(
            ModelCapability.CHAT to chat,
            ModelCapability.LIVE_AUDIO to live,
            ModelCapability.VISION to vision,
            ModelCapability.EMBEDDINGS to embed
        )
    }

    val uiState: StateFlow<ApiManagerUiState> = combine(
        apiKeyManager.observeSlots(),
        modelRegistry.observeModels(),
        selectionsFlow,
        modelRegistry.observeSwitchLogs(),
        usageRecordDao.observeSummaryByProvider(),
        busyState
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val slots = arr[0] as List<ApiKeySlot>
        @Suppress("UNCHECKED_CAST")
        val models = arr[1] as List<DiscoveredModel>
        @Suppress("UNCHECKED_CAST")
        val selections = arr[2] as Map<ModelCapability, ModelSelection>
        @Suppress("UNCHECKED_CAST")
        val logs = arr[3] as List<ModelSwitchLogEntity>
        @Suppress("UNCHECKED_CAST")
        val usage = arr[4] as List<ProviderUsageSummary>
        @Suppress("UNCHECKED_CAST")
        val busy = arr[5] as Pair<Boolean, String?>

        ApiManagerUiState(
            keySlots = slots,
            discoveredModels = models,
            selections = selections,
            switchLogs = logs,
            usageByProvider = usage,
            isBusy = busy.first,
            statusMessage = busy.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ApiManagerUiState()
    )

    init {
        modelRegistry.schedulePeriodicRefresh()
    }

    fun addApiKey(provider: AiProviderType, label: String, rawKey: String) {
        if (rawKey.isBlank()) {
            busyState.value = false to "API key cannot be empty."
            return
        }
        viewModelScope.launch {
            busyState.value = true to "Saving encrypted key and testing connection..."
            runCatching {
                val slot = apiKeyManager.addSlot(provider, label, rawKey)
                if (slot.isValid) {
                    busyState.value = true to "Key valid! Discovering and probing ${provider.displayName} models..."
                    modelRegistry.refreshAll(probeCandidates = true)
                    "Added ${slot.label} and refreshed model registry."
                } else {
                    "Saved ${slot.label}, but connection test reported: ${slot.lastTestMessage}"
                }
            }.onSuccess { msg ->
                busyState.value = false to msg
            }.onFailure { err ->
                busyState.value = false to "Error: ${err.message}"
            }
        }
    }

    fun testSlot(slotId: String) {
        viewModelScope.launch {
            busyState.value = true to "Running live connection test..."
            runCatching { apiKeyManager.testSlot(slotId) }
                .onSuccess { slot ->
                    busyState.value = false to "${slot.label}: ${slot.lastTestMessage}"
                }
                .onFailure { err ->
                    busyState.value = false to "Test failed: ${err.message}"
                }
        }
    }

    fun toggleSlot(slotId: String, enabled: Boolean) {
        viewModelScope.launch {
            apiKeyManager.toggleSlot(slotId, enabled)
        }
    }

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            apiKeyManager.deleteSlot(slotId)
            busyState.update { it.first to "Deleted key slot from encrypted vault." }
        }
    }

    fun discoverAndProbeAll() {
        viewModelScope.launch {
            busyState.value = true to "Discovering models across all active keys and probing candidates..."
            modelRegistry.refreshAll(probeCandidates = true)
                .onSuccess { count ->
                    busyState.value = false to "Discovered $count models and updated capability rankings."
                }
                .onFailure { err ->
                    busyState.value = false to "Model discovery failed: ${err.message}"
                }
        }
    }

    fun pinModel(capability: ModelCapability, compositeModelId: String) {
        viewModelScope.launch {
            modelRegistry.pinModelOverride(capability, compositeModelId)
            busyState.update { it.first to "Pinned $compositeModelId for ${capability.displayName}." }
        }
    }

    fun clearOverride(capability: ModelCapability) {
        viewModelScope.launch {
            modelRegistry.clearModelOverride(capability)
            busyState.update { it.first to "Cleared manual override for ${capability.displayName}." }
        }
    }
}
