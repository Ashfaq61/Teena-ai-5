package com.teena.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teena.assistant.ui.components.TeenaCard
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.MemoryHistoryViewModel

@Composable
fun MemoryHistoryScreen(
    viewModel: MemoryHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newMemoryText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeenaCard {
                Text(
                    text = "Long-Term Memory (Room + Embeddings)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TeenaColors.TextPrimary
                )
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    label = { Text("Search memories...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemoryText,
                        onValueChange = { newMemoryText = it },
                        label = { Text("Add user-controlled memory fact") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            viewModel.addMemory(newMemoryText)
                            newMemoryText = ""
                        },
                        enabled = newMemoryText.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Save memory")
                    }
                }
                if (state.memories.isNotEmpty()) {
                    TextButton(onClick = viewModel::clearMemories) {
                        Text("Clear All Memories", color = TeenaColors.Rose)
                    }
                }
            }
        }

        items(state.memories, key = { it.id }) { mem ->
            TeenaCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${mem.category} • ${if (mem.embeddingJson != null) "Embedded" else "Keyword"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeenaColors.Cyan
                        )
                        Text(
                            text = mem.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeenaColors.TextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.deleteMemory(mem.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = TeenaColors.Rose)
                    }
                }
            }
        }

        item {
            Text(
                text = "System Audit Events (${state.systemEvents.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TeenaColors.TextPrimary
            )
        }

        items(state.systemEvents, key = { it.id }) { ev ->
            TeenaCard {
                Text(
                    text = ev.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = TeenaColors.Cyan
                )
                Text(
                    text = ev.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeenaColors.TextPrimary
                )
            }
        }
    }
}
