package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarEvent
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.repository.TimeBlockRepository
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * Use case to detect scheduling conflicts with existing calendar events, TimeBlocks AND QuestFlow Tasks
 * across ALL calendars (Google Calendar, QuestFlow, Outlook, etc.) plus TimeBlocks and Tasks
 */
class DetectScheduleConflictsUseCase @Inject constructor(
    private val calendarManager: CalendarManager,
    private val timeBlockRepository: TimeBlockRepository,
    private val calendarEventLinkDao: com.example.questflow.data.database.dao.CalendarEventLinkDao
) {
    /**
     * Check if a time slot conflicts with existing events, TimeBlocks AND QuestFlow Tasks
     * @param startTime Start of the proposed time slot
     * @param endTime End of the proposed time slot
     * @param excludeEventId Optional event ID to exclude (for editing existing calendar events)
     * @param excludeTaskId Optional task ID to exclude (for editing existing tasks)
     * @param excludeLinkId Optional CalendarEventLink ID to exclude (for editing existing calendar links)
     * @return List of conflicting events (empty if no conflicts) - includes calendar events, TimeBlocks AND QuestFlow Tasks converted to CalendarEvent
     */
    suspend operator fun invoke(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ): List<CalendarEvent> {
        android.util.Log.d("ConflictDetection", "====== CONFLICT CHECK START ======")
        android.util.Log.d("ConflictDetection", "Checking: $startTime → $endTime")
        android.util.Log.d("ConflictDetection", "Exclude: linkId=$excludeLinkId, taskId=$excludeTaskId, eventId=$excludeEventId")

        // Query all calendar events in the date range
        val startDate = startTime.toLocalDate()
        val endDate = endTime.toLocalDate()

        val allEvents = calendarManager.getAllCalendarEvents(startDate, endDate)

        // Filter for actual event conflicts
        val eventConflicts = allEvents.filter { event ->
            // Skip if this is the event we're editing
            if (excludeEventId != null && event.id == excludeEventId) {
                return@filter false
            }

            // Check for time overlap
            // Events overlap if: start1 < end2 AND start2 < end1
            val hasOverlap = startTime < event.endTime && endTime > event.startTime

            hasOverlap
        }

        // Check for TimeBlock conflicts
        val activeTimeBlocks = timeBlockRepository.getActiveTimeBlocks()
        val timeBlockConflicts = mutableListOf<CalendarEvent>()

        // Check each day in the range
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayStart = LocalDateTime.of(currentDate, LocalTime.MIN)
            val dayEnd = LocalDateTime.of(currentDate, LocalTime.MAX)

            // Only check TimeBlocks that are active on this specific date
            activeTimeBlocks
                .filter { it.isActiveOn(currentDate) }
                .forEach { timeBlock ->
                    if (timeBlock.allDay) {
                        // All-day TimeBlock conflicts with any time on this day
                        if (startTime.toLocalDate() == currentDate || endTime.toLocalDate() == currentDate) {
                            timeBlockConflicts.add(convertTimeBlockToCalendarEvent(timeBlock, dayStart, dayEnd))
                        }
                    } else if (timeBlock.startTime != null && timeBlock.endTime != null) {
                        // Specific time window
                        val blockStartTime = parseTimeString(timeBlock.startTime)
                        val blockEndTime = parseTimeString(timeBlock.endTime)

                        if (blockStartTime != null && blockEndTime != null) {
                            val blockStart = LocalDateTime.of(currentDate, blockStartTime)
                            val blockEnd = LocalDateTime.of(currentDate, blockEndTime)

                            // Check for overlap with this TimeBlock
                            val hasOverlap = startTime < blockEnd && endTime > blockStart
                            if (hasOverlap) {
                                timeBlockConflicts.add(convertTimeBlockToCalendarEvent(timeBlock, blockStart, blockEnd))
                            }
                        }
                    }
                }

            currentDate = currentDate.plusDays(1)
        }

        // Check for QuestFlow Task conflicts (CalendarEventLinks)
        val taskConflicts = mutableListOf<CalendarEvent>()
        val startDateTimeString = startTime.toString()
        val endDateTimeString = endTime.plusDays(1).toString() // Add 1 day to include endTime

        val tasksInRange = calendarEventLinkDao.getEventsInRangeSync(startDateTimeString, endDateTimeString)
        android.util.Log.d("ConflictDetection", "Found ${tasksInRange.size} tasks in range")

        tasksInRange.forEach { task ->
            android.util.Log.d("ConflictDetection", "Checking Task: id=${task.id}, taskId=${task.taskId}, ${task.startsAt} → ${task.endsAt}, title='${task.title}'")

            // Skip if this is the CalendarEventLink we're editing (primary exclusion)
            if (excludeLinkId != null && task.id == excludeLinkId) {
                android.util.Log.d("ConflictDetection", "  ✓ SKIP: Matches excludeLinkId")
                return@forEach
            }

            // Skip if this is the task we're editing (for tasks with multiple links)
            if (excludeTaskId != null && task.taskId == excludeTaskId) {
                android.util.Log.d("ConflictDetection", "  ✓ SKIP: Matches excludeTaskId")
                return@forEach
            }

            // Skip if this is linked to the calendar event we're editing (for synced events)
            if (excludeEventId != null && task.calendarEventId == excludeEventId) {
                android.util.Log.d("ConflictDetection", "  ✓ SKIP: Matches excludeEventId")
                return@forEach
            }

            // CRITICAL FIX: Skip if this link has EXACTLY the same times as what we're checking
            // This prevents finding "ourselves" when editing times creates temporary duplicates
            if (task.startsAt == startTime && task.endsAt == endTime) {
                android.util.Log.d("ConflictDetection", "  ✓ SKIP: Exact time match (self-exclusion)")
                return@forEach
            }

            // Check for time overlap
            // Tasks overlap if: start1 < end2 AND start2 < end1
            val hasOverlap = startTime < task.endsAt && endTime > task.startsAt

            if (hasOverlap) {
                android.util.Log.d("ConflictDetection", "  ❌ CONFLICT: Task overlaps!")
                taskConflicts.add(convertTaskToCalendarEvent(task))
            } else {
                android.util.Log.d("ConflictDetection", "  ✓ NO overlap")
            }
        }

        // Combine event, TimeBlock AND Task conflicts
        val allConflicts = eventConflicts + timeBlockConflicts + taskConflicts
        android.util.Log.d("ConflictDetection", "====== RESULT: ${allConflicts.size} total conflicts ======")
        allConflicts.forEach { conflict ->
            android.util.Log.d("ConflictDetection", "  - ${conflict.title} (${conflict.startTime} → ${conflict.endTime})")
        }
        return allConflicts
    }

    /**
     * Check if a TimeBlock is active on a specific date based on its recurrence rules
     */
    private fun com.example.questflow.data.database.entity.TimeBlockEntity.isActiveOn(date: java.time.LocalDate): Boolean {
        // Check validity period
        if (validFrom != null) {
            val validFrom = java.time.LocalDate.parse(validFrom)
            if (date.isBefore(validFrom)) return false
        }
        if (validUntil != null) {
            val validUntil = java.time.LocalDate.parse(validUntil)
            if (date.isAfter(validUntil)) return false
        }

        // Check specific dates (highest priority)
        if (!specificDates.isNullOrBlank()) {
            val specificDates = specificDates.split(",").map { it.trim() }
            if (specificDates.contains(date.toString())) {
                return true
            }
        }

        // Check day of week (1=Monday, 7=Sunday)
        if (!daysOfWeek.isNullOrBlank()) {
            val daysOfWeek = daysOfWeek.split(",").map { it.trim().toIntOrNull() }
            val dayOfWeek = date.dayOfWeek.value  // Monday=1, Sunday=7
            if (daysOfWeek.contains(dayOfWeek)) {
                return true
            }
        }

        // Check day of month (1-31)
        if (!daysOfMonth.isNullOrBlank()) {
            val daysOfMonth = daysOfMonth.split(",").map { it.trim().toIntOrNull() }
            val dayOfMonth = date.dayOfMonth
            if (daysOfMonth.contains(dayOfMonth)) {
                return true
            }
        }

        // Check months (1=January, 12=December)
        if (!monthsOfYear.isNullOrBlank()) {
            val monthsOfYear = monthsOfYear.split(",").map { it.trim().toIntOrNull() }
            val monthValue = date.monthValue
            if (monthsOfYear.contains(monthValue)) {
                return true
            }
        }

        // If no specific recurrence rules are set, match all dates within validity period
        if (daysOfWeek.isNullOrBlank() &&
            daysOfMonth.isNullOrBlank() &&
            monthsOfYear.isNullOrBlank() &&
            specificDates.isNullOrBlank()) {
            return true
        }

        return false
    }

    /**
     * Parse time string in format "HH:mm:ss" or "HH:mm" to LocalTime
     */
    private fun parseTimeString(timeString: String): LocalTime? {
        return try {
            when {
                timeString.matches(Regex("\\d{2}:\\d{2}:\\d{2}")) -> {
                    // HH:mm:ss format
                    val parts = timeString.split(":")
                    LocalTime.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }
                timeString.matches(Regex("\\d{2}:\\d{2}")) -> {
                    // HH:mm format
                    val parts = timeString.split(":")
                    LocalTime.of(parts[0].toInt(), parts[1].toInt())
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert a TimeBlock to a CalendarEvent for conflict reporting
     */
    private fun convertTimeBlockToCalendarEvent(
        timeBlock: com.example.questflow.data.database.entity.TimeBlockEntity,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): CalendarEvent {
        return CalendarEvent(
            id = -timeBlock.id, // Negative ID to distinguish from real calendar events
            title = timeBlock.name ?: "Blockierte Zeit",
            description = timeBlock.description ?: "",
            startTime = startTime,
            endTime = endTime,
            calendarId = -1L, // Special calendar ID for TimeBlocks
            calendarName = "TimeBlock",
            isExternal = false
        )
    }

    /**
     * Convert a QuestFlow Task (CalendarEventLinkEntity) to a CalendarEvent for conflict reporting
     */
    private fun convertTaskToCalendarEvent(
        task: com.example.questflow.data.database.entity.CalendarEventLinkEntity
    ): CalendarEvent {
        return CalendarEvent(
            id = task.id, // Use the CalendarEventLink ID (positive to distinguish from TimeBlocks)
            title = task.title,
            description = "", // CalendarEventLinkEntity doesn't have description
            startTime = task.startsAt,
            endTime = task.endsAt,
            calendarId = task.taskId ?: 0L, // Use taskId as calendarId for navigation
            calendarName = "QuestFlow Task",
            isExternal = false // QuestFlow tasks are internal
        )
    }

    /**
     * Check if a specific time slot is completely free
     */
    suspend fun isSlotFree(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ): Boolean {
        val conflicts = invoke(startTime, endTime, excludeEventId, excludeTaskId, excludeLinkId)
        return conflicts.isEmpty()
    }
}
