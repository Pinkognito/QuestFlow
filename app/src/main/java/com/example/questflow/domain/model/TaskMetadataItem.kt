package com.example.questflow.domain.model

import com.example.questflow.data.database.entity.*

/**
 * Domain model representing different types of metadata that can be attached to tasks.
 * Uses sealed class for type-safe handling.
 */
sealed class TaskMetadataItem {
    abstract val metadataId: Long
    abstract val taskId: Long
    abstract val displayOrder: Int

    data class Location(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val location: MetadataLocationEntity
    ) : TaskMetadataItem()

    data class Contact(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val contact: MetadataContactEntity
    ) : TaskMetadataItem()

    data class Phone(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val phone: MetadataPhoneEntity
    ) : TaskMetadataItem()

    data class Address(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val address: MetadataAddressEntity
    ) : TaskMetadataItem()

    data class Email(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val email: MetadataEmailEntity
    ) : TaskMetadataItem()

    data class Url(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val url: MetadataUrlEntity
    ) : TaskMetadataItem()

    data class Note(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val note: MetadataNoteEntity
    ) : TaskMetadataItem()

    data class FileAttachment(
        override val metadataId: Long,
        override val taskId: Long,
        override val displayOrder: Int,
        val file: MetadataFileAttachmentEntity
    ) : TaskMetadataItem()
}
