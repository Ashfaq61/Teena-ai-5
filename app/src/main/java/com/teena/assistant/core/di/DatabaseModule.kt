package com.teena.assistant.core.di

import android.content.Context
import androidx.room.Room
import com.teena.assistant.core.data.local.TeenaDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TeenaDatabase =
        Room.databaseBuilder(
            context,
            TeenaDatabase::class.java,
            "teena_assistant.db"
        )
            .addMigrations(TeenaDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideFeatureRecordDao(database: TeenaDatabase): FeatureRecordDao =
        database.featureRecordDao()

    @Provides
    fun provideSystemEventDao(database: TeenaDatabase): SystemEventDao =
        database.systemEventDao()

    @Provides
    fun provideApiKeySlotDao(database: TeenaDatabase): ApiKeySlotDao =
        database.apiKeySlotDao()

    @Provides
    fun provideDiscoveredModelDao(database: TeenaDatabase): DiscoveredModelDao =
        database.discoveredModelDao()

    @Provides
    fun provideModelOverrideDao(database: TeenaDatabase): ModelOverrideDao =
        database.modelOverrideDao()

    @Provides
    fun provideModelSwitchLogDao(database: TeenaDatabase): ModelSwitchLogDao =
        database.modelSwitchLogDao()

    @Provides
    fun provideChatDao(database: TeenaDatabase): ChatDao =
        database.chatDao()

    @Provides
    fun provideMemoryDao(database: TeenaDatabase): MemoryDao =
        database.memoryDao()

    @Provides
    fun provideVoiceTranscriptDao(database: TeenaDatabase): VoiceTranscriptDao =
        database.voiceTranscriptDao()

    @Provides
    fun provideUsageRecordDao(database: TeenaDatabase): UsageRecordDao =
        database.usageRecordDao()
}
