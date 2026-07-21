package com.nanzhufeng.transcriber.data.task

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTranscriptionTaskRepositoryInstrumentedTest {
    @Test
    fun taskPersistsAndInterruptedStateRecoversExplicitly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "task-repository-persistence-test.db"
        context.deleteDatabase(databaseName)
        try {
            var database = Room.databaseBuilder(
                context,
                TranscriptionDatabase::class.java,
                databaseName,
            ).build()
            var repository = RoomTranscriptionTaskRepository(
                database = database,
                nowMillis = { 1_000L },
                newId = { "task-1" },
            )
            val task = repository.create(
                NewTranscriptionTask(
                    sourceUri = "content://probe/video.mp4",
                    sourceDisplayName = "video.mp4",
                    sourceAccessMode = SourceAccessMode.PERSISTED_DOCUMENT,
                    modelId = "small",
                    modelVersion = "v1",
                    outputFormat = TranscriptionOutputFormat.SRT,
                    queuePosition = 0L,
                ),
            )
            assertTrue(
                repository.transition(task.id, TranscriptionTaskState.PREPARING) is
                    TaskMutationResult.Updated,
            )
            assertTrue(
                repository.transition(task.id, TranscriptionTaskState.TRANSCRIBING) is
                    TaskMutationResult.Updated,
            )
            assertTrue(
                repository.updateProgress(
                    id = task.id,
                    expectedState = TranscriptionTaskState.TRANSCRIBING,
                    progressMillis = 123L,
                    totalDurationMillis = 456L,
                    userMessage = "后台进度已持久化",
                ) is TaskMutationResult.Updated,
            )
            database.close()

            database = Room.databaseBuilder(
                context,
                TranscriptionDatabase::class.java,
                databaseName,
            ).build()
            repository = RoomTranscriptionTaskRepository(database, nowMillis = { 2_000L })
            assertEquals(
                TranscriptionTaskState.TRANSCRIBING.name,
                repository.findById(task.id)?.state,
            )

            assertEquals(1, repository.recoverInterruptedTasks())
            val recovered = requireNotNull(repository.findById(task.id))
            assertEquals(TranscriptionTaskState.RECOVERY_REQUIRED.name, recovered.state)
            assertEquals("PROCESS_INTERRUPTED", recovered.errorCode)
            assertTrue(recovered.userMessage?.contains("重新开始") == true)
            assertEquals(123L, recovered.progressMillis)
            assertEquals(456L, recovered.totalDurationMillis)

            val reset = repository.resetForExecution(task.id)
            assertTrue(reset is TaskMutationResult.Updated)
            assertEquals(
                TranscriptionTaskState.QUEUED.name,
                repository.findById(task.id)?.state,
            )

            val invalid = repository.transition(task.id, TranscriptionTaskState.COMPLETED)
            assertTrue(invalid is TaskMutationResult.Rejected)
            database.close()
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}
