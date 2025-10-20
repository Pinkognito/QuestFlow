package com.example.questflow.domain.usecase

import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.domain.model.CalendarColorConfig
import com.example.questflow.domain.model.TaskType
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Enhanced Day Occupancy Calculator with 10 Task Types
 *
 * Supports full task type classification and customizable color configuration:
 * 1. OWN_TASK - Currently selected/edited task
 * 2. PARENT_TASK - Parent of current task
 * 3. SUBTASK - Subtask of current task
 * 4. SAME_CATEGORY - Task from same category
 * 5. OTHER_CATEGORY - Task from different category
 * 6. NO_CATEGORY - Task without category
 * 7. EXPIRED_TASK - Task past due date
 * 8. COMPLETED_TASK - Completed task
 * 9. EXTERNAL_EVENT - Google Calendar event
 * 10. OVERLAP - Conflict between tasks/events
 */
class DayOccupancyCalculatorV2 @Inject constructor() {

    /**
     * Represents a time segment in a day with task type classification
     */
    data class TimeSegment(
        val startHour: Float,
        val endHour: Float,
        val isOccupied: Boolean,
        val taskType: TaskType? = null,  // Type of task in this segment
        val categoryId: Long? = null      // Category ID for CATEGORY color mode
    ) {
        val durationHours: Float get() = endHour - startHour
        val weightInDay: Float get() = durationHours / 24f
    }

    /**
     * Internal representation of occupied time slot with metadata
     */
    private data class OccupiedSlot(
        val start: Float,
        val end: Float,
        val taskId: Long? = null,
        val parentTaskId: Long? = null,
        val categoryId: Long? = null,
        val isOwnEvent: Boolean = false,  // true = app task, false = external event
        val isCompleted: Boolean = false,
        val isExpired: Boolean = false
    )

    /**
     * Calculate occupancy segments for a specific day
     *
     * @param events All calendar events (will be filtered by date)
     * @param date The day to analyze
     * @param tasks All tasks (will be filtered by dueDate)
     * @param currentTaskId ID of the currently selected task
     * @param currentCategoryId Category ID of the currently selected task
     * @param currentParentId Parent ID of the currently selected task
     * @param colorConfig Color configuration for filtering/display
     * @return List of time segments with task type classification
     */
    fun calculateDayOccupancy(
        events: List<CalendarEventLinkEntity>,
        date: LocalDate,
        tasks: List<TaskEntity> = emptyList(),
        currentTaskId: Long? = null,
        currentCategoryId: Long? = null,
        currentParentId: Long? = null,
        colorConfig: CalendarColorConfig = CalendarColorConfig.default()
    ): List<TimeSegment> {
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        val now = LocalDateTime.now()

        val occupiedSlots = mutableListOf<OccupiedSlot>()

        // 1. Add calendar events that overlap with this day
        events.filter { event ->
            event.startsAt.isBefore(dayEnd) && event.endsAt.isAfter(dayStart)
        }.forEach { event ->
            val startHour = toHourFloat(event.startsAt, date)
            val endHour = toHourFloat(event.endsAt, date)
            val isOwnTask = event.taskId != null

            // Check if task is expired (event ended before now and not claimed)
            val isExpired = event.endsAt.isBefore(now) &&
                           event.status == "PENDING"

            occupiedSlots.add(
                OccupiedSlot(
                    startHour,
                    endHour,
                    taskId = event.taskId,
                    categoryId = event.categoryId,
                    isOwnEvent = isOwnTask,
                    isCompleted = event.status == "CLAIMED",
                    isExpired = isExpired
                )
            )
        }

        // 2. Add tasks with dueDate on this day (deduplicated)
        val taskIdsInEvents = events.mapNotNull { it.taskId }.toSet()

        tasks.filter { task ->
            task.dueDate != null &&
            task.dueDate.toLocalDate() == date &&
            task.id !in taskIdsInEvents  // Avoid duplicates
        }.forEach { task ->
            val dueDateTime = task.dueDate!!
            val estimatedDuration = (task.estimatedMinutes ?: 60) / 60f

            val startHour = dueDateTime.hour + dueDateTime.minute / 60f
            val endHour = (startHour + estimatedDuration).coerceAtMost(24f)

            // Check if task is expired (past due date and not completed)
            val isExpired = dueDateTime.isBefore(now) && !task.isCompleted

            occupiedSlots.add(
                OccupiedSlot(
                    startHour,
                    endHour,
                    taskId = task.id,
                    parentTaskId = task.parentTaskId,
                    categoryId = task.categoryId,
                    isOwnEvent = true,
                    isCompleted = task.isCompleted,
                    isExpired = isExpired
                )
            )
        }

        // Filter invalid slots
        val validSlots = occupiedSlots.filter {
            it.start < it.end && it.end > 0f && it.start < 24f
        }

        if (validSlots.isEmpty()) {
            return listOf(TimeSegment(0f, 24f, isOccupied = false))
        }

        // Build segments with task type detection and color config filtering
        return buildSegmentsWithTaskTypes(
            validSlots,
            currentTaskId,
            currentCategoryId,
            currentParentId,
            colorConfig
        )
    }

