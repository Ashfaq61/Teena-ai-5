package com.teena.assistant.core.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.registry.ApiKeyManager
import com.teena.assistant.core.ai.registry.ModelRegistry
import com.teena.assistant.core.data.local.dao.VoiceTranscriptDao
import com.teena.assistant.core.data.local.entity.VoiceTranscriptEntity
import com.teena.assistant.core.safety.EmergencyStopController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class LiveVoiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRegistry: ModelRegistry,
    private val apiKeyManager: ApiKeyManager,
    private val geminiLiveClient: GeminiLiveClient,
    private val audioEngine: LiveAudioEngine,
    private val toolExecutor: LiveToolExecutor,
    private val voiceTranscriptDao: VoiceTranscriptDao,
    private val emergencyStopController: EmergencyStopController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _snapshot = MutableStateFlow(LiveSessionSnapshot())
    val snapshot: StateFlow<LiveSessionSnapshot> = _snapshot.asStateFlow()

    val recentTranscripts: Flow<List<VoiceTranscriptEntity>> =
        voiceTranscriptDao.observeRecent(100)

    private var resumptionHandle: String? = null
    private var currentSlotId: String? = null
    private var currentModel: DiscoveredModel? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var intentionallyStopped = true
    private var networkCallbackRegistered = false
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val outputTranscriptBuffer = StringBuilder()
    private val inputTranscriptBuffer = StringBuilder()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!intentionallyStopped && _snapshot.value.state is LiveVoiceState.Reconnecting) {
                scope.launch { connectSession(isReconnect = true, previewOnly = false) }
            }
        }
    }

    init {
        scope.launch {
            emergencyStopController.stopEvents.collect {
                stopSession()
            }
        }
        scope.launch {
            refreshReadiness()
        }
    }

    suspend fun refreshReadiness() {
        val blocker = checkBlockers()
        val currentState = _snapshot.value.state
        if (blocker != null && (currentState is LiveVoiceState.Idle || currentState is LiveVoiceState.Blocked)) {
            _snapshot.update { it.copy(state = LiveVoiceState.Blocked(blocker)) }
        } else if (blocker == null && currentState is LiveVoiceState.Blocked) {
            _snapshot.update { it.copy(state = LiveVoiceState.Idle) }
        }
    }

    fun selectVoice(voice: LivePrebuiltVoice) {
        _snapshot.update { it.copy(selectedVoice = voice) }
    }

    fun selectRoute(route: AudioOutputRoute) {
        _snapshot.update { it.copy(selectedRoute = route) }
        audioEngine.applyRoute(route)
    }

    fun selectVadSensitivity(sensitivity: VadSensitivity) {
        _snapshot.update { it.copy(vadSensitivity = sensitivity) }
    }

    fun toggleContinuousListening(enabled: Boolean) {
        _snapshot.update { it.copy(continuousListening = enabled) }
    }

    fun toggleMute() {
        val next = !_snapshot.value.isMuted
        audioEngine.setMuted(next)
        _snapshot.update { snap ->
            val updatedState = if (snap.state is LiveVoiceState.Listening) {
                LiveVoiceState.Listening(snap.state.modelId, next)
            } else {
                snap.state
            }
            snap.copy(isMuted = next, state = updatedState)
        }
    }

    fun startSession() {
        emergencyStopController.clearStop()
        intentionallyStopped = false
        reconnectAttempts = 0
        scope.launch {
            val blocker = checkBlockers()
            if (blocker != null) {
                _snapshot.update { it.copy(state = LiveVoiceState.Blocked(blocker)) }
                return@launch
            }
            startForegroundMicService()
            registerNetworkCallbackIfNeeded()
            connectSession(isReconnect = false, previewOnly = false)
        }
    }

    fun previewSelectedVoice() {
        scope.launch {
            val blocker = checkBlockers()
            if (blocker != null) {
                _snapshot.update { it.copy(state = LiveVoiceState.Blocked(blocker)) }
                return@launch
            }
            intentionallyStopped = false
            _snapshot.update { it.copy(isPreviewingVoice = true) }
            connectSession(isReconnect = false, previewOnly = true)
        }
    }

    fun approvePendingTool() {
        val pending = _snapshot.value.pendingConfirmation ?: return
        _snapshot.update { it.copy(pendingConfirmation = null) }
        scope.launch {
            val result = toolExecutor.execute(pending.toolName, pending.rawArgsJson)
            geminiLiveClient.sendToolResponse(pending.callId, pending.toolName, result)
        }
    }

    fun denyPendingTool() {
        val pending = _snapshot.value.pendingConfirmation ?: return
        _snapshot.update { it.copy(pendingConfirmation = null) }
        geminiLiveClient.sendToolResponse(
            pending.callId,
            pending.toolName,
            buildJsonObject {
                put("status", "denied_by_user")
                put("reason", "User declined confirmation for ${pending.toolName}")
            }
        )
    }

    fun stopSession() {
        intentionallyStopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        unregisterNetworkCallbackIfNeeded()
        geminiLiveClient.disconnect()
        audioEngine.stopAudio()
        flushTranscriptBuffers()
        stopForegroundMicService()
        scope.launch {
            val blocker = checkBlockers()
            _snapshot.update {
                it.copy(
                    state = if (blocker != null) LiveVoiceState.Blocked(blocker) else LiveVoiceState.Idle,
                    isMuted = false,
                    isPreviewingVoice = false,
                    pendingConfirmation = null,
                    activeModelId = null
                )
            }
        }
    }

    private suspend fun checkBlockers(): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return "BLOCKED: Microphone permission (RECORD_AUDIO) not granted."
        }
        if (!isNetworkConnected()) {
            return "BLOCKED: No active internet connection for Gemini Live WebSocket."
        }
        val keys = apiKeyManager.getActiveKeyCandidates(AiProviderType.GEMINI)
        if (keys.isEmpty()) {
            return "BLOCKED: No active Gemini API key configured in API Manager."
        }
        val selection = modelRegistry.getSelection(ModelCapability.LIVE_AUDIO)
        if (selection.selectedModel == null) {
            return "BLOCKED: No Live-capable (bidiGenerateContent) model detected. Run Model Discovery in API Manager."
        }
        return null
    }

    private suspend fun connectSession(isReconnect: Boolean, previewOnly: Boolean) {
        val keys = apiKeyManager.getActiveKeyCandidates(AiProviderType.GEMINI)
        if (keys.isEmpty()) {
            _snapshot.update {
                it.copy(state = LiveVoiceState.Blocked("BLOCKED: All Gemini keys are cooling down or invalid."))
            }
            return
        }
        val selection = modelRegistry.getSelection(ModelCapability.LIVE_AUDIO)
        val candidates = buildList {
            selection.selectedModel?.let { add(it) }
            addAll(selection.fallbackChain)
        }.filter { it.provider == AiProviderType.GEMINI }

        val targetModel = candidates.firstOrNull()
        if (targetModel == null) {
            _snapshot.update {
                it.copy(state = LiveVoiceState.Blocked("BLOCKED: No Gemini Live (bidiGenerateContent) model available."))
            }
            return
        }

        val (slot, apiKey) = keys.first()
        currentSlotId = slot.id
        currentModel = targetModel

        _snapshot.update {
            it.copy(
                state = if (isReconnect) {
                    LiveVoiceState.Reconnecting("Reconnecting ${targetModel.modelId}...", reconnectAttempts)
                } else {
                    LiveVoiceState.Connecting(targetModel.modelId)
                },
                activeModelId = targetModel.modelId
            )
        }

        val voice = _snapshot.value.selectedVoice
        val route = _snapshot.value.selectedRoute
        val vad = _snapshot.value.vadSensitivity

        geminiLiveClient.connect(
            apiKey = apiKey,
            modelId = targetModel.modelId,
            voice = voice,
            vadSensitivity = vad,
            resumptionHandle = if (previewOnly) null else resumptionHandle,
            previewOnly = previewOnly,
            callbacks = object : LiveClientCallbacks {
                override fun onSetupComplete() {
                    scope.launch {
                        reconnectAttempts = 0
                        apiKeyManager.recordSuccess(slot.id, 0L)
                        if (previewOnly) {
                            audioEngine.startAudio(
                                scope = scope,
                                route = route,
                                onPcm16kChunk = {},
                                onAudioFocusLost = { stopSession() }
                            )
                            audioEngine.setMuted(true)
                            _snapshot.update { it.copy(state = LiveVoiceState.Speaking(targetModel.modelId)) }
                            geminiLiveClient.sendUserTextTurn(
                                "Say a short friendly 1-sentence greeting as TEENA AI in your current voice."
                            )
                        } else {
                            audioEngine.startAudio(
                                scope = scope,
                                route = route,
                                onPcm16kChunk = { chunk ->
                                    geminiLiveClient.sendAudioChunk16k(chunk)
                                },
                                onAudioFocusLost = {
                                    if (!_snapshot.value.isMuted) toggleMute()
                                }
                            )
                            audioEngine.setMuted(_snapshot.value.isMuted)
                            _snapshot.update {
                                it.copy(
                                    state = LiveVoiceState.Listening(
                                        modelId = targetModel.modelId,
                                        muted = it.isMuted
                                    )
                                )
                            }
                        }
                    }
                }

                override fun onAudioChunk24k(pcmBytes: ByteArray) {
                    audioEngine.writeOutputPcm24k(pcmBytes)
                    _snapshot.update {
                        if (it.state !is LiveVoiceState.Speaking) {
                            it.copy(state = LiveVoiceState.Speaking(targetModel.modelId))
                        } else {
                            it
                        }
                    }
                }

                override fun onInputTranscript(text: String) {
                    inputTranscriptBuffer.append(text)
                    _snapshot.update { it.copy(liveInputCaption = inputTranscriptBuffer.toString()) }
                }

                override fun onOutputTranscript(text: String) {
                    outputTranscriptBuffer.append(text)
                    _snapshot.update { it.copy(liveOutputCaption = outputTranscriptBuffer.toString()) }
                }

                override fun onTurnComplete() {
                    scope.launch {
                        flushTranscriptBuffers()
                        if (previewOnly) {
                            delay(600L)
                            stopSession()
                        } else if (_snapshot.value.continuousListening) {
                            _snapshot.update {
                                it.copy(
                                    state = LiveVoiceState.Listening(
                                        modelId = targetModel.modelId,
                                        muted = it.isMuted
                                    )
                                )
                            }
                        } else {
                            stopSession()
                        }
                    }
                }

                override fun onInterrupted() {
                    audioEngine.flushOutputForBargeIn()
                    scope.launch {
                        flushTranscriptBuffers()
                        _snapshot.update {
                            it.copy(
                                state = LiveVoiceState.Listening(
                                    modelId = targetModel.modelId,
                                    muted = it.isMuted
                                )
                            )
                        }
                    }
                }

                override fun onSessionResumptionUpdate(newHandle: String) {
                    resumptionHandle = newHandle
                }

                override fun onGoAway(timeLeft: String?) {
                    scope.launch {
                        if (!intentionallyStopped && !previewOnly) {
                            scheduleReconnect("Server goAway (${timeLeft ?: "closing soon"})", rotateModel = false)
                        }
                    }
                }

                override fun onToolCall(callId: String, name: String, argsJson: String) {
                    scope.launch {
                        _snapshot.update { it.copy(state = LiveVoiceState.Thinking(targetModel.modelId)) }
                        if (toolExecutor.requiresUserConfirmation(name)) {
                            _snapshot.update {
                                it.copy(
                                    pendingConfirmation = PendingToolConfirmation(
                                        callId = callId,
                                        toolName = name,
                                        argsSummary = argsJson,
                                        rawArgsJson = argsJson
                                    )
                                )
                            }
                        } else {
                            val result = toolExecutor.execute(name, argsJson)
                            geminiLiveClient.sendToolResponse(callId, name, result)
                        }
                    }
                }

                override fun onClosed(code: Int, reason: String) {
                    scope.launch {
                        if (!intentionallyStopped && !previewOnly) {
                            scheduleReconnect("Socket closed ($code): $reason", rotateModel = code == 1008)
                        }
                    }
                }

                override fun onFailure(httpCode: Int?, message: String) {
                    scope.launch {
                        currentSlotId?.let { id ->
                            apiKeyManager.recordFailure(id, httpCode, message)
                        }
                        if (httpCode == 404 || message.contains("not found", true)) {
                            currentModel?.let { m ->
                                modelRegistry.markModelFailedAndLogSwitch(
                                    ModelCapability.LIVE_AUDIO,
                                    m,
                                    message
                                )
                            }
                        }
                        if (!intentionallyStopped && !previewOnly) {
                            scheduleReconnect(message, rotateModel = true)
                        } else {
                            _snapshot.update { it.copy(state = LiveVoiceState.Error(message), isPreviewingVoice = false) }
                        }
                    }
                }
            }
        )
    }

    private fun scheduleReconnect(reason: String, rotateModel: Boolean) {
        if (intentionallyStopped) return
        reconnectJob?.cancel()
        reconnectAttempts++
        if (reconnectAttempts > 5) {
            _snapshot.update {
                it.copy(state = LiveVoiceState.Error("Live connection lost after 5 retries: $reason"))
            }
            audioEngine.stopAudio()
            stopForegroundMicService()
            return
        }
        val backoffMs = min(16_000L, 1_000L * (1L shl (reconnectAttempts - 1)))
        _snapshot.update {
            it.copy(state = LiveVoiceState.Reconnecting(reason, reconnectAttempts))
        }
        reconnectJob = scope.launch {
            if (rotateModel) {
                delay(250L)
            }
            delay(backoffMs)
            if (!intentionallyStopped && isNetworkConnected()) {
                connectSession(isReconnect = true, previewOnly = false)
            }
        }
    }

    private fun flushTranscriptBuffers() {
        val modelId = currentModel?.modelId ?: "gemini-live"
        val voiceName = _snapshot.value.selectedVoice.apiName
        val userText = inputTranscriptBuffer.toString().trim()
        val assistantText = outputTranscriptBuffer.toString().trim()
        inputTranscriptBuffer.clear()
        outputTranscriptBuffer.clear()

        if (userText.isNotEmpty() || assistantText.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                if (userText.isNotEmpty()) {
                    voiceTranscriptDao.insert(
                        VoiceTranscriptEntity(
                            speaker = "user",
                            text = userText,
                            modelUsed = modelId,
                            voiceName = voiceName,
                            timestampEpochMs = now
                        )
                    )
                }
                if (assistantText.isNotEmpty()) {
                    voiceTranscriptDao.insert(
                        VoiceTranscriptEntity(
                            speaker = "teena",
                            text = assistantText,
                            modelUsed = modelId,
                            voiceName = voiceName,
                            timestampEpochMs = now + 1
                        )
                    )
                }
            }
        }
    }

    private fun startForegroundMicService() {
        val intent = Intent(context, GeminiLiveService::class.java).apply {
            action = GeminiLiveService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopForegroundMicService() {
        runCatching {
            context.stopService(Intent(context, GeminiLiveService::class.java))
        }
    }

    private fun registerNetworkCallbackIfNeeded() {
        if (!networkCallbackRegistered) {
            runCatching {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                networkCallbackRegistered = true
            }
        }
    }

    private fun unregisterNetworkCallbackIfNeeded() {
        if (networkCallbackRegistered) {
            runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
            networkCallbackRegistered = false
        }
    }

    private fun isNetworkConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
