package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Globale Statistiken für Tag-Nutzung
 * Für Auto-Suggest und Ranking
 */
@Entity(
    tableName = "tag_usage_stats",
    indices = [Index("tag"), Index("usageCount")]
)
data class TagUsageStatsEntity(
    @PrimaryKey
    val tag: String,                        // Tag-Name (unique)

    val usageCount: Int = 0,                // Wie oft wurde dieser Tag gesamt verwendet?
    val lastUsedAt: LocalDateTime? = null,  // Wann zuletzt verwendet?

    val createdAt: LocalDateTime = LocalDateTime.now()
)
