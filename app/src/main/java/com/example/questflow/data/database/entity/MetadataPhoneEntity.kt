package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores phone number information for tasks
 * Can be linked to a contact or standalone
 */
@Entity(
    tableName = "metadata_phones",
    foreignKeys = [
        ForeignKey(
            entity = MetadataContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId")]
)
data class MetadataPhoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val contactId: Long? = null, // Optional link to contact
    val phoneNumber: String,
    val phoneType: PhoneType = PhoneType.MOBILE,
    val label: String? = null,
    val countryCode: String? = null,
    val isVerified: Boolean = false
)

enum class PhoneType {
    MOBILE,
    HOME,
    WORK,
    FAX,
    OTHER
}
