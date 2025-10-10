package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores contact information for tasks
 * Can reference Android system contacts or be standalone
 */
@Entity(
    tableName = "metadata_contacts",
    indices = [Index("systemContactId"), Index("lookupKey")]
)
data class MetadataContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Android Contacts Integration
    val systemContactId: String? = null,
    val lookupKey: String? = null,

    // Basic Info
    val displayName: String,
    val givenName: String? = null,
    val familyName: String? = null,

    // Communication
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,

    // Additional
    @Deprecated("Use photoMediaId instead for streamed media")
    val photoUri: String? = null,
    val photoMediaId: String? = null,     // Reference to MediaLibraryEntity for streamed photo
    val iconEmoji: String? = null,        // Emoji/Text icon for calendar identification (e.g. 🐟 for Tom)
    val organization: String? = null,
    val jobTitle: String? = null,
    val note: String? = null
)
