package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create media_usage table to track where media files are used
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS media_usage (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mediaLibraryId TEXT NOT NULL,
                usageType TEXT NOT NULL,
                referenceId INTEGER NOT NULL,
                categoryId INTEGER,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(mediaLibraryId) REFERENCES media_library(id) ON DELETE CASCADE
            )
        """)

        // Create index for faster queries
        database.execSQL("CREATE INDEX IF NOT EXISTS index_media_usage_mediaLibraryId ON media_usage(mediaLibraryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_media_usage_reference ON media_usage(usageType, referenceId)")

        // Populate media_usage table with existing collection items
        database.execSQL("""
            INSERT INTO media_usage (mediaLibraryId, usageType, referenceId, categoryId, createdAt)
            SELECT
                mediaLibraryId,
                'COLLECTION_ITEM' as usageType,
                id as referenceId,
                categoryId,
                strftime('%s', 'now') * 1000 as createdAt
            FROM collection_items
            WHERE mediaLibraryId IS NOT NULL AND mediaLibraryId != ''
        """)
    }
}
