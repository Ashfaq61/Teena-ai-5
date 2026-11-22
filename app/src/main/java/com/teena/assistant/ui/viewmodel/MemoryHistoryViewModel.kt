package com.teena.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teena.assistant.core.ai.memory.MemoryRepository
import com.teena.assistant.core.data.local.dao.SystemEventDao
import com.teena.assistant.core.data.local.entity.MemoryItemEntity
import com.teena.assistant.core.data.local.entity.SystemEventEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryHistoryUiState(
    val memories: List<MemoryItemEntity> = emptyList(),
    val searchQuery: String = "",
    val systemEvents: List<SystemEventEntity> = emptyList()
)

@HiltViewModel
class MemoryHistoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    systemEventDao: SystemEventDao
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val uiState: StateFlow<MemoryHistoryUiState> = combine(
        memoryRepository.observeMemories(),
        queryFlow,
        systemEventDao.observeRecent(40)
    ) { allMemories, query, events ->
        val filtered = if (query.isBlank()) {
            allMemories
        } else {
            allMemories.filter {
                it.content.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }
        MemoryHistoryUiState(
            memories = filtered,
            searchQuery = query,
            systemEvents = events
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MemoryHistoryUiState()
    )

    fun setSearchQuery(q: String) {
        queryFlow.value = q
    }

    fun addMemory(content: String) {
        viewModelScope.launch {
            memoryRepository.saveMemory(content, category = "MANUAL")
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            memoryRepository.clearAllMemories()
        }
    }
}
