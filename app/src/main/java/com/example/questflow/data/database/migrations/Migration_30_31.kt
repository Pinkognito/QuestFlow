package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 30 → 31: Betreff-Feld für Textbausteine
 *
 * Neue Features:
 * - subject Feld in text_templates Tabelle für E-Mail/Termin-Betreff
 */
val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Füge subject-Spalte zur text_templates Tabelle hinzu
        database.execSQL("ALTER TABLE text_templates ADD COLUMN subject TEXT DEFAULT NULL")
    }
}
