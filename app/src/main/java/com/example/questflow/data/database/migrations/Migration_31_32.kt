package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 31 → 32: Contact Media Integration
 *
 * Neue Features:
 * - photoMediaId für gestreamte Kontaktbilder (ersetzt photoUri langfristig)
 * - iconMediaId für Kontakt-Icons zur schnellen Identifikation
 */
val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Füge photoMediaId-Spalte hinzu (für gestreamte Bilder wie bei Collections)
        database.execSQL("ALTER TABLE metadata_contacts ADD COLUMN photoMediaId TEXT DEFAULT NULL")

        // Füge iconMediaId-Spalte hinzu (für Icons zur Kontakt-Identifikation)
        database.execSQL("ALTER TABLE metadata_contacts ADD COLUMN iconMediaId TEXT DEFAULT NULL")
    }
}
