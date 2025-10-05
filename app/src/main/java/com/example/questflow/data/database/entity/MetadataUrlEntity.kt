package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores URL/link information for tasks
 */
@Entity(tableName = "metadata_urls")
data class MetadataUrlEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,
    val title: String? = null,
    val description: String? = null,
    val faviconUrl: String? = null,
    val urlType: UrlType = UrlType.OTHER
)

enum class UrlType {
    WEBSITE,
    SOCIAL_MEDIA,
    DOCUMENTATION,
    VIDEO,
    OTHER
}
