package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 35 -> 36
 *
 * Changes:
 * 1. Add task_search_filter_settings table for configurable task search filters
 */
val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create task search filter settings table with default values
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS task_search_filter_settings (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                taskTitle INTEGER NOT NULL DEFAULT 1,
                taskDescription INTEGER NOT NULL DEFAULT 1,
                taskTags INTEGER NOT NULL DEFAULT 1,
                categoryName INTEGER NOT NULL DEFAULT 1,
                contactEnabled INTEGER NOT NULL DEFAULT 1,
                contactDisplayName INTEGER NOT NULL DEFAULT 1,
                contactGivenName INTEGER NOT NULL DEFAULT 1,
                contactFamilyName INTEGER NOT NULL DEFAULT 1,
                contactPrimaryPhone INTEGER NOT NULL DEFAULT 1,
                contactPrimaryEmail INTEGER NOT NULL DEFAULT 1,
                contactOrganization INTEGER NOT NULL DEFAULT 0,
                contactJobTitle INTEGER NOT NULL DEFAULT 0,
                contactNote INTEGER NOT NULL DEFAULT 0,
                parentTaskEnabled INTEGER NOT NULL DEFAULT 1,
                parentTaskTitle INTEGER NOT NULL DEFAULT 1,
                parentTaskDescription INTEGER NOT NULL DEFAULT 0,
                locationEnabled INTEGER NOT NULL DEFAULT 0,
                locationName INTEGER NOT NULL DEFAULT 0,
                locationAddress INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // Insert default settings row
        db.execSQL("""
            INSERT INTO task_search_filter_settings (id) VALUES (1)
        """.trimIndent())
    }
}
