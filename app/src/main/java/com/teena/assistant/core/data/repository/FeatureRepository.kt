package com.teena.assistant.core.data.repository

import com.teena.assistant.BuildConfig
import com.teena.assistant.core.data.local.dao.FeatureRecordDao
import com.teena.assistant.core.data.local.dao.SystemEventDao
import com.teena.assistant.core.data.local.entity.FeatureRecordEntity
import com.teena.assistant.core.data.local.entity.SystemEventEntity
import com.teena.assistant.core.model.FeatureCatalog
import com.teena.assistant.core.model.FeatureSpec
import com.teena.assistant.core.model.FeatureStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureRepository @Inject constructor(
    private val featureRecordDao: FeatureRecordDao,
    private val systemEventDao: SystemEventDao
) {

    fun observeFeatures(): Flow<List<FeatureSpec>> =
        featureRecordDao.observeAll().map { entities ->
            entities.map { entity ->
                FeatureSpec(
                    id = entity.id,
                    title = entity.title,
                    sectionRoute = entity.sectionRoute,
                    phase = entity.phase,
                    status = FeatureStatus.entries.firstOrNull { it.name == entity.status }
                        ?: FeatureStatus.NOT_IMPLEMENTED,
                    details = entity.details
                )
            }
        }

    fun observeEvents(limit: Int = 30): Flow<List<SystemEventEntity>> =
        systemEventDao.observeRecent(limit)

    suspend fun syncCatalogIfNeeded() {
        val now = System.currentTimeMillis()
        val isFirstSeed = featureRecordDao.count() == 0
        val records = FeatureCatalog.initialFeatures.map { spec ->
            FeatureRecordEntity(
                id = spec.id,
                title = spec.title,
                sectionRoute = spec.sectionRoute,
                phase = spec.phase,
                status = spec.status.name,
                details = spec.details,
                updatedAtEpochMs = now
            )
        }
        featureRecordDao.upsertAll(records)
        if (isFirstSeed) {
            systemEventDao.insert(
                SystemEventEntity(
                    category = "SYSTEM",
                    message = "Initialized TEENA AI v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) Room feature registry.",
                    timestampEpochMs = now
                )
            )
        }
    }
}
