package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create collection_items table (no DEFAULT values, no indices - Room handles those)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS collection_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                imageUri TEXT NOT NULL,
                rarity TEXT NOT NULL,
                requiredLevel INTEGER NOT NULL,
                categoryId INTEGER,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create collection_unlocks table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS collection_unlocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                collectionItemId INTEGER NOT NULL,
                levelAtUnlock INTEGER NOT NULL,
                unlockedAt TEXT NOT NULL,
                FOREIGN KEY(collectionItemId) REFERENCES collection_items(id) ON DELETE CASCADE
            )
        """.trimIndent())
    }
}
