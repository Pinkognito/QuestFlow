package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 34 -> 35
 *
 * Changes:
 * 1. Drop memes table (replaced by collections system)
 * 2. Drop meme_unlocks table (replaced by collections system)
 * 3. Remove deprecated columns from tasks table:
 *    - difficulty (replaced by xpPercentage)
 *    - xpOverride (replaced by xpPercentage)
 */
val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Drop obsolete meme tables
        db.execSQL("DROP TABLE IF EXISTS memes")
        db.execSQL("DROP TABLE IF EXISTS meme_unlocks")

        // 2. Recreate tasks table without deprecated columns
        // SQLite doesn't support DROP COLUMN, so we need to recreate the table

        // Create new table with updated schema
        db.execSQL("""
            CREATE TABLE tasks_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                isCompleted INTEGER NOT NULL DEFAULT 0,
                priority TEXT NOT NULL DEFAULT 'MEDIUM',
                dueDate TEXT,
                xpReward INTEGER NOT NULL DEFAULT 10,
                xpPercentage INTEGER NOT NULL DEFAULT 40,
                categoryId INTEGER,
                calendarEventId INTEGER,
                createdAt TEXT NOT NULL,
                completedAt TEXT,
                estimatedMinutes INTEGER,
                tags TEXT,
                isRecurring INTEGER NOT NULL DEFAULT 0,
                recurringType TEXT,
                recurringInterval INTEGER,
                recurringDays TEXT,
                specificTime TEXT,
                lastCompletedAt TEXT,
                nextDueDate TEXT,
                triggerMode TEXT,
                isEditable INTEGER NOT NULL DEFAULT 1,
                parentTaskId INTEGER,
                autoCompleteParent INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                FOREIGN KEY (parentTaskId) REFERENCES tasks(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Copy data from old table (excluding difficulty and xpOverride)
        db.execSQL("""
            INSERT INTO tasks_new (
                id, title, description, isCompleted, priority, dueDate,
                xpReward, xpPercentage, categoryId, calendarEventId, createdAt, completedAt,
                estimatedMinutes, tags, isRecurring, recurringType, recurringInterval,
                recurringDays, specificTime, lastCompletedAt, nextDueDate, triggerMode,
                isEditable, parentTaskId, autoCompleteParent
            )
            SELECT
                id, title, description, isCompleted, priority, dueDate,
                xpReward, xpPercentage, categoryId, calendarEventId, createdAt, completedAt,
                estimatedMinutes, tags, isRecurring, recurringType, recurringInterval,
                recurringDays, specificTime, lastCompletedAt, nextDueDate, triggerMode,
                isEditable, parentTaskId, autoCompleteParent
            FROM tasks
        """.trimIndent())

        // Drop old table
        db.execSQL("DROP TABLE tasks")

        // Rename new table to original name
        db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

        // Create indices AFTER renaming (so they have the correct table name)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_new_categoryId ON tasks(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_new_calendarEventId ON tasks(calendarEventId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_new_parentTaskId ON tasks(parentTaskId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_new_isCompleted ON tasks(isCompleted)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_new_dueDate ON tasks(dueDate)")
    }
}
