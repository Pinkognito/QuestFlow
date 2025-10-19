package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 46→47: Add oldValue and newValue columns to task_history table
 *
 * This enables tracking of property changes with before/after values.
 * Examples:
 * - PRIORITY_CHANGED: oldValue="LOW", newValue="HIGH"
 * - DIFFICULTY_CHANGED: oldValue="20", newValue="100"
 * - CATEGORY_CHANGED: oldValue="1", newValue="5"
 * - TITLE_CHANGED: oldValue="Old Title", newValue="New Title"
 */
val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add oldValue column (nullable)
        database.execSQL(
            """
            ALTER TABLE task_history
            ADD COLUMN oldValue TEXT DEFAULT NULL
            """
        )

        // Add newValue column (nullable)
        database.execSQL(
            """
            ALTER TABLE task_history
            ADD COLUMN newValue TEXT DEFAULT NULL
            """
        )
    }
}
