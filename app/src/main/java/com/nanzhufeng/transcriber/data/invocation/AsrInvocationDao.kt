package com.nanzhufeng.transcriber.data.invocation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AsrInvocationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: AsrInvocationRecordEntity)

    @Query("SELECT * FROM asr_invocation_records ORDER BY completedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<AsrInvocationRecordEntity>>
}
