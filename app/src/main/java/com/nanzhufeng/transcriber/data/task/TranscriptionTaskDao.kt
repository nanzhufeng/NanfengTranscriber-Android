package com.nanzhufeng.transcriber.data.task

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionTaskDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TranscriptionTaskEntity)

    @Update
    suspend fun update(task: TranscriptionTaskEntity): Int

    @Query("SELECT * FROM transcription_tasks WHERE id = :id")
    suspend fun findById(id: String): TranscriptionTaskEntity?

    @Query("SELECT * FROM transcription_tasks ORDER BY queuePosition ASC, createdAtMillis ASC")
    fun observeAll(): Flow<List<TranscriptionTaskEntity>>

    @Query("SELECT * FROM transcription_tasks WHERE state IN (:states)")
    suspend fun findByStates(states: List<String>): List<TranscriptionTaskEntity>

    @Query("SELECT * FROM transcription_tasks WHERE state = :state ORDER BY queuePosition ASC, createdAtMillis ASC LIMIT 1")
    suspend fun findFirstByState(state: String): TranscriptionTaskEntity?

    @Query("DELETE FROM transcription_tasks WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
