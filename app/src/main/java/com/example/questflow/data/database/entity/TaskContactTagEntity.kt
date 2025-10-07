package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.questflow.data.database.TaskEntity
import java.time.LocalDateTime

/**
 * Tags für Kontakte innerhalb eines Tasks (task-spezifisch)
 * Beispiel: Max Mustermann → "Teilnehmer", "Entscheidungsträger" (nur für diesen Task)
 *
 * MIGRATION: Unterstützt sowohl String-Tags (legacy) als auch tagId (neu)
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
        ),
        ForeignKey(
            entity = MetadataTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("contactId"),
        Index("tag"),
        Index("tagId")
    ]
)
data class TaskContactTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val taskId: Long,
    val contactId: Long,

    // LEGACY: String-basierte Tags (für Rückwärtskompatibilität)
    val tag: String,                        // Tag-Name als String

    // NEU: Referenz zu globalem Tag
    val tagId: Long? = null,                // Optional: Verknüpfung mit MetadataTagEntity

    val createdAt: LocalDateTime = LocalDateTime.now()
)
