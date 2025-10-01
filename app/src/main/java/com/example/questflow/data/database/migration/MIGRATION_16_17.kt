package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Indices were already created in MIGRATION_15_16, but entity definition was updated
        // This migration ensures indices exist (idempotent operation)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_media_usage_mediaLibraryId ON media_usage(mediaLibraryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_media_usage_reference ON media_usage(usageType, referenceId)")
    }
}
