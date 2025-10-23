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
        android.util.Log.d("ConflictDetection", "╔══════════════════════════════════════════════════════════════")
        android.util.Log.d("ConflictDetection", "║ CONFLICT CHECK START")
        android.util.Log.d("ConflictDetection", "╠══════════════════════════════════════════════════════════════")
        android.util.Log.d("ConflictDetection", "║ Checking Time Range:")
        android.util.Log.d("ConflictDetection", "║   Start: $startTime")
        android.util.Log.d("ConflictDetection", "║   End:   $endTime")
        android.util.Log.d("ConflictDetection", "╠══════════════════════════════════════════════════════════════")
        android.util.Log.d("ConflictDetection", "║ Exclusion Parameters:")
        android.util.Log.d("ConflictDetection", "║   excludeLinkId:  $excludeLinkId")
        android.util.Log.d("ConflictDetection", "║   excludeTaskId:  $excludeTaskId")
        android.util.Log.d("ConflictDetection", "║   excludeEventId: $excludeEventId")
        android.util.Log.d("ConflictDetection", "╚══════════════════════════════════════════════════════════════")

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

            // TIMING-BUG FIX: Skip if this event likely belongs to the task being edited
            // This handles the case where Auto-Save creates a Calendar Event (new ID)
            // but the State still has the old ID (0), causing the task to find itself
            //
            // Context: When a task time is changed:
            // 1. LaunchedEffect triggers conflict check IMMEDIATELY
            // 2. Auto-Save creates Google Calendar event with NEW Event ID (e.g. 1488)
            // 3. But State still has OLD Event ID (0) → excludeEventId = 0
            // 4. Conflict check finds the NEW event (1488) but can't exclude it (0 ≠ 1488)
            // 5. Task finds itself as conflict!
            //
            // Solution: Skip events that are likely the SAME task based on:
            // - High time overlap (>90%)
            // - QuestFlow emoji prefix in title (🎯, 💻, 📚, etc.)
            if (excludeTaskId != null) {
                // Check if event has QuestFlow emoji prefix (created by QuestFlow)
                val hasQuestFlowEmoji = event.title.matches(Regex("^[\\p{So}\\p{Cn}]\\s+.*"))

                if (hasQuestFlowEmoji) {
                    // Check time overlap percentage to detect if this is the same task
                    val eventDuration = java.time.Duration.between(event.startTime, event.endTime).toMinutes()
                    val checkDuration = java.time.Duration.between(startTime, endTime).toMinutes()

                    // Calculate overlap
                    val overlapStart = if (startTime.isAfter(event.startTime)) startTime else event.startTime
                    val overlapEnd = if (endTime.isBefore(event.endTime)) endTime else event.endTime

                    if (overlapStart.isBefore(overlapEnd)) {
                        val overlapDuration = java.time.Duration.between(overlapStart, overlapEnd).toMinutes()
                        val overlapPercentage = if (eventDuration > 0 && checkDuration > 0) {
                            val minDuration = if (eventDuration < checkDuration) eventDuration else checkDuration
                            (overlapDuration.toDouble() / minDuration) * 100
                        } else 0.0

                        // If overlap is > 90%, it's likely the same task (timing bug)
                        if (overlapPercentage > 90.0) {
                            android.util.Log.d("ConflictDetection", "⚠️ TIMING-BUG DETECTED: Skipping Google Calendar event '${event.title}'")
                            android.util.Log.d("ConflictDetection", "   Event ID: ${event.id}, Overlap: ${overlapPercentage.toInt()}%")
                            android.util.Log.d("ConflictDetection", "   Reason: QuestFlow emoji + high overlap → likely same task")
                            return@filter false
                        }
                    }
                }
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
        android.util.Log.d("ConflictDetection", "")
        android.util.Log.d("ConflictDetection", "╔══════════════════════════════════════════════════════════════")
        android.util.Log.d("ConflictDetection", "║ QUESTFLOW TASK CONFLICTS CHECK")
        android.util.Log.d("ConflictDetection", "╠══════════════════════════════════════════════════════════════")
        android.util.Log.d("ConflictDetection", "║ Found ${tasksInRange.size} tasks in range")
        android.util.Log.d("ConflictDetection", "╚══════════════════════════════════════════════════════════════")

        tasksInRange.forEachIndexed { index, task ->
            android.util.Log.d("ConflictDetection", "")
            android.util.Log.d("ConflictDetection", "┌─────────────────────────────────────────────────────────────")
            android.util.Log.d("ConflictDetection", "│ Task #${index + 1} of ${tasksInRange.size}")
            android.util.Log.d("ConflictDetection", "├─────────────────────────────────────────────────────────────")
            android.util.Log.d("ConflictDetection", "│ Title:       '${task.title}'")
            android.util.Log.d("ConflictDetection", "│ Link ID:     ${task.id}")
            android.util.Log.d("ConflictDetection", "│ Task ID:     ${task.taskId}")
            android.util.Log.d("ConflictDetection", "│ Event ID:    ${task.calendarEventId}")
            android.util.Log.d("ConflictDetection", "│ Time Range:  ${task.startsAt} → ${task.endsAt}")
            android.util.Log.d("ConflictDetection", "├─────────────────────────────────────────────────────────────")
            android.util.Log.d("ConflictDetection", "│ EXCLUSION CHECKS:")

            // Skip if this is the CalendarEventLink we're editing (primary exclusion)
            if (excludeLinkId != null && task.id == excludeLinkId) {
                android.util.Log.d("ConflictDetection", "│   [CHECK 1] excludeLinkId match?")
                android.util.Log.d("ConflictDetection", "│     excludeLinkId = $excludeLinkId")
                android.util.Log.d("ConflictDetection", "│     task.id       = ${task.id}")
                android.util.Log.d("ConflictDetection", "│     ✓ MATCH! Skipping this task (same CalendarEventLink)")
                android.util.Log.d("ConflictDetection", "└─────────────────────────────────────────────────────────────")
                return@forEachIndexed
            } else {
                android.util.Log.d("ConflictDetection", "│   [CHECK 1] excludeLinkId match?")
                android.util.Log.d("ConflictDetection", "│     excludeLinkId = $excludeLinkId")
                android.util.Log.d("ConflictDetection", "│     task.id       = ${task.id}")
                android.util.Log.d("ConflictDetection", "│     ✗ No match")
            }

            // Skip if this is the task we're editing (for tasks with multiple links)
            if (excludeTaskId != null && task.taskId == excludeTaskId) {
                android.util.Log.d("ConflictDetection", "│   [CHECK 2] excludeTaskId match?")
                android.util.Log.d("ConflictDetection", "│     excludeTaskId = $excludeTaskId")
                android.util.Log.d("ConflictDetection", "│     task.taskId   = ${task.taskId}")
                android.util.Log.d("ConflictDetection", "│     ✓ MATCH! Skipping this task (same Task)")
                android.util.Log.d("ConflictDetection", "└─────────────────────────────────────────────────────────────")
                return@forEachIndexed
            } else {
                android.util.Log.d("ConflictDetection", "│   [CHECK 2] excludeTaskId match?")
                android.util.Log.d("ConflictDetection", "│     excludeTaskId = $excludeTaskId")
                android.util.Log.d("ConflictDetection", "│     task.taskId   = ${task.taskId}")
                android.util.Log.d("ConflictDetection", "│     ✗ No match")
            }

            // Skip if this is linked to the calendar event we're editing (for synced events)
            if (excludeEventId != null && task.calendarEventId == excludeEventId) {
                android.util.Log.d("ConflictDetection", "│   [CHECK 3] excludeEventId match?")
                android.util.Log.d("ConflictDetection", "│     excludeEventId      = $excludeEventId")
                android.util.Log.d("ConflictDetection", "│     task.calendarEventId = ${task.calendarEventId}")
                android.util.Log.d("ConflictDetection", "│     ✓ MATCH! Skipping this task (same Calendar Event)")
                android.util.Log.d("ConflictDetection", "└─────────────────────────────────────────────────────────────")
                return@forEachIndexed
            } else {
                android.util.Log.d("ConflictDetection", "│   [CHECK 3] excludeEventId match?")
                android.util.Log.d("ConflictDetection", "│     excludeEventId       = $excludeEventId")
                android.util.Log.d("ConflictDetection", "│     task.calendarEventId = ${task.calendarEventId}")
                android.util.Log.d("ConflictDetection", "│     ✗ No match")
            }

            // CRITICAL FIX: Skip if this link has EXACTLY the same times as what we're checking
            // This prevents finding "ourselves" when editing times creates temporary duplicates
            if (task.startsAt == startTime && task.endsAt == endTime) {
                android.util.Log.d("ConflictDetection", "│   [CHECK 4] Exact time match?")
                android.util.Log.d("ConflictDetection", "│     Checking time:  $startTime → $endTime")
                android.util.Log.d("ConflictDetection", "│     Task time:      ${task.startsAt} → ${task.endsAt}")
                android.util.Log.d("ConflictDetection", "│     ✓ EXACT MATCH! Skipping this task (same time = self)")
                android.util.Log.d("ConflictDetection", "└─────────────────────────────────────────────────────────────")
                return@forEachIndexed
            } else {
                android.util.Log.d("ConflictDetection", "│   [CHECK 4] Exact time match?")
                android.util.Log.d("ConflictDetection", "│     Checking time:  $startTime → $endTime")
                android.util.Log.d("ConflictDetection", "│     Task time:      ${task.startsAt} → ${task.endsAt}")
                android.util.Log.d("ConflictDetection", "│     ✗ Different times")
            }

            android.util.Log.d("ConflictDetection", "├─────────────────────────────────────────────────────────────")
            android.util.Log.d("ConflictDetection", "│ OVERLAP CHECK:")

            // Check for time overlap
            // Tasks overlap if: start1 < end2 AND start2 < end1
            val condition1 = startTime < task.endsAt
            val condition2 = endTime > task.startsAt
            val hasOverlap = condition1 && condition2

            android.util.Log.d("ConflictDetection", "│   Checking time:  $startTime → $endTime")
            android.util.Log.d("ConflictDetection", "│   Task time:      ${task.startsAt} → ${task.endsAt}")
            android.util.Log.d("ConflictDetection", "│   Condition 1: startTime < task.endsAt?")
            android.util.Log.d("ConflictDetection", "│     $startTime < ${task.endsAt} = $condition1")
            android.util.Log.d("ConflictDetection", "│   Condition 2: endTime > task.startsAt?")
            android.util.Log.d("ConflictDetection", "│     $endTime > ${task.startsAt} = $condition2")
            android.util.Log.d("ConflictDetection", "│   Overlap = $condition1 && $condition2 = $hasOverlap")

            if (hasOverlap) {
                android.util.Log.d("ConflictDetection", "│   ❌ CONFLICT DETECTED! Adding to conflict list")
                android.util.Log.d("ConflictDetection", "└─────────────────────────────────────────────────────────────")
                taskConflicts.add(convertTaskToCalendarEvent(task))
            } else {
                android.util.Log.d("ConflictDetection", "│   ✓ NO CONFLICT - Times don't overlap")
                android.util.Log.d("ConflictDetection", "└─────────────────────────────────────────────────────────────")
            }
        }

        // Combine event, TimeBlock AND Task conflicts
        val allConflicts = eventConflicts + timeBlockConflicts + taskConflicts

        android.util.Log.d("ConflictDetection", "")
        android.util.Log.d("ConflictDetection", "╔══════════════════════════════════════════════════════════════")
        android.util.Log.d("ConflictDetection", "║ FINAL RESULT: ${allConflicts.size} TOTAL CONFLICTS")
        android.util.Log.d("ConflictDetection", "╠══════════════════════════════════════════════════════════════")
        android.util.Log.d("ConflictDetection", "║ Event Conflicts:     ${eventConflicts.size}")
        android.util.Log.d("ConflictDetection", "║ TimeBlock Conflicts: ${timeBlockConflicts.size}")
        android.util.Log.d("ConflictDetection", "║ Task Conflicts:      ${taskConflicts.size}")
        android.util.Log.d("ConflictDetection", "╠══════════════════════════════════════════════════════════════")
        if (allConflicts.isNotEmpty()) {
            android.util.Log.d("ConflictDetection", "║ Conflict Details:")
            allConflicts.forEachIndexed { index, conflict ->
                android.util.Log.d("ConflictDetection", "║   ${index + 1}. '${conflict.title}'")
                android.util.Log.d("ConflictDetection", "║      ${conflict.startTime} → ${conflict.endTime}")
                android.util.Log.d("ConflictDetection", "║      Source: ${conflict.calendarName}")
            }
        }
        android.util.Log.d("ConflictDetection", "╚══════════════════════════════════════════════════════════════")

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
