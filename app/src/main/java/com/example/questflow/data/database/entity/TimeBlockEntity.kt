package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Flexibles Zeitblockierungs-System für wiederkehrende oder einmalige Zeitbereiche
 *
 * Anwendungsfälle:
 * - Arbeitszeiten definieren
 * - Urlaubszeiten blockieren
 * - Meeting-Zeiten reservieren
 * - Flexible Zeitfenster mit komplexen Wiederholungsregeln
 *
 * Flexibilität:
 * - Tage: Spezifische Wochentage (Mo-So), Tage im Monat (1-31)
 * - Monate: Spezifische Monate (Jan-Dez)
 * - Datumsbereiche: Von-Bis mit validFrom/validUntil
 * - Einzeldaten: Komma-separierte Liste spezifischer Daten
 * - Status: Aktiv/Inaktiv mit verzögerter Aktivierung
 * - Bedingungen: JSON für erweiterte Logik
 */
@Entity(
    tableName = "time_blocks",
    foreignKeys = [
        ForeignKey(
            entity = MetadataContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("contactId"),
        Index("isActive"),
        Index("type"),
        Index("validFrom"),
        Index("validUntil")
    ]
)
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // === GRUNDINFORMATIONEN ===
    val name: String,                           // "Arbeitszeit", "Urlaub Italien", "Meeting-Block"
    val description: String? = null,            // Detaillierte Beschreibung
    val type: String? = null,                   // "WORK", "VACATION", "MEETING", "PERSONAL", etc.

    // === ZEITDEFINITION ===
    val startTime: String? = null,              // Uhrzeit von (HH:mm:ss)
    val endTime: String? = null,                // Uhrzeit bis (HH:mm:ss)
    val allDay: Boolean = false,                // Ganztägige Blockierung

    // === WIEDERHOLUNGSMUSTER ===
    // Wochentage: 1=Montag, 2=Dienstag, ..., 7=Sonntag
    // Format: "1,3,5" = Montag, Mittwoch, Freitag
    val daysOfWeek: String? = null,

    // Tage im Monat: 1-31
    // Format: "1,15,31" = 1., 15., 31. des Monats
    val daysOfMonth: String? = null,

    // Monate: 1=Januar, 2=Februar, ..., 12=Dezember
    // Format: "1,6,12" = Januar, Juni, Dezember
    val monthsOfYear: String? = null,

    // === DATUMSBEREICHE ===
    val validFrom: String? = null,              // Gültig ab (ISO Date: yyyy-MM-dd)
    val validUntil: String? = null,             // Gültig bis (ISO Date: yyyy-MM-dd)

    // Spezifische Einzeldaten (komma-separiert)
    // Format: "2025-01-15,2025-02-20,2025-03-10"
    val specificDates: String? = null,

    // === ERWEITERTE WIEDERHOLUNG ===
    val repeatInterval: Int? = null,            // z.B. "2" für alle 2 Einheiten
    val repeatUnit: String? = null,             // "DAY", "WEEK", "MONTH", "YEAR"

    // === STATUS & AKTIVIERUNG ===
    val isActive: Boolean = true,               // Aktiv/Inaktiv
    val activationDate: String? = null,         // Wird aktiviert am (ISO DateTime)
    val deactivationDate: String? = null,       // Wird deaktiviert am (ISO DateTime)

    // === BEDINGUNGEN ===
    // JSON für erweiterte Bedingungslogik
    // Beispiel: {"weatherCondition": "sunny", "minTemperature": 20}
    val conditions: String? = null,

    // === RELATIONEN ===
    val contactId: Long? = null,                // Optional: Verknüpfung mit Kontakt

    // === DARSTELLUNG ===
    val color: String? = null,                  // Hex-Color für visuelle Darstellung (#FF5722)

    // === METADATEN ===
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString()
)

/**
 * Wiederholungs-Einheiten für repeatUnit
 */
enum class RepeatUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

/**
 * Vordefinierte Typen für TimeBlocks (erweiterbar durch Nutzer)
 */
enum class TimeBlockType {
    WORK,           // Arbeitszeit
    VACATION,       // Urlaub
    MEETING,        // Meeting
    PERSONAL,       // Privat
    BREAK,          // Pause
    COMMUTE,        // Pendeln
    CUSTOM          // Benutzerdefiniert
}
