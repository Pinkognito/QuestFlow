package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.questflow.data.database.TaskEntity
import java.time.LocalDateTime

/**
 * Historie aller Aktionen die im Task durchgeführt wurden
 * Vollständige Nachverfolgung für Projektmanagement
 */
@Entity(
    tableName = "action_history",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("executedAt"), Index("actionType")]
)
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val taskId: Long,
    val actionType: String,                 // "WHATSAPP", "SMS", "EMAIL", "CALL", "MEETING"

    val targetContactIds: String,           // Comma-separated IDs: "1,5,12"
    val targetContactNames: String,         // "Max, Lisa, Tom" (für schnelle Anzeige)

    val detailsJson: String?,               // JSON für zusätzliche Daten

    val message: String? = null,            // Die gesendete Nachricht (falls zutreffend)
    val templateUsed: String? = null,       // Name des verwendeten Templates

    val success: Boolean = true,            // Wurde die Aktion erfolgreich ausgeführt?
    val errorMessage: String? = null,       // Fehlermeldung falls nicht erfolgreich

    val executedAt: LocalDateTime = LocalDateTime.now()
)
