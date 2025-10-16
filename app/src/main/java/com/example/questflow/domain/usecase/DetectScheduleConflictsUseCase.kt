package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarEvent
import com.example.questflow.data.calendar.CalendarManager
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case to detect scheduling conflicts with existing calendar events
 * across ALL calendars (Google Calendar, QuestFlow, Outlook, etc.)
 */
class DetectScheduleConflictsUseCase @Inject constructor(
    private val calendarManager: CalendarManager
) {
    /**
     * Check if a time slot conflicts with existing events
     * @param startTime Start of the proposed time slot
     * @param endTime End of the proposed time slot
     * @param excludeEventId Optional event ID to exclude (for editing existing events)
     * @return List of conflicting events (empty if no conflicts)
     */
    suspend operator fun invoke(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeEventId: Long? = null
    ): List<CalendarEvent> {
        // Query all calendar events in the date range
        val startDate = startTime.toLocalDate()
        val endDate = endTime.toLocalDate()

        val allEvents = calendarManager.getAllCalendarEvents(startDate, endDate)

        // Filter for actual conflicts
        return allEvents.filter { event ->
            // Skip if this is the event we're editing
            if (excludeEventId != null && event.id == excludeEventId) {
                return@filter false
            }

            // Check for time overlap
            // Events overlap if: start1 < end2 AND start2 < end1
            val hasOverlap = startTime < event.endTime && endTime > event.startTime

            hasOverlap
        }
    }

    /**
     * Check if a specific time slot is completely free
     */
    suspend fun isSlotFree(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeEventId: Long? = null
    ): Boolean {
        val conflicts = invoke(startTime, endTime, excludeEventId)
        return conflicts.isEmpty()
    }
}
