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
        android.util.Log.d("FreeTimeSlots", "====== FREE TIME SEARCH START ======")
        android.util.Log.d("FreeTimeSlots", "Search range: $startDate → $endDate")
        android.util.Log.d("FreeTimeSlots", "Min duration: $minDurationMinutes minutes")
        android.util.Log.d("FreeTimeSlots", "Exclude: linkId=$excludeLinkId, taskId=$excludeTaskId, eventId=$excludeEventId")

        val (effectiveStart, effectiveEnd) = if (workingHoursStart != null && workingHoursEnd != null) {
            Pair(workingHoursStart, workingHoursEnd)
        } else {
            getActiveTimeWindow()
        }

        android.util.Log.d("FreeTimeSlots", "Working hours: $effectiveStart → $effectiveEnd")

        val dailyResults = mutableListOf<DailyFreeTime>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val dayStart = LocalDateTime.of(currentDate, effectiveStart)
            val dayEnd = LocalDateTime.of(currentDate, effectiveEnd)

            android.util.Log.d("FreeTimeSlots", "--- Checking day: $currentDate ($dayStart → $dayEnd) ---")

            // Use DetectScheduleConflictsUseCase to get ALL conflicts (Events, TimeBlocks, Tasks)
            val allConflicts = detectScheduleConflictsUseCase(
                startTime = dayStart,
                endTime = dayEnd,
                excludeEventId = excludeEventId,
                excludeTaskId = excludeTaskId,
                excludeLinkId = excludeLinkId
            ).sortedBy { it.startTime }

            android.util.Log.d("FreeTimeSlots", "  Found ${allConflicts.size} conflicts on $currentDate")
            allConflicts.forEach { conflict ->
                android.util.Log.d("FreeTimeSlots", "    - ${conflict.title}: ${conflict.startTime} → ${conflict.endTime}")
            }

            val freeSlots = mutableListOf<FreeSlot>()
            var lastEnd = dayStart

            allConflicts.forEach { conflict ->
                val conflictStart = maxOf(conflict.startTime, dayStart)
                val conflictEnd = minOf(conflict.endTime, dayEnd)

                android.util.Log.d("FreeTimeSlots", "  Conflict: $conflictStart → $conflictEnd (clipped to day)")

                if (conflictStart.isAfter(lastEnd)) {
                    val gapMinutes = java.time.Duration.between(lastEnd, conflictStart).toMinutes()
                    android.util.Log.d("FreeTimeSlots", "  → Gap found: $lastEnd → $conflictStart ($gapMinutes min)")
                    // For multi-day tasks, collect ALL gaps (even small ones) for later merging
                    if (minDurationMinutes > 1440 || gapMinutes >= minDurationMinutes) {
                        freeSlots.add(FreeSlot(lastEnd, conflictStart, gapMinutes))
                        android.util.Log.d("FreeTimeSlots", "    ✓ Added free slot")
                    } else {
                        android.util.Log.d("FreeTimeSlots", "    ✗ Gap too short (< $minDurationMinutes min)")
                    }
                }
                lastEnd = maxOf(lastEnd, conflictEnd)
                android.util.Log.d("FreeTimeSlots", "  → lastEnd updated to: $lastEnd")
            }

            if (lastEnd.isBefore(dayEnd)) {
                val gapMinutes = java.time.Duration.between(lastEnd, dayEnd).toMinutes()
                android.util.Log.d("FreeTimeSlots", "  → Final gap: $lastEnd → $dayEnd ($gapMinutes min)")
                // For multi-day tasks, collect ALL gaps (even small ones) for later merging
                if (minDurationMinutes > 1440 || gapMinutes >= minDurationMinutes) {
                    freeSlots.add(FreeSlot(lastEnd, dayEnd, gapMinutes))
                    android.util.Log.d("FreeTimeSlots", "    ✓ Added final free slot")
                } else {
                    android.util.Log.d("FreeTimeSlots", "    ✗ Final gap too short")
                }
            }

            val totalMinutes = freeSlots.sumOf { it.durationMinutes }
            android.util.Log.d("FreeTimeSlots", "  RESULT for $currentDate: ${freeSlots.size} free slots, $totalMinutes total minutes")

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

        android.util.Log.d("FreeTimeSlots", "====== FINAL RESULT: ${dailyResults.size} days processed ======")
        dailyResults.forEach { day ->
            android.util.Log.d("FreeTimeSlots", "  ${day.date}: ${day.freeSlots.size} slots, ${day.totalFreeHours} hours free")
        }

        // CRITICAL FIX: For multi-day tasks (duration > 24h), merge consecutive free days
        if (minDurationMinutes > 1440) { // More than 24 hours
            android.util.Log.d("FreeTimeSlots", "====== MULTI-DAY TASK DETECTED (${minDurationMinutes} min = ${minDurationMinutes / 1440.0} days) ======")
            return mergeConsecutiveFreeDays(dailyResults, minDurationMinutes)
        }

        return dailyResults
    }

    /**
     * Merge consecutive free time across multiple days
     * This is needed when searching for slots longer than 24 hours
     * IMPROVED: Merges ANY consecutive free time, not just full days
     */
    private fun mergeConsecutiveFreeDays(
        dailyResults: List<DailyFreeTime>,
        minDurationMinutes: Long
    ): List<DailyFreeTime> {
        android.util.Log.d("FreeTimeSlots", "--- Merging consecutive free time across days ---")

        // Collect ALL free slots from all days into a single chronological list
        val allSlots = dailyResults.flatMap { day ->
            day.freeSlots.map { slot -> slot }
        }.sortedBy { it.startTime }

        android.util.Log.d("FreeTimeSlots", "Total slots to check: ${allSlots.size}")

        // Try to merge consecutive slots that span across days
        val mergedSlots = mutableListOf<FreeSlot>()
        var currentMerge: FreeSlot? = null

        allSlots.forEach { slot ->
            android.util.Log.d("FreeTimeSlots", "  Checking slot: ${slot.startTime} → ${slot.endTime} (${slot.durationMinutes} min)")

            if (currentMerge == null) {
                // Start new merge
                currentMerge = slot
                android.util.Log.d("FreeTimeSlots", "    → Start new merge")
            } else {
                // Check if this slot is adjacent to current merge (end time matches start time)
                // Allow small gap (up to 1 minute for rounding)
                val gap = java.time.Duration.between(currentMerge!!.endTime, slot.startTime).toMinutes()

                if (gap <= 1) {
                    // Extend current merge
                    val totalMinutes = currentMerge!!.durationMinutes + slot.durationMinutes
                    currentMerge = FreeSlot(
                        startTime = currentMerge!!.startTime,
                        endTime = slot.endTime,
                        durationMinutes = totalMinutes
                    )
                    android.util.Log.d("FreeTimeSlots", "    → Extended merge to $totalMinutes min (gap: $gap min)")

                    // Check if we've accumulated enough
                    if (totalMinutes >= minDurationMinutes) {
                        android.util.Log.d("FreeTimeSlots", "    ✓ Merge meets duration requirement!")
                        mergedSlots.add(currentMerge!!)
                        currentMerge = null // Start fresh
                    }
                } else {
                    // Gap too large - save current merge if it meets duration
                    android.util.Log.d("FreeTimeSlots", "    → Gap too large ($gap min), ending merge")
                    if (currentMerge!!.durationMinutes >= minDurationMinutes) {
                        mergedSlots.add(currentMerge!!)
                        android.util.Log.d("FreeTimeSlots", "      ✓ Saved merge (${currentMerge!!.durationMinutes} min)")
                    } else {
                        android.util.Log.d("FreeTimeSlots", "      ✗ Discarded merge (${currentMerge!!.durationMinutes} < $minDurationMinutes min)")
                    }
                    // Start new merge with current slot
                    currentMerge = slot
                }
            }
        }

        // Handle final merge
        if (currentMerge != null && currentMerge!!.durationMinutes >= minDurationMinutes) {
            mergedSlots.add(currentMerge!!)
            android.util.Log.d("FreeTimeSlots", "  → Final merge saved: ${currentMerge!!.durationMinutes} min")
        }

        // Convert merged slots back to DailyFreeTime format
        val results = mergedSlots.map { slot ->
            DailyFreeTime(
                date = slot.startTime.toLocalDate(),
                freeSlots = listOf(slot),
                totalFreeMinutes = slot.durationMinutes,
                totalFreeHours = slot.durationMinutes / 60.0
            )
        }

        android.util.Log.d("FreeTimeSlots", "====== MERGED RESULT: ${results.size} multi-day periods found ======")
        results.forEach { period ->
            period.freeSlots.forEach { slot ->
                android.util.Log.d("FreeTimeSlots", "  → ${slot.startTime} → ${slot.endTime} (${slot.durationMinutes} min = ${slot.durationHours} hours)")
            }
        }

        return results
    }

    /**
     * Find the next available free time slot starting from a given date/time
     * IMPORTANT: Searches from START OF DAY to find ALL available slots
     */
    suspend fun findNextAvailableSlot(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime,
        maxDaysToSearch: Int = 7,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ): FreeSlot? {
        // CRITICAL FIX: Start search from beginning of the day
        val searchStartDate = startSearchFrom.toLocalDate()
        val actualSearchStart = LocalDateTime.of(searchStartDate, LocalTime.MIN)
        val endDate = searchStartDate.plusDays(maxDaysToSearch.toLong())

        val dailyFreeTime = invoke(
            startDate = searchStartDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            excludeEventId = excludeEventId,
            excludeTaskId = excludeTaskId,
            excludeLinkId = excludeLinkId
        )

        for (day in dailyFreeTime) {
            for (slot in day.freeSlots) {
                if (slot.durationMinutes >= requiredDurationMinutes) {
                    // Accept slots that start at/after start of search day
                    if (slot.startTime >= actualSearchStart) {
                        return slot
                    }
                }
            }
        }

        return null
    }

    /**
     * Suggest multiple time slots for scheduling
     * IMPORTANT: Searches from START OF DAY, not from startSearchFrom time
     * This ensures we find ALL free slots in the day, not just after current task time
     */
    suspend fun suggestTimeSlots(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime,
        maxSuggestions: Int = 5,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ): List<FreeSlot> {
        android.util.Log.d("FreeTimeSlots", "====== SUGGEST TIME SLOTS ======")
        android.util.Log.d("FreeTimeSlots", "Required duration: $requiredDurationMinutes minutes")
        android.util.Log.d("FreeTimeSlots", "Start search from: $startSearchFrom")
        android.util.Log.d("FreeTimeSlots", "Max suggestions: $maxSuggestions")

        // CRITICAL FIX: Start search from beginning of the day, not from specific time
        // This allows finding free slots throughout the entire day
        val searchStartDate = startSearchFrom.toLocalDate()
        val actualSearchStart = LocalDateTime.of(searchStartDate, LocalTime.MIN)
        val endDate = searchStartDate.plusDays(60) // Search up to 2 months (extended from 14 days)

        android.util.Log.d("FreeTimeSlots", "Actual search start: $actualSearchStart (start of day)")
        android.util.Log.d("FreeTimeSlots", "Search end: $endDate (60 days)")

        val dailyFreeTime = invoke(
            startDate = searchStartDate,
            endDate = endDate,
            minDurationMinutes = requiredDurationMinutes,
            excludeEventId = excludeEventId,
            excludeTaskId = excludeTaskId,
            excludeLinkId = excludeLinkId
        )

        android.util.Log.d("FreeTimeSlots", "Got ${dailyFreeTime.size} days of free time data")

        val suggestions = mutableListOf<FreeSlot>()

        for (day in dailyFreeTime) {
            android.util.Log.d("FreeTimeSlots", "Checking day ${day.date}: ${day.freeSlots.size} free slots")
            for (slot in day.freeSlots) {
                android.util.Log.d("FreeTimeSlots", "  Slot: ${slot.startTime} → ${slot.endTime} (${slot.durationMinutes} min)")

                // Accept slots that are >= required duration AND start at/after start of search day
                if (slot.durationMinutes >= requiredDurationMinutes && slot.startTime >= actualSearchStart) {
                    suggestions.add(slot)
                    android.util.Log.d("FreeTimeSlots", "    ✓ Added to suggestions (${suggestions.size}/$maxSuggestions)")
                    if (suggestions.size >= maxSuggestions) {
                        android.util.Log.d("FreeTimeSlots", "====== FOUND $maxSuggestions SUGGESTIONS ======")
                        return suggestions
                    }
                } else {
                    if (slot.durationMinutes < requiredDurationMinutes) {
                        android.util.Log.d("FreeTimeSlots", "    ✗ Too short (${slot.durationMinutes} < $requiredDurationMinutes)")
                    } else {
                        android.util.Log.d("FreeTimeSlots", "    ✗ Before search start (${slot.startTime} < $actualSearchStart)")
                    }
                }
            }
        }

        android.util.Log.d("FreeTimeSlots", "====== FOUND ${suggestions.size} SUGGESTIONS (less than max) ======")
        return suggestions
    }
}
