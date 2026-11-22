package com.teena.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teena.assistant.core.data.local.dao.VoiceTranscriptDao
import com.teena.assistant.core.data.local.entity.VoiceTranscriptEntity
import com.teena.assistant.core.voice.AudioOutputRoute
import com.teena.assistant.core.voice.LivePrebuiltVoice
import com.teena.assistant.core.voice.LiveSessionSnapshot
import com.teena.assistant.core.voice.LiveVoiceController
import com.teena.assistant.core.voice.VadSensitivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveVoiceUiState(
    val session: LiveSessionSnapshot = LiveSessionSnapshot(),
    val transcripts: List<VoiceTranscriptEntity> = emptyList()
)

@HiltViewModel
class LiveVoiceViewModel @Inject constructor(
    private val controller: LiveVoiceController,
    private val voiceTranscriptDao: VoiceTranscriptDao
) : ViewModel() {

    val uiState: StateFlow<LiveVoiceUiState> = combine(
        controller.snapshot,
        controller.recentTranscripts
    ) { snapshot, transcripts ->
        LiveVoiceUiState(snapshot, transcripts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LiveVoiceUiState()
    )

    fun refreshReadiness() {
        viewModelScope.launch { controller.refreshReadiness() }
    }

    fun startSession() = controller.startSession()
    fun stopSession() = controller.stopSession()
    fun toggleMute() = controller.toggleMute()
    fun selectVoice(voice: LivePrebuiltVoice) = controller.selectVoice(voice)
    fun previewVoice() = controller.previewSelectedVoice()
    fun selectRoute(route: AudioOutputRoute) = controller.selectRoute(route)
    fun selectVadSensitivity(sensitivity: VadSensitivity) = controller.selectVadSensitivity(sensitivity)
    fun toggleContinuousListening(enabled: Boolean) = controller.toggleContinuousListening(enabled)
    fun approvePendingTool() = controller.approvePendingTool()
    fun denyPendingTool() = controller.denyPendingTool()

    fun clearHistory() {
        viewModelScope.launch { voiceTranscriptDao.clearAll() }
    }
}
