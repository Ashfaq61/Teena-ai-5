package com.teena.assistant.core.ai.memory

import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.provider.AiProvider
import com.teena.assistant.core.ai.registry.ApiKeyManager
import com.teena.assistant.core.ai.registry.ModelRegistry
import com.teena.assistant.core.data.local.dao.MemoryDao
import com.teena.assistant.core.data.local.entity.MemoryItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao,
    private val modelRegistry: ModelRegistry,
    private val apiKeyManager: ApiKeyManager,
    private val providers: Map<AiProviderType, @JvmSuppressWildcards AiProvider>,
    private val json: Json
) {

    fun observeMemories(): Flow<List<MemoryItemEntity>> = memoryDao.observeAll()

    suspend fun saveMemory(content: String, category: String = "USER_FACT") {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val embedding = runCatching { computeEmbedding(trimmed) }.getOrNull()
        val embeddingJson = embedding?.let { json.encodeToString(it.toList()) }
        memoryDao.insert(
            MemoryItemEntity(
                content = trimmed,
                category = category,
                embeddingJson = embeddingJson,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteById(id)
    }

    suspend fun clearAllMemories() {
        memoryDao.clearAll()
    }

    suspend fun retrieveRelevantMemories(query: String, limit: Int = 5): List<MemoryItemEntity> {
        val all = memoryDao.getAll()
        if (all.isEmpty()) return emptyList()

        val queryEmbedding = runCatching { computeEmbedding(query) }.getOrNull()
        if (queryEmbedding != null) {
            val scored = all.mapNotNull { item ->
                val vec = item.embeddingJson?.let { raw ->
                    runCatching { json.decodeFromString<List<Float>>(raw).toFloatArray() }.getOrNull()
                }
                if (vec != null && vec.size == queryEmbedding.size) {
                    item to cosineSimilarity(queryEmbedding, vec)
                } else {
                    null
                }
            }.sortedByDescending { it.second }
            if (scored.isNotEmpty()) {
                return scored.take(limit).filter { it.second > 0.2f }.map { it.first }
            }
        }

        val queryTokens = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        if (queryTokens.isEmpty()) return all.take(limit)

        return all.sortedByDescending { item ->
            val words = item.content.lowercase().split(Regex("\\W+")).toSet()
            queryTokens.intersect(words).size
        }.take(limit)
    }

    private suspend fun computeEmbedding(text: String): FloatArray? {
        val selection = modelRegistry.getSelection(ModelCapability.EMBEDDINGS).selectedModel
            ?: return null
        val keys = apiKeyManager.getActiveKeyCandidates(selection.provider)
        val (_, key) = keys.firstOrNull() ?: return null
        val provider = providers[selection.provider] ?: return null
        return provider.embedText(key, selection.modelId, text)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0f else (dot / denom).toFloat()
    }
}
