package com.teena.assistant.core.ai.registry

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.teena.assistant.core.ai.model.AiProviderType
import com.teena.assistant.core.ai.model.DiscoveredModel
import com.teena.assistant.core.ai.model.ModelCapability
import com.teena.assistant.core.ai.model.ModelSelection
import com.teena.assistant.core.ai.model.ProbeStatus
import com.teena.assistant.core.ai.provider.AiProvider
import com.teena.assistant.core.ai.provider.ProviderApiException
import com.teena.assistant.core.data.local.dao.DiscoveredModelDao
import com.teena.assistant.core.data.local.dao.ModelOverrideDao
import com.teena.assistant.core.data.local.dao.ModelSwitchLogDao
import com.teena.assistant.core.data.local.entity.DiscoveredModelEntity
import com.teena.assistant.core.data.local.entity.ModelOverrideEntity
import com.teena.assistant.core.data.local.entity.ModelSwitchLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyManager: ApiKeyManager,
    private val providers: Map<AiProviderType, @JvmSuppressWildcards AiProvider>,
    private val modelDao: DiscoveredModelDao,
    private val overrideDao: ModelOverrideDao,
    private val switchLogDao: ModelSwitchLogDao
) {
    private val refreshMutex = Mutex()

    fun observeModels(): Flow<List<DiscoveredModel>> =
        modelDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeSwitchLogs(limit: Int = 30): Flow<List<ModelSwitchLogEntity>> =
        switchLogDao.observeRecent(limit)

    fun observeSelection(capability: ModelCapability): Flow<ModelSelection> =
        combine(observeModels(), overrideDao.observeAll()) { models, overrides ->
            val pinned = overrides.firstOrNull { it.capability == capability.name }?.compositeModelId
            ModelRanker.selectBest(models, capability, pinned)
        }

    suspend fun getSelection(capability: ModelCapability): ModelSelection {
        val models = modelDao.getAll().map { it.toDomain() }
        val pinned = overrideDao.getAll()
            .firstOrNull { it.capability == capability.name }?.compositeModelId
        return ModelRanker.selectBest(models, capability, pinned)
    }

    suspend fun pinModelOverride(capability: ModelCapability, compositeModelId: String) {
        val current = getSelection(capability).selectedModel?.compositeId
        overrideDao.upsert(
            ModelOverrideEntity(
                capability = capability.name,
                compositeModelId = compositeModelId,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        switchLogDao.insert(
            ModelSwitchLogEntity(
                capability = capability.name,
                fromModelId = current,
                toModelId = compositeModelId,
                reason = "User manual override pinned",
                timestampEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearModelOverride(capability: ModelCapability) {
        overrideDao.clear(capability.name)
        val newBest = getSelection(capability).selectedModel?.compositeId ?: "none"
        switchLogDao.insert(
            ModelSwitchLogEntity(
                capability = capability.name,
                fromModelId = null,
                toModelId = newBest,
                reason = "Cleared manual override; reverted to auto-detect ranking",
                timestampEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun refreshIfStale(maxAgeHours: Long = 12L) {
        val existing = modelDao.getAll()
        val now = System.currentTimeMillis()
        val maxAgeMs = TimeUnit.HOURS.toMillis(maxAgeHours)
        val newest = existing.maxOfOrNull { it.discoveredAtEpochMs } ?: 0L
        if (existing.isEmpty() || (now - newest) > maxAgeMs) {
            refreshAll(probeCandidates = true)
        }
    }

    suspend fun refreshAll(probeCandidates: Boolean = true): Result<Int> = refreshMutex.withLock {
        val previousSelections = ModelCapability.entries.associateWith {
            getSelection(it).selectedModel?.compositeId
        }
        val existingMap = modelDao.getAll().associateBy { it.compositeId }
        var discoveredTotal = 0

        for (providerType in AiProviderType.entries) {
            val keys = apiKeyManager.getActiveKeyCandidates(providerType)
            if (keys.isEmpty()) continue
            val provider = providers[providerType] ?: continue

            val (slot, apiKey) = keys.first()
            try {
                val discovered = provider.discoverModels(apiKey)
                apiKeyManager.recordSuccess(slot.id, 0L)
                discoveredTotal += discovered.size

                val merged = discovered.map { model ->
                    val prev = existingMap[model.compositeId]
                    if (prev != null && !probeCandidates) {
                        model.copy(
                            probeStatus = ProbeStatus.entries.firstOrNull { it.name == prev.probeStatus }
                                ?: ProbeStatus.UNPROBED,
                            probeLatencyMs = prev.probeLatencyMs,
                            probeError = prev.probeError,
                            probedAtEpochMs = prev.probedAtEpochMs
                        )
                    } else {
                        model
                    }
                }
                modelDao.deleteByProvider(providerType.name)
                modelDao.upsertAll(merged.map { it.toEntity() })

                if (probeCandidates) {
                    probeTopCandidates(provider, apiKey, merged)
                }
            } catch (ex: ProviderApiException) {
                apiKeyManager.recordFailure(slot.id, ex.httpCode, ex.message)
            } catch (t: Throwable) {
                apiKeyManager.recordFailure(slot.id, null, t.message ?: "Discovery failed")
            }
        }

        val now = System.currentTimeMillis()
        ModelCapability.entries.forEach { cap ->
            val oldId = previousSelections[cap]
            val newSel = getSelection(cap)
            val newId = newSel.selectedModel?.compositeId
            if (newId != null && newId != oldId) {
                switchLogDao.insert(
                    ModelSwitchLogEntity(
                        capability = cap.name,
                        fromModelId = oldId,
                        toModelId = newId,
                        reason = newSel.selectionReason,
                        timestampEpochMs = now
                    )
                )
            }
        }

        return@withLock Result.success(discoveredTotal)
    }

    private suspend fun probeTopCandidates(
        provider: AiProvider,
        apiKey: String,
        models: List<DiscoveredModel>
    ) {
        val candidatesToProbe = linkedMapOf<String, Pair<DiscoveredModel, ModelCapability>>()
        listOf(
            ModelCapability.CHAT,
            ModelCapability.LIVE_AUDIO,
            ModelCapability.EMBEDDINGS
        ).forEach { cap ->
            models.filter { cap in it.capabilities }
                .sortedWith(
                    compareBy<DiscoveredModel>(
                        { if (it.isPreview) 1 else 0 },
                        { if (it.isFree) 0 else 1 },
                        { -it.contextWindow }
                    )
                )
                .take(4)
                .forEach { model ->
                    candidatesToProbe.putIfAbsent(model.compositeId, model to cap)
                }
        }

        val now = System.currentTimeMillis()
        candidatesToProbe.values.forEach { (model, cap) ->
            val outcome = provider.probeModel(apiKey, model.modelId, cap)
            val updated = model.copy(
                probeStatus = outcome.status,
                probeLatencyMs = outcome.latencyMs,
                probeError = outcome.errorMessage,
                probedAtEpochMs = now
            )
            modelDao.upsert(updated.toEntity())
        }
    }

    suspend fun markModelFailedAndLogSwitch(
        capability: ModelCapability,
        failedModel: DiscoveredModel,
        error: String
    ) {
        val now = System.currentTimeMillis()
        modelDao.upsert(
            failedModel.copy(
                probeStatus = ProbeStatus.FAILED,
                probeError = error.take(200),
                probedAtEpochMs = now
            ).toEntity()
        )
        val next = getSelection(capability).selectedModel
        switchLogDao.insert(
            ModelSwitchLogEntity(
                capability = capability.name,
                fromModelId = failedModel.compositeId,
                toModelId = next?.compositeId ?: "none",
                reason = "Failover after error: ${error.take(120)}",
                timestampEpochMs = now
            )
        )
    }

    fun schedulePeriodicRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<ModelRefreshWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun DiscoveredModelEntity.toDomain(): DiscoveredModel = DiscoveredModel(
        compositeId = compositeId,
        provider = AiProviderType.valueOf(provider),
        modelId = modelId,
        displayName = displayName,
        capabilities = capabilitiesCsv.split(",")
            .mapNotNull { token -> ModelCapability.entries.firstOrNull { it.name == token } }
            .toSet(),
        isFree = isFree,
        isPreview = isPreview,
        contextWindow = contextWindow,
        pricingSummary = pricingSummary,
        probeStatus = ProbeStatus.entries.firstOrNull { it.name == probeStatus }
            ?: ProbeStatus.UNPROBED,
        probeLatencyMs = probeLatencyMs,
        probeError = probeError,
        discoveredAtEpochMs = discoveredAtEpochMs,
        probedAtEpochMs = probedAtEpochMs
    )

    private fun DiscoveredModel.toEntity(): DiscoveredModelEntity = DiscoveredModelEntity(
        compositeId = compositeId,
        provider = provider.name,
        modelId = modelId,
        displayName = displayName,
        capabilitiesCsv = capabilities.joinToString(",") { it.name },
        isFree = isFree,
        isPreview = isPreview,
        contextWindow = contextWindow,
        pricingSummary = pricingSummary,
        probeStatus = probeStatus.name,
        probeLatencyMs = probeLatencyMs,
        probeError = probeError,
        discoveredAtEpochMs = discoveredAtEpochMs,
        probedAtEpochMs = probedAtEpochMs
    )

    private companion object {
        const val WORK_NAME = "teena_model_registry_24h_refresh"
    }
}
