package com.example.questflow.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Migration 29 → 30: Globales Tag-System mit Hierarchie
 *
 * Neue Features:
 * - MetadataTagEntity: Globale Tags mit Typen (CONTACT, TASK, LOCATION, etc.)
 * - Hierarchie: Parent-Child Beziehungen zwischen Tags
 * - ContactTagEntity: Globale Tags für Kontakte
 * - TaskContactTagEntity: Erweitert um tagId (optional)
 *
 * Standard-Tags werden automatisch erstellt
 */
val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // ========== 1. MetadataTagEntity Tabelle ==========
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS metadata_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                parentTagId INTEGER,
                color TEXT,
                icon TEXT,
                description TEXT,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                FOREIGN KEY(parentTagId) REFERENCES metadata_tags(id) ON DELETE SET NULL
            )
        """.trimIndent())

        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_metadata_tags_name ON metadata_tags(name)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_tags_type ON metadata_tags(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_metadata_tags_parentTagId ON metadata_tags(parentTagId)")

        // ========== 2. ContactTagEntity Tabelle ==========
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS contact_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contactId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                FOREIGN KEY(contactId) REFERENCES metadata_contacts(id) ON DELETE CASCADE,
                FOREIGN KEY(tagId) REFERENCES metadata_tags(id) ON DELETE CASCADE
            )
        """.trimIndent())

        database.execSQL("CREATE INDEX IF NOT EXISTS index_contact_tags_contactId ON contact_tags(contactId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_contact_tags_tagId ON contact_tags(tagId)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contact_tags_contactId_tagId ON contact_tags(contactId, tagId)")

        // ========== 3. TaskContactTagEntity erweitern ==========
        // Alte Tabelle existiert bereits, füge neue Spalte hinzu
        database.execSQL("ALTER TABLE task_contact_tags ADD COLUMN tagId INTEGER DEFAULT NULL")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_task_contact_tags_tagId ON task_contact_tags(tagId)")

        // ========== 4. Standard-Tags erstellen ==========
        // Nur wenn noch keine Tags existieren (idempotent)
        val existingTagCount = database.query("SELECT COUNT(*) FROM metadata_tags").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

        if (existingTagCount == 0) {
            // CONTACT Tags (Hierarchie: Kunde → VIP-Kunde, Neukunde)
            insertTag(database, "Kunde", "CONTACT", null, "#2196F3", "business", "Geschäftskunden", now)
        val kundeId = getLastInsertId(database)
        insertTag(database, "VIP-Kunde", "CONTACT", kundeId, "#FF9800", "star", "VIP Geschäftskunde", now)
        insertTag(database, "Neukunde", "CONTACT", kundeId, "#4CAF50", "new_releases", "Neuer Kunde", now)
        insertTag(database, "Stammkunde", "CONTACT", kundeId, "#9C27B0", "loyalty", "Treuer Stammkunde", now)

        insertTag(database, "Lieferant", "CONTACT", null, "#FF5722", "local_shipping", "Lieferanten und Partner", now)
        insertTag(database, "Kollege", "CONTACT", null, "#795548", "people", "Arbeitskollegen", now)
        insertTag(database, "Familie", "CONTACT", null, "#E91E63", "family_restroom", "Familienmitglieder", now)
        insertTag(database, "Freund", "CONTACT", null, "#3F51B5", "favorite", "Persönliche Freunde", now)

        // TASK Tags (Hierarchie: Dringend → Sofort, Heute)
        insertTag(database, "Dringend", "TASK", null, "#F44336", "priority_high", "Dringende Aufgaben", now)
        val dringendId = getLastInsertId(database)
        insertTag(database, "Sofort", "TASK", dringendId, "#D32F2F", "notification_important", "Sofort erledigen", now)
        insertTag(database, "Heute", "TASK", dringendId, "#FF5722", "today", "Heute erledigen", now)

        insertTag(database, "Wichtig", "TASK", null, "#FF9800", "label_important", "Wichtige Aufgaben", now)
        insertTag(database, "Optional", "TASK", null, "#9E9E9E", "low_priority", "Optionale Aufgaben", now)
        insertTag(database, "Privat", "TASK", null, "#E91E63", "lock", "Private Aufgaben", now)
        insertTag(database, "Geschäftlich", "TASK", null, "#2196F3", "work", "Geschäftliche Aufgaben", now)
        insertTag(database, "Projekt", "TASK", null, "#9C27B0", "folder", "Projekt-Aufgaben", now)

        // LOCATION Tags
        insertTag(database, "Büro", "LOCATION", null, "#607D8B", "business_center", "Büro-Standorte", now)
        insertTag(database, "Home-Office", "LOCATION", null, "#4CAF50", "home", "Zuhause arbeiten", now)
        insertTag(database, "Remote", "LOCATION", null, "#00BCD4", "laptop", "Remote Arbeit", now)
        insertTag(database, "Baustelle", "LOCATION", null, "#FF9800", "construction", "Baustellen", now)
        insertTag(database, "Filiale", "LOCATION", null, "#3F51B5", "store", "Filialen", now)

        // TEMPLATE Tags
        insertTag(database, "Business", "TEMPLATE", null, "#2196F3", "business", "Geschäftliche Templates", now)
        insertTag(database, "Privat", "TEMPLATE", null, "#E91E63", "person", "Private Templates", now)
        insertTag(database, "Meeting", "TEMPLATE", null, "#9C27B0", "event", "Meeting Templates", now)
        insertTag(database, "E-Mail", "TEMPLATE", null, "#FF5722", "email", "E-Mail Templates", now)

        // GENERAL Tags (universell verwendbar)
        insertTag(database, "Favorit", "GENERAL", null, "#FFC107", "star", "Favoriten", now)
        insertTag(database, "Archiv", "GENERAL", null, "#9E9E9E", "archive", "Archiviert", now)
        insertTag(database, "Review", "GENERAL", null, "#00BCD4", "rate_review", "Zu überprüfen", now)
        } // Ende if (existingTagCount == 0)
    }

    private fun insertTag(
        database: SupportSQLiteDatabase,
        name: String,
        type: String,
        parentTagId: Long?,
        color: String,
        icon: String,
        description: String,
        createdAt: String
    ) {
        val parentIdStr = parentTagId?.toString() ?: "NULL"
        database.execSQL("""
            INSERT INTO metadata_tags (name, type, parentTagId, color, icon, description, createdAt, updatedAt)
            VALUES ('$name', '$type', $parentIdStr, '$color', '$icon', '$description', '$createdAt', '$createdAt')
        """.trimIndent())
    }

    private fun getLastInsertId(database: SupportSQLiteDatabase): Long {
        val cursor = database.query("SELECT last_insert_rowid()")
        cursor.moveToFirst()
        val id = cursor.getLong(0)
        cursor.close()
        return id
    }
}
