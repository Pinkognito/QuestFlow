package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 42 -> 43: Add type and isDefault fields to text_templates
 * Enables categorization of templates and automatic default template selection
 */
val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add type field (GENERAL, WHATSAPP, EMAIL, CALENDAR)
        db.execSQL("ALTER TABLE text_templates ADD COLUMN type TEXT NOT NULL DEFAULT 'GENERAL'")

        // Add isDefault flag for automatic template selection
        db.execSQL("ALTER TABLE text_templates ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
    }
}
