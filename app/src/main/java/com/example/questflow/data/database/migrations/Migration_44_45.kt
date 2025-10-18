package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 44 -> 45: Add expiredAt timestamp to calendar_event_links
 * 
 * Purpose: Track when tasks expire for efficient recurring task detection
 * - Allows filtering by time range instead of checking all EXPIRED tasks
 * - Enables gap detection (tasks that expired during sync downtime)
 */
val MIGRATION_44_45 = object : Migration(44, 45) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add expiredAt column (nullable, default NULL for existing rows)
        database.execSQL(
            """
            ALTER TABLE calendar_event_links 
            ADD COLUMN expiredAt TEXT DEFAULT NULL
            """
        )
        
        // For existing EXPIRED tasks, set expiredAt to endsAt (best guess)
        database.execSQL(
            """
            UPDATE calendar_event_links 
            SET expiredAt = endsAt 
            WHERE status = 'EXPIRED' AND expiredAt IS NULL
            """
        )
    }
}
