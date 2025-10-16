package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 41 -> 42: Add calendar event custom fields to tasks
 * Enables dynamic Google Calendar event title/description with placeholders
 */
val MIGRATION_41_42 = object : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add calendar event custom title and description fields
        // These support placeholders like {task.title}, {kontakt.name}, {task.description}
        db.execSQL("ALTER TABLE tasks ADD COLUMN calendarEventCustomTitle TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN calendarEventCustomDescription TEXT DEFAULT NULL")
    }
}
