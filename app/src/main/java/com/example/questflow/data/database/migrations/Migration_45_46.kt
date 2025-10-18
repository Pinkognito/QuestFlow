package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create task_history table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                eventType TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                newDueDate TEXT,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
            )
            """
        )

        // Create indices for efficient queries
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_task_history_taskId_timestamp
            ON task_history(taskId, timestamp)
            """
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_task_history_eventType
            ON task_history(eventType)
            """
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_task_history_timestamp
            ON task_history(timestamp)
            """
        )
    }
}
