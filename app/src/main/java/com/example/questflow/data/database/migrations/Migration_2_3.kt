package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add xpPercentage column to tasks table
        database.execSQL("ALTER TABLE tasks ADD COLUMN xpPercentage INTEGER NOT NULL DEFAULT 40")

        // Update existing tasks to set xpPercentage based on their difficulty
        database.execSQL("""
            UPDATE tasks
            SET xpPercentage = CASE
                WHEN difficulty = 'EASY' THEN 40
                WHEN difficulty = 'MEDIUM' THEN 60
                WHEN difficulty = 'HARD' THEN 80
                ELSE 60
            END
        """)
    }
}