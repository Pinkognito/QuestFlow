package com.example.questflow.domain.usecase

import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Calculates day occupancy for visual representation in month view calendar
 *
 * Converts ALL time sources into visual segments showing occupied (red) and free (green) time:
 * - CalendarEventLinkEntity: Google Calendar events with startsAt/endsAt
 * - TaskEntity: Tasks with dueDate (shown as occupied time)
 * - WorkingHours: Optional working hours restriction (default 8-22 Uhr)
 *
 * Example: 8-9 Uhr Google Event + 14-15 Uhr Task
 * Result: [
 *   TimeSegment(0f, 8f, false),    // 0-8: Grün (free)
 *   TimeSegment(8f, 9f, true),     // 8-9: Rot (Google Event)
 *   TimeSegment(9f, 14f, false),   // 9-14: Grün (free)
 *   TimeSegment(14f, 15f, true),   // 14-15: Rot (Task)
 *   TimeSegment(15f, 24f, false)   // 15-24: Grün (free)
 * ]
 */
class DayOccupancyCalculator @Inject constructor() {

    /**
     * Represents a time segment in a day
     * @param startHour Start hour (0.0 - 24.0)
     * @param endHour End hour (0.0 - 24.0)
     * @param isOccupied true = occupied (red/blue/purple), false = free (green)
     * @param hasOverlap true = multiple events/tasks overlap in this segment (black)
     * @param isOwnEvent true = this is your own task/event (blue), false = external event (red)
     * @param isCurrentTask true = this is the currently selected task (white)
     * @param isSameCategory true = this is a task from the same category as current task (yellow)
     */
    data class TimeSegment(
        val startHour: Float,
        val endHour: Float,
        val isOccupied: Boolean,
        val hasOverlap: Boolean = false,
        // Separate flags for each task type (can have multiple true)
        val hasCurrentTask: Boolean = false,       // Aktuell ausgewählter Task
        val hasSameCategory: Boolean = false,      // Task aus gleicher Kategorie
        val hasOtherOwnTasks: Boolean = false,     // Andere eigene Tasks
        val hasExternalEvents: Boolean = false,    // Externe Google Calendar Events
        // Legacy flags (deprecated but kept for compatibility)
        @Deprecated("Use hasOtherOwnTasks instead") val isOwnEvent: Boolean = false,
        @Deprecated("Use hasCurrentTask instead") val isCurrentTask: Boolean = false,
        @Deprecated("Use hasSameCategory instead") val isSameCategory: Boolean = false
    ) {
        val durationHours: Float get() = endHour - startHour
        val weightInDay: Float get() = durationHours / 24f
    }

    /**
     * Calculate occupancy segments for a specific day
     * @param events All calendar events (will be filtered by date)
     * @param date The day to analyze
     * @param tasks All tasks (will be filtered by dueDate)
     * @param currentTaskId ID of the currently selected task (for highlighting)
     * @param currentCategoryId Category ID of the currently selected task (for same-category highlighting)
     * @return List of time segments showing occupied/free periods
     */
    fun calculateDayOccupancy(
        events: List<CalendarEventLinkEntity>,
        date: LocalDate,
        tasks: List<TaskEntity> = emptyList(),
        currentTaskId: Long? = null,
        currentCategoryId: Long? = null
    ): List<TimeSegment> {
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()

        val occupiedSlots = mutableListOf<OccupiedSlot>()

        // 1. Add calendar events that overlap with this day
        // - taskId != null → OWN task (blue/white/yellow)
        // - taskId == null → EXTERNAL Google Calendar event (red)
        events.filter { event ->
            event.startsAt.isBefore(dayEnd) && event.endsAt.isAfter(dayStart)
        }.forEach { event ->
            val startHour = toHourFloat(event.startsAt, date)
            val endHour = toHourFloat(event.endsAt, date)
            val isOwnTask = event.taskId != null  // Has taskId → own task
            occupiedSlots.add(OccupiedSlot(
                startHour,
                endHour,
                isOwnEvent = isOwnTask,
                taskId = event.taskId,
                categoryId = event.categoryId
            ))
        }

        // 2. Add tasks with dueDate on this day (OWN - Tasks)
        // DEDUPLICATION: Only add tasks that DON'T already have a CalendarEventLink
        // to prevent false overlap detection (task showing as purple when it shouldn't)
        val taskIdsInEvents = events.mapNotNull { it.taskId }.toSet()

        tasks.filter { task ->
            task.dueDate != null &&
            task.dueDate.toLocalDate() == date &&
            !task.isCompleted &&
            task.id !in taskIdsInEvents  // EXCLUDE tasks already in CalendarEventLinks
        }.forEach { task ->
            val dueDateTime = task.dueDate!!
            val estimatedDuration = (task.estimatedMinutes ?: 60) / 60f // Default 1 hour

            val startHour = dueDateTime.hour + dueDateTime.minute / 60f
            val endHour = (startHour + estimatedDuration).coerceAtMost(24f)

            occupiedSlots.add(OccupiedSlot(
                startHour,
                endHour,
                isOwnEvent = true,
                taskId = task.id,
                categoryId = task.categoryId
            ))
        }

        // Filter invalid slots
        val validSlots = occupiedSlots.filter { it.start < it.end && it.end > 0f && it.start < 24f }

        if (validSlots.isEmpty()) {
            return listOf(TimeSegment(0f, 24f, false))
        }

        // Build segments WITH overlap detection (don't merge - detect overlaps!)
        return buildSegmentsWithOverlapDetection(validSlots, currentTaskId, currentCategoryId)
    }

