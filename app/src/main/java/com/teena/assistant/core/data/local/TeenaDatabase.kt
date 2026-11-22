package com.teena.assistant.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teena.assistant.core.data.local.dao.FeatureRecordDao
import com.teena.assistant.core.data.local.dao.SystemEventDao
import com.teena.assistant.core.data.local.entity.FeatureRecordEntity
import com.teena.assistant.core.data.local.entity.SystemEventEntity

@Database(
    entities = [
        FeatureRecordEntity::class,
        SystemEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TeenaDatabase : RoomDatabase() {
    abstract fun featureRecordDao(): FeatureRecordDao
    abstract fun systemEventDao(): SystemEventDao
}
