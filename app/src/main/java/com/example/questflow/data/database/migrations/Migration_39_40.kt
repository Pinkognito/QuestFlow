package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 39 to 40
 * Adds elementLayoutConfig field to task_display_settings for 2-column layout configuration
 */
val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add elementLayoutConfig column (JSON string for layout configuration)
        db.execSQL("""
            ALTER TABLE task_display_settings
            ADD COLUMN elementLayoutConfig TEXT NOT NULL DEFAULT ''
        """)
    }
}
