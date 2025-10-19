package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.questflow.data.database.TaskEntity
import java.time.LocalDateTime

/**
 * Dokumentiert Status-Änderungen von Tasks für Statistiken und Audit-Trail.
 *
 * Speichert detaillierte Informationen:
 * - Welcher Task (taskId)
 * - Was ist passiert (eventType) - siehe HistoryEventType enum
 * - Wann ist es passiert (timestamp)
 * - Bei Recurring: Neue Fälligkeit (newDueDate)
 * - Bei Property Changes: Alter Wert (oldValue)
 * - Bei Property Changes: Neuer Wert (newValue)
 *
 * Beispiele:
 * - PRIORITY_CHANGED: oldValue="LOW", newValue="HIGH"
 * - DIFFICULTY_CHANGED: oldValue="20", newValue="100"
 * - CATEGORY_CHANGED: oldValue="1", newValue="5"
 * - TITLE_CHANGED: oldValue="Alter Titel", newValue="Neuer Titel"
 *
 * Auto-Cleanup: Einträge älter als 90 Tage werden automatisch gelöscht.
 * User-konfigurierbar: Via HistoryPreferences können Event-Typen deaktiviert werden.
 */
@Entity(
    tableName = "task_history",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId", "timestamp"]),
        Index(value = ["eventType"]),
        Index(value = ["timestamp"])
    ]
)
data class TaskHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val taskId: Long,

    /**
     * Event-Typen:
     * - EXPIRED: Task ist abgelaufen
     * - COMPLETED: Task wurde manuell abgeschlossen
     * - CLAIMED: XP wurde claimed
     * - RECLAIMED: XP wurde erneut claimed (nach reactivate)
     * - RECURRING_CREATED: Neue recurring Instanz wurde erstellt
     * - RESCHEDULED: Task-Zeit wurde geändert (bei recurring)
     * - REACTIVATED: Task wurde wieder aktiv (via shouldReactivate oder datetime)
     * - XP_RECLAIMABLE: XP-Beanspruchung wieder freigeschaltet
     * - PARENT_ASSIGNED: Übergeordneter Task zugewiesen
     */
    val eventType: String,

    val timestamp: LocalDateTime,

    /**
     * Bei RECURRING_CREATED/RESCHEDULED: Die neue Fälligkeit
     * Null bei anderen Events
     */
    val newDueDate: LocalDateTime? = null,

    /**
     * Bei Property-Änderungen: Der alte Wert als String
     * Beispiele:
     * - PRIORITY_CHANGED: "LOW", "MEDIUM", "HIGH", "URGENT"
     * - DIFFICULTY_CHANGED: "20", "40", "60", "80", "100"
     * - CATEGORY_CHANGED: Category-ID als String
     * - TITLE_CHANGED: Vollständiger alter Titel
     * - DUE_DATE_CHANGED: ISO-formatiertes Datum
     */
    val oldValue: String? = null,

    /**
     * Bei Property-Änderungen: Der neue Wert als String
     * Format wie bei oldValue
     */
    val newValue: String? = null
)
