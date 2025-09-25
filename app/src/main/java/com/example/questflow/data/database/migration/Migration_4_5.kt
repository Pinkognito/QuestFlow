package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to memes table
        database.execSQL("ALTER TABLE memes ADD COLUMN name TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE memes ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE memes ADD COLUMN imageResourceId INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE memes ADD COLUMN rarity TEXT NOT NULL DEFAULT 'COMMON'")
        database.execSQL("ALTER TABLE memes ADD COLUMN requiredLevel INTEGER NOT NULL DEFAULT 1")
    }
}