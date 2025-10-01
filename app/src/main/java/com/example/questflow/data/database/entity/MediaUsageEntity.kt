package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaUsageType {
    COLLECTION_ITEM,
    SKILL_ICON,
    CATEGORY_ICON,
    OTHER
}

@Entity(
    tableName = "media_usage",
    foreignKeys = [
        ForeignKey(
            entity = MediaLibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaLibraryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mediaLibraryId"]),
        Index(value = ["usageType", "referenceId"])
    ]
)
data class MediaUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaLibraryId: String, // Reference to MediaLibraryEntity
    val usageType: MediaUsageType, // Where is this media used
    val referenceId: Long, // ID of the entity using this media (e.g., collection item ID, skill node ID)
    val categoryId: Long? = null, // Optional: Category context
    val createdAt: Long = System.currentTimeMillis()
)
