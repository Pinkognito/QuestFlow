package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 33 -> 34
 *
 * Changes:
 * 1. Add specificTime column to tasks table for recurring task time configuration
 */
val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add specificTime column to tasks table
        db.execSQL("ALTER TABLE tasks ADD COLUMN specificTime TEXT DEFAULT NULL")
    }
}
