package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores email address information for tasks
 * Can be linked to a contact or standalone
 */
@Entity(
    tableName = "metadata_emails",
    foreignKeys = [
        ForeignKey(
            entity = MetadataContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("emailAddress"), Index("contactId")]
)
data class MetadataEmailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val contactId: Long? = null, // Optional link to contact
    val emailAddress: String,
    val emailType: EmailType = EmailType.PERSONAL,
    val label: String? = null,
    val isVerified: Boolean = false
)

enum class EmailType {
    PERSONAL,
    WORK,
    OTHER
}
