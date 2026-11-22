package com.teena.assistant.ui.screens

import android.Manifest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teena.assistant.core.voice.AudioOutputRoute
import com.teena.assistant.core.voice.LivePrebuiltVoice
import com.teena.assistant.core.voice.LiveVoiceState
import com.teena.assistant.core.voice.VadSensitivity
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.components.TeenaVoiceOrb
import com.teena.assistant.ui.components.VoiceOrbState
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.LiveVoiceViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveVoiceScreen(
    viewModel: LiveVoiceViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = uiState.session

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshReadiness()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshReadiness()
    }

    val orbState = when (val s = session.state) {
        is LiveVoiceState.Blocked -> VoiceOrbState.Unavailable(s.reason)
        is LiveVoiceState.Idle -> VoiceOrbState.Idle
        is LiveVoiceState.Connecting -> VoiceOrbState.Connecting
        is LiveVoiceState.Listening -> VoiceOrbState.Listening
        is LiveVoiceState.Thinking -> VoiceOrbState.Thinking
        is LiveVoiceState.Speaking -> VoiceOrbState.Speaking
        is LiveVoiceState.Reconnecting -> VoiceOrbState.Connecting
        is LiveVoiceState.Error -> VoiceOrbState.Error(s.message)
    }

    val isSessionActive = session.state is LiveVoiceState.Connecting ||
        session.state is LiveVoiceState.Listening ||
        session.state is LiveVoiceState.Thinking ||
        session.state is LiveVoiceState.Speaking ||
        session.state is LiveVoiceState.Reconnecting

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gemini Live Voice (BidiGenerateContent)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    TeenaVoiceOrb(state = orbState)

                    session.activeModelId?.let { modelId ->
                        Text(
                            text = "Active Live Model: $modelId",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeenaColors.Cyan
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isSessionActive) {
                            Button(
                                onClick = {
                                    if (session.state is LiveVoiceState.Blocked &&
                                        (session.state as LiveVoiceState.Blocked).reason.contains("RECORD_AUDIO")
                                    ) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.startSession()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Cyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = TeenaColors.Void)
                                Text(" Start Live", color = TeenaColors.Void)
                            }
                        } else {
                            Button(
                                onClick = viewModel::stopSession,
                                colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Rose),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null)
                                Text(" Stop")
                            }
                            OutlinedButton(
                                onClick = viewModel::toggleMute,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (session.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                    contentDescription = null
                                )
                                Text(if (session.isMuted) " Unmute" else " Mute")
                            }
                        }
                    }
                }
            }
        }

        if (session.liveInputCaption.isNotBlank() || session.liveOutputCaption.isNotBlank()) {
            item {
                TeenaCard {
                    Text(
                        text = "Live Captions",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    if (session.liveInputCaption.isNotBlank()) {
                        Text(
                            text = "You: ${session.liveInputCaption}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeenaColors.Cyan
                        )
                    }
                    if (session.liveOutputCaption.isNotBlank()) {
                        Text(
                            text = "TEENA: ${session.liveOutputCaption}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeenaColors.Emerald
                        )
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
                        text = "Prebuilt Live Voices",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    OutlinedButton(
                        onClick = viewModel::previewVoice,
                        enabled = !isSessionActive
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(" Preview Live")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LivePrebuiltVoice.entries.forEach { voice ->
                        FilterChip(
                            selected = session.selectedVoice == voice,
                            onClick = { viewModel.selectVoice(voice) },
                            label = { Text("${voice.apiName} (${voice.gender})") }
                        )
                    }
                }

                Text(
                    text = "Audio Output Route",
                    style = MaterialTheme.typography.titleMedium,
                    color = TeenaColors.TextPrimary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AudioOutputRoute.entries.forEach { route ->
                        FilterChip(
                            selected = session.selectedRoute == route,
                            onClick = { viewModel.selectRoute(route) },
                            label = { Text(route.displayName) }
                        )
                    }
                }

                Text(
                    text = "VAD Sensitivity",
                    style = MaterialTheme.typography.titleMedium,
                    color = TeenaColors.TextPrimary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VadSensitivity.entries.forEach { vad ->
                        FilterChip(
                            selected = session.vadSensitivity == vad,
                            onClick = { viewModel.selectVadSensitivity(vad) },
                            label = { Text(vad.displayName) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continuous Listening (foreground mic)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                    Switch(
                        checked = session.continuousListening,
                        onCheckedChange = viewModel::toggleContinuousListening
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voice Transcript History (${uiState.transcripts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                if (uiState.transcripts.isNotEmpty()) {
                    TextButton(onClick = viewModel::clearHistory) {
                        Text("Clear")
                    }
                }
            }
        }

        items(uiState.transcripts, key = { it.id }) { item ->
            TeenaCard {
                Text(
                    text = "${item.speaker.uppercase()} • ${item.voiceName} (${item.modelUsed})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.speaker == "user") TeenaColors.Cyan else TeenaColors.Emerald
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextPrimary
                )
            }
        }
    }

    session.pendingConfirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::denyPendingTool,
            title = { Text("Confirm Sensitive Voice Tool") },
            text = {
                Text("Gemini Live requested tool '${pending.toolName}' with arguments:\n${pending.argsSummary}")
            },
            confirmButton = {
                Button(onClick = viewModel::approvePendingTool) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::denyPendingTool) {
                    Text("Deny")
                }
            }
        )
    }
}
