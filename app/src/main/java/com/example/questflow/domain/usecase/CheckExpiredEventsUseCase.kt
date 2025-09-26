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
    private val syncPreferences: SyncPreferences
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
                        calendarManager.deleteCalendarEvent(expiredLink.calendarEventId)
                        deletedCount++
                        Log.d("CheckExpiredEvents", "Deleted expired calendar event: ${expiredLink.title}")
                    } catch (e: Exception) {
                        Log.e("CheckExpiredEvents", "Failed to delete calendar event: ${e.message}")
                    }
                }

                // Check if this is a recurring event
                expiredLink.recurringTaskId?.let { taskId ->
                    val task = taskRepository.getTaskById(taskId)
                    task?.let { taskModel ->
                        when (taskModel.triggerMode) {
                            "AFTER_EXPIRY" -> {
                                // Create new instance after expiry
                                if (handleRecurringTask(taskModel, now)) {
                                    recurringCreated++
                                }
                            }
                            "FIXED_INTERVAL" -> {
                                // Create next instance at fixed time
                                if (taskModel.nextDueDate != null && taskModel.nextDueDate <= now) {
                                    if (handleRecurringTask(taskModel, taskModel.nextDueDate)) {
                                        recurringCreated++
                                    }
                                }
                            }
                            else -> {
                                // Do nothing for AFTER_COMPLETION or null
                            }
                        }
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

    private suspend fun handleRecurringTask(task: com.example.questflow.domain.model.Task, baseTime: LocalDateTime): Boolean {
        // Calculate next due date based on recurring settings
        val nextDueDate = when (task.recurringType) {
            "DAILY" -> baseTime.plusDays(task.recurringInterval?.toLong() ?: 1)
            "WEEKLY" -> baseTime.plusWeeks(1)
            "MONTHLY" -> baseTime.plusMonths(1)
            "CUSTOM" -> {
                val minutes = task.recurringInterval ?: 60
                baseTime.plusMinutes(minutes.toLong())
            }
            else -> null
        }

        nextDueDate?.let { nextDate ->
            // Create new task instance
            val newTask = task.copy(
                id = 0,
                dueDate = nextDate,
                nextDueDate = nextDate,
                isCompleted = false,
                parentTaskId = task.id,
                isEditable = true
            )
            taskRepository.insertTask(newTask)
            Log.d("CheckExpiredEvents", "Created recurring task: ${task.title} due at $nextDate")
            return true
        }
        return false
    }
}