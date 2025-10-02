package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add tags column to media_library table
        database.execSQL("ALTER TABLE media_library ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    }
}
