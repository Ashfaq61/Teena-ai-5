package com.teena.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teena.assistant.core.model.FeatureSpec
import com.teena.assistant.core.model.FeatureStatus
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

@Composable
fun DashboardScreen(
    uiState: TeenaUiState,
    onNavigate: (TeenaDestination) -> Unit,
    modifier: Modifier = Modifier
) {
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
                        text = "Phase 1 Scaffold • Honest Subsystem Telemetry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                    TeenaVoiceOrb(
                        state = VoiceOrbState.Unavailable(
                            "Gemini Live voice engine is NOT IMPLEMENTED yet (scheduled for Phase 3)."
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Live Provider & Agent Telemetry",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                KeyValueRow("Connected Providers", "0 (Phase 2)")
                KeyValueRow("Active Chat Model", "None — ModelRegistry Phase 2")
                KeyValueRow("Active Live Voice Model", "None — Gemini Live Phase 3")
                KeyValueRow("Working Agents", "0 / 17 (Phase 7)")
                KeyValueRow("Verified Tools", "0 (Phase 7)")
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
    if (destination == TeenaDestination.ABOUT) {
        return FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED
    }
    if (matchingFeatures.isEmpty()) {
        return FeatureStatus.NOT_IMPLEMENTED
    }
    return matchingFeatures.first().status
}
