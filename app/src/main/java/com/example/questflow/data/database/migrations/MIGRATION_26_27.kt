package com.example.questflow.data.database.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_26_27", "Starting migration from 26 to 27...")

        // Recreate metadata_phones with foreign key
        db.execSQL("ALTER TABLE metadata_phones RENAME TO metadata_phones_old")
        db.execSQL("""
            CREATE TABLE metadata_phones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contactId INTEGER,
                phoneNumber TEXT NOT NULL,
                phoneType TEXT NOT NULL,
                label TEXT,
                countryCode TEXT,
                isVerified INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(contactId) REFERENCES metadata_contacts(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX index_metadata_phones_contactId ON metadata_phones(contactId)")
        db.execSQL("INSERT INTO metadata_phones (id, phoneNumber, phoneType, label, countryCode, isVerified) SELECT id, phoneNumber, phoneType, label, countryCode, isVerified FROM metadata_phones_old")
        db.execSQL("DROP TABLE metadata_phones_old")
        Log.d("MIGRATION_26_27", "Recreated metadata_phones with contactId")

        // Recreate metadata_emails with foreign key
        db.execSQL("ALTER TABLE metadata_emails RENAME TO metadata_emails_old")
        db.execSQL("""
            CREATE TABLE metadata_emails (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contactId INTEGER,
                emailAddress TEXT NOT NULL,
                emailType TEXT NOT NULL,
                label TEXT,
                isVerified INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(contactId) REFERENCES metadata_contacts(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX index_metadata_emails_emailAddress ON metadata_emails(emailAddress)")
        db.execSQL("CREATE INDEX index_metadata_emails_contactId ON metadata_emails(contactId)")
        db.execSQL("INSERT INTO metadata_emails (id, emailAddress, emailType, label, isVerified) SELECT id, emailAddress, emailType, label, isVerified FROM metadata_emails_old")
        db.execSQL("DROP TABLE metadata_emails_old")
        Log.d("MIGRATION_26_27", "Recreated metadata_emails with contactId")

        // Recreate metadata_addresses with foreign key
        db.execSQL("ALTER TABLE metadata_addresses RENAME TO metadata_addresses_old")
        db.execSQL("""
            CREATE TABLE metadata_addresses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contactId INTEGER,
                street TEXT NOT NULL,
                houseNumber TEXT,
                addressLine2 TEXT,
                city TEXT NOT NULL,
                postalCode TEXT NOT NULL,
                state TEXT,
                country TEXT NOT NULL,
                addressType TEXT NOT NULL,
                label TEXT,
                FOREIGN KEY(contactId) REFERENCES metadata_contacts(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX index_metadata_addresses_contactId ON metadata_addresses(contactId)")
        db.execSQL("INSERT INTO metadata_addresses (id, street, houseNumber, addressLine2, city, postalCode, state, country, addressType, label) SELECT id, street, houseNumber, addressLine2, city, postalCode, state, country, addressType, label FROM metadata_addresses_old")
        db.execSQL("DROP TABLE metadata_addresses_old")
        Log.d("MIGRATION_26_27", "Recreated metadata_addresses with contactId")

        Log.d("MIGRATION_26_27", "Migration completed successfully")
    }
}
