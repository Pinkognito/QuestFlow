package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 37 -> 38
 *
 * Changes:
 * Add UI state fields for collapsible sections in task search filter settings
 */
val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add collapsed/expanded state columns for each section
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN taskFieldsSectionExpanded INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN contactsSectionExpanded INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN parentTaskSectionExpanded INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN xpPercentageSectionExpanded INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN timeFilterSectionExpanded INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_search_filter_settings ADD COLUMN dateRangeSectionExpanded INTEGER NOT NULL DEFAULT 0")
    }
}
