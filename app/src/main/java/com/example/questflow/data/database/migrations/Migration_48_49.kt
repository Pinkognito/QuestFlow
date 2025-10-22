package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 48 to 49
 * Adds TimeBlock system (time_blocks and time_block_tags tables)
 */
val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create time_blocks table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS time_blocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                type TEXT,
                startTime TEXT,
                endTime TEXT,
                allDay INTEGER NOT NULL DEFAULT 0,
                daysOfWeek TEXT,
                daysOfMonth TEXT,
                monthsOfYear TEXT,
                validFrom TEXT,
                validUntil TEXT,
                specificDates TEXT,
                repeatInterval INTEGER,
                repeatUnit TEXT,
                isActive INTEGER NOT NULL DEFAULT 1,
                activationDate TEXT,
                deactivationDate TEXT,
                conditions TEXT,
                contactId INTEGER,
                color TEXT,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                FOREIGN KEY(contactId) REFERENCES metadata_contacts(id) ON DELETE SET NULL
            )
        """)

        // Create time_block_tags cross-reference table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS time_block_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timeBlockId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                FOREIGN KEY(timeBlockId) REFERENCES time_blocks(id) ON DELETE CASCADE,
                FOREIGN KEY(tagId) REFERENCES metadata_tags(id) ON DELETE CASCADE
            )
        """)

        // Create indices for better query performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_isActive ON time_blocks(isActive)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_type ON time_blocks(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_contactId ON time_blocks(contactId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_block_tags_timeBlockId ON time_block_tags(timeBlockId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_block_tags_tagId ON time_block_tags(tagId)")
    }
}
