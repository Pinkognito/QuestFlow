package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add fields for recurring tasks to tasks table
        database.execSQL("ALTER TABLE tasks ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE tasks ADD COLUMN recurringType TEXT DEFAULT NULL") // DAILY, WEEKLY, MONTHLY, CUSTOM
        database.execSQL("ALTER TABLE tasks ADD COLUMN recurringInterval INTEGER DEFAULT NULL") // Minutes for CUSTOM
        database.execSQL("ALTER TABLE tasks ADD COLUMN recurringDays TEXT DEFAULT NULL") // JSON array of weekdays for WEEKLY
        database.execSQL("ALTER TABLE tasks ADD COLUMN lastCompletedAt TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE tasks ADD COLUMN nextDueDate TEXT DEFAULT NULL")

        // Add fields for task editing
        database.execSQL("ALTER TABLE tasks ADD COLUMN isEditable INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE tasks ADD COLUMN parentTaskId INTEGER DEFAULT NULL")

        // Add field to track if calendar event should be deleted on claim
        database.execSQL("ALTER TABLE calendar_event_links ADD COLUMN deleteOnClaim INTEGER NOT NULL DEFAULT 0")
        // Add taskId to link calendar events with tasks
        database.execSQL("ALTER TABLE calendar_event_links ADD COLUMN taskId INTEGER DEFAULT NULL")
    }
}