package com.example.questflow.data.database.migration

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_22_23", "Starting migration from version 22 to 23")

        try {
            // Drop old widget_configs table
            db.execSQL("DROP TABLE IF EXISTS widget_configs")
            Log.d("MIGRATION_22_23", "Dropped widget_configs table")

            // Create new dynamic_charts table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS dynamic_charts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    chartType TEXT NOT NULL,
                    dataSource TEXT NOT NULL,
                    xAxisField TEXT NOT NULL,
                    yAxisField TEXT,
                    yAxisAggregation TEXT NOT NULL,
                    groupingType TEXT,
                    dateInterval TEXT,
                    filters TEXT,
                    sortBy TEXT,
                    sortDirection TEXT NOT NULL DEFAULT 'ASC',
                    showLegend INTEGER NOT NULL DEFAULT 1,
                    showValues INTEGER NOT NULL DEFAULT 1,
                    colorScheme TEXT,
                    position INTEGER NOT NULL DEFAULT 0,
                    height INTEGER NOT NULL DEFAULT 300,
                    isVisible INTEGER NOT NULL DEFAULT 1
                )
            """)
            Log.d("MIGRATION_22_23", "Created dynamic_charts table")

            // Insert default charts
            db.execSQL("""
                INSERT INTO dynamic_charts
                (title, chartType, dataSource, xAxisField, yAxisField, yAxisAggregation,
                 groupingType, dateInterval, sortDirection, position, height)
                VALUES
                ('Tasks nach Kategorie', 'BAR_CHART', 'TASKS', 'categoryName', 'title', 'COUNT',
                 'BY_CATEGORY', NULL, 'DESC', 0, 300)
            """)

            db.execSQL("""
                INSERT INTO dynamic_charts
                (title, chartType, dataSource, xAxisField, yAxisField, yAxisAggregation,
                 groupingType, dateInterval, sortDirection, position, height)
                VALUES
                ('XP-Verlauf', 'LINE_CHART', 'XP_TRANSACTIONS', 'timestamp', 'amount', 'SUM',
                 'BY_DATE', 'DAY', 'ASC', 1, 350)
            """)

            db.execSQL("""
                INSERT INTO dynamic_charts
                (title, chartType, dataSource, xAxisField, yAxisField, yAxisAggregation,
                 groupingType, dateInterval, sortDirection, position, height)
                VALUES
                ('Prioritäten-Verteilung', 'PIE_CHART', 'TASKS', 'priority', 'title', 'COUNT',
                 'BY_CATEGORY', NULL, 'DESC', 2, 300)
            """)

            Log.d("MIGRATION_22_23", "Inserted default charts")

            Log.d("MIGRATION_22_23", "Migration from version 22 to 23 completed successfully")
        } catch (e: Exception) {
            Log.e("MIGRATION_22_23", "Error during migration from 22 to 23", e)
            throw e
        }
    }
}
