package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 43 -> 44: Add working_hours_settings table
 * Allows users to configure custom working hours for task scheduling
 */
val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create working_hours_settings table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS working_hours_settings (
                id INTEGER PRIMARY KEY NOT NULL,
                startHour INTEGER NOT NULL DEFAULT 8,
                startMinute INTEGER NOT NULL DEFAULT 0,
                endHour INTEGER NOT NULL DEFAULT 22,
                endMinute INTEGER NOT NULL DEFAULT 0,
                enabled INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

        // Insert default settings (8:00 - 22:00)
        db.execSQL("""
            INSERT OR REPLACE INTO working_hours_settings (id, startHour, startMinute, endHour, endMinute, enabled)
            VALUES (1, 8, 0, 22, 0, 1)
        """.trimIndent())
    }
}
