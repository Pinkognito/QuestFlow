package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Stores saved filter/sort/group presets
 * Users can save their frequently used filter combinations
 */
@Entity(tableName = "task_filter_presets")
data class TaskFilterPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Preset metadata
    val name: String,
    val description: String = "",
    val isDefault: Boolean = false,  // If true, this preset is applied on app start
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // === SERIALIZED FILTER DATA (JSON format) ===
    // Using JSON string for flexible storage of complex filter objects
    val filterJson: String = "{}"
)
