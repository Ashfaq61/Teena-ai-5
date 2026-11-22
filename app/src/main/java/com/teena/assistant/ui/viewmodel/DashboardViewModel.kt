package com.teena.assistant.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teena.assistant.BuildConfig
import com.teena.assistant.core.data.local.entity.SystemEventEntity
import com.teena.assistant.core.data.repository.FeatureRepository
import com.teena.assistant.core.model.FeatureSpec
import com.teena.assistant.core.model.FeatureStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionStatusItem(
    val permissionName: String,
    val purpose: String,
    val granted: Boolean,
    val phaseNote: String
)

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
    val declaredPermissions: List<PermissionStatusItem> = emptyList(),
    val statusCounts: Map<FeatureStatus, Int> = emptyMap()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val featureRepository: FeatureRepository
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

    private val declaredPermissions = listOf(
        PermissionStatusItem(
            permissionName = Manifest.permission.INTERNET,
            purpose = "Required for AI provider APIs, ModelRegistry discovery, and Gemini Live WebSocket.",
            granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_GRANTED,
            phaseNote = "Declared in Phase 1"
        ),
        PermissionStatusItem(
            permissionName = Manifest.permission.ACCESS_NETWORK_STATE,
            purpose = "Detects offline/online transitions for provider failover and Live reconnects.",
            granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED,
            phaseNote = "Declared in Phase 1"
        )
    )

    val uiState: StateFlow<TeenaUiState> = combine(
        featureRepository.observeFeatures(),
        featureRepository.observeEvents()
    ) { features, events ->
        val counts = FeatureStatus.entries.associateWith { status ->
            features.count { it.status == status }
        }
        TeenaUiState(
            features = features,
            recentEvents = events,
            telemetry = telemetry,
            declaredPermissions = declaredPermissions,
            statusCounts = counts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TeenaUiState(
            telemetry = telemetry,
            declaredPermissions = declaredPermissions
        )
    )

    init {
        viewModelScope.launch {
            featureRepository.syncCatalogIfNeeded()
        }
    }
}
