package com.teena.assistant.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teena.assistant.core.data.local.entity.VoiceTranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceTranscriptDao {

    @Query("SELECT * FROM voice_transcripts ORDER BY timestampEpochMs DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<VoiceTranscriptEntity>>

    @Insert
    suspend fun insert(entry: VoiceTranscriptEntity): Long

    @Query("DELETE FROM voice_transcripts")
    suspend fun clearAll()
}
