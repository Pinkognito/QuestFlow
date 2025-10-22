package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.repository.TimeBlockRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlin.math.min

/**
 * Use case to find free time slots in the calendar
 * Now uses DetectScheduleConflictsUseCase to include ALL conflict types:
 * - External calendar events (Google Calendar, etc.)
 * - TimeBlocks (blocked time windows)
 * - QuestFlow Tasks (CalendarEventLinks)
 */
class FindFreeTimeSlotsUseCase @Inject constructor(
    private val calendarManager: CalendarManager,
    private val timeBlockRepository: TimeBlockRepository,
    private val detectScheduleConflictsUseCase: DetectScheduleConflictsUseCase
) {
    data class FreeSlot(
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val durationMinutes: Long
    ) {
        val durationHours: Double
            get() = durationMinutes / 60.0
    }

    data class DailyFreeTime(
        val date: LocalDate,
        val freeSlots: List<FreeSlot>,
        val totalFreeMinutes: Long,
        val totalFreeHours: Double
    ) {
        val hasFreeTime: Boolean
            get() = totalFreeMinutes > 0
    }

    private suspend fun getActiveTimeWindow(): Pair<LocalTime, LocalTime> {
        // Default: Full day if no time blocks defined
        return Pair(LocalTime.of(0, 0), LocalTime.of(23, 59))
    }

    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        minDurationMinutes: Long = 30,
        workingHoursStart: LocalTime? = null,
        workingHoursEnd: LocalTime? = null,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ): List<DailyFreeTime> {
        val (effectiveStart, effectiveEnd) = if (workingHoursStart != null && workingHoursEnd != null) {
            Pair(workingHoursStart, workingHoursEnd)
        } else {
            getActiveTimeWindow()
        }

        val dailyResults = mutableListOf<DailyFreeTime>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val dayStart = LocalDateTime.of(currentDate, effectiveStart)
            val dayEnd = LocalDateTime.of(currentDate, effectiveEnd)

            // Use DetectScheduleConflictsUseCase to get ALL conflicts (Events, TimeBlocks, Tasks)
            val allConflicts = detectScheduleConflictsUseCase(
                startTime = dayStart,
                endTime = dayEnd,
                excludeEventId = excludeEventId,
                excludeTaskId = excludeTaskId,
                excludeLinkId = excludeLinkId
            ).sortedBy { it.startTime }

            val freeSlots = mutableListOf<FreeSlot>()
            var lastEnd = dayStart

            allConflicts.forEach { conflict ->
                val conflictStart = maxOf(conflict.startTime, dayStart)
                val conflictEnd = minOf(conflict.endTime, dayEnd)

                if (conflictStart.isAfter(lastEnd)) {
                    val gapMinutes = java.time.Duration.between(lastEnd, conflictStart).toMinutes()
                    if (gapMinutes >= minDurationMinutes) {
                        freeSlots.add(FreeSlot(lastEnd, conflictStart, gapMinutes))
                    }
                }
                lastEnd = maxOf(lastEnd, conflictEnd)
            }

            if (lastEnd.isBefore(dayEnd)) {
                val gapMinutes = java.time.Duration.between(lastEnd, dayEnd).toMinutes()
                if (gapMinutes >= minDurationMinutes) {
                    freeSlots.add(FreeSlot(lastEnd, dayEnd, gapMinutes))
                }
            }

            val totalMinutes = freeSlots.sumOf { it.durationMinutes }
            dailyResults.add(
                DailyFreeTime(
                    date = currentDate,
                    freeSlots = freeSlots,
                    totalFreeMinutes = totalMinutes,
                    totalFreeHours = totalMinutes / 60.0
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        return dailyResults
    }

    /**
     * Find the next available free time slot starting from a given date/time
     */
    suspend fun findNextAvailableSlot(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime,
        maxDaysToSearch: Int = 7,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ): FreeSlot? {
        val startDate = startSearchFrom.toLocalDate()
        val endDate = startDate.plusDays(maxDaysToSearch.toLong())

        val dailyFreeTime = invoke(
            startDate = startDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            excludeEventId = excludeEventId,
            excludeTaskId = excludeTaskId,
            excludeLinkId = excludeLinkId
        )

        for (day in dailyFreeTime) {
            for (slot in day.freeSlots) {
                if (slot.durationMinutes >= requiredDurationMinutes) {
                    // If slot is on or after startSearchFrom
                    if (slot.startTime >= startSearchFrom) {
                        return slot
                    }
                }
            }
        }

        return null
    }

    /**
     * Suggest multiple time slots for scheduling
     */
    suspend fun suggestTimeSlots(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime,
        maxSuggestions: Int = 5,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ): List<FreeSlot> {
        val startDate = startSearchFrom.toLocalDate()
        val endDate = startDate.plusDays(14) // Search up to 2 weeks

        val dailyFreeTime = invoke(
            startDate = startDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            excludeEventId = excludeEventId,
            excludeTaskId = excludeTaskId,
            excludeLinkId = excludeLinkId
        )

        val suggestions = mutableListOf<FreeSlot>()

        for (day in dailyFreeTime) {
            for (slot in day.freeSlots) {
                if (slot.durationMinutes >= requiredDurationMinutes && slot.startTime >= startSearchFrom) {
                    suggestions.add(slot)
                    if (suggestions.size >= maxSuggestions) {
                        return suggestions
                    }
                }
            }
        }

        return suggestions
    }
}
