package com.teena.assistant.core.ai.registry

import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.ApiKeySlot
import com.teena.assistant.core.ai.model.ProbeOutcome
import com.teena.assistant.core.ai.model.ProbeStatus
import com.teena.assistant.core.ai.provider.AiProvider
import com.teena.assistant.core.data.local.dao.ApiKeySlotDao
import com.teena.assistant.core.data.local.entity.ApiKeySlotEntity
import com.teena.assistant.core.security.KeyVault
import com.teena.assistant.core.security.maskApiKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ApiKeyManager @Inject constructor(
    private val slotDao: ApiKeySlotDao,
    private val keyVault: KeyVault,
    private val providers: Map<AiProviderType, @JvmSuppressWildcards AiProvider>
) {

    fun observeSlots(): Flow<List<ApiKeySlot>> =
        slotDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getSlots(): List<ApiKeySlot> =
        slotDao.getAll().map { it.toDomain() }

    suspend fun addSlot(
        provider: AiProviderType,
        label: String,
        rawKey: String
    ): ApiKeySlot {
        val trimmed = rawKey.trim()
        require(trimmed.isNotBlank()) { "API key cannot be empty." }
        val slotId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        keyVault.saveKey(slotId, trimmed)

        val entity = ApiKeySlotEntity(
            id = slotId,
            provider = provider.name,
            label = label.ifBlank { "${provider.displayName} Key" },
            maskedKey = maskApiKey(trimmed),
            enabled = true,
            isValid = false,
            lastTestMessage = "Added (testing connection...)",
            lastLatencyMs = null,
            cooldownUntilEpochMs = 0L,
            consecutiveFailures = 0,
            createdAtEpochMs = now
        )
        slotDao.upsert(entity)
        return testSlot(slotId)
    }

    suspend fun testSlot(slotId: String): ApiKeySlot {
        val entity = slotDao.getById(slotId)
            ?: throw IllegalArgumentException("Key slot not found")
        val rawKey = keyVault.getKey(slotId)
        if (rawKey.isNullOrBlank()) {
            slotDao.updateTestResult(slotId, false, "Key missing from Android Keystore vault", null)
            return slotDao.getById(slotId)!!.toDomain()
        }

        val providerType = AiProviderType.valueOf(entity.provider)
        val provider = providers[providerType]
            ?: throw IllegalStateException("Provider $providerType not registered")

        val outcome: ProbeOutcome = provider.testConnection(rawKey)
        val isValid = outcome.status == ProbeStatus.WORKING
        val msg = when (outcome.status) {
            ProbeStatus.WORKING -> "Connected (${outcome.latencyMs}ms)"
            ProbeStatus.RATE_LIMITED -> "Rate limited: ${outcome.errorMessage.orEmpty()}"
            else -> outcome.errorMessage ?: "Connection failed"
        }
        slotDao.updateTestResult(slotId, isValid, msg, outcome.latencyMs)
        return slotDao.getById(slotId)!!.toDomain()
    }

    suspend fun toggleSlot(slotId: String, enabled: Boolean) {
        slotDao.setEnabled(slotId, enabled)
    }

    suspend fun deleteSlot(slotId: String) {
        keyVault.deleteKey(slotId)
        slotDao.deleteById(slotId)
    }

    suspend fun getActiveKeyCandidates(provider: AiProviderType): List<Pair<ApiKeySlot, String>> {
        val now = System.currentTimeMillis()
        return slotDao.getAll()
            .filter {
                it.provider == provider.name &&
                    it.enabled &&
                    it.cooldownUntilEpochMs <= now
            }
            .sortedWith(compareByDescending<ApiKeySlotEntity> { it.isValid }.thenBy { it.consecutiveFailures })
            .mapNotNull { slot ->
                val raw = keyVault.getKey(slot.id)
                if (raw.isNullOrBlank()) null else slot.toDomain() to raw
            }
    }

    suspend fun recordFailure(slotId: String, httpCode: Int?, message: String) {
        val entity = slotDao.getById(slotId) ?: return
        val failures = entity.consecutiveFailures + 1
        if (httpCode == 401 || httpCode == 403) {
            slotDao.updateTestResult(slotId, false, "Auth error ($httpCode): $message", entity.lastLatencyMs)
            return
        }
        val backoffSeconds = min(300L, 15L * (1L shl min(failures - 1, 4)))
        val cooldownUntil = System.currentTimeMillis() + backoffSeconds * 1000L
        slotDao.updateCooldown(
            id = slotId,
            cooldownUntil = cooldownUntil,
            failures = failures,
            message = "Cooldown ${backoffSeconds}s ($message)"
        )
    }

    suspend fun recordSuccess(slotId: String, latencyMs: Long) {
        slotDao.updateTestResult(slotId, true, "OK (${latencyMs}ms)", latencyMs)
    }

    private fun ApiKeySlotEntity.toDomain(): ApiKeySlot = ApiKeySlot(
        id = id,
        provider = AiProviderType.valueOf(provider),
        label = label,
        maskedKey = maskedKey,
        enabled = enabled,
        isValid = isValid,
        lastTestMessage = lastTestMessage,
        lastLatencyMs = lastLatencyMs,
        cooldownUntilEpochMs = cooldownUntilEpochMs,
        createdAtEpochMs = createdAtEpochMs
    )
}
