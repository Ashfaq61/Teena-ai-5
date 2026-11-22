package com.teena.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.ui.components.FeatureStatusBadge
import com.teena.assistant.ui.components.KeyValueRow
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.navigation.TeenaDestination
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.TeenaUiState

@Composable
fun SettingsScreen(
    uiState: TeenaUiState,
    onEmergencyStop: () -> Unit,
    onClearEmergencyStop: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenApiManager: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenColorOsAutoStart: () -> Unit,
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
                        text = "Safety & Emergency Stop",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureStatusBadge(status = FeatureStatus.IMPLEMENTED_NOT_DEVICE_VERIFIED)
                }
                Text(
                    text = if (uiState.emergencyStopped) {
                        "Emergency Stop is currently ENGAGED. All voice sessions and background automations remain halted until cleared."
                    } else {
                        "Emergency Stop immediately shuts down Gemini Live WebSocket audio, stops the microphone service, and cancels active automation loops."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.emergencyStopped) {
                        OutlinedButton(onClick = onClearEmergencyStop) {
                            Text("Clear Emergency Stop")
                        }
                    } else {
                        Button(
                            onClick = onEmergencyStop,
                            colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Rose)
                        ) {
                            Text("Trigger Emergency Stop")
                        }
                    }
                }
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Action Confirmation Policy",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                KeyValueRow("Read-only tools (time/battery/status)", "Auto-allowed")
                KeyValueRow("Sending messages / calls / deletes", "Explicit user confirmation")
                KeyValueRow("Screen / web / notification text", "Treated strictly as untrusted data")
                KeyValueRow("API Keys & Secrets", "Android Keystore AES256-GCM only")
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Onboarding, Permissions & Providers",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReopenOnboarding,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review Onboarding")
                    }
                    OutlinedButton(
                        onClick = onOpenPermissions,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Permissions")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBatteryOptimization,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Battery Settings")
                    }
                    OutlinedButton(
                        onClick = onOpenColorOsAutoStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ColorOS Auto-Start")
                    }
                }
                OutlinedButton(
                    onClick = onOpenApiManager,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage AI Provider Keys & ModelRegistry (${TeenaDestination.API_MANAGER.title})")
                }
            }
        }
    }
}
