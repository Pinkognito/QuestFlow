package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.preferences.SyncPreferences
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.TaskRepository
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import android.util.Log

class CheckExpiredEventsUseCase @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val taskRepository: TaskRepository,
    private val calendarManager: CalendarManager,
    private val syncPreferences: SyncPreferences,
    private val findFreeTimeSlotsUseCase: FindFreeTimeSlotsUseCase  // FIX P1-002: For smart rescheduling
) {
    data class CheckResult(
        val expiredCount: Int = 0,
        val deletedCount: Int = 0,
        val recurringCreated: Int = 0
    )

    suspend operator fun invoke(forceFullCheck: Boolean = false): CheckResult {
        val now = LocalDateTime.now()
        val lastCheckTime = if (forceFullCheck) {
            null
        } else {
            syncPreferences.getLastCheckTime().firstOrNull()
        }

        Log.d("CheckExpiredEvents", "Checking events from ${lastCheckTime ?: "beginning"} to $now")

        var expiredCount = 0
        var deletedCount = 0
        var recurringCreated = 0

        try {
            syncPreferences.setSyncInProgress(true)

            // Check all pending calendar events
            val links = calendarLinkRepository.getAllLinks().first()

            links.filter { link ->
                // Check if event ended between last check and now
                val isInTimeRange = if (lastCheckTime != null) {
                    link.endsAt > lastCheckTime && link.endsAt <= now
                } else {
                    link.endsAt <= now
                }

                link.status == "PENDING" && !link.rewarded && isInTimeRange
            }.forEach { expiredLink ->
                expiredCount++

                // Mark as expired
                calendarLinkRepository.updateLink(
                    expiredLink.copy(status = "EXPIRED")
                )
                Log.d("CheckExpiredEvents", "Marked as expired: ${expiredLink.title}")

                // Delete from calendar if configured
                if (expiredLink.deleteOnExpiry && calendarManager.hasCalendarPermission()) {
                    try {
                        calendarManager.deleteEvent(expiredLink.calendarEventId)
                        deletedCount++
                        Log.d("CheckExpiredEvents", "Deleted expired calendar event: ${expiredLink.title}")
                    } catch (e: Exception) {
                        Log.e("CheckExpiredEvents", "Failed to delete calendar event: ${e.message}")
                    }
                }

                // FIX P1-002: Check if this is a recurring event
                Log.d("QuestFlow_Recurring", "=== EXPIRED LINK CHECK ===")
                Log.d("QuestFlow_Recurring", "Link: ${expiredLink.title}")
                Log.d("QuestFlow_Recurring", "Link ID: ${expiredLink.id}")
                Log.d("QuestFlow_Recurring", "taskId: ${expiredLink.taskId}")
                Log.d("QuestFlow_Recurring", "recurringTaskId: ${expiredLink.recurringTaskId}")
                Log.d("QuestFlow_Recurring", "isRecurring: ${expiredLink.isRecurring}")

                expiredLink.recurringTaskId?.let { taskId ->
                    Log.d("QuestFlow_Recurring", "Found recurringTaskId: $taskId - fetching task...")
                    val task = taskRepository.getTaskById(taskId)

                    if (task == null) {
                        Log.w("QuestFlow_Recurring", "Task with ID $taskId not found!")
                        return@let
                    }

                    Log.d("QuestFlow_Recurring", "Task found: ${task.title}")
                    Log.d("QuestFlow_Recurring", "Task.isRecurring: ${task.isRecurring}")
                    Log.d("QuestFlow_Recurring", "Task.triggerMode: ${task.triggerMode}")
                    Log.d("QuestFlow_Recurring", "Task.recurringType: ${task.recurringType}")
                    Log.d("QuestFlow_Recurring", "Task.recurringInterval: ${task.recurringInterval}")

                    when (task.triggerMode) {
                        "AFTER_EXPIRY" -> {
                            Log.d("QuestFlow_Recurring", "Trigger mode: AFTER_EXPIRY - updating task times")
                            if (handleRecurringTask(task, now, expiredLink)) {
                                recurringCreated++
                                Log.d("QuestFlow_Recurring", "✅ Task times updated successfully")
                            } else {
                                Log.w("QuestFlow_Recurring", "❌ Failed to update task times")
                            }
                        }
                        "FIXED_INTERVAL" -> {
                            Log.d("QuestFlow_Recurring", "Trigger mode: FIXED_INTERVAL")
                            if (task.nextDueDate != null && task.nextDueDate <= now) {
                                Log.d("QuestFlow_Recurring", "nextDueDate (${ task.nextDueDate}) <= now - updating times")
                                if (handleRecurringTask(task, task.nextDueDate, expiredLink)) {
                                    recurringCreated++
                                    Log.d("QuestFlow_Recurring", "✅ Task times updated successfully")
                                } else {
                                    Log.w("QuestFlow_Recurring", "❌ Failed to update task times")
                                }
                            } else {
                                Log.d("QuestFlow_Recurring", "nextDueDate not ready yet (nextDueDate: ${task.nextDueDate})")
                            }
                        }
                        else -> {
                            Log.d("QuestFlow_Recurring", "Trigger mode: ${task.triggerMode} - skipping (not AFTER_EXPIRY or FIXED_INTERVAL)")
                        }
                    }
                } ?: run {
                    if (expiredLink.isRecurring) {
                        Log.w("QuestFlow_Recurring", "⚠️ Link marked as recurring but recurringTaskId is NULL!")
                        Log.w("QuestFlow_Recurring", "   This task was likely created BEFORE the fix was deployed")
                    } else {
                        Log.d("QuestFlow_Recurring", "Link is not recurring - skipping")
                    }
                }
            }

            // Save current time as last check time
            syncPreferences.saveLastCheckTime(now)

        } finally {
            syncPreferences.setSyncInProgress(false)
        }

        Log.d("CheckExpiredEvents", "Check complete: $expiredCount expired, $deletedCount deleted, $recurringCreated recurring created")
        return CheckResult(expiredCount, deletedCount, recurringCreated)
    }

    private suspend fun handleRecurringTask(
        task: com.example.questflow.domain.model.Task,
        baseTime: LocalDateTime,
        expiredLink: com.example.questflow.data.database.entity.CalendarEventLinkEntity
    ): Boolean {
        val taskDurationMinutes = 60L

        val calculatedNextStart = when (task.recurringType) {
            "DAILY" -> baseTime.plusDays(task.recurringInterval?.toLong() ?: 1)
            "WEEKLY" -> baseTime.plusWeeks(1)
            "MONTHLY" -> baseTime.plusMonths(1)
            "CUSTOM" -> {
                val minutes = task.recurringInterval ?: 60
                baseTime.plusMinutes(minutes.toLong())
            }
            else -> null
        }

        calculatedNextStart?.let { initialNextStart ->
            val nextStartTime = try {
                Log.d("QuestFlow_Recurring", "Searching for free slot from $initialNextStart (duration: ${taskDurationMinutes}min)")

                val freeSlot = findFreeTimeSlotsUseCase.findNextAvailableSlot(
                    requiredDurationMinutes = taskDurationMinutes,
                    startSearchFrom = initialNextStart,
                    maxDaysToSearch = 7,
                    excludeEventId = expiredLink.calendarEventId
                )

                if (freeSlot != null) {
                    Log.d("QuestFlow_Recurring", "Found free slot: ${freeSlot.startTime} - ${freeSlot.endTime}")
                    freeSlot.startTime
                } else {
                    Log.w("QuestFlow_Recurring", "No free slot found, using calculated time: $initialNextStart")
                    initialNextStart
                }
            } catch (e: Exception) {
                Log.e("QuestFlow_Recurring", "Error finding free slot: ${e.message}", e)
                initialNextStart
            }

            val nextEndTime = nextStartTime.plusMinutes(taskDurationMinutes)

            Log.d("QuestFlow_Recurring", "Updating task ID=${task.id} times: $nextStartTime - $nextEndTime")

            // Update Task with new dueDate
            val updatedTask = task.copy(
                dueDate = nextStartTime,
                isCompleted = false
            )
            taskRepository.updateTask(updatedTask)

            // Update CalendarLink with new times and status PENDING
            val updatedLink = expiredLink.copy(
                startsAt = nextStartTime,
                endsAt = nextEndTime,
                status = "PENDING",
                expiredAt = null,
                rewarded = false
            )
            calendarLinkRepository.updateLink(updatedLink)

            // Update Google Calendar event if exists
            if (calendarManager.hasCalendarPermission() && expiredLink.calendarEventId > 0) {
                try {
                    calendarManager.updateTaskEvent(
                        eventId = expiredLink.calendarEventId,
                        taskTitle = expiredLink.title,
                        taskDescription = "",
                        startTime = nextStartTime,
                        endTime = nextEndTime
                    )
                    Log.d("QuestFlow_Recurring", "Updated Google Calendar event ID=${expiredLink.calendarEventId}")
                } catch (e: Exception) {
                    Log.e("QuestFlow_Recurring", "Failed to update calendar event: ${e.message}")
                }
            }

            Log.d("QuestFlow_Recurring", "✅ Updated task ID=${task.id} to new time: $nextStartTime - $nextEndTime")
            return true
        }
        return false
    }
}
