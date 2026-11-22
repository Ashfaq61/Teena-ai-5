package com.teena.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.ui.components.FeatureStatusBadge
import com.teena.assistant.ui.components.KeyValueRow
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.components.TeenaVoiceOrb
import com.teena.assistant.ui.components.VoiceOrbState
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
                    text = "Fresh Kotlin + Jetpack Compose build with Hilt, Room, WorkManager, and RTL layout support.",
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
                        "• Release APKs are signed via GitHub Actions using secrets only.\n" +
                        "• If an older TEENA APK with a different signature is installed, uninstall it before installing the first fresh release.\n" +
                        "• Never lose the release keystore backup—in-place updates require the same signing certificate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun PermissionsScreen(
    uiState: TeenaUiState,
    modifier: Modifier = Modifier
) {
    val upcomingPermissions = listOf(
        "RECORD_AUDIO + FOREGROUND_SERVICE_MICROPHONE" to "Phase 3 — Gemini Live bidirectional voice",
        "CAMERA + FOREGROUND_SERVICE_CAMERA" to "Phase 6 — CameraX photo/video understanding",
        "AccessibilityService + Restricted Settings Guide" to "Phase 5 — Screen vision & UI automation",
        "NotificationListenerService" to "Phase 6 — Notification triage & authorized replies",
        "ROLE_DIALER / ROLE_CALL_SCREENING" to "Phase 6 — Telecom call handling",
        "ColorOS Auto-Start & Battery Optimization Deep Links" to "Phase 4 — Oppo F15 background survival"
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
                        text = "Manifest Permissions (Live Check)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED)
                }
                Text(
                    text = "Only permissions required by active Phase 1 components are declared in AndroidManifest.xml.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }

        items(uiState.declaredPermissions, key = { it.permissionName }) { perm ->
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perm.permissionName.substringAfterLast('.'),
                        style = MaterialTheme.typography.titleMedium,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(
                        status = if (perm.granted) {
                            FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED
                        } else {
                            FeatureStatus.BLOCKED
                        }
                    )
                }
                Text(
                    text = perm.purpose,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                Text(
                    text = "${if (perm.granted) "GRANTED" else "DENIED"} • ${perm.phaseNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TeenaColors.Cyan
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
                        text = "Scheduled Permissions (Phases 3–6)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    FeatureStatusBadge(status = FeatureStatus.NOT_IMPLEMENTED)
                }
                upcomingPermissions.forEach { (perm, note) ->
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
                    text = "GitHub Actions CI (.github/workflows/android-ci.yml) runs unit tests, Android Lint, signed release APK builds, apksigner verification, SHA-256 checksums, and GitHub Release publishing. In-app GitHub Release polling & update verification is scheduled for Phase 9.",
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

        if (destination == TeenaDestination.LIVE_VOICE) {
            item {
                TeenaCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TeenaVoiceOrb(
                            state = VoiceOrbState.Unavailable(
                                "Blocked / Not Implemented: Gemini Live WebSocket engine, AudioRecord/AudioTrack 16k/24k PCM pipeline, and ModelRegistry bidiGenerateContent discovery will be built in Phases 2–3."
                            )
                        )
                    }
                }
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
