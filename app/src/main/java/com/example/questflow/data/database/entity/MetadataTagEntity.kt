package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Globale Tag-Verwaltung mit Typen und Hierarchie
 *
 * Features:
 * - Typisierung: CONTACT, TASK, LOCATION, TEMPLATE, GENERAL
 * - Hierarchie: Parent-Child Beziehungen für automatische Expansion
 * - Filterbar: Tags können nach Typ gefiltert werden
 * - Suchbar: Für intelligente Tag-basierte Suchen
 */
@Entity(
    tableName = "metadata_tags",
    foreignKeys = [
        ForeignKey(
            entity = MetadataTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("name", unique = true),
        Index("type"),
        Index("parentTagId")
    ]
)
data class MetadataTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,                       // Tag-Name: "VIP-Kunde", "Dringend", etc.
    val type: TagType,                      // Typ für Filterung

    // Hierarchie
    val parentTagId: Long? = null,          // Optional: Parent-Tag für Hierarchie

    // UI Customization
    val color: String? = null,              // Hex-Farbe: "#FF5722"
    val icon: String? = null,               // Optional: Material Icon name

    // Metadata
    val description: String? = null,        // Beschreibung für Benutzer
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Tag-Typen für kontextbasierte Filterung
 */
enum class TagType {
    CONTACT,        // Tags für Kontakte: "VIP", "Kunde", "Lieferant"
    TASK,           // Tags für Tasks: "Dringend", "Wichtig", "Privat"
    LOCATION,       // Tags für Orte: "Büro", "Remote", "Baustelle"
    TEMPLATE,       // Tags für Templates: "Business", "Privat"
    TIMEBLOCK,      // Tags für Zeitblockierungen: "Regulär", "Flexibel", "Projekt"
    GENERAL         // Universelle Tags (in allen Kontexten verwendbar)
}
