package com.example.questflow.domain.model

import java.time.LocalDateTime

/**
 * Represents a task in the timeline view with time boundaries and conflict information.
 * Combines data from Task and CalendarEventLink entities for timeline rendering.
 */
data class TimelineTask(
    val id: Long,                              // Unique timeline task ID
    val taskId: Long?,                         // Associated Task ID (null for calendar-only events)
    val linkId: Long?,                         // Associated CalendarEventLink ID
    val title: String,                         // Task title
    val description: String = "",              // Task description
    val startTime: LocalDateTime,              // Start time of the task
    val endTime: LocalDateTime,                // End time of the task
    val xpPercentage: Int = 60,               // Difficulty percentage (20-100)
    val categoryId: Long? = null,             // Associated category
    val categoryColor: String? = null,        // Category color hex
    val categoryEmoji: String? = null,        // Category emoji
    val conflictState: ConflictState = ConflictState.NO_CONFLICT,  // Overlap status
    val isCompleted: Boolean = false,         // Completion status
    val calendarEventId: Long? = null        // Associated calendar event ID
) {
    /**
     * Calculate duration in minutes
     */
    fun durationMinutes(): Long {
        return java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime)
    }

    /**
     * Check if task spans multiple days
     */
    fun isMultiDay(): Boolean {
        return startTime.toLocalDate() != endTime.toLocalDate()
    }

    /**
     * Get display color based on conflict state and category
     */
    fun getDisplayColor(): String {
        return when (conflictState) {
            ConflictState.OVERLAP -> "#FF5252"           // Red - direct overlap
            ConflictState.TOLERANCE_WARNING -> "#2196F3" // Blue - tolerance warning
            ConflictState.NO_CONFLICT -> categoryColor ?: "#4CAF50"  // Green/Category color
        }
    }
}

/**
 * Represents the conflict state of a timeline task
 */
enum class ConflictState {
    /**
     * No conflict - task has sufficient spacing (green)
     */
    NO_CONFLICT,

    /**
     * Tolerance warning - gap between tasks is less than tolerance (blue)
     */
    TOLERANCE_WARNING,

    /**
     * Direct overlap - tasks overlap in time (red)
     */
    OVERLAP
}
