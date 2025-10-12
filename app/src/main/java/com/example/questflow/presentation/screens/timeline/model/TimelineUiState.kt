package com.example.questflow.presentation.screens.timeline.model

import androidx.compose.ui.geometry.Offset
import com.example.questflow.domain.model.TimelineTask
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * UI State for timeline screen.
 *
 * UPDATED: After refactoring to vertical time axis and infinite scroll:
 * - No more fixed time ranges
 * - Days load dynamically (infinite scroll)
 * - Always show full day (00:00-23:59)
 */
data class TimelineUiState(
    // Data
    val days: List<DayTimeline> = emptyList(),
    val focusedTask: TimelineTask? = null,

    // View settings (dynamic range)
    val viewStart: LocalDate = LocalDate.now().minusDays(3),
    val viewEnd: LocalDate = LocalDate.now().plusDays(3),

    // 3-day window offset (which 3 days to show)
    val dayWindowOffset: Int = 0, // 0 = today centered, -1 = shift left, +1 = shift right

    // Display settings
    val toleranceMinutes: Int = 30,
    val visibleHours: Float = 12f, // How many hours visible on screen (zoom level)
    val pixelsPerMinute: Float = 2f, // Calculated dynamically from visibleHours and screen height
    val snapToGridMinutes: Int = 15,

    // Interaction state
    val dragState: DragState? = null,
    val selectedTask: TimelineTask? = null,
    val showSettings: Boolean = false,

    // Multi-selection state
    val selectedTaskIds: Set<Long> = emptySet(),
    val customTaskOrder: List<Long> = emptyList(), // Manual ordering in selection list
    val selectionBox: SelectionBox? = null,
    val showSelectionList: Boolean = false,

    // Loading states
    val isLoading: Boolean = false,
    val isLoadingPast: Boolean = false,
    val isLoadingFuture: Boolean = false,
    val error: String? = null
) {
    /**
     * Calculate total timeline height in pixels (full 24 hours)
     */
    fun calculateTimelineHeight(): Float {
        return 24 * 60 * pixelsPerMinute
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

    /**
     * Get currently loaded date range
     */
    fun getLoadedRange(): ClosedRange<LocalDate> {
        if (days.isEmpty()) return LocalDate.now()..LocalDate.now()
        return days.first().date..days.last().date
    }

    /**
     * Get the 3 visible days based on dayWindowOffset
     */
    fun getVisibleDays(): List<DayTimeline> {
        val todayIndex = days.indexOfFirst { it.isToday }.coerceAtLeast(0)
        val centerIndex = todayIndex + dayWindowOffset
        val startIndex = (centerIndex - 1).coerceAtLeast(0)
        return days.drop(startIndex).take(3)
    }

    /**
     * Get selected tasks in custom order (if defined) or insertion order
     */
    fun getSelectedTasksOrdered(): List<TimelineTask> {
        val allTasks = getAllTasks()
        return if (customTaskOrder.isNotEmpty()) {
            // Use custom order
            customTaskOrder.mapNotNull { taskId ->
                allTasks.find { it.id == taskId }
            }
        } else {
            // Use selection order
            allTasks.filter { it.id in selectedTaskIds }
        }
    }

    /**
     * Get tasks within selection box time range
     */
    fun getTasksInSelectionBox(): List<TimelineTask> {
        val box = selectionBox ?: return emptyList()
        return getAllTasks().filter { task ->
            task.startTime >= box.startTime && task.endTime <= box.endTime
        }
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
 * State during drag operation (now VERTICAL drag)
 */
data class DragState(
    val task: TimelineTask,
    val originalStartTime: LocalDateTime,
    val originalEndTime: LocalDateTime,
    val currentOffsetY: Float, // Changed from Offset to just Y
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
 * Selection box defining time range for batch operations
 */
data class SelectionBox(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
) {
    /**
     * Get duration in minutes
     */
    fun durationMinutes(): Long {
        return java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime)
    }

    /**
     * Check if time range is valid
     */
    fun isValid(): Boolean {
        return startTime.isBefore(endTime)
    }
}
