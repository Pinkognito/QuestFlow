package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create categories table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                color TEXT NOT NULL,
                emoji TEXT NOT NULL,
                currentXp INTEGER NOT NULL,
                currentLevel INTEGER NOT NULL,
                totalXp INTEGER NOT NULL,
                skillPoints INTEGER NOT NULL,
                levelScalingFactor REAL NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
        """)

        // Create category_xp_transactions table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS category_xp_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                xpAmount INTEGER NOT NULL,
                source TEXT NOT NULL,
                sourceId INTEGER,
                multiplier REAL NOT NULL,
                previousLevel INTEGER NOT NULL,
                newLevel INTEGER NOT NULL,
                previousTotalXp INTEGER NOT NULL,
                newTotalXp INTEGER NOT NULL,
                timestamp TEXT NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
        """)

        // Create index for category_xp_transactions
        database.execSQL("CREATE INDEX IF NOT EXISTS index_category_xp_transactions_categoryId ON category_xp_transactions(categoryId)")

        // Add categoryId to tasks table
        database.execSQL("ALTER TABLE tasks ADD COLUMN categoryId INTEGER DEFAULT NULL")

        // Add categoryId to calendar_event_links table
        database.execSQL("ALTER TABLE calendar_event_links ADD COLUMN categoryId INTEGER DEFAULT NULL")

        // Create default "Allgemein" category with ISO date format
        val now = java.time.LocalDateTime.now().toString()
        database.execSQL("""
            INSERT INTO categories (name, description, color, emoji, currentXp, currentLevel, totalXp, skillPoints, levelScalingFactor, isActive, createdAt, updatedAt)
            VALUES ('Allgemein', 'Standard-Kategorie für allgemeine Aufgaben', '#2196F3', '🎯', 0, 1, 0, 0, 1.0, 1, '$now', '$now')
        """)
    }
}