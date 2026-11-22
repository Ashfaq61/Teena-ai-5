package com.teena.assistant.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teena.assistant.core.data.local.dao.ApiKeySlotDao
import com.teena.assistant.core.data.local.dao.ChatDao
import com.teena.assistant.core.data.local.dao.DiscoveredModelDao
import com.teena.assistant.core.data.local.dao.FeatureRecordDao
import com.teena.assistant.core.data.local.dao.MemoryDao
import com.teena.assistant.core.data.local.dao.ModelOverrideDao
import com.teena.assistant.core.data.local.dao.ModelSwitchLogDao
import com.teena.assistant.core.data.local.dao.SystemEventDao
import com.teena.assistant.core.data.local.dao.UsageRecordDao
import com.teena.assistant.core.data.local.dao.VoiceTranscriptDao
import com.teena.assistant.core.data.local.entity.ApiKeySlotEntity
import com.teena.assistant.core.data.local.entity.ChatMessageEntity
import com.teena.assistant.core.data.local.entity.ChatSessionEntity
import com.teena.assistant.core.data.local.entity.DiscoveredModelEntity
import com.teena.assistant.core.data.local.entity.FeatureRecordEntity
import com.teena.assistant.core.data.local.entity.MemoryItemEntity
import com.teena.assistant.core.data.local.entity.ModelOverrideEntity
import com.teena.assistant.core.data.local.entity.ModelSwitchLogEntity
import com.teena.assistant.core.data.local.entity.SystemEventEntity
import com.teena.assistant.core.data.local.entity.UsageRecordEntity
import com.teena.assistant.core.data.local.entity.VoiceTranscriptEntity

@Database(
    entities = [
        FeatureRecordEntity::class,
        SystemEventEntity::class,
        ApiKeySlotEntity::class,
        DiscoveredModelEntity::class,
        ModelOverrideEntity::class,
        ModelSwitchLogEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MemoryItemEntity::class,
        VoiceTranscriptEntity::class,
        UsageRecordEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class TeenaDatabase : RoomDatabase() {
    abstract fun featureRecordDao(): FeatureRecordDao
    abstract fun systemEventDao(): SystemEventDao
    abstract fun apiKeySlotDao(): ApiKeySlotDao
    abstract fun discoveredModelDao(): DiscoveredModelDao
    abstract fun modelOverrideDao(): ModelOverrideDao
    abstract fun modelSwitchLogDao(): ModelSwitchLogDao
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun voiceTranscriptDao(): VoiceTranscriptDao
    abstract fun usageRecordDao(): UsageRecordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `api_key_slots` (
                        `id` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `maskedKey` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `isValid` INTEGER NOT NULL,
                        `lastTestMessage` TEXT NOT NULL,
                        `lastLatencyMs` INTEGER,
                        `cooldownUntilEpochMs` INTEGER NOT NULL,
                        `consecutiveFailures` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `discovered_models` (
                        `compositeId` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `capabilitiesCsv` TEXT NOT NULL,
                        `isFree` INTEGER NOT NULL,
                        `isPreview` INTEGER NOT NULL,
                        `contextWindow` INTEGER NOT NULL,
                        `pricingSummary` TEXT NOT NULL,
                        `probeStatus` TEXT NOT NULL,
                        `probeLatencyMs` INTEGER,
                        `probeError` TEXT,
                        `discoveredAtEpochMs` INTEGER NOT NULL,
                        `probedAtEpochMs` INTEGER,
                        PRIMARY KEY(`compositeId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_overrides` (
                        `capability` TEXT NOT NULL,
                        `compositeModelId` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`capability`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_switch_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `capability` TEXT NOT NULL,
                        `fromModelId` TEXT,
                        `toModelId` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_sessions` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `languageHint` TEXT NOT NULL,
                        `providerUsed` TEXT,
                        `modelUsed` TEXT,
                        `inputTokens` INTEGER NOT NULL,
                        `outputTokens` INTEGER NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memory_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `content` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `embeddingJson` TEXT,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `voice_transcripts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `speaker` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `modelUsed` TEXT NOT NULL,
                        `voiceName` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `usage_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `slotId` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `inputTokens` INTEGER NOT NULL,
                        `outputTokens` INTEGER NOT NULL,
                        `success` INTEGER NOT NULL,
                        `httpCode` INTEGER,
                        `latencyMs` INTEGER NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
