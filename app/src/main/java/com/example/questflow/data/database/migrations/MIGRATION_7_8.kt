package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add status field to calendar_event_links to track expired events
        database.execSQL("ALTER TABLE calendar_event_links ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
        // Status can be: PENDING, CLAIMED, EXPIRED

        // Add triggerMode field to tasks to track how recurring tasks should trigger
        database.execSQL("ALTER TABLE tasks ADD COLUMN triggerMode TEXT DEFAULT NULL")
        // TriggerMode can be: FIXED_INTERVAL, AFTER_COMPLETION, AFTER_EXPIRY

        // Add isRecurring flag for calendar event links to handle recurring events
        database.execSQL("ALTER TABLE calendar_event_links ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE calendar_event_links ADD COLUMN recurringTaskId INTEGER DEFAULT NULL")

        // Add deleteOnExpiry field to automatically delete expired events
        database.execSQL("ALTER TABLE calendar_event_links ADD COLUMN deleteOnExpiry INTEGER NOT NULL DEFAULT 0")
    }
}