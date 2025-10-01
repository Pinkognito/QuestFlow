package com.example.questflow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add categoryId and colorHex to skill_nodes
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN categoryId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE skill_nodes ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#FFD700'")

        // Create index for category filtering
        db.execSQL("CREATE INDEX IF NOT EXISTS index_skill_nodes_categoryId ON skill_nodes(categoryId)")
    }
}
