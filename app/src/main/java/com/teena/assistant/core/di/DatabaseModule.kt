package com.teena.assistant.core.di

import android.content.Context
import androidx.room.Room
import com.teena.assistant.core.data.local.TeenaDatabase
import com.teena.assistant.core.data.local.dao.FeatureRecordDao
import com.teena.assistant.core.data.local.dao.SystemEventDao
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
        ).build()

    @Provides
    fun provideFeatureRecordDao(database: TeenaDatabase): FeatureRecordDao =
        database.featureRecordDao()

    @Provides
    fun provideSystemEventDao(database: TeenaDatabase): SystemEventDao =
        database.systemEventDao()
}
