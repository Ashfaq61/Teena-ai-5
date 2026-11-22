package com.teena.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teena.assistant.core.ai.AiBrainRouter
import com.teena.assistant.core.ai.RouterStreamUpdate
import com.teena.assistant.core.ai.memory.MemoryRepository
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ModelSelection
import com.teena.assistant.core.ai.registry.ModelRegistry
import com.teena.assistant.core.data.local.dao.ChatDao
import com.teena.assistant.core.data.local.entity.ChatMessageEntity
import com.teena.assistant.core.data.local.entity.ChatSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val activeSessionId: String = "default_session",
    val sessions: List<ChatSessionEntity> = emptyList(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val streamingPartialText: String? = null,
    val isStreaming: Boolean = false,
    val activeModelLabel: String = "Checking ModelRegistry...",
    val fallbackBanner: String? = null,
    val errorBanner: String? = null,
    val useLongTermMemory: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiBrainRouter: AiBrainRouter,
    private val chatDao: ChatDao,
    private val memoryRepository: MemoryRepository,
    modelRegistry: ModelRegistry
) : ViewModel() {

    private val activeSessionId = MutableStateFlow("default_session")
    private val streamingPartialText = MutableStateFlow<String?>(null)
    private val isStreaming = MutableStateFlow(false)
    private val fallbackBanner = MutableStateFlow<String?>(null)
    private val errorBanner = MutableStateFlow<String?>(null)
    private val useLongTermMemory = MutableStateFlow(true)
    private var streamJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val messagesFlow = activeSessionId.flatMapLatest { id ->
        chatDao.observeMessages(id)
    }

    private val streamMetaFlow = combine(
        streamingPartialText,
        isStreaming,
        fallbackBanner,
        errorBanner,
        useLongTermMemory
    ) { partial, streaming, fallback, error, memory ->
        StreamMeta(partial, streaming, fallback, error, memory)
    }

    val uiState: StateFlow<ChatUiState> = combine(
        activeSessionId,
        chatDao.observeSessions(),
        messagesFlow,
        modelRegistry.observeSelection(ModelCapability.CHAT),
        streamMetaFlow
    ) { sessionId, sessions, messages, chatSelection: ModelSelection, meta ->
        val modelLabel = chatSelection.selectedModel?.let {
            "${it.provider.displayName} • ${it.modelId}"
        } ?: "No working Chat model (configure API Manager)"

        ChatUiState(
            activeSessionId = sessionId,
            sessions = sessions,
            messages = messages,
            streamingPartialText = meta.partial,
            isStreaming = meta.streaming,
            activeModelLabel = modelLabel,
            fallbackBanner = meta.fallback,
            errorBanner = meta.error,
            useLongTermMemory = meta.memory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState()
    )

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isStreaming.value) return

        errorBanner.value = null
        fallbackBanner.value = null
        streamingPartialText.value = ""
        isStreaming.value = true

        val sid = activeSessionId.value
        val useMem = useLongTermMemory.value

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            aiBrainRouter.sendMessageStreaming(
                sessionId = sid,
                userText = trimmed,
                useLongTermMemory = useMem
            ).collect { update ->
                when (update) {
                    is RouterStreamUpdate.UsingModel -> {
                        if (update.fallbackNotice != null) {
                            fallbackBanner.value = update.fallbackNotice
                        }
                    }
                    is RouterStreamUpdate.TokenDelta -> {
                        streamingPartialText.value = update.partialText
                    }
                    is RouterStreamUpdate.Finished -> {
                        streamingPartialText.value = null
                        isStreaming.value = false
                    }
                    is RouterStreamUpdate.Failed -> {
                        streamingPartialText.value = null
                        isStreaming.value = false
                        errorBanner.value = update.reason
                    }
                }
            }
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamingPartialText.value = null
        isStreaming.value = false
    }

    fun selectSession(sessionId: String) {
        activeSessionId.value = sessionId
        errorBanner.value = null
    }

    fun createNewSession() {
        activeSessionId.value = "chat_${UUID.randomUUID().toString().take(8)}"
        errorBanner.value = null
        fallbackBanner.value = null
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatDao.deleteSessionMessages(sessionId)
            chatDao.deleteSession(sessionId)
            if (activeSessionId.value == sessionId) {
                activeSessionId.value = "default_session"
            }
        }
    }

    fun toggleLongTermMemory(enabled: Boolean) {
        useLongTermMemory.value = enabled
    }

    fun saveMessageToMemory(content: String) {
        viewModelScope.launch {
            memoryRepository.saveMemory(content, category = "CHAT_PINNED")
        }
    }

    private data class StreamMeta(
        val partial: String?,
        val streaming: Boolean,
        val fallback: String?,
        val error: String?,
        val memory: Boolean
    )
}
