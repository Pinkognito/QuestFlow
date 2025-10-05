package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores location/place information for tasks
 * Supports Google Places integration
 */
@Entity(
    tableName = "metadata_locations",
    indices = [Index("placeId")]
)
data class MetadataLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Google Places Integration
    val placeId: String? = null,
    val placeName: String,

    // Coordinates
    val latitude: Double,
    val longitude: Double,

    // Address Details
    val formattedAddress: String? = null,
    val street: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null,

    // UI
    val iconUrl: String? = null,
    val customLabel: String? = null
)
