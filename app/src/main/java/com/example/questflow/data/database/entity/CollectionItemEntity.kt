package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "collection_items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
        // Note: No foreign key for mediaLibraryId because it can be empty (legacy items)
    ]
)
data class CollectionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val mediaLibraryId: String = "", // Reference to MediaLibraryEntity (empty for legacy items)
    val imageUri: String = "", // DEPRECATED: Legacy field for migration
    val rarity: String = "COMMON",
    val requiredLevel: Int = 1,
    val categoryId: Long? = null, // null = global collection
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
