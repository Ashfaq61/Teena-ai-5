package com.teena.assistant.core.voice

enum class LivePrebuiltVoice(
    val apiName: String,
    val gender: String,
    val description: String
) {
    AOEDE("Aoede", "Female", "Warm conversational"),
    KORE("Kore", "Female", "Clear neutral"),
    LEDA("Leda", "Female", "Soft expressive"),
    ZEPHYR("Zephyr", "Female", "Bright articulate"),
    PUCK("Puck", "Male", "Upbeat agile"),
    CHARON("Charon", "Male", "Deep calm"),
    FENRIR("Fenrir", "Male", "Bold resonant"),
    ORUS("Orus", "Male", "Steady informative")
}

enum class AudioOutputRoute(val displayName: String) {
    AUTO("Auto"),
    EARPIECE("Earpiece"),
    SPEAKER("Speaker"),
    BLUETOOTH("Bluetooth")
}

enum class VadSensitivity(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

data class PendingToolConfirmation(
    val callId: String,
    val toolName: String,
    val argsSummary: String,
    val rawArgsJson: String
)

sealed interface LiveVoiceState {
    data class Blocked(val reason: String) : LiveVoiceState
    data object Idle : LiveVoiceState
    data class Connecting(val modelId: String) : LiveVoiceState
    data class Listening(val modelId: String, val muted: Boolean) : LiveVoiceState
    data class Thinking(val modelId: String) : LiveVoiceState
    data class Speaking(val modelId: String) : LiveVoiceState
    data class Reconnecting(val reason: String, val attempt: Int) : LiveVoiceState
    data class Error(val message: String) : LiveVoiceState
}

data class LiveSessionSnapshot(
    val state: LiveVoiceState = LiveVoiceState.Idle,
    val selectedVoice: LivePrebuiltVoice = LivePrebuiltVoice.KORE,
    val selectedRoute: AudioOutputRoute = AudioOutputRoute.AUTO,
    val vadSensitivity: VadSensitivity = VadSensitivity.MEDIUM,
    val continuousListening: Boolean = true,
    val isMuted: Boolean = false,
    val isPreviewingVoice: Boolean = false,
    val liveInputCaption: String = "",
    val liveOutputCaption: String = "",
    val activeModelId: String? = null,
    val pendingConfirmation: PendingToolConfirmation? = null
)
