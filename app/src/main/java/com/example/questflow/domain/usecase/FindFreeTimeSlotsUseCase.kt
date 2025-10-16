package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.min

/**
 * Use case to find free time slots in the calendar
 */
class FindFreeTimeSlotsUseCase @Inject constructor(
    private val calendarManager: CalendarManager
) {
    /**
     * Represents a free time slot
     */
    data class FreeSlot(
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val durationMinutes: Long
    ) {
        val durationHours: Double
            get() = durationMinutes / 60.0
    }

    /**
     * Represents daily statistics about free time
     */
    data class DailyFreeTime(
        val date: LocalDate,
        val freeSlots: List<FreeSlot>,
        val totalFreeMinutes: Long,
        val totalFreeHours: Double
    ) {
        val hasFreeTime: Boolean
            get() = totalFreeMinutes > 0
    }

    /**
     * Find all free time slots in a date range
     *
     * @param startDate Start date to search
     * @param endDate End date to search
     * @param minDurationMinutes Minimum duration for a slot to be considered (default 30 min)
     * @param workingHoursStart Start of working hours (default 8:00)
     * @param workingHoursEnd End of working hours (default 22:00)
     * @param excludeEventId Optional event ID to exclude from busy calculation
     * @return List of daily free time statistics
     */
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        minDurationMinutes: Long = 30,
        workingHoursStart: LocalTime = LocalTime.of(8, 0),
        workingHoursEnd: LocalTime = LocalTime.of(22, 0),
        excludeEventId: Long? = null
    ): List<DailyFreeTime> {
        // Get all calendar events in the range
        val allEvents = calendarManager.getAllCalendarEvents(startDate, endDate)
            .filter { event ->
                // Exclude the event we're editing
                excludeEventId == null || event.id != excludeEventId
            }
            .sortedBy { it.startTime }

        val dailyResults = mutableListOf<DailyFreeTime>()

        // Process each day
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayStart = LocalDateTime.of(currentDate, workingHoursStart)
            val dayEnd = LocalDateTime.of(currentDate, workingHoursEnd)

            // Get events for this day
            val dayEvents = allEvents.filter { event ->
                event.startTime.toLocalDate() == currentDate ||
                event.endTime.toLocalDate() == currentDate ||
                (event.startTime.toLocalDate().isBefore(currentDate) &&
                 event.endTime.toLocalDate().isAfter(currentDate))
            }.sortedBy { it.startTime }

            // Find free slots
            val freeSlots = mutableListOf<FreeSlot>()
            var checkTime = dayStart

            for (event in dayEvents) {
                val eventStart = maxOf(event.startTime, dayStart)
                val eventEnd = minOf(event.endTime, dayEnd)

                // Check if there's free time before this event
                if (checkTime < eventStart) {
                    val gapMinutes = java.time.Duration.between(checkTime, eventStart).toMinutes()
                    if (gapMinutes >= minDurationMinutes) {
                        freeSlots.add(
                            FreeSlot(
                                startTime = checkTime,
                                endTime = eventStart,
                                durationMinutes = gapMinutes
                            )
                        )
                    }
                }

                // Move check time to end of this event
                checkTime = maxOf(checkTime, eventEnd)
            }

            // Check for free time after last event
            if (checkTime < dayEnd) {
                val gapMinutes = java.time.Duration.between(checkTime, dayEnd).toMinutes()
                if (gapMinutes >= minDurationMinutes) {
                    freeSlots.add(
                        FreeSlot(
                            startTime = checkTime,
                            endTime = dayEnd,
                            durationMinutes = gapMinutes
                        )
                    )
                }
            }

            val totalFreeMinutes = freeSlots.sumOf { it.durationMinutes }
            dailyResults.add(
                DailyFreeTime(
                    date = currentDate,
                    freeSlots = freeSlots,
                    totalFreeMinutes = totalFreeMinutes,
                    totalFreeHours = totalFreeMinutes / 60.0
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        return dailyResults
    }

    /**
     * Find next available slot that fits the required duration
     *
     * @param requiredDurationMinutes Duration needed for the task
     * @param startSearchFrom Start searching from this date/time
     * @param maxDaysToSearch Maximum days to search ahead (default 30)
     * @return First available free slot, or null if none found
     */
    suspend fun findNextAvailableSlot(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime = LocalDateTime.now(),
        maxDaysToSearch: Int = 30,
        workingHoursStart: LocalTime = LocalTime.of(8, 0),
        workingHoursEnd: LocalTime = LocalTime.of(22, 0),
        excludeEventId: Long? = null
    ): FreeSlot? {
        val startDate = startSearchFrom.toLocalDate()
        val endDate = startDate.plusDays(maxDaysToSearch.toLong())

        val dailyFreeTime = invoke(
            startDate = startDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            workingHoursStart = workingHoursStart,
            workingHoursEnd = workingHoursEnd,
            excludeEventId = excludeEventId
        )

        // Find first slot that fits
        for (day in dailyFreeTime) {
            for (slot in day.freeSlots) {
                // For the current day, skip slots that start before startSearchFrom
                if (day.date == startDate && slot.startTime < startSearchFrom) {
                    continue
                }

                if (slot.durationMinutes >= requiredDurationMinutes) {
                    return slot
                }
            }
        }

        return null
    }

    /**
     * Suggest optimal time slots based on required duration
     * Returns up to 5 suggestions
     */
    suspend fun suggestTimeSlots(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime = LocalDateTime.now(),
        maxSuggestions: Int = 5,
        maxDaysToSearch: Int = 30,
        workingHoursStart: LocalTime = LocalTime.of(8, 0),
        workingHoursEnd: LocalTime = LocalTime.of(22, 0),
        excludeEventId: Long? = null
    ): List<FreeSlot> {
        val startDate = startSearchFrom.toLocalDate()
        val endDate = startDate.plusDays(maxDaysToSearch.toLong())

        val dailyFreeTime = invoke(
            startDate = startDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            workingHoursStart = workingHoursStart,
            workingHoursEnd = workingHoursEnd,
            excludeEventId = excludeEventId
        )

        val suggestions = mutableListOf<FreeSlot>()

        for (day in dailyFreeTime) {
            for (slot in day.freeSlots) {
                // For the current day, skip slots that start before startSearchFrom
                if (day.date == startDate && slot.startTime < startSearchFrom) {
                    continue
                }

                if (slot.durationMinutes >= requiredDurationMinutes) {
                    // Add a slot that fits exactly the required duration
                    suggestions.add(
                        FreeSlot(
                            startTime = slot.startTime,
                            endTime = slot.startTime.plusMinutes(requiredDurationMinutes),
                            durationMinutes = requiredDurationMinutes
                        )
                    )

                    if (suggestions.size >= maxSuggestions) {
                        return suggestions
                    }
                }
            }
        }

        return suggestions
    }
}
