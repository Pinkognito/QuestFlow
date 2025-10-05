package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores file attachment information for tasks
 */
@Entity(tableName = "metadata_file_attachments")
data class MetadataFileAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val fileName: String,
    val fileUri: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailUri: String? = null,
    val checksum: String? = null
)
