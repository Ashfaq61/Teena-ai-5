package com.teena.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teena.assistant.core.model.FeatureSpec
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.core.voice.LiveVoiceState
import com.teena.assistant.ui.components.FeatureStatusBadge
import com.teena.assistant.ui.components.KeyValueRow
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.components.TeenaVoiceOrb
import com.teena.assistant.ui.components.VoiceOrbState
import com.teena.assistant.ui.navigation.TeenaDestination
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.TeenaUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: TeenaUiState,
    onNavigate: (TeenaDestination) -> Unit,
    onEmergencyStop: () -> Unit,
    onClearEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orbState = when (val voice = uiState.liveVoiceState) {
        is LiveVoiceState.Blocked -> VoiceOrbState.Unavailable(voice.reason)
        is LiveVoiceState.Idle -> VoiceOrbState.Idle
        is LiveVoiceState.Connecting -> VoiceOrbState.Connecting
        is LiveVoiceState.Listening -> VoiceOrbState.Listening
        is LiveVoiceState.Thinking -> VoiceOrbState.Thinking
        is LiveVoiceState.Speaking -> VoiceOrbState.Speaking
        is LiveVoiceState.Reconnecting -> VoiceOrbState.Connecting
        is LiveVoiceState.Error -> VoiceOrbState.Error(voice.message)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeenaCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TEENA AI Core",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TeenaColors.TextPrimary
                    )
                    Text(
                        text = "Phases 1–4 Implemented (Not Device-Verified on Oppo F15)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                    TeenaVoiceOrb(
                        state = orbState,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(TeenaDestination.LIVE_VOICE) },
                            colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Cyan)
                        ) {
                            Text("Open Live Voice", color = TeenaColors.Void)
                        }
                        OutlinedButton(onClick = { onNavigate(TeenaDestination.CHAT) }) {
                            Text("Open AI Chat")
                        }
                        OutlinedButton(onClick = { onNavigate(TeenaDestination.API_MANAGER) }) {
                            Text("API Manager")
                        }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (uiState.emergencyStopped) TeenaColors.Rose else TeenaColors.Amber
                        )
                        Text(
                            text = if (uiState.emergencyStopped) "Emergency Stop ACTIVE" else "Safety & Kill Switch",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.emergencyStopped) TeenaColors.Rose else TeenaColors.TextPrimary
                        )
                    }
                    if (uiState.emergencyStopped) {
                        OutlinedButton(onClick = onClearEmergencyStop) {
                            Text("Resume Ready")
                        }
                    } else {
                        Button(
                            onClick = onEmergencyStop,
                            colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Rose)
                        ) {
                            Text("Emergency Stop")
                        }
                    }
                }
                Text(
                    text = if (uiState.emergencyStopped) {
                        "All Gemini Live voice sessions and automation loops are halted until you tap Resume Ready or start a new session."
                    } else {
                        "Halts Gemini Live WebSocket audio, foreground mic service, and pending automation loops immediately."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Live Provider & Model Telemetry",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                KeyValueRow("Connected Providers", uiState.connectedProviderCount.toString())
                KeyValueRow("Active Chat Model", uiState.activeChatModel)
                KeyValueRow("Active Live Voice Model", uiState.activeLiveModel)
                KeyValueRow("Active Vision Model", uiState.activeVisionModel)
                KeyValueRow("Active Embeddings Model", uiState.activeEmbeddingsModel)
                KeyValueRow("Working Agents", "0 / 17 (Phase 7)")
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Oppo F15 / Runtime Readiness",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                KeyValueRow(
                    "Microphone (RECORD_AUDIO)",
                    if (uiState.permissionState.recordAudioGranted) "GRANTED" else "DENIED"
                )
                KeyValueRow(
                    "Notifications (POST_NOTIFICATIONS)",
                    if (uiState.permissionState.postNotificationsGranted) "GRANTED" else "DENIED"
                )
                KeyValueRow(
                    "Battery Optimization",
                    if (uiState.permissionState.batteryOptimizationIgnored) "UNRESTRICTED" else "OPTIMIZED (ACTION NEEDED)"
                )
                KeyValueRow(
                    "ColorOS / Oplus Device",
                    if (uiState.permissionState.isColorOsOem) "Detected" else "Standard Android"
                )
                OutlinedButton(onClick = { onNavigate(TeenaDestination.PERMISSIONS) }) {
                    Text("Manage Permissions & ColorOS Guides")
                }
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Feature Health Summary (Room DB)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                FeatureStatus.entries.forEach { status ->
                    KeyValueRow(
                        label = status.label,
                        value = (uiState.statusCounts[status] ?: 0).toString()
                    )
                }
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Device & Build Identity",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                KeyValueRow("Application ID", uiState.telemetry.applicationId)
                KeyValueRow(
                    "Version",
                    "${uiState.telemetry.versionName} (${uiState.telemetry.versionCode})"
                )
                KeyValueRow(
                    "Device",
                    "${uiState.telemetry.manufacturer} ${uiState.telemetry.model}"
                )
                KeyValueRow(
                    "Android",
                    "${uiState.telemetry.androidRelease} (API ${uiState.telemetry.sdkInt})"
                )
                KeyValueRow("ABIs", uiState.telemetry.supportedAbis)
            }
        }

        item {
            Text(
                text = "All 16 Sections",
                style = MaterialTheme.typography.titleLarge,
                color = TeenaColors.TextPrimary
            )
        }

        items(TeenaDestination.entries, key = { it.route }) { destination ->
            val matchingFeatures = uiState.features.filter { it.sectionRoute == destination.route }
            val sectionStatus = resolveSectionStatus(destination, matchingFeatures)

            TeenaCard(onClick = { onNavigate(destination) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            tint = TeenaColors.Cyan
                        )
                        Column {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TeenaColors.TextPrimary
                            )
                            Text(
                                text = "Target Phase ${destination.targetPhase}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TeenaColors.TextSecondary
                            )
                        }
                    }
                    FeatureStatusBadge(status = sectionStatus)
                }
                Text(
                    text = destination.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
            }
        }

        if (uiState.recentEvents.isNotEmpty()) {
            item {
                TeenaCard {
                    Text(
                        text = "Room System Audit Log",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    uiState.recentEvents.forEachIndexed { index, event ->
                        if (index > 0) {
                            HorizontalDivider(color = TeenaColors.Border)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${event.category} • ${formatter.format(Date(event.timestampEpochMs))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TeenaColors.Cyan
                            )
                            Text(
                                text = event.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TeenaColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

fun resolveSectionStatus(
    destination: TeenaDestination,
    matchingFeatures: List<FeatureSpec>
): FeatureStatus {
    if (destination == TeenaDestination.ABOUT || destination == TeenaDestination.SETTINGS) {
        return FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED
    }
    if (matchingFeatures.isEmpty()) {
        return FeatureStatus.NOT_IMPLEMENTED
    }
    return matchingFeatures.first().status
}
