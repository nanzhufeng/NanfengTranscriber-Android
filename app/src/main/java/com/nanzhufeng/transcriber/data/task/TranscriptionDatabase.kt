package com.nanzhufeng.transcriber.data.task

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TranscriptionTaskEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class TranscriptionDatabase : RoomDatabase() {
    abstract fun taskDao(): TranscriptionTaskDao
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
