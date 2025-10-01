package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    IMAGE,
    GIF,
    AUDIO
}

@Entity(tableName = "media_library")
data class MediaLibraryEntity(
    @PrimaryKey
    val id: String, // UUID
    val fileName: String,
    val filePath: String, // Internal storage path
    val mediaType: MediaType,
    val uploadedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0, // in bytes
    val mimeType: String = "",
    val thumbnailPath: String? = null // For videos/large images
)
