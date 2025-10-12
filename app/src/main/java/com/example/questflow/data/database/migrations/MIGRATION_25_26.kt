package com.example.questflow.data.database.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_25_26", "Starting migration from version 25 to 26 - Adding Task Metadata System")

        // 1. Create task_metadata registry table
        Log.d("MIGRATION_25_26", "Creating task_metadata table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS task_metadata (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                metadataType TEXT NOT NULL,
                referenceId INTEGER NOT NULL,
                displayOrder INTEGER NOT NULL DEFAULT 0,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_task_metadata_taskId ON task_metadata(taskId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_task_metadata_metadataType ON task_metadata(metadataType)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_task_metadata_referenceId ON task_metadata(referenceId)")

        // 2. Create metadata_locations table
        Log.d("MIGRATION_25_26", "Creating metadata_locations table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_locations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                placeId TEXT,
                placeName TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                formattedAddress TEXT,
                street TEXT,
                city TEXT,
                postalCode TEXT,
                country TEXT,
                iconUrl TEXT,
                customLabel TEXT
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_locations_placeId ON metadata_locations(placeId)")

        // 3. Create metadata_contacts table
        Log.d("MIGRATION_25_26", "Creating metadata_contacts table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                systemContactId TEXT,
                lookupKey TEXT,
                displayName TEXT NOT NULL,
                givenName TEXT,
                familyName TEXT,
                primaryPhone TEXT,
                primaryEmail TEXT,
                photoUri TEXT,
                organization TEXT,
                jobTitle TEXT,
                note TEXT
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_contacts_systemContactId ON metadata_contacts(systemContactId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_contacts_lookupKey ON metadata_contacts(lookupKey)")

        // 4. Create metadata_phones table
        Log.d("MIGRATION_25_26", "Creating metadata_phones table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_phones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                phoneNumber TEXT NOT NULL,
                phoneType TEXT NOT NULL,
                label TEXT,
                countryCode TEXT,
                isVerified INTEGER NOT NULL DEFAULT 0
            )
        """)

        // 5. Create metadata_addresses table
        Log.d("MIGRATION_25_26", "Creating metadata_addresses table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_addresses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                street TEXT NOT NULL,
                houseNumber TEXT,
                addressLine2 TEXT,
                city TEXT NOT NULL,
                postalCode TEXT NOT NULL,
                state TEXT,
                country TEXT NOT NULL,
                addressType TEXT NOT NULL,
                label TEXT
            )
        """)

        // 6. Create metadata_emails table
        Log.d("MIGRATION_25_26", "Creating metadata_emails table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_emails (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                emailAddress TEXT NOT NULL,
                emailType TEXT NOT NULL,
                label TEXT,
                isVerified INTEGER NOT NULL DEFAULT 0
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_emails_emailAddress ON metadata_emails(emailAddress)")

        // 7. Create metadata_urls table
        Log.d("MIGRATION_25_26", "Creating metadata_urls table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_urls (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                url TEXT NOT NULL,
                title TEXT,
                description TEXT,
                faviconUrl TEXT,
                urlType TEXT NOT NULL
            )
        """)

        // 8. Create metadata_notes table
        Log.d("MIGRATION_25_26", "Creating metadata_notes table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                content TEXT NOT NULL,
                format TEXT NOT NULL,
                isPinned INTEGER NOT NULL DEFAULT 0
            )
        """)

        // 9. Create metadata_file_attachments table
        Log.d("MIGRATION_25_26", "Creating metadata_file_attachments table")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_file_attachments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                fileName TEXT NOT NULL,
                fileUri TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                fileSize INTEGER NOT NULL,
                thumbnailUri TEXT,
                checksum TEXT
            )
        """)

        Log.d("MIGRATION_25_26", "Migration completed successfully - All 9 metadata tables created")
    }
}
