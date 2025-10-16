package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 40 -> 41: Add task_filter_presets table
 * Adds comprehensive filter preset system for saving filter/sort/group configurations
 */
val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create task_filter_presets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS task_filter_presets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                isDefault INTEGER NOT NULL DEFAULT 0,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                filterJson TEXT NOT NULL
            )
        """)

        // Create index for faster default preset lookup
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_task_filter_presets_isDefault
            ON task_filter_presets(isDefault)
        """)
    }
}
