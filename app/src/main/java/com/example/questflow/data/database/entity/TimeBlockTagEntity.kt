package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Verknüpfungstabelle zwischen TimeBlocks und Tags
 *
 * Ermöglicht flexible Kategorisierung und Filterung von Zeitblöcken:
 * - "Wichtig", "Dringend", "Optional"
 * - "Team-Meeting", "1:1", "All-Hands"
 * - "Projekt X", "Kunde Y"
 */
@Entity(
    tableName = "time_block_tags",
    foreignKeys = [
        ForeignKey(
            entity = TimeBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["timeBlockId"],
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
        Index("timeBlockId"),
        Index("tagId")
    ]
)
data class TimeBlockTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timeBlockId: Long,                  // Verknüpfung zu TimeBlock
    val tagId: Long,                        // Verknüpfung zu globalem Tag

    val createdAt: String = LocalDateTime.now().toString()
)