    /**
     * Convert LocalDateTime to hour float for a specific date
     */
    private fun toHourFloat(dateTime: LocalDateTime, targetDate: LocalDate): Float {
        val eventDate = dateTime.toLocalDate()

        return when {
            eventDate == targetDate -> dateTime.hour + dateTime.minute / 60f
            eventDate.isBefore(targetDate) -> 0f
            else -> 24f
        }
    }

    /**
     * Build segments with task type classification and color config filtering
     */
    private fun buildSegmentsWithTaskTypes(
        slots: List<OccupiedSlot>,
        currentTaskId: Long?,
        currentCategoryId: Long?,
        currentParentId: Long?,
        colorConfig: CalendarColorConfig
    ): List<TimeSegment> {
        if (slots.isEmpty()) {
            return listOf(TimeSegment(0f, 24f, isOccupied = false))
        }

        // Collect all time boundaries
        val boundaries = mutableSetOf<Float>()
        boundaries.add(0f)
        boundaries.add(24f)
        slots.forEach {
            boundaries.add(it.start.coerceIn(0f, 24f))
            boundaries.add(it.end.coerceIn(0f, 24f))
        }

        val sortedBoundaries = boundaries.sorted()
        val segments = mutableListOf<TimeSegment>()

        // For each time interval between boundaries
        for (i in 0 until sortedBoundaries.size - 1) {
            val start = sortedBoundaries[i]
            val end = sortedBoundaries[i + 1]

            // Find all slots that overlap this interval
            val overlappingSlots = slots.filter { slot ->
                slot.start < end && slot.end > start
            }

            if (overlappingSlots.isEmpty()) {
                // Free time
                segments.add(TimeSegment(start, end, isOccupied = false))
            } else {
                // Determine task type based on priority
                val taskType = determineTaskType(
                    overlappingSlots,
                    currentTaskId,
                    currentCategoryId,
                    currentParentId
                )

                // Check if this task type should be displayed
                val setting = colorConfig.getSettingForType(taskType)
                if (setting.enabled) {
                    // Get category ID for CATEGORY color mode
                    val categoryId = overlappingSlots.firstOrNull()?.categoryId

                    segments.add(
                        TimeSegment(
                            start,
                            end,
                            isOccupied = true,
                            taskType = taskType,
                            categoryId = categoryId
                        )
                    )
                }
                // If not enabled, treat as free time
                else {
                    segments.add(TimeSegment(start, end, isOccupied = false))
                }
            }
        }

        return segments
    }

    /**
     * Determine task type based on priority rules
     */
    private fun determineTaskType(
        overlappingSlots: List<OccupiedSlot>,
        currentTaskId: Long?,
        currentCategoryId: Long?,
        currentParentId: Long?
    ): TaskType {
        val hasOwnEvents = overlappingSlots.any { it.isOwnEvent }
        val hasExternalEvents = overlappingSlots.any { !it.isOwnEvent }

        // Priority 1: OVERLAP (highest priority)
        if (hasOwnEvents && hasExternalEvents) {
            return TaskType.OVERLAP
        }

        // Priority 2: COMPLETED_TASK
        if (overlappingSlots.any { it.isCompleted }) {
            return TaskType.COMPLETED_TASK
        }

        // Priority 3: EXPIRED_TASK
        if (overlappingSlots.any { it.isExpired }) {
            return TaskType.EXPIRED_TASK
        }

        // Priority 4: OWN_TASK (currently selected task)
        if (currentTaskId != null && overlappingSlots.any { it.taskId == currentTaskId }) {
            return TaskType.OWN_TASK
        }

        // Priority 5: PARENT_TASK (parent of current task)
        if (currentParentId != null && overlappingSlots.any { it.taskId == currentParentId }) {
            return TaskType.PARENT_TASK
        }

        // Priority 6: SUBTASK (subtask of current task)
        if (currentTaskId != null && overlappingSlots.any { it.parentTaskId == currentTaskId }) {
            return TaskType.SUBTASK
        }

        // Priority 7: EXTERNAL_EVENT (Google Calendar)
        if (hasExternalEvents) {
            return TaskType.EXTERNAL_EVENT
        }

        // Priority 8-10: Own app tasks by category
        if (hasOwnEvents) {
            return when {
                // SAME_CATEGORY
                currentCategoryId != null &&
                overlappingSlots.any { it.categoryId == currentCategoryId } ->
                    TaskType.SAME_CATEGORY

                // NO_CATEGORY
                overlappingSlots.any { it.categoryId == null } ->
                    TaskType.NO_CATEGORY

                // OTHER_CATEGORY
                else -> TaskType.OTHER_CATEGORY
            }
        }

        // Fallback (should not happen)
        return TaskType.OTHER_CATEGORY
    }
}
