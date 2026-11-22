package com.teena.assistant.core.device

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("teena_onboarding_prefs", Context.MODE_PRIVATE)
    private val _completed = MutableStateFlow(prefs.getBoolean(KEY_COMPLETED, false))
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
        _completed.value = true
    }

    fun resetForReview() {
        prefs.edit().putBoolean(KEY_COMPLETED, false).apply()
        _completed.value = false
    }

    private companion object {
        const val KEY_COMPLETED = "onboarding_completed"
    }
}
