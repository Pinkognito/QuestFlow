package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores email address information for tasks
 */
@Entity(
    tableName = "metadata_emails",
    indices = [Index("emailAddress")]
)
data class MetadataEmailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

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
