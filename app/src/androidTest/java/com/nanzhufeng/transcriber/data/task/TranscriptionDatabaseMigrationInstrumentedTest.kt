package com.nanzhufeng.transcriber.data.task

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranscriptionDatabaseMigrationInstrumentedTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TranscriptionDatabase::class.java,
    )

    @Test
    fun migrationOneToFourPreservesTaskAndAddsExecutionDefaults() {
        val databaseName = "task-migration-1-2.db"
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                """
                INSERT INTO transcription_tasks (
                    id, sourceUri, sourceDisplayName, sourceAccessMode, stagedInputPath,
                    sourceFingerprint, modelId, modelVersion, language, outputFormat,
                    outputUri, state, progressMillis, totalDurationMillis, errorCode,
                    userMessage, technicalDetail, attemptCount, queuePosition,
                    createdAtMillis, updatedAtMillis
                ) VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, NULL, ?, ?, ?, NULL, ?, NULL, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "migration-task",
                    "content://migration/jfk.wav",
                    "jfk.wav",
                    SourceAccessMode.PERSISTED_DOCUMENT.name,
                    "base",
                    "hf-5359861c",
                    "en",
                    TranscriptionOutputFormat.TXT.name,
                    "TRANSCRIBING",
                    4_000L,
                    11_000L,
                    "正在转写",
                    1,
                    99L,
                    1_000L,
                    2_000L,
                ),
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).use { database ->
            database.query(
                "SELECT sourceDisplayName, state, progressMillis, threadCount, " +
                    "exportDirectoryUri, outputConflictPolicy, postProcessEnabled, " +
                    "postProcessBaseUrl, postProcessModel " +
                    "FROM transcription_tasks WHERE id = 'migration-task'",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("jfk.wav", cursor.getString(0))
                assertEquals("TRANSCRIBING", cursor.getString(1))
                assertEquals(4_000L, cursor.getLong(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals(null, cursor.getString(4))
                assertEquals(OutputConflictPolicy.RENAME.name, cursor.getString(5))
                assertEquals(0, cursor.getInt(6))
                assertEquals("https://api.openai.com/v1", cursor.getString(7))
                assertEquals("gpt-4o-mini", cursor.getString(8))
            }
        }
    }

    @Test
    fun migrationTwoToFourPreservesTaskAndAddsOutputContract() {
        val databaseName = "task-migration-2-3.db"
        helper.createDatabase(databaseName, 2).apply {
            execSQL(
                """
                INSERT INTO transcription_tasks (
                    id, sourceUri, sourceDisplayName, sourceAccessMode, stagedInputPath,
                    sourceFingerprint, modelId, modelVersion, language, threadCount,
                    outputFormat, outputUri, state, progressMillis, totalDurationMillis,
                    errorCode, userMessage, technicalDetail, attemptCount, queuePosition,
                    createdAtMillis, updatedAtMillis
                ) VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, NULL, ?, ?, NULL, ?, 0, NULL, NULL, NULL, NULL, 0, 1, 1, 1)
                """.trimIndent(),
                arrayOf(
                    "v2-task",
                    "content://migration/source.wav",
                    "source.wav",
                    SourceAccessMode.PERSISTED_DOCUMENT.name,
                    "small-q5_1",
                    "hf-5359861c",
                    4,
                    TranscriptionOutputFormat.DOCX.name,
                    "QUEUED",
                ),
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 4, true, MIGRATION_2_3, MIGRATION_3_4).use { database ->
            database.query(
                "SELECT modelId, threadCount, outputFormat, exportDirectoryUri, outputConflictPolicy " +
                    "FROM transcription_tasks WHERE id = 'v2-task'",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("small-q5_1", cursor.getString(0))
                assertEquals(4, cursor.getInt(1))
                assertEquals(TranscriptionOutputFormat.DOCX.name, cursor.getString(2))
                assertEquals(null, cursor.getString(3))
                assertEquals(OutputConflictPolicy.RENAME.name, cursor.getString(4))
            }
        }
    }

    @Test
    fun migrationThreeToFourAddsDisabledPostProcessingWithoutChangingOutputSettings() {
        val databaseName = "task-migration-3-4.db"
        helper.createDatabase(databaseName, 3).apply {
            execSQL(
                """
                INSERT INTO transcription_tasks (
                    id, sourceUri, sourceDisplayName, sourceAccessMode, stagedInputPath,
                    sourceFingerprint, modelId, modelVersion, language, threadCount,
                    outputFormat, outputUri, exportDirectoryUri, outputConflictPolicy,
                    state, progressMillis, totalDurationMillis, errorCode, userMessage,
                    technicalDetail, attemptCount, queuePosition, createdAtMillis, updatedAtMillis
                ) VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, NULL, 0, ?, NULL, ?, ?, ?, 0, NULL, NULL, NULL, NULL, 0, 1, 1, 1)
                """.trimIndent(),
                arrayOf(
                    "v3-task",
                    "content://migration/v3.wav",
                    "v3.wav",
                    SourceAccessMode.PERSISTED_DOCUMENT.name,
                    "base",
                    "hf-5359861c",
                    TranscriptionOutputFormat.MARKDOWN.name,
                    "content://output/tree",
                    OutputConflictPolicy.SKIP.name,
                    "QUEUED",
                ),
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 4, true, MIGRATION_3_4).use { database ->
            database.query(
                "SELECT exportDirectoryUri, outputConflictPolicy, postProcessEnabled, " +
                    "postProcessBaseUrl, postProcessModel FROM transcription_tasks WHERE id = 'v3-task'",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("content://output/tree", cursor.getString(0))
                assertEquals(OutputConflictPolicy.SKIP.name, cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals("https://api.openai.com/v1", cursor.getString(3))
                assertEquals("gpt-4o-mini", cursor.getString(4))
            }
        }
    }
}
