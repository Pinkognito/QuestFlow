package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 32 -> 33
 *
 * Changes:
 * 1. Drop iconMediaId column from metadata_contacts (not needed)
 * 2. Add iconEmoji column to metadata_contacts (text/emoji for calendar identification)
 */
val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support DROP COLUMN directly, so we need to recreate the table

        // 1. Create new table with updated schema
        db.execSQL("""
            CREATE TABLE metadata_contacts_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                systemContactId TEXT,
                lookupKey TEXT,
                displayName TEXT NOT NULL,
                givenName TEXT,
                familyName TEXT,
                primaryPhone TEXT,
                primaryEmail TEXT,
                photoUri TEXT,
                photoMediaId TEXT,
                iconEmoji TEXT,
                organization TEXT,
                jobTitle TEXT,
                note TEXT
            )
        """.trimIndent())

        // 2. Create indices on new table
        db.execSQL("CREATE INDEX index_metadata_contacts_new_systemContactId ON metadata_contacts_new(systemContactId)")
        db.execSQL("CREATE INDEX index_metadata_contacts_new_lookupKey ON metadata_contacts_new(lookupKey)")

        // 3. Copy data from old table (excluding iconMediaId)
        db.execSQL("""
            INSERT INTO metadata_contacts_new (
                id, systemContactId, lookupKey, displayName, givenName, familyName,
                primaryPhone, primaryEmail, photoUri, photoMediaId, organization, jobTitle, note
            )
            SELECT
                id, systemContactId, lookupKey, displayName, givenName, familyName,
                primaryPhone, primaryEmail, photoUri, photoMediaId, organization, jobTitle, note
            FROM metadata_contacts
        """.trimIndent())

        // 4. Drop old table
        db.execSQL("DROP TABLE metadata_contacts")

        // 5. Rename new table to original name
        db.execSQL("ALTER TABLE metadata_contacts_new RENAME TO metadata_contacts")
    }
}
