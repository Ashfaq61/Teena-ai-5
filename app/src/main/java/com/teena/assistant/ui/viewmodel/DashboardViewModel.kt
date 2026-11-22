package com.teena.assistant.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teena.assistant.BuildConfig
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.registry.ApiKeyManager
import com.teena.assistant.core.ai.registry.ModelRegistry
import com.teena.assistant.core.data.local.entity.SystemEventEntity
import com.teena.assistant.core.data.repository.FeatureRepository
import com.teena.assistant.core.device.DeviceCompatibilityHelper
import com.teena.assistant.core.device.LivePermissionState
import com.teena.assistant.core.device.OnboardingPreferences
import com.teena.assistant.core.model.FeatureSpec
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.core.safety.EmergencyStopController
import com.teena.assistant.core.voice.LiveVoiceController
import com.teena.assistant.core.voice.LiveVoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceTelemetry(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val supportedAbis: String
)

data class TeenaUiState(
    val features: List<FeatureSpec> = emptyList(),
    val recentEvents: List<SystemEventEntity> = emptyList(),
    val telemetry: DeviceTelemetry,
    val permissionState: LivePermissionState,
    val statusCounts: Map<FeatureStatus, Int> = emptyMap(),
    val connectedProviderCount: Int = 0,
    val activeChatModel: String = "None (add API key in API Manager)",
    val activeLiveModel: String = "None (no bidiGenerateContent model)",
    val activeVisionModel: String = "None",
    val activeEmbeddingsModel: String = "None",
    val liveVoiceState: LiveVoiceState = LiveVoiceState.Idle,
    val emergencyStopped: Boolean = false,
    val onboardingCompleted: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val featureRepository: FeatureRepository,
    apiKeyManager: ApiKeyManager,
    private val modelRegistry: ModelRegistry,
    private val deviceHelper: DeviceCompatibilityHelper,
    private val onboardingPreferences: OnboardingPreferences,
    private val emergencyStopController: EmergencyStopController,
    private val liveVoiceController: LiveVoiceController
) : ViewModel() {

    private val telemetry = DeviceTelemetry(
        applicationId = BuildConfig.APPLICATION_ID,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidRelease = Build.VERSION.RELEASE,
        sdkInt = Build.VERSION.SDK_INT,
        supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ")
    )

    private val permissionStateFlow = MutableStateFlow(deviceHelper.inspectPermissions(appContext))

    private val modelsMetaFlow = combine(
        apiKeyManager.observeSlots(),
        modelRegistry.observeSelection(ModelCapability.CHAT),
        modelRegistry.observeSelection(ModelCapability.LIVE_AUDIO),
        modelRegistry.observeSelection(ModelCapability.VISION),
        modelRegistry.observeSelection(ModelCapability.EMBEDDINGS)
    ) { slots, chatSel, liveSel, visionSel, embedSel ->
        val connectedProviders = slots.filter { it.enabled && it.isValid }.map { it.provider }.distinct().size
        ModelsSummary(
            connectedProviders = connectedProviders,
            chat = chatSel.selectedModel?.let { "${it.provider.displayName} • ${it.modelId}" }
                ?: "None (add API key in API Manager)",
            live = liveSel.selectedModel?.let { "${it.provider.displayName} • ${it.modelId}" }
                ?: "None (no bidiGenerateContent model)",
            vision = visionSel.selectedModel?.let { "${it.provider.displayName} • ${it.modelId}" }
                ?: "None",
            embeddings = embedSel.selectedModel?.let { "${it.provider.displayName} • ${it.modelId}" }
                ?: "None"
        )
    }

    private val safetyFlow = combine(
        liveVoiceController.snapshot,
        emergencyStopController.isEmergencyStopped,
        onboardingPreferences.completed,
        permissionStateFlow
    ) { voiceSnap, emergency, onboarded, perms ->
        SafetySummary(voiceSnap.state, emergency, onboarded, perms)
    }

    val uiState: StateFlow<TeenaUiState> = combine(
        featureRepository.observeFeatures(),
        featureRepository.observeEvents(),
        modelsMetaFlow,
        safetyFlow
    ) { features, events, models, safety ->
        val counts = FeatureStatus.entries.associateWith { status ->
            features.count { it.status == status }
        }
        TeenaUiState(
            features = features,
            recentEvents = events,
            telemetry = telemetry,
            permissionState = safety.perms,
            statusCounts = counts,
            connectedProviderCount = models.connectedProviders,
            activeChatModel = models.chat,
            activeLiveModel = models.live,
            activeVisionModel = models.vision,
            activeEmbeddingsModel = models.embeddings,
            liveVoiceState = safety.voiceState,
            emergencyStopped = safety.emergencyStopped,
            onboardingCompleted = safety.onboardingCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TeenaUiState(
            telemetry = telemetry,
            permissionState = permissionStateFlow.value,
            onboardingCompleted = onboardingPreferences.completed.value
        )
    )

    init {
        viewModelScope.launch {
            featureRepository.syncCatalogIfNeeded()
            modelRegistry.refreshIfStale()
        }
    }

    fun refreshDevicePermissions() {
        permissionStateFlow.value = deviceHelper.inspectPermissions(appContext)
        viewModelScope.launch {
            liveVoiceController.refreshReadiness()
        }
    }

    fun triggerEmergencyStop() {
        emergencyStopController.triggerStop()
    }

    fun clearEmergencyStop() {
        emergencyStopController.clearStop()
    }

    fun openAppDetailsSettings(): Boolean = deviceHelper.openAppDetailsSettings(appContext)

    fun openBatteryOptimizationSettings(): Boolean =
        deviceHelper.openBatteryOptimizationSettings(appContext)

    fun openColorOsAutoStart(): Boolean = deviceHelper.openColorOsAutoStart(appContext)

    fun openAccessibilitySettings(): Boolean = deviceHelper.openAccessibilitySettings(appContext)

    fun openNotificationListenerSettings(): Boolean =
        deviceHelper.openNotificationListenerSettings(appContext)

    fun completeOnboarding() {
        onboardingPreferences.markCompleted()
        refreshDevicePermissions()
    }

    fun reopenOnboarding() {
        onboardingPreferences.resetForReview()
    }

    private data class ModelsSummary(
        val connectedProviders: Int,
        val chat: String,
        val live: String,
        val vision: String,
        val embeddings: String
    )

    private data class SafetySummary(
        val voiceState: LiveVoiceState,
        val emergencyStopped: Boolean,
        val onboardingCompleted: Boolean,
        val perms: LivePermissionState
    )
}
