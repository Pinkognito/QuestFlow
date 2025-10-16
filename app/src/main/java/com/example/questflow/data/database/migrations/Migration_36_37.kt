package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 36 -> 37
 *
 * Changes:
 * 1. Add difficulty/XP percentage filter fields to task_search_filter_settings
 * 2. Add time-based filter fields (start time, end time, date filters)
 * 3. Add date range filter fields (past, future, overdue tasks)
 */
val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns for XP percentage (difficulty) filters
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN xpPercentageEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN xpPercentage20 INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN xpPercentage40 INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN xpPercentage60 INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN xpPercentage80 INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN xpPercentage100 INTEGER NOT NULL DEFAULT 1")

        // Add time-based filter columns
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN timeFilterEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN filterByStartTime INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN filterByEndTime INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN filterByDueDate INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN filterByCreatedDate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN filterByCompletedDate INTEGER NOT NULL DEFAULT 0")

        // Add date range filter columns
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN dateRangeEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN includePastTasks INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN includeFutureTasks INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN includeOverdueTasks INTEGER NOT NULL DEFAULT 1")
    }
}
