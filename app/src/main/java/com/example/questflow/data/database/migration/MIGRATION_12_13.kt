package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create media_library table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS media_library (
                id TEXT NOT NULL PRIMARY KEY,
                fileName TEXT NOT NULL,
                filePath TEXT NOT NULL,
                mediaType TEXT NOT NULL,
                uploadedAt INTEGER NOT NULL,
                fileSize INTEGER NOT NULL,
                mimeType TEXT NOT NULL,
                thumbnailPath TEXT
            )
        """)
    }
}
