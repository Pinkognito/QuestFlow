package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add autoCompleteParent column to tasks table for subtask functionality
        database.execSQL("""
            ALTER TABLE tasks
            ADD COLUMN autoCompleteParent INTEGER NOT NULL DEFAULT 0
        """)
    }
}
