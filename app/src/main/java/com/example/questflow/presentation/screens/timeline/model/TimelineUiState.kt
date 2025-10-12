package com.example.questflow.presentation.screens.timeline.model

import androidx.compose.ui.geometry.Offset
import com.example.questflow.data.preferences.TimeRange
import com.example.questflow.domain.model.TimelineTask
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * UI State for timeline screen
 */
data class TimelineUiState(
    // Data
    val days: List<DayTimeline> = emptyList(),
    val focusedTask: TimelineTask? = null,

    // View settings
    val timeRange: TimeRange = TimeRange.THREE_DAYS,
    val viewStart: LocalDate = LocalDate.now().minusDays(1),
    val viewEnd: LocalDate = LocalDate.now().plusDays(1),

    // Display settings
    val toleranceMinutes: Int = 30,
    val hourRangeStart: Int = 6,
    val hourRangeEnd: Int = 22,
    val pixelsPerMinute: Float = 2f,
    val snapToGridMinutes: Int = 15,

    // Interaction state
    val dragState: DragState? = null,
    val selectedTask: TimelineTask? = null,
    val showSettings: Boolean = false,

    // Loading states
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Calculate total timeline width in pixels
     */
    fun calculateTimelineWidth(): Float {
        val totalHours = hourRangeEnd - hourRangeStart
        return totalHours * 60 * pixelsPerMinute
    }

    /**
     * Get all tasks across all days
     */
    fun getAllTasks(): List<TimelineTask> {
        return days.flatMap { it.tasks }
    }

    /**
     * Check if a specific day is today
     */
    fun isToday(date: LocalDate): Boolean {
        return date == LocalDate.now()
    }

    /**
     * Get tasks for a specific date
     */
    fun getTasksForDate(date: LocalDate): List<TimelineTask> {
        return days.find { it.date == date }?.tasks ?: emptyList()
    }
}

/**
 * Timeline data for a single day
 */
data class DayTimeline(
    val date: LocalDate,
    val tasks: List<TimelineTask>,
    val isToday: Boolean = false
) {
    /**
     * Get display label for the day
     */
    fun getDisplayLabel(): String {
        val dayOfWeek = date.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.SHORT,
            java.util.Locale.GERMAN
        )
        val dayOfMonth = date.dayOfMonth
        val month = date.monthValue

        return when {
            isToday -> "Heute\n$dayOfMonth.$month"
            date == LocalDate.now().plusDays(1) -> "Morgen\n$dayOfMonth.$month"
            date == LocalDate.now().minusDays(1) -> "Gestern\n$dayOfMonth.$month"
            else -> "$dayOfWeek\n$dayOfMonth.$month"
        }
    }

    /**
     * Count tasks by conflict state
     */
    fun countConflicts(): ConflictCounts {
        var overlaps = 0
        var warnings = 0
        var noConflict = 0

        tasks.forEach { task ->
            when (task.conflictState) {
                com.example.questflow.domain.model.ConflictState.OVERLAP -> overlaps++
                com.example.questflow.domain.model.ConflictState.TOLERANCE_WARNING -> warnings++
                com.example.questflow.domain.model.ConflictState.NO_CONFLICT -> noConflict++
            }
        }

        return ConflictCounts(overlaps, warnings, noConflict)
    }

    /**
     * Check if this day has any conflicts
     */
    fun hasConflicts(): Boolean {
        return tasks.any {
            it.conflictState != com.example.questflow.domain.model.ConflictState.NO_CONFLICT
        }
    }
}

/**
 * Conflict count statistics for a day
 */
data class ConflictCounts(
    val overlaps: Int,
    val warnings: Int,
    val noConflict: Int
) {
    val total: Int get() = overlaps + warnings + noConflict
    val hasConflicts: Boolean get() = overlaps > 0 || warnings > 0
}

/**
 * State during drag operation
 */
data class DragState(
    val task: TimelineTask,
    val originalStartTime: LocalDateTime,
    val originalEndTime: LocalDateTime,
    val currentOffset: Offset,
    val previewStartTime: LocalDateTime,
    val previewEndTime: LocalDateTime
) {
    /**
     * Calculate time offset in minutes
     */
    fun getTimeOffsetMinutes(): Long {
        return java.time.temporal.ChronoUnit.MINUTES.between(originalStartTime, previewStartTime)
    }

    /**
     * Check if position has changed
     */
    fun hasChanged(): Boolean {
        return originalStartTime != previewStartTime
    }
}

/**
 * Settings dialog state
 */
data class TimelineSettingsState(
    val toleranceMinutes: Int = 30,
    val timeRange: TimeRange = TimeRange.THREE_DAYS,
    val hourRangeStart: Int = 6,
    val hourRangeEnd: Int = 22,
    val pixelsPerMinute: Float = 2f,
    val snapToGridMinutes: Int = 15
) {
    /**
     * Validate and clamp values to valid ranges
     */
    fun validated(): TimelineSettingsState {
        return copy(
            toleranceMinutes = toleranceMinutes.coerceIn(0, 120),
            hourRangeStart = hourRangeStart.coerceIn(0, 23),
            hourRangeEnd = hourRangeEnd.coerceIn(hourRangeStart + 1, 24),
            pixelsPerMinute = pixelsPerMinute.coerceIn(0.5f, 10f),
            snapToGridMinutes = snapToGridMinutes.coerceIn(1, 60)
        )
    }
}
