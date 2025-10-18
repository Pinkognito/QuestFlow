package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.database.dao.WorkingHoursSettingsDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.min

/**
 * Use case to find free time slots in the calendar
 */
class FindFreeTimeSlotsUseCase @Inject constructor(
    private val calendarManager: CalendarManager,
    private val workingHoursSettingsDao: WorkingHoursSettingsDao
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
     * Load working hours from database, fallback to defaults if disabled or not found
     */
    private suspend fun getWorkingHours(): Pair<LocalTime, LocalTime> {
        val settings = workingHoursSettingsDao.getWorkingHoursSettingsOnce()
        return if (settings != null && settings.enabled) {
            Pair(
                LocalTime.of(settings.startHour, settings.startMinute),
                LocalTime.of(settings.endHour, settings.endMinute)
            )
        } else {
            // Fallback: 0:00 - 23:59 (full day) if disabled
            Pair(LocalTime.MIN, LocalTime.MAX)
        }
    }

    /**
     * Find all free time slots in a date range
     *
     * @param startDate Start date to search
     * @param endDate End date to search
     * @param minDurationMinutes Minimum duration for a slot to be considered (default 30 min)
     * @param workingHoursStart Start of working hours (null = use database settings)
     * @param workingHoursEnd End of working hours (null = use database settings)
     * @param excludeEventId Optional event ID to exclude from busy calculation
     * @return List of daily free time statistics
     */
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        minDurationMinutes: Long = 30,
        workingHoursStart: LocalTime? = null,
        workingHoursEnd: LocalTime? = null,
        excludeEventId: Long? = null
    ): List<DailyFreeTime> {
        // Load working hours from database if not explicitly provided
        val (effectiveStart, effectiveEnd) = if (workingHoursStart != null && workingHoursEnd != null) {
            Pair(workingHoursStart, workingHoursEnd)
        } else {
            getWorkingHours()
        }
        // Get all calendar events in the range
        val allEvents = calendarManager.getAllCalendarEvents(startDate, endDate)
            .filter { event ->
                // Exclude the event we're editing
                excludeEventId == null || event.id != excludeEventId
            }
            .sortedBy { it.startTime }
        android.util.Log.d("FindFreeSlot_Invoke", "Found ${allEvents.size} calendar events (after exclusion)")

        val dailyResults = mutableListOf<DailyFreeTime>()

        // Process each day
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayStart = LocalDateTime.of(currentDate, effectiveStart)
            val dayEnd = LocalDateTime.of(currentDate, effectiveEnd)

            // Get events for this day
            val dayEvents = allEvents.filter { event ->
                event.startTime.toLocalDate() == currentDate ||
                event.endTime.toLocalDate() == currentDate ||
                (event.startTime.toLocalDate().isBefore(currentDate) &&
                 event.endTime.toLocalDate().isAfter(currentDate))
            }.sortedBy { it.startTime }
            android.util.Log.d("FindFreeSlot_Invoke", "Processing day: $currentDate (${dayStart.toLocalTime()} - ${dayEnd.toLocalTime()}), ${dayEvents.size} events")

            // Find free slots
            val freeSlots = mutableListOf<FreeSlot>()
            var checkTime = dayStart

            for (event in dayEvents) {
                val eventStart = maxOf(event.startTime, dayStart)

                // Check if there's free time before this event
                val eventEnd = minOf(event.endTime, dayEnd)
                android.util.Log.d("FindFreeSlot_Invoke", "  Event: $eventStart - $eventEnd")
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
     * @param workingHoursStart Start of working hours (null = use database settings)
     * @param workingHoursEnd End of working hours (null = use database settings)
     * @return First available free slot, or null if none found
     */
    suspend fun findNextAvailableSlot(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime = LocalDateTime.now(),
        maxDaysToSearch: Int = 30,
        workingHoursStart: LocalTime? = null,
        workingHoursEnd: LocalTime? = null,
        excludeEventId: Long? = null
    ): FreeSlot? {
        android.util.Log.d("FindFreeSlot", "═════════════════════════════════════════")
        android.util.Log.d("FindFreeSlot", "=== FIND FREE SLOT DEBUG ===")
        android.util.Log.d("FindFreeSlot", "Required duration: ${requiredDurationMinutes}min")
        android.util.Log.d("FindFreeSlot", "Start search from: $startSearchFrom")
        android.util.Log.d("FindFreeSlot", "Max days to search: $maxDaysToSearch")
        android.util.Log.d("FindFreeSlot", "Exclude event ID: $excludeEventId")
        // Load working hours from database if not explicitly provided
        val (effectiveStart, effectiveEnd) = if (workingHoursStart != null && workingHoursEnd != null) {
            Pair(workingHoursStart, workingHoursEnd)
        } else {
            getWorkingHours()
        }
        android.util.Log.d("FindFreeSlot", "Working hours: $effectiveStart - $effectiveEnd")
        val startDate = startSearchFrom.toLocalDate()
        val endDate = startDate.plusDays(maxDaysToSearch.toLong())

        android.util.Log.d("FindFreeSlot", "Search date range: $startDate to $endDate")
        val dailyFreeTime = invoke(
            startDate = startDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            workingHoursStart = effectiveStart,
            workingHoursEnd = effectiveEnd,
            excludeEventId = excludeEventId
        )

        android.util.Log.d("FindFreeSlot", "Found ${dailyFreeTime.size} days with potential free time")
        // Find first slot that fits
        var dayIndex = 0
        for (day in dailyFreeTime) {
            android.util.Log.d("FindFreeSlot", "Day $dayIndex: ${day.date}, ${day.freeSlots.size} free slots")

            var slotIndex = 0
            for (slot in day.freeSlots) {
                // For the current day, adjust slot to start at startSearchFrom if needed
                val adjustedSlot = if (day.date == startDate && slot.startTime < startSearchFrom) {
                    // Slot starts before our search time
                    if (slot.endTime <= startSearchFrom) {
                        // Entire slot is before search time - skip it
                        android.util.Log.d("FindFreeSlot", "  Slot $slotIndex: ${slot.startTime} - ${slot.endTime} (${slot.durationMinutes}min) - SKIPPED (entirely before search start)")
                        slotIndex++
                        continue
                    } else {
                        // Slot overlaps with search time - adjust to start at startSearchFrom
                        val adjustedDuration = java.time.Duration.between(startSearchFrom, slot.endTime).toMinutes()
                        android.util.Log.d("FindFreeSlot", "  Slot $slotIndex: ${slot.startTime} - ${slot.endTime} ADJUSTED to $startSearchFrom - ${slot.endTime} (${adjustedDuration}min)")
                        FreeSlot(
                            startTime = startSearchFrom,
                            endTime = slot.endTime,
                            durationMinutes = adjustedDuration
                        )
                    }
                } else {
                    android.util.Log.d("FindFreeSlot", "  Slot $slotIndex: ${slot.startTime} - ${slot.endTime} (${slot.durationMinutes}min)")
                    slot
                }

                if (adjustedSlot.durationMinutes >= requiredDurationMinutes) {
                    android.util.Log.d("FindFreeSlot", "✅ FOUND SUITABLE SLOT: ${adjustedSlot.startTime} - ${adjustedSlot.endTime}")
                    android.util.Log.d("FindFreeSlot", "═════════════════════════════════════════")
                    return adjustedSlot
                }
                slotIndex++
            }
            dayIndex++
        }

        android.util.Log.d("FindFreeSlot", "❌ NO SUITABLE SLOT FOUND")
        android.util.Log.d("FindFreeSlot", "═════════════════════════════════════════")
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
        workingHoursStart: LocalTime? = null,
        workingHoursEnd: LocalTime? = null,
        excludeEventId: Long? = null
    ): List<FreeSlot> {
        // Load working hours from database if not explicitly provided
        val (effectiveStart, effectiveEnd) = if (workingHoursStart != null && workingHoursEnd != null) {
            Pair(workingHoursStart, workingHoursEnd)
        } else {
            getWorkingHours()
        }

        val startDate = startSearchFrom.toLocalDate()
        val endDate = startDate.plusDays(maxDaysToSearch.toLong())

        val dailyFreeTime = invoke(
            startDate = startDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            workingHoursStart = effectiveStart,
            workingHoursEnd = effectiveEnd,
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
