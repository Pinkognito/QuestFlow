package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.questflow.data.database.TaskEntity
import java.time.LocalDateTime

/**
 * Tags für Kontakte innerhalb eines Tasks
 * Beispiel: Max Mustermann → "Teilnehmer", "Entscheidungsträger"
 */
@Entity(
    tableName = "task_contact_tags",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MetadataContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("contactId"), Index("tag")]
)
data class TaskContactTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val taskId: Long,
    val contactId: Long,
    val tag: String,                        // Tag-Name: "Teilnehmer", "Verantwortlich", etc.

    val createdAt: LocalDateTime = LocalDateTime.now()
)