    /**
     * Convert LocalDateTime to hour float for a specific date
     * Handles events spanning midnight
     */
    private fun toHourFloat(dateTime: LocalDateTime, targetDate: LocalDate): Float {
        val eventDate = dateTime.toLocalDate()

        return when {
            eventDate == targetDate -> {
                // Event is on target date
                dateTime.hour + dateTime.minute / 60f
            }
            eventDate.isBefore(targetDate) -> {
                // Event started before target date (spans midnight)
                0f
            }
            else -> {
                // Event ends after target date (spans midnight)
                24f
            }
        }
    }

    /**
     * Internal representation of occupied time slot
     */
    private data class OccupiedSlot(
        val start: Float,
        val end: Float,
        val isOwnEvent: Boolean = false,  // true = Task (own), false = Google Calendar (external)
        val taskId: Long? = null,  // ID of the task (null for external events)
        val categoryId: Long? = null  // Category ID of the task/event
    )

    /**
     * Merge overlapping time slots
     * Example: [6-10, 9-12] -> [6-12]
     */
    private fun mergeOverlappingSlots(slots: List<OccupiedSlot>): List<OccupiedSlot> {
        if (slots.isEmpty()) return emptyList()

        val sorted = slots.sortedBy { it.start }
        val merged = mutableListOf<OccupiedSlot>()

        var current = sorted.first()

        for (i in 1 until sorted.size) {
            val next = sorted[i]

            if (next.start <= current.end) {
                // Overlapping or adjacent - merge
                current = OccupiedSlot(
                    current.start,
                    maxOf(current.end, next.end)
                )
            } else {
                // Gap - save current and start new
                merged.add(current)
                current = next
            }
        }

        merged.add(current)
        return merged
    }

