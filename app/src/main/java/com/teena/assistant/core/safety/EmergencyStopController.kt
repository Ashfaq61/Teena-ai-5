package com.teena.assistant.core.safety

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyStopController @Inject constructor() {

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    private val _stopEvents = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val stopEvents: SharedFlow<Long> = _stopEvents.asSharedFlow()

    fun triggerStop() {
        _isEmergencyStopped.value = true
        _stopEvents.tryEmit(System.currentTimeMillis())
    }

    fun clearStop() {
        _isEmergencyStopped.value = false
    }
}
