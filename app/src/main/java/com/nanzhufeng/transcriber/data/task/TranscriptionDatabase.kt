package com.nanzhufeng.transcriber.data.task

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nanzhufeng.transcriber.data.invocation.AsrInvocationDao
import com.nanzhufeng.transcriber.data.invocation.AsrInvocationRecordEntity

@Database(
    entities = [TranscriptionTaskEntity::class, AsrInvocationRecordEntity::class],
    version = 7,
    exportSchema = true,
)
abstract class TranscriptionDatabase : RoomDatabase() {
    abstract fun taskDao(): TranscriptionTaskDao
    abstract fun asrInvocationDao(): AsrInvocationDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE transcription_tasks " +
                "ADD COLUMN threadCount INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE transcription_tasks ADD COLUMN exportDirectoryUri TEXT",
        )
        database.execSQL(
            "ALTER TABLE transcription_tasks " +
                "ADD COLUMN outputConflictPolicy TEXT NOT NULL DEFAULT 'RENAME'",
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE transcription_tasks " +
                "ADD COLUMN postProcessEnabled INTEGER NOT NULL DEFAULT 0",
        )
        database.execSQL(
            "ALTER TABLE transcription_tasks " +
                "ADD COLUMN postProcessBaseUrl TEXT NOT NULL DEFAULT 'https://api.openai.com/v1'",
        )
        database.execSQL(
            "ALTER TABLE transcription_tasks " +
                "ADD COLUMN postProcessModel TEXT NOT NULL DEFAULT 'gpt-4o-mini'",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS asr_invocation_records (" +
                "id TEXT NOT NULL, providerId TEXT NOT NULL, modelId TEXT NOT NULL, " +
                "completedAtMillis INTEGER NOT NULL, durationMillis INTEGER NOT NULL, status TEXT NOT NULL, " +
                "inputTokens INTEGER, outputTokens INTEGER, totalTokens INTEGER, errorCode TEXT, " +
                "PRIMARY KEY(id))",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asr_invocation_records_completedAtMillis " +
                "ON asr_invocation_records(completedAtMillis)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asr_invocation_records_providerId_modelId " +
                "ON asr_invocation_records(providerId, modelId)",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE asr_invocation_records ADD COLUMN taskId TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE asr_invocation_records ADD COLUMN sourceDisplayName TEXT NOT NULL DEFAULT '迁移前调用记录'")
        database.execSQL("ALTER TABLE asr_invocation_records ADD COLUMN requestCount INTEGER NOT NULL DEFAULT 1")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asr_invocation_records_taskId " +
                "ON asr_invocation_records(taskId)",
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE asr_invocation_records ADD COLUMN billableAudioMillis INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE asr_invocation_records ADD COLUMN costPriceVersion TEXT")
        database.execSQL("ALTER TABLE asr_invocation_records ADD COLUMN costCurrencyCode TEXT")
        database.execSQL("ALTER TABLE asr_invocation_records ADD COLUMN estimatedCostMicros INTEGER")
    }
}
