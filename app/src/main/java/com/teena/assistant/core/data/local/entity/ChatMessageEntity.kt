package com.teena.assistant.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val role: String,
    val content: String,
    val languageHint: String,
    val providerUsed: String?,
    val modelUsed: String?,
    val inputTokens: Int,
    val outputTokens: Int,
    val timestampEpochMs: Long
)
