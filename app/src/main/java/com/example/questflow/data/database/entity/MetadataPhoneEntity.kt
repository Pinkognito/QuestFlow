package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores phone number information for tasks
 */
@Entity(tableName = "metadata_phones")
data class MetadataPhoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

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
