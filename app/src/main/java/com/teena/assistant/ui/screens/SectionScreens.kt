package com.teena.assistant.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.ui.components.FeatureStatusBadge
import com.teena.assistant.ui.components.KeyValueRow
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.navigation.TeenaDestination
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.TeenaUiState

@Composable
fun AboutScreen(
    uiState: TeenaUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEENA AI Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED)
                }
                Text(
                    text = "Fresh Kotlin + Jetpack Compose build with Hilt, Room v2, WorkManager, EncryptedSharedPreferences, ModelRegistry, and Gemini Live WebSocket voice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }
        item {
            TeenaCard {
                Text(
                    text = "Build & Device Telemetry",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                KeyValueRow("Application ID", uiState.telemetry.applicationId)
                KeyValueRow("Version Name", uiState.telemetry.versionName)
                KeyValueRow("Version Code", uiState.telemetry.versionCode.toString())
                KeyValueRow("Device", "${uiState.telemetry.manufacturer} ${uiState.telemetry.model}")
                KeyValueRow("Android OS", "${uiState.telemetry.androidRelease} (SDK ${uiState.telemetry.sdkInt})")
                KeyValueRow("Supported ABIs", uiState.telemetry.supportedAbis)
            }
        }
        item {
            TeenaCard {
                Text(
                    text = "Signing & Update Policy",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                Text(
                    text = "• Package ID is permanently fixed to com.teena.assistant.\n" +
                        "• Release APKs are built by GitHub Actions.\n" +
                        "• Permanent keystore secrets preserve in-place updates; temporary runner keys require uninstall/reinstall.\n" +
                        "• Never lose the release keystore backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PermissionsScreen(
    uiState: TeenaUiState,
    onRefreshPermissions: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenColorOsAutoStart: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val perms = uiState.permissionState

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefreshPermissions()
    }

    val scheduledPermissions = listOf(
        "CAMERA + FOREGROUND_SERVICE_CAMERA" to "Phase 6 — CameraX photo/video understanding",
        "AccessibilityService" to "Phase 5 — Screen vision & verified UI automation",
        "NotificationListenerService" to "Phase 6 — Notification triage & authorized replies",
        "ROLE_DIALER / ROLE_CALL_SCREENING" to "Phase 6 — Telecom call handling"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Runtime Permissions (Live Check)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureStatusBadge(status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED)
                }
                Text(
                    text = "Only active Phase 1–4 permissions are requested. Every permission below shows real live status from Android PackageManager/PowerManager.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                OutlinedButton(onClick = onRefreshPermissions) {
                    Text("Re-check Live Status")
                }
            }
        }

        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Microphone (RECORD_AUDIO)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(
                        status = if (perms.recordAudioGranted) {
                            FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED
                        } else {
                            FeatureStatus.BLOCKED
                        }
                    )
                }
                Text(
                    text = "Required for Gemini Live 16kHz bidirectional voice streaming and foreground microphone service.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                if (!perms.recordAudioGranted) {
                    Button(
                        onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Cyan)
                    ) {
                        Text("Grant Microphone", color = TeenaColors.Void)
                    }
                }
            }
        }

        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications (POST_NOTIFICATIONS)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(
                        status = if (perms.postNotificationsGranted) {
                            FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED
                        } else {
                            FeatureStatus.BLOCKED
                        }
                    )
                }
                Text(
                    text = "Required on Android 13+ so the active Gemini Live microphone service notification (Mute / Stop) stays visible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                if (!perms.postNotificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Cyan)
                    ) {
                        Text("Grant Notifications", color = TeenaColors.Void)
                    }
                }
            }
        }

        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Internet Access (INTERNET)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(
                        status = if (perms.internetGranted) {
                            FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED
                        } else {
                            FeatureStatus.BLOCKED
                        }
                    )
                }
                Text(
                    text = "Required for Gemini, Groq, and OpenRouter model discovery, streaming chat, and Gemini Live WebSocket.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }

        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Oppo F15 / ColorOS Background Survival",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureStatusBadge(
                        status = if (perms.batteryOptimizationIgnored) {
                            FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED
                        } else {
                            FeatureStatus.PARTIAL
                        }
                    )
                }
                KeyValueRow(
                    "Battery Optimization",
                    if (perms.batteryOptimizationIgnored) "UNRESTRICTED" else "OPTIMIZED (MAY KILL LIVE AUDIO)"
                )
                KeyValueRow(
                    "ColorOS / Oplus OEM",
                    if (perms.isColorOsOem) "Detected" else "Standard Android"
                )
                Text(
                    text = "ColorOS kills background services unless TEENA AI is allowed Auto-Start and unrestricted battery usage:\n" +
                        "1. Allow Battery Optimization exemption ('Don't optimize' / 'Allow background activity').\n" +
                        "2. Enable Auto-Start in ColorOS Phone Manager / Startup Manager.\n" +
                        "3. Optional: Lock TEENA AI in Recents (swipe down or tap lock icon).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenBatteryOptimization,
                        colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Cyan)
                    ) {
                        Text("Battery Exemption", color = TeenaColors.Void)
                    }
                    OutlinedButton(onClick = onOpenColorOsAutoStart) {
                        Text("ColorOS Auto-Start")
                    }
                }
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Android 13+ Restricted Settings Guide (Sideloaded APK)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                Text(
                    text = if (perms.restrictedSettingsApplies) {
                        "Your device runs Android 13+. Because APKs installed outside Play Store trigger Restricted Settings for Accessibility and Notification Listener:\n" +
                            "1. Tap 'Open App Info' below.\n" +
                            "2. Tap the 3-dot menu (⋮) in the top-right corner.\n" +
                            "3. Tap 'Allow restricted settings' and confirm with PIN/fingerprint.\n" +
                            "4. Return here and enable Accessibility or Notification access when Phase 5–6 services ship."
                    } else {
                        "Your Android version (${uiState.telemetry.androidRelease}, SDK ${uiState.telemetry.sdkInt}) does not enforce Android 13+ sideloaded Restricted Settings."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onOpenAppDetails) {
                        Text("Open App Info")
                    }
                    OutlinedButton(onClick = onOpenAccessibilitySettings) {
                        Text("Accessibility Settings")
                    }
                    OutlinedButton(onClick = onOpenNotificationListenerSettings) {
                        Text("Notification Access")
                    }
                }
            }
        }

        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Permissions (Phases 5–6)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(status = FeatureStatus.NOT_IMPLEMENTED)
                }
                scheduledPermissions.forEach { (perm, note) ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = perm,
                            style = MaterialTheme.typography.titleMedium,
                            color = TeenaColors.TextPrimary
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeenaColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApkBuildScreen(
    uiState: TeenaUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "APK Build & Release Pipeline",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(status = FeatureStatus.PARTIAL)
                }
                Text(
                    text = "GitHub Actions CI (.github/workflows/android-ci.yml) runs assembleDebug, unit tests, Android Lint, signed release APK builds, SHA-256 checksums, and GitHub Release publishing. In-app GitHub Release polling & update verification is scheduled for Phase 9.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Current Installed Build",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                KeyValueRow("Application ID", uiState.telemetry.applicationId)
                KeyValueRow("Version Name", uiState.telemetry.versionName)
                KeyValueRow("Version Code", uiState.telemetry.versionCode.toString())
                KeyValueRow("ABI Splits", "arm64-v8a, armeabi-v7a, x86_64, universal")
                KeyValueRow("CI Workflow", ".github/workflows/android-ci.yml")
            }
        }
    }
}

@Composable
fun SectionStatusScreen(
    destination: TeenaDestination,
    uiState: TeenaUiState,
    modifier: Modifier = Modifier
) {
    val sectionFeatures = uiState.features.filter { it.sectionRoute == destination.route }
    val status = resolveSectionStatus(destination, sectionFeatures)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureStatusBadge(status = status)
                }
                Text(
                    text = destination.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                KeyValueRow("Route", destination.route)
                KeyValueRow("Scheduled Implementation", "Phase ${destination.targetPhase}")
            }
        }

        if (sectionFeatures.isNotEmpty()) {
            items(sectionFeatures, key = { it.id }) { feature ->
                TeenaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TeenaColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        FeatureStatusBadge(status = feature.status)
                    }
                    Text(
                        text = feature.details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                    KeyValueRow("Feature ID", feature.id)
                    KeyValueRow("Target Phase", "Phase ${feature.phase}")
                }
            }
        }
    }
}
