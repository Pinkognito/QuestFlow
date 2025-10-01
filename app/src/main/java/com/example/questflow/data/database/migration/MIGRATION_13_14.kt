package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add iconMediaId column to skill_nodes table
        database.execSQL("ALTER TABLE skill_nodes ADD COLUMN iconMediaId TEXT DEFAULT NULL")
    }
}
