package com.example.questflow.presentation.screens.timeline.model

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
    val edgeBorderWidthDp: Float = 30f, // Edge detection border width for auto-scroll (TUNED: 80dp → 30dp for screen edges only)

    // Absolute coordinate calculation (for multi-day drag)
    val currentScrollOffsetPx: Float = 0f, // Current scroll position in pixels
    val headerHeightPx: Float = 144f, // Header height in pixels (default: 48dp × 3.0 density)
    val timeColumnWidthPx: Float = 180f, // Time column width in pixels (default: 60dp × 3.0 density)
    val contentWidthPx: Float = 900f, // Content area width in pixels (screen width - timeColumnWidth)
    val density: Float = 3.0f, // Screen density for DP→PX conversion in coordinate calculations

    // Interaction state
    val selectedTask: TimelineTask? = null,
    val showSettings: Boolean = false,

    // Multi-selection state
    val selectedTaskIds: Set<Long> = emptySet(),
    val customTaskOrder: List<Long> = emptyList(), // Manual ordering in selection list
    val selectionBox: SelectionBox? = null,
    val showSelectionList: Boolean = false,

    // Drag-to-select state (live preview during drag)
    val dragSelectionState: DragSelectionState? = null,

    // Loading states
    val isLoading: Boolean = false,
    val isLoadingPast: Boolean = false,
    val isLoadingFuture: Boolean = false,
    val error: String? = null,

    // Debug state for gesture visualization
    val gestureDebugInfo: GestureDebugInfo? = null,

    // Context menu state (radial menu after drag release)
    val contextMenu: ContextMenuState? = null
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

/**
 * Drag selection state for live preview during drag-to-select gesture
 */
data class DragSelectionState(
    val startTime: LocalDateTime,
    val currentTime: LocalDateTime,
    val isActive: Boolean = true
) {
    /**
     * Convert to SelectionBox with proper start/end ordering
     */
    fun toSelectionBox(): SelectionBox {
        return if (startTime.isBefore(currentTime)) {
            SelectionBox(startTime, currentTime)
        } else {
            SelectionBox(currentTime, startTime)
        }
    }
}

/**
 * Debug info for gesture visualization with history
 */
data class GestureDebugInfo(
    val gestureType: String, // "WAITING", "VERTICAL_SCROLL", "HORIZONTAL_SWIPE", "LONG_PRESS", "DRAGGING"
    val elapsedMs: Long,
    val dragX: Float,
    val dragY: Float,
    val message: String,
    val history: List<String> = emptyList(), // Last 5 gesture states
    val atEdge: EdgePosition? = null // Which edge is currently touched (for auto-scroll)
)

/**
 * Edge position for auto-scroll triggering
 */
enum class EdgePosition {
    LEFT, RIGHT, TOP, BOTTOM, NONE
}

/**
 * Context menu state for radial menu after drag selection
 */
data class ContextMenuState(
    val centerX: Float, // Screen position where finger was released
    val centerY: Float,
    val selectionBox: SelectionBox,
    val selectedTasksInBox: Int, // Number of tasks within the selection box
    val menuType: ContextMenuType
)

/**
 * Type of context menu based on selection context
 */
enum class ContextMenuType {
    SELECTION_WITH_TASKS,   // SelectionBox contains tasks → Insert/Delete/Edit options
    SELECTION_EMPTY,        // SelectionBox is empty → Create/Insert/Edit options
    SINGLE_TASK             // Long-press on single task → Complete/Edit/Delete options
}

/**
 * Context menu action definition
 */
data class ContextMenuAction(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val angle: Float, // Angle in degrees: 0°=right, 90°=top, 180°=left, 270°=bottom
    val color: androidx.compose.ui.graphics.Color? = null
)
