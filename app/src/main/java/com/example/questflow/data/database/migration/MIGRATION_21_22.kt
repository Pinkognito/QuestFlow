package com.example.questflow.data.database.migration

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_21_22", "Starting migration from version 21 to 22")

        try {
            // Create widget_configs table for customizable statistics dashboard
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS widget_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    widgetType TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    dataSource TEXT NOT NULL,
                    timeRange TEXT NOT NULL,
                    customStartDate TEXT,
                    customEndDate TEXT,
                    categoryFilter INTEGER,
                    difficultyFilter TEXT,
                    priorityFilter TEXT,
                    chartStyle TEXT,
                    widgetSize TEXT NOT NULL DEFAULT 'MEDIUM',
                    isVisible INTEGER NOT NULL DEFAULT 1
                )
            """)
            Log.d("MIGRATION_21_22", "Created widget_configs table")

            // Insert default widgets for new statistics dashboard
            val defaultWidgets = listOf(
                // Summary Cards
                "('SUMMARY_CARD', 0, 'Abgeschlossene Tasks', 'TASKS_COMPLETED', 'LAST_7_DAYS', NULL, NULL, NULL, NULL, NULL, NULL, 'SMALL', 1)",
                "('SUMMARY_CARD', 1, 'Gesamtes XP', 'XP_TOTAL', 'LAST_7_DAYS', NULL, NULL, NULL, NULL, NULL, NULL, 'SMALL', 1)",
                "('SUMMARY_CARD', 2, 'Aktueller Streak', 'STREAK_CURRENT', 'ALL_TIME', NULL, NULL, NULL, NULL, NULL, NULL, 'SMALL', 1)",
                "('SUMMARY_CARD', 3, 'Erfolgsrate', 'COMPLETION_RATE', 'LAST_7_DAYS', NULL, NULL, NULL, NULL, NULL, NULL, 'SMALL', 1)",
                // Charts
                "('LINE_CHART', 4, 'XP-Verlauf', 'XP_TREND', 'LAST_7_DAYS', NULL, NULL, NULL, NULL, NULL, NULL, 'LARGE', 1)",
                "('BAR_CHART', 5, 'Tasks pro Tag', 'TASK_COMPLETION', 'LAST_7_DAYS', NULL, NULL, NULL, NULL, NULL, NULL, 'LARGE', 1)",
                "('PIE_CHART', 6, 'Kategorie-Verteilung', 'CATEGORY_DISTRIBUTION', 'LAST_7_DAYS', NULL, NULL, NULL, NULL, NULL, NULL, 'MEDIUM', 1)",
                "('PIE_CHART', 7, 'Schwierigkeitsgrad', 'DIFFICULTY_DISTRIBUTION', 'LAST_7_DAYS', NULL, NULL, NULL, NULL, NULL, NULL, 'MEDIUM', 1)"
            )

            defaultWidgets.forEach { values ->
                db.execSQL("INSERT INTO widget_configs (widgetType, position, title, dataSource, timeRange, customStartDate, customEndDate, categoryFilter, difficultyFilter, priorityFilter, chartStyle, widgetSize, isVisible) VALUES $values")
            }
            Log.d("MIGRATION_21_22", "Inserted default widgets")

            Log.d("MIGRATION_21_22", "Migration from version 21 to 22 completed successfully")
        } catch (e: Exception) {
            Log.e("MIGRATION_21_22", "Error during migration from 21 to 22", e)
            throw e
        }
    }
}
