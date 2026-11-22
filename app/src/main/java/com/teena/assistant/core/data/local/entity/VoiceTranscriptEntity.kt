package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_transcripts")
data class VoiceTranscriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val speaker: String,
    val text: String,
    val modelUsed: String,
    val voiceName: String,
    val timestampEpochMs: Long
)
