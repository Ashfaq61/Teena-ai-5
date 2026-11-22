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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.teena.assistant.core.model.FeatureStatus
import com.teena.assistant.ui.components.FeatureStatusBadge
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.TeenaUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    uiState: TeenaUiState,
    onRefreshPermissions: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenColorOsAutoStart: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onCompleteOnboarding: () -> Unit,
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

    Surface(
        color = TeenaColors.Void,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TeenaCard {
                    Text(
                        text = "Welcome to TEENA AI",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TeenaColors.Cyan
                    )
                    Text(
                        text = "Before entering the assistant, review and grant the permissions TEENA needs on ${uiState.telemetry.manufacturer} ${uiState.telemetry.model} (Android ${uiState.telemetry.androidRelease}). Every step explains why it is needed and can be skipped or revisited later in Permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextPrimary
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
                            text = "1. Microphone (RECORD_AUDIO)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TeenaColors.TextPrimary,
                            modifier = Modifier.weight(1f)
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
                        text = "Needed for real-time Gemini Live bidirectional audio conversations over WebSocket. Without this, AI Chat works, while Live Voice stays blocked.",
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
                            text = "2. Notifications (POST_NOTIFICATIONS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TeenaColors.TextPrimary,
                            modifier = Modifier.weight(1f)
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
                        text = "Allows Android to display the persistent Gemini Live microphone service notification with instant Mute and Stop controls.",
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
                            text = "3. ColorOS / Oppo Battery & Auto-Start",
                            style = MaterialTheme.typography.titleMedium,
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
                    Text(
                        text = "Oppo ColorOS aggressively kills background voice sessions and 24h model-refresh jobs. Exempt TEENA AI from battery optimization and enable Auto-Start.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onOpenBatteryOptimization) {
                            Text("Battery Exemption")
                        }
                        OutlinedButton(onClick = onOpenColorOsAutoStart) {
                            Text("ColorOS Auto-Start")
                        }
                        OutlinedButton(onClick = onOpenAppDetails) {
                            Text("App Info (Restricted Settings)")
                        }
                    }
                }
            }

            item {
                TeenaCard {
                    Text(
                        text = "4. Add Your AI Keys Next",
                        style = MaterialTheme.typography.titleMedium,
                        color = TeenaColors.TextPrimary
                    )
                    Text(
                        text = "Once inside TEENA AI, open API Manager to add your Gemini, Groq, or OpenRouter API keys. Keys are stored exclusively in Android Keystore EncryptedSharedPreferences.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onCompleteOnboarding,
                            colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Cyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enter TEENA AI", color = TeenaColors.Void)
                        }
                        TextButton(onClick = onCompleteOnboarding) {
                            Text("Skip for Now")
                        }
                    }
                }
            }
        }
    }
}
