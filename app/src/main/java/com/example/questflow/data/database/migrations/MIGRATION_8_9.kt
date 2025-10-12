package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add deleteOnExpiry column if it doesn't exist
        // Some databases may not have this column from MIGRATION_7_8
        try {
            db.execSQL("ALTER TABLE calendar_event_links ADD COLUMN deleteOnExpiry INTEGER NOT NULL DEFAULT 0")
        } catch (e: Exception) {
            // Column might already exist, ignore the error
        }
    }
}