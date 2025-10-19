package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user's working hours preferences
 * Defines the time range where tasks can be scheduled
 */
@Entity(tableName = "working_hours_settings")
data class WorkingHoursSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Single row - user settings
    val startHour: Int = 8, // Default: 8:00
    val startMinute: Int = 0,
    val endHour: Int = 22, // Default: 22:00
    val endMinute: Int = 0,
    val enabled: Boolean = true // Whether to use working hours filtering
)
