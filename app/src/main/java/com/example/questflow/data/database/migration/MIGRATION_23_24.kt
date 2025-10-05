package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to dynamic_charts table
        database.execSQL("""
            ALTER TABLE dynamic_charts
            ADD COLUMN timeRangeType TEXT DEFAULT NULL
        """)

        database.execSQL("""
            ALTER TABLE dynamic_charts
            ADD COLUMN showAxisLabels INTEGER NOT NULL DEFAULT 1
        """)

        database.execSQL("""
            ALTER TABLE dynamic_charts
            ADD COLUMN categoryId INTEGER DEFAULT NULL
        """)

        database.execSQL("""
            ALTER TABLE dynamic_charts
            ADD COLUMN isTemplate INTEGER NOT NULL DEFAULT 0
        """)
    }
}
