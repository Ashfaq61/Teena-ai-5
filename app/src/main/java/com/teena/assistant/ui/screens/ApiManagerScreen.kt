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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ProbeStatus
import com.teena.assistant.ui.components.KeyValueRow
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.ApiManagerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiManagerScreen(
    viewModel: ApiManagerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeenaCard {
                Text(
                    text = "Encrypted API Key Vault & ModelRegistry",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                Text(
                    text = "Keys are encrypted with Android Keystore (AES256-GCM), masked in UI, and never logged. Model discovery queries real provider endpoints and probes candidates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        enabled = !state.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = TeenaColors.Cyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = TeenaColors.Void)
                        Text(" Add Key", color = TeenaColors.Void)
                    }
                    OutlinedButton(
                        onClick = { viewModel.discoverAndProbeAll() },
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                        }
                        Text(" Discover Models")
                    }
                }
                state.statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = TeenaColors.Cyan
                    )
                }
            }
        }

        item {
            TeenaCard {
                Text(
                    text = "Active Auto-Selected Models (Section 1A)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                listOf(
                    ModelCapability.CHAT,
                    ModelCapability.LIVE_AUDIO,
                    ModelCapability.VISION,
                    ModelCapability.EMBEDDINGS
                ).forEach { cap ->
                    val sel = state.selections[cap]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cap.displayName}: ${sel?.selectedModel?.modelId ?: "None"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TeenaColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (sel?.isUserOverride == true) {
                                TextButton(onClick = { viewModel.clearOverride(cap) }) {
                                    Text("Revert Auto")
                                }
                            }
                        }
                        Text(
                            text = sel?.selectionReason ?: "Waiting for discovery...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeenaColors.TextSecondary
                        )
                        sel?.suggestedBetterModel?.let { better ->
                            Text(
                                text = "Auto-detect suggestion: ${better.modelId} (${better.probeLatencyMs ?: "?"}ms) is working faster.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TeenaColors.Amber
                            )
                        }
                        HorizontalDivider(color = TeenaColors.Border)
                    }
                }
            }
        }

        item {
            Text(
                text = "API Key Slots (${state.keySlots.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TeenaColors.TextPrimary
            )
        }

        if (state.keySlots.isEmpty()) {
            item {
                TeenaCard {
                    Text(
                        text = "No API keys added yet. Tap 'Add Key' to connect Gemini, Groq, or OpenRouter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                }
            }
        } else {
            items(state.keySlots, key = { it.id }) { slot ->
                TeenaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${slot.provider.displayName} • ${slot.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TeenaColors.TextPrimary
                            )
                            Text(
                                text = slot.maskedKey,
                                style = MaterialTheme.typography.labelSmall,
                                color = TeenaColors.Cyan
                            )
                        }
                        Switch(
                            checked = slot.enabled,
                            onCheckedChange = { viewModel.toggleSlot(slot.id, it) }
                        )
                    }
                    Text(
                        text = slot.lastTestMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (slot.isValid) TeenaColors.Emerald else TeenaColors.Amber
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testSlot(slot.id) },
                            enabled = !state.isBusy
                        ) {
                            Text("Test Connection")
                        }
                        IconButton(onClick = { viewModel.deleteSlot(slot.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete key", tint = TeenaColors.Rose)
                        }
                    }
                }
            }
        }

        if (state.usageByProvider.isNotEmpty()) {
            item {
                TeenaCard {
                    Text(
                        text = "Usage & Quota Tracking",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    state.usageByProvider.forEach { usage ->
                        KeyValueRow(
                            label = "${usage.provider} (${usage.requestCount} reqs)",
                            value = "in:${usage.totalInputTokens} / out:${usage.totalOutputTokens}"
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Discovered Models (${state.discoveredModels.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TeenaColors.TextPrimary
            )
        }

        items(state.discoveredModels.take(60), key = { it.compositeId }) { model ->
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = model.modelId,
                            style = MaterialTheme.typography.titleMedium,
                            color = TeenaColors.TextPrimary
                        )
                        Text(
                            text = "${model.provider.displayName} • ${if (model.isFree) "FREE" else "PAID"} • ctx ${model.contextWindow}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (model.isFree) TeenaColors.Emerald else TeenaColors.Amber
                        )
                    }
                    val probeColor = when (model.probeStatus) {
                        ProbeStatus.WORKING -> TeenaColors.Emerald
                        ProbeStatus.RATE_LIMITED -> TeenaColors.Amber
                        ProbeStatus.FAILED -> TeenaColors.Rose
                        ProbeStatus.UNPROBED -> TeenaColors.TextSecondary
                    }
                    Text(
                        text = model.probeStatus.label + (model.probeLatencyMs?.let { " (${it}ms)" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = probeColor
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    model.capabilities.forEach { cap ->
                        FilterChip(
                            selected = state.selections[cap]?.selectedModel?.compositeId == model.compositeId,
                            onClick = { viewModel.pinModel(cap, model.compositeId) },
                            label = { Text(cap.displayName) }
                        )
                    }
                }

                model.probeError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = TeenaColors.Rose
                    )
                }
            }
        }

        if (state.switchLogs.isNotEmpty()) {
            item {
                TeenaCard {
                    Text(
                        text = "Model Switch & Failover Log",
                        style = MaterialTheme.typography.titleLarge,
                        color = TeenaColors.TextPrimary
                    )
                    state.switchLogs.take(15).forEach { log ->
                        Text(
                            text = "[${log.capability}] ${log.fromModelId ?: "init"} → ${log.toModelId}: ${log.reason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeenaColors.TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddApiKeyDialog(
            onDismiss = { showAddDialog = false },
            onSave = { provider, label, key ->
                showAddDialog = false
                viewModel.addApiKey(provider, label, key)
            }
        )
    }
}

@Composable
private fun AddApiKeyDialog(
    onDismiss: () -> Unit,
    onSave: (AiProviderType, String, String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(AiProviderType.GEMINI) }
    var label by remember { mutableStateOf("Primary") }
    var rawKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Encrypted API Key Slot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiProviderType.entries.forEach { provider ->
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = { Text(provider.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Slot Label (e.g. Personal, Backup)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawKey,
                    onValueChange = { rawKey = it },
                    label = { Text("API Key (stored in Android Keystore)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedProvider, label, rawKey) }) {
                Text("Save & Test")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
