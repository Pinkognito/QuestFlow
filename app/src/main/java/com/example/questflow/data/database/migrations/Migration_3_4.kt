package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add xpPercentage column to calendar_event_links table
        database.execSQL(
            "ALTER TABLE calendar_event_links ADD COLUMN xpPercentage INTEGER NOT NULL DEFAULT 60"
        )
    }
}