package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores postal address information for tasks
 * Can be linked to a contact or standalone
 */
@Entity(
    tableName = "metadata_addresses",
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
data class MetadataAddressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val contactId: Long? = null, // Optional link to contact
    val street: String,
    val houseNumber: String? = null,
    val addressLine2: String? = null,
    val city: String,
    val postalCode: String,
    val state: String? = null,
    val country: String,

    val addressType: AddressType = AddressType.OTHER,
    val label: String? = null
)

enum class AddressType {
    HOME,
    WORK,
    BILLING,
    SHIPPING,
    OTHER
}
