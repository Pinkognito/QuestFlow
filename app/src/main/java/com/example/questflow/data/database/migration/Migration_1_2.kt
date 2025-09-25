package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to tasks table
        database.execSQL("ALTER TABLE tasks ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'MEDIUM'")
        database.execSQL("ALTER TABLE tasks ADD COLUMN xpOverride INTEGER")
        database.execSQL("ALTER TABLE tasks ADD COLUMN calendarEventId INTEGER")

        // Create user_stats table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_stats (
                id INTEGER NOT NULL DEFAULT 0,
                xp INTEGER NOT NULL DEFAULT 0,
                level INTEGER NOT NULL DEFAULT 1,
                points INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(id)
            )
        """)

        // Create xp_transactions table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS xp_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp TEXT NOT NULL,
                source TEXT NOT NULL,
                referenceId INTEGER,
                amount INTEGER NOT NULL
            )
        """)

        // Create calendar_event_links table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS calendar_event_links (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                calendarEventId INTEGER NOT NULL,
                title TEXT NOT NULL,
                startsAt TEXT NOT NULL,
                endsAt TEXT NOT NULL,
                xp INTEGER NOT NULL,
                rewarded INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Create memes table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS memes (
                id INTEGER NOT NULL,
                title TEXT NOT NULL,
                resourceName TEXT NOT NULL,
                PRIMARY KEY(id)
            )
        """)

        // Create meme_unlocks table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS meme_unlocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                memeId INTEGER NOT NULL,
                unlockedAt TEXT NOT NULL,
                levelAtUnlock INTEGER NOT NULL
            )
        """)

        // Create skill_nodes table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS skill_nodes (
                id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                type TEXT NOT NULL,
                value REAL NOT NULL,
                PRIMARY KEY(id)
            )
        """)

        // Create skill_edges table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS skill_edges (
                parentId TEXT NOT NULL,
                childId TEXT NOT NULL,
                PRIMARY KEY(parentId, childId)
            )
        """)

        // Create skill_unlocks table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS skill_unlocks (
                nodeId TEXT NOT NULL,
                unlockedAt TEXT NOT NULL,
                PRIMARY KEY(nodeId)
            )
        """)

        // Insert initial user stats row
        database.execSQL("INSERT INTO user_stats (id, xp, level, points) VALUES (0, 0, 1, 0)")

        // Insert seed memes (50 memes)
        for (i in 1..50) {
            database.execSQL("INSERT INTO memes (id, title, resourceName) VALUES ($i, 'Meme $i', 'meme_$i')")
        }

        // Insert skill nodes
        database.execSQL("INSERT INTO skill_nodes (id, title, description, type, value) VALUES ('xp_mult_1', 'XP Boost I', 'Increase XP gains by 10%', 'XP_MULT', 1.1)")
        database.execSQL("INSERT INTO skill_nodes (id, title, description, type, value) VALUES ('xp_mult_2', 'XP Boost II', 'Increase XP gains by 20%', 'XP_MULT', 1.2)")
        database.execSQL("INSERT INTO skill_nodes (id, title, description, type, value) VALUES ('streak_guard', 'Streak Guardian', 'Protect your streak once per week', 'STREAK_GUARD', 1.0)")
        database.execSQL("INSERT INTO skill_nodes (id, title, description, type, value) VALUES ('extra_meme', 'Meme Master', 'Get an extra meme on level up', 'EXTRA_MEME', 1.0)")

        // Insert skill edges (simple tree structure)
        database.execSQL("INSERT INTO skill_edges (parentId, childId) VALUES ('xp_mult_1', 'xp_mult_2')")
        database.execSQL("INSERT INTO skill_edges (parentId, childId) VALUES ('xp_mult_1', 'streak_guard')")
        database.execSQL("INSERT INTO skill_edges (parentId, childId) VALUES ('xp_mult_2', 'extra_meme')")
    }
}