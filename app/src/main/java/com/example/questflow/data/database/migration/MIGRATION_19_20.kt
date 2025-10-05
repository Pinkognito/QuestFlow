package com.example.questflow.data.database.migration

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_19_20", "Starting migration from version 19 to 20")

        try {
            // Add new statistics metadata columns to tasks table
            db.execSQL("ALTER TABLE tasks ADD COLUMN estimatedMinutes INTEGER")
            Log.d("MIGRATION_19_20", "Added estimatedMinutes column to tasks")

            db.execSQL("ALTER TABLE tasks ADD COLUMN tags TEXT")
            Log.d("MIGRATION_19_20", "Added tags column to tasks")

            // Create statistics_config table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS statistics_config (
                    id INTEGER PRIMARY KEY NOT NULL,
                    visibleCharts TEXT NOT NULL DEFAULT 'xp_trend,task_completion,category_distribution,difficulty_distribution',
                    defaultTimeRange TEXT NOT NULL DEFAULT 'WEEK',
                    chartOrder TEXT NOT NULL DEFAULT 'xp_trend,task_completion,category_distribution,difficulty_distribution,productivity_heatmap,priority_distribution,xp_sources,streak_calendar',
                    aggregationLevel TEXT NOT NULL DEFAULT 'DAILY',
                    selectedCategoryFilter INTEGER,
                    selectedDifficultyFilter TEXT,
                    selectedPriorityFilter TEXT,
                    showCompletedOnly INTEGER NOT NULL DEFAULT 0
                )
            """)
            Log.d("MIGRATION_19_20", "Created statistics_config table")

            // Insert default config
            db.execSQL("""
                INSERT INTO statistics_config (
                    id,
                    visibleCharts,
                    defaultTimeRange,
                    chartOrder,
                    aggregationLevel,
                    showCompletedOnly
                ) VALUES (
                    1,
                    'xp_trend,task_completion,category_distribution,difficulty_distribution',
                    'WEEK',
                    'xp_trend,task_completion,category_distribution,difficulty_distribution,productivity_heatmap,priority_distribution,xp_sources,streak_calendar',
                    'DAILY',
                    0
                )
            """)
            Log.d("MIGRATION_19_20", "Inserted default statistics config")

            Log.d("MIGRATION_19_20", "Migration from version 19 to 20 completed successfully")
        } catch (e: Exception) {
            Log.e("MIGRATION_19_20", "Error during migration from 19 to 20", e)
            throw e
        }
    }
}
