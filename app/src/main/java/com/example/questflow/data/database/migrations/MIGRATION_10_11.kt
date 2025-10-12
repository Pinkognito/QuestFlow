package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add new columns to skill_nodes
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN effectType TEXT NOT NULL DEFAULT 'XP_MULTIPLIER'")
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN baseValue REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN scalingPerPoint REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN maxInvestment INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN iconName TEXT NOT NULL DEFAULT 'star'")
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN positionX REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN positionY REAL NOT NULL DEFAULT 0.0")

        // 2. Migrate existing skill nodes to new schema
        // XP_MULT nodes: Convert to XP_MULTIPLIER effect
        db.execSQL("""
            UPDATE skill_nodes
            SET effectType = 'XP_MULTIPLIER',
                baseValue = 0.0,
                scalingPerPoint = (value - 1.0) * 100.0,
                maxInvestment = 5
            WHERE type = 'XP_MULT'
        """)

        // EXTRA_MEME nodes: Convert to EXTRA_COLLECTION_UNLOCK effect
        db.execSQL("""
            UPDATE skill_nodes
            SET effectType = 'EXTRA_COLLECTION_UNLOCK',
                baseValue = 1.0,
                scalingPerPoint = 0.0,
                maxInvestment = 1
            WHERE type = 'EXTRA_MEME'
        """)

        // STREAK_GUARD nodes: Convert to STREAK_PROTECTION effect
        db.execSQL("""
            UPDATE skill_nodes
            SET effectType = 'STREAK_PROTECTION',
                baseValue = 1.0,
                scalingPerPoint = 0.0,
                maxInvestment = 1
            WHERE type = 'STREAK_GUARD'
        """)

        // 3. Add minParentInvestment to skill_edges
        db.execSQL("ALTER TABLE skill_edges ADD COLUMN minParentInvestment INTEGER NOT NULL DEFAULT 1")

        // 4. Add investment tracking to skill_unlocks
        db.execSQL("ALTER TABLE skill_unlocks ADD COLUMN investedPoints INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE skill_unlocks ADD COLUMN lastInvestedAt TEXT NOT NULL DEFAULT '2025-01-01T00:00:00'")

        // 5. Insert default skill nodes if table is empty
        db.execSQL("""
            INSERT OR IGNORE INTO skill_nodes (id, title, description, effectType, baseValue, scalingPerPoint, maxInvestment, iconName, positionX, positionY, type, value)
            VALUES
            ('xp_mult_basic', 'XP Verstärker', 'Erhöht alle erhaltenen XP', 'XP_MULTIPLIER', 0.0, 3.0, 10, 'star', 0.0, 0.0, 'XP_MULT', 0.0),
            ('task_xp_bonus', 'Task Meister', 'Zusätzliche XP für abgeschlossene Tasks', 'TASK_XP_BONUS', 0.0, 2.0, 10, 'star', 1.0, 0.0, 'XP_MULT', 0.0),
            ('skill_point_gain', 'Weiser Verstand', 'Zusätzliche Skillpunkte pro Level-Up', 'SKILL_POINT_GAIN', 0.0, 1.0, 5, 'star', 2.0, 0.0, 'XP_MULT', 0.0),
            ('extra_collection', 'Sammler', 'Zusätzliche Collection-Items pro Level-Up', 'EXTRA_COLLECTION_UNLOCK', 1.0, 0.0, 1, 'star', 0.0, 1.0, 'EXTRA_MEME', 0.0),
            ('rare_collection_chance', 'Glückspilz', 'Erhöht die Chance auf seltenere Items', 'RARE_COLLECTION_CHANCE', 0.0, 5.0, 8, 'star', 1.0, 1.0, 'EXTRA_MEME', 0.0),
            ('calendar_xp_bonus', 'Kalender Enthusiast', 'Zusätzliche XP aus Kalender-Events', 'CALENDAR_XP_BONUS', 0.0, 3.0, 10, 'star', 2.0, 1.0, 'XP_MULT', 0.0)
        """)

        // 6. Insert default skill edges (prerequisites)
        db.execSQL("""
            INSERT OR IGNORE INTO skill_edges (parentId, childId, minParentInvestment)
            VALUES
            ('xp_mult_basic', 'task_xp_bonus', 3),
            ('xp_mult_basic', 'skill_point_gain', 5),
            ('extra_collection', 'rare_collection_chance', 1),
            ('task_xp_bonus', 'calendar_xp_bonus', 5)
        """)
    }
}
