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
 * Speichert minimale Informationen:
 * - Welcher Task (taskId)
 * - Was ist passiert (eventType)
 * - Wann ist es passiert (timestamp)
 * - Bei Recurring: Neue Fälligkeit (newDueDate)
 *
 * Auto-Cleanup: Einträge älter als 90 Tage werden automatisch gelöscht.
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
     */
    val eventType: String,

    val timestamp: LocalDateTime,

    /**
     * Bei RECURRING_CREATED/RESCHEDULED: Die neue Fälligkeit
     * Null bei anderen Events
     */
    val newDueDate: LocalDateTime? = null
)
