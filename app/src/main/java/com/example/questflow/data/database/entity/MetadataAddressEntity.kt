package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores postal address information for tasks
 */
@Entity(tableName = "metadata_addresses")
data class MetadataAddressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

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
