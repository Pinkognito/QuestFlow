package com.example.questflow.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.questflow.data.database.TaskEntity
import java.time.LocalDateTime

/**
 * Registry entity that links tasks to their metadata items.
 * Uses a polymorphic pattern where different metadata types reference different tables.
 */
@Entity(
    tableName = "task_metadata",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("metadataType"),
        Index("referenceId")
    ]
)
data class TaskMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "taskId")
    val taskId: Long,

    @ColumnInfo(name = "metadataType")
    val metadataType: MetadataType,

    /**
     * Foreign key to the specific metadata table (e.g., metadata_locations.id)
     * The actual table is determined by metadataType
     */
    @ColumnInfo(name = "referenceId")
    val referenceId: Long,

    /**
     * Display order for UI sorting
     */
    val displayOrder: Int = 0,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Types of metadata that can be attached to tasks
 */
enum class MetadataType {
    LOCATION,
    CONTACT,
    PHONE,
    ADDRESS,
    EMAIL,
    URL,
    NOTE,
    FILE_ATTACHMENT
}
