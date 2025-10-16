package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 38 -> 39
 *
 * Changes:
 * Add task_display_settings table for customizable task list display options
 */
val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create task_display_settings table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS task_display_settings (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                showTaskTitle INTEGER NOT NULL DEFAULT 1,
                showTaskDescription INTEGER NOT NULL DEFAULT 1,
                showParentTaskPath INTEGER NOT NULL DEFAULT 1,
                showDueDate INTEGER NOT NULL DEFAULT 1,
                showCreatedDate INTEGER NOT NULL DEFAULT 0,
                showCompletedDate INTEGER NOT NULL DEFAULT 0,
                showCategory INTEGER NOT NULL DEFAULT 1,
                showPriority INTEGER NOT NULL DEFAULT 1,
                showDifficulty INTEGER NOT NULL DEFAULT 1,
                showXpReward INTEGER NOT NULL DEFAULT 0,
                showExpiredBadge INTEGER NOT NULL DEFAULT 1,
                showCompletedBadge INTEGER NOT NULL DEFAULT 1,
                showSubtaskCount INTEGER NOT NULL DEFAULT 1,
                showRecurringIcon INTEGER NOT NULL DEFAULT 1,
                showLinkedContacts INTEGER NOT NULL DEFAULT 1,
                showContactAvatars INTEGER NOT NULL DEFAULT 1,
                maxContactsVisible INTEGER NOT NULL DEFAULT 3,
                showMatchBadges INTEGER NOT NULL DEFAULT 1,
                maxMatchBadgesVisible INTEGER NOT NULL DEFAULT 3,
                compactMode INTEGER NOT NULL DEFAULT 0,
                showDescriptionPreview INTEGER NOT NULL DEFAULT 1,
                descriptionMaxLines INTEGER NOT NULL DEFAULT 2
            )
        """)

        // Insert default settings
        db.execSQL("""
            INSERT INTO task_display_settings (id) VALUES (1)
        """)
    }
}
