package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 47→48: Add TimeBlock system for flexible time blocking
 *
 * Enables users to define:
 * - Recurring or one-time time blocks
 * - Complex patterns (days of week, days of month, months, date ranges)
 * - Status management (active/inactive, delayed activation)
 * - Conditions and relations (contacts, tags)
 *
 * Use cases:
 * - Work hours definition
 * - Vacation blocking
 * - Meeting time reservations
 * - Flexible scheduling constraints
 */
val MIGRATION_47_48 = object : Migration(47, 48) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // === TIME_BLOCKS TABLE ===
        database.execSQL(
            """
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
            """
        )

        // === TIME_BLOCK_TAGS TABLE ===
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS time_block_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timeBlockId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                FOREIGN KEY(timeBlockId) REFERENCES time_blocks(id) ON DELETE CASCADE,
                FOREIGN KEY(tagId) REFERENCES metadata_tags(id) ON DELETE CASCADE
            )
            """
        )

        // === INDICES FOR TIME_BLOCKS ===
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_contactId ON time_blocks(contactId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_isActive ON time_blocks(isActive)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_type ON time_blocks(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_validFrom ON time_blocks(validFrom)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_blocks_validUntil ON time_blocks(validUntil)")

        // === INDICES FOR TIME_BLOCK_TAGS ===
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_block_tags_timeBlockId ON time_block_tags(timeBlockId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_block_tags_tagId ON time_block_tags(tagId)")
    }
}
