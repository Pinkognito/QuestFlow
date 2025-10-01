package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Step 1: Create media_library table if not exists
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS media_library (
                id TEXT PRIMARY KEY NOT NULL,
                fileName TEXT NOT NULL,
                filePath TEXT NOT NULL,
                mediaType TEXT NOT NULL,
                uploadedAt INTEGER NOT NULL,
                fileSize INTEGER NOT NULL,
                mimeType TEXT NOT NULL,
                thumbnailPath TEXT
            )
        """)

        // Step 2: Add mediaLibraryId column to collection_items (NOT NULL with default empty string)
        database.execSQL("ALTER TABLE collection_items ADD COLUMN mediaLibraryId TEXT NOT NULL DEFAULT ''")

        // Step 3: Migrate existing imageUri data to media_library
        // For each existing collection item, create a corresponding media entry
        database.execSQL("""
            INSERT INTO media_library (id, fileName, filePath, mediaType, uploadedAt, fileSize, mimeType, thumbnailPath)
            SELECT
                'legacy_' || id as id,
                name || '.jpg' as fileName,
                imageUri as filePath,
                'IMAGE' as mediaType,
                strftime('%s', 'now') * 1000 as uploadedAt,
                0 as fileSize,
                'image/jpeg' as mimeType,
                NULL as thumbnailPath
            FROM collection_items
            WHERE imageUri IS NOT NULL AND imageUri != ''
        """)

        // Step 4: Update collection_items to reference the new media_library entries
        database.execSQL("""
            UPDATE collection_items
            SET mediaLibraryId = 'legacy_' || id
            WHERE imageUri IS NOT NULL AND imageUri != ''
        """)

        // Note: We keep imageUri column for backward compatibility but it's deprecated
        // Items with empty mediaLibraryId will fall back to imageUri when displaying
    }
}