    /**
     * Build segments WITH overlap detection and category highlighting
     * Color coding:
     * - Weiß (white): Current task (isCurrentTask = true)
     * - Gelb (yellow): Same category as current task (isSameCategory = true)
     * - Blau (blue): Other own tasks (isOwnEvent = true, no category match)
     * - Rot (red): External Google Calendar events (!isOwnEvent)
     * - Schwarz (black): Overlap (hasOverlap = true)
     */
    private fun buildSegmentsWithOverlapDetection(
        slots: List<OccupiedSlot>,
        currentTaskId: Long?,
        currentCategoryId: Long?
    ): List<TimeSegment> {
        android.util.Log.d("DayOccupancy", "🎨 5-Color Debug: currentTaskId=$currentTaskId, currentCategoryId=$currentCategoryId")
        android.util.Log.d("DayOccupancy", "🎨 Slots: ${slots.joinToString { "taskId=${it.taskId}, catId=${it.categoryId}, isOwn=${it.isOwnEvent}" }}")

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
                // Determine all distinct task types in this segment
                // IMPORTANT: Use specific categories for each slot, not just booleans
                val distinctTypes = mutableSetOf<String>()

                overlappingSlots.forEach { slot ->
                    // Classify each slot into exactly ONE type (priority order)
                    val slotType = when {
                        // Priority 1: Current task (white)
                        currentTaskId != null && slot.taskId == currentTaskId ->
                            "CURRENT_TASK"
                        // Priority 2: Same category task (yellow)
                        currentCategoryId != null && slot.categoryId == currentCategoryId && slot.isOwnEvent ->
                            "SAME_CATEGORY"
                        // Priority 3: Other own task (blue)
                        slot.isOwnEvent ->
                            "OTHER_OWN_TASK"
                        // Priority 4: External event (red)
                        else ->
                            "EXTERNAL_EVENT"
                    }
                    distinctTypes.add(slotType)
                }

                // OVERLAP = 2 or more distinct task types in same time segment
                val hasOverlap = distinctTypes.size >= 2

                // Determine which task types are present in this segment
                val hasCurrentTaskType = currentTaskId != null &&
                    overlappingSlots.any { it.taskId == currentTaskId }

                val hasSameCategoryType = currentCategoryId != null &&
                    overlappingSlots.any {
                        it.categoryId == currentCategoryId &&
                        it.isOwnEvent &&
                        it.taskId != currentTaskId  // Exclude current task
                    }

                val hasOtherOwnTasksType = overlappingSlots.any {
                    it.isOwnEvent &&
                    it.taskId != currentTaskId &&
                    (currentCategoryId == null || it.categoryId != currentCategoryId)
                }

                val hasExternalEventsType = overlappingSlots.any { !it.isOwnEvent }

                // Legacy flags for compatibility
                val isCurrentTask = hasCurrentTaskType
                val isSameCategory = !hasCurrentTaskType && hasSameCategoryType
                val hasOwnEvents = overlappingSlots.any { it.isOwnEvent }
                val hasExternalEvents = overlappingSlots.any { !it.isOwnEvent }

                val color = when {
                    hasOverlap -> "⚫ SCHWARZ (Overlap: ${distinctTypes.size} types)"
                    hasCurrentTaskType -> "⚪ WEISS (Current)"
                    hasSameCategoryType -> "🟡 GELB (Category)"
                    hasOtherOwnTasksType -> "🔵 BLAU (Own)"
                    else -> "🔴 ROT (External)"
                }
                android.util.Log.d("DayOccupancy", "  [$start-$end] → $color | Types: $distinctTypes | Flags: cur=$hasCurrentTaskType sam=$hasSameCategoryType oth=$hasOtherOwnTasksType ext=$hasExternalEventsType")

                segments.add(
                    TimeSegment(
                        start,
                        end,
                        isOccupied = true,
                        hasOverlap = hasOverlap,
                        // NEW separate flags
                        hasCurrentTask = hasCurrentTaskType,
                        hasSameCategory = hasSameCategoryType,
                        hasOtherOwnTasks = hasOtherOwnTasksType,
                        hasExternalEvents = hasExternalEventsType,
                        // LEGACY flags (for compatibility)
                        isOwnEvent = hasOwnEvents && !hasExternalEvents,
                        isCurrentTask = isCurrentTask,
                        isSameCategory = isSameCategory
                    )
                )
            }
        }

        return segments
    }

    /**
     * Build final segment list with occupied (red) and free (green) periods
     * LEGACY - kept for compatibility
     */
    private fun buildSegments(occupiedSlots: List<OccupiedSlot>): List<TimeSegment> {
        if (occupiedSlots.isEmpty()) {
            return listOf(TimeSegment(0f, 24f, false))
        }

        val segments = mutableListOf<TimeSegment>()
        var currentHour = 0f

        for (slot in occupiedSlots) {
            // Clamp slot times to valid day range (0-24)
            val clampedStart = slot.start.coerceIn(0f, 24f)
            val clampedEnd = slot.end.coerceIn(0f, 24f)

            // Skip invalid segments
            if (clampedStart >= clampedEnd) continue

            // Add free segment before occupied slot (if any)
            if (currentHour < clampedStart) {
                segments.add(TimeSegment(currentHour, clampedStart, false))
            }

            // Add occupied segment (only if it has positive duration)
            if (clampedEnd > clampedStart) {
                segments.add(TimeSegment(clampedStart, clampedEnd, true))
                currentHour = clampedEnd
            }
        }

        // Add final free segment (if any)
        if (currentHour < 24f) {
            segments.add(TimeSegment(currentHour, 24f, false))
        }

        return segments
    }
}
