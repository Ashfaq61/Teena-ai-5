package com.teena.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teena.assistant.core.ai.model.LanguageDetector
import com.teena.assistant.core.ai.model.LanguageHint
import com.teena.assistant.ui.theme.TeenaColors
import com.teena.assistant.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.streamingPartialText) {
        val total = state.messages.size + if (state.streamingPartialText != null) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = TeenaColors.Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.activeModelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TeenaColors.Cyan,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Memory",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeenaColors.TextSecondary
                        )
                        Switch(
                            checked = state.useLongTermMemory,
                            onCheckedChange = viewModel::toggleLongTermMemory
                        )
                        IconButton(onClick = viewModel::createNewSession) {
                            Icon(Icons.Rounded.Add, contentDescription = "New Chat", tint = TeenaColors.Cyan)
                        }
                    }
                }

                if (state.sessions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.sessions, key = { it.id }) { session ->
                            FilterChip(
                                selected = session.id == state.activeSessionId,
                                onClick = { viewModel.selectSession(session.id) },
                                label = { Text(session.title.take(18)) }
                            )
                        }
                    }
                }

                state.fallbackBanner?.let { banner ->
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelSmall,
                        color = TeenaColors.Amber
                    )
                }
                state.errorBanner?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.labelSmall,
                        color = TeenaColors.Rose
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.messages.isEmpty() && state.streamingPartialText == null) {
                item {
                    Text(
                        text = "Ask TEENA AI in English, Urdu (اردو), Roman Urdu, or Hinglish. Replies stream directly from your active ModelRegistry provider with automatic failover.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TeenaColors.TextSecondary
                    )
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                val lang = LanguageHint.entries.firstOrNull { it.name == msg.languageHint }
                    ?: LanguageDetector.detect(msg.content)
                val direction = if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isUser) TeenaColors.SurfaceElevated else TeenaColors.Surface,
                        border = BorderStroke(
                            1.dp,
                            if (isUser) TeenaColors.Cyan.copy(alpha = 0.45f) else TeenaColors.Border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "You • ${lang.displayName}" else "${msg.modelUsed ?: "TEENA"} • ${lang.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) TeenaColors.Cyan else TeenaColors.Emerald
                                )
                                if (!isUser) {
                                    TextButton(onClick = { viewModel.saveMessageToMemory(msg.content) }) {
                                        Icon(
                                            Icons.Rounded.BookmarkAdd,
                                            contentDescription = "Pin to Memory",
                                            tint = TeenaColors.TextSecondary
                                        )
                                        Text(" Save", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            SelectionContainer {
                                Text(
                                    text = msg.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TeenaColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            state.streamingPartialText?.let { partial ->
                item {
                    val isRtl = LanguageDetector.detect(partial).isRtl
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TeenaColors.Surface,
                            border = BorderStroke(1.dp, TeenaColors.Violet.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Streaming...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TeenaColors.Violet
                                )
                                Text(
                                    text = partial.ifEmpty { "..." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TeenaColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(color = TeenaColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Message TEENA (Urdu / English / Hinglish)...") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                if (state.isStreaming) {
                    IconButton(onClick = viewModel::stopStreaming) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = TeenaColors.Rose)
                    }
                } else {
                    IconButton(
                        onClick = {
                            val toSend = inputText
                            inputText = ""
                            viewModel.sendMessage(toSend)
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = TeenaColors.Cyan
                        )
                    }
                }
            }
        }
    }
}
