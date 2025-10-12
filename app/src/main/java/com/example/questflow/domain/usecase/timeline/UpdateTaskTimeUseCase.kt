package com.example.questflow.domain.usecase.timeline

import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.domain.usecase.UpdateTaskWithCalendarUseCase
import com.example.questflow.presentation.components.RecurringConfig
import com.example.questflow.presentation.components.RecurringMode
import com.example.questflow.presentation.components.TriggerMode
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Updates task time after drag & drop in timeline view.
 * Handles both task and calendar event updates.
 */
class UpdateTaskTimeUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarLinkRepository: CalendarLinkRepository,
    private val calendarManager: CalendarManager,
    private val updateTaskWithCalendarUseCase: UpdateTaskWithCalendarUseCase
) {

    /**
     * Result of update operation
     */
    sealed class Result {
        object Success : Result()
        data class Error(val message: String, val throwable: Throwable? = null) : Result()
    }

    /**
     * Update task times after drag & drop.
     *
     * @param taskId Task ID (can be null for calendar-only events)
     * @param linkId CalendarEventLink ID (can be null for tasks without calendar)
     * @param newStartTime New start time
     * @param newEndTime New end time
     * @return Result of the operation
     */
    suspend operator fun invoke(
        taskId: Long?,
        linkId: Long?,
        newStartTime: LocalDateTime,
        newEndTime: LocalDateTime
    ): Result {
        return try {
            android.util.Log.d("UpdateTaskTimeUseCase", "Updating task: taskId=$taskId, linkId=$linkId, start=$newStartTime, end=$newEndTime")

            when {
                // Task with calendar link - update both
                taskId != null && linkId != null -> {
                    updateTaskWithCalendar(taskId, linkId, newStartTime, newEndTime)
                }
                // Task only (no calendar link) - update dueDate
                taskId != null && linkId == null -> {
                    updateTaskOnly(taskId, newStartTime)
                }
                // Calendar link only (no task) - update link
                taskId == null && linkId != null -> {
                    updateLinkOnly(linkId, newStartTime, newEndTime)
                }
                else -> {
                    android.util.Log.e("UpdateTaskTimeUseCase", "Invalid state: both taskId and linkId are null")
                    Result.Error("Invalid task/link combination")
                }
            }

            android.util.Log.d("UpdateTaskTimeUseCase", "Update successful")
            Result.Success

        } catch (e: Exception) {
            android.util.Log.e("UpdateTaskTimeUseCase", "Update failed", e)
            Result.Error("Failed to update task time: ${e.message}", e)
        }
    }

    /**
     * Update task with calendar link using central UpdateTaskWithCalendarUseCase
     */
    private suspend fun updateTaskWithCalendar(
        taskId: Long,
        linkId: Long,
        newStartTime: LocalDateTime,
        newEndTime: LocalDateTime
    ) {
        val task = taskRepository.getTaskById(taskId) ?: run {
            throw IllegalStateException("Task not found: $taskId")
        }

        val link = calendarLinkRepository.getAllLinks().first().find { it.id == linkId } ?: run {
            throw IllegalStateException("Calendar link not found: $linkId")
        }

        // Use UpdateTaskWithCalendarUseCase for consistency
        // Convert task recurring fields to RecurringConfig to preserve existing settings
        val recurringConfig = if (task.isRecurring) {
            taskToRecurringConfig(task)
        } else null

        val params = UpdateTaskWithCalendarUseCase.UpdateParams(
            taskId = taskId,
            linkId = linkId,
            title = task.title,
            description = task.description,
            xpPercentage = task.xpPercentage,
            startDateTime = newStartTime,
            endDateTime = newEndTime,
            categoryId = task.categoryId,
            shouldReactivate = false,
            addToCalendar = link.calendarEventId != 0L,
            deleteOnClaim = link.deleteOnClaim,
            deleteOnExpiry = link.deleteOnExpiry,
            isRecurring = task.isRecurring,
            recurringConfig = recurringConfig,
            parentTaskId = task.parentTaskId,
            autoCompleteParent = task.autoCompleteParent
        )

        when (val result = updateTaskWithCalendarUseCase(params)) {
            is UpdateTaskWithCalendarUseCase.UpdateResult.Error -> {
                throw IllegalStateException(result.message, result.throwable)
            }
            is UpdateTaskWithCalendarUseCase.UpdateResult.Success -> {
                android.util.Log.d("UpdateTaskTimeUseCase", "Task with calendar updated successfully")
            }
        }
    }

    /**
     * Update task without calendar link (only dueDate)
     */
    private suspend fun updateTaskOnly(taskId: Long, newStartTime: LocalDateTime) {
        val task = taskRepository.getTaskById(taskId) ?: run {
            throw IllegalStateException("Task not found: $taskId")
        }

        val updatedTask = task.copy(dueDate = newStartTime)
        taskRepository.updateTask(updatedTask)

        android.util.Log.d("UpdateTaskTimeUseCase", "Task-only updated: $taskId")
    }

    /**
     * Update calendar link without associated task
     */
    private suspend fun updateLinkOnly(linkId: Long, newStartTime: LocalDateTime, newEndTime: LocalDateTime) {
        val link = calendarLinkRepository.getAllLinks().first().find { it.id == linkId } ?: run {
            throw IllegalStateException("Calendar link not found: $linkId")
        }

        // Update calendar event if exists
        if (link.calendarEventId != 0L) {
            calendarManager.updateTaskEvent(
                eventId = link.calendarEventId,
                taskTitle = link.title,
                taskDescription = "",
                startTime = newStartTime,
                endTime = newEndTime
            )
        }

        // Update link entity
        val updatedLink = link.copy(
            startsAt = newStartTime,
            endsAt = newEndTime
        )
        calendarLinkRepository.updateLink(updatedLink)

        android.util.Log.d("UpdateTaskTimeUseCase", "Link-only updated: $linkId")
    }

    /**
     * Convert Task recurring fields to RecurringConfig object.
     * Preserves existing recurring settings when updating task times.
     */
    private fun taskToRecurringConfig(task: com.example.questflow.domain.model.Task): RecurringConfig {
        // Parse recurringType
        val mode = when (task.recurringType) {
            "DAILY" -> RecurringMode.DAILY
            "WEEKLY" -> RecurringMode.WEEKLY
            "MONTHLY" -> RecurringMode.MONTHLY
            "CUSTOM" -> RecurringMode.CUSTOM
            else -> RecurringMode.DAILY
        }

        // Parse triggerMode
        val triggerMode = when (task.triggerMode) {
            "AFTER_COMPLETION" -> TriggerMode.AFTER_COMPLETION
            "AFTER_EXPIRY" -> TriggerMode.AFTER_EXPIRY
            else -> TriggerMode.FIXED_INTERVAL
        }

        // Parse weeklyDays (comma-separated string like "MONDAY,FRIDAY")
        val weeklyDays = task.recurringDays?.split(",")?.mapNotNull { dayStr ->
            try {
                java.time.DayOfWeek.valueOf(dayStr.trim())
            } catch (e: Exception) {
                null
            }
        }?.toSet() ?: emptySet()

        // Parse specificTime (HH:mm format string)
        val specificTime = task.specificTime?.let { timeStr ->
            try {
                java.time.LocalTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                null
            }
        }

        // Deserialize: recurringInterval is ALWAYS stored in MINUTES
        val intervalMinutes = task.recurringInterval ?: 60

        return RecurringConfig(
            mode = mode,
            dailyInterval = when (mode) {
                RecurringMode.DAILY -> intervalMinutes / (24 * 60)
                else -> 1
            },
            weeklyDays = weeklyDays,
            monthlyDay = when (mode) {
                RecurringMode.MONTHLY -> intervalMinutes / (24 * 60)
                else -> 1
            },
            customMinutes = when (mode) {
                RecurringMode.CUSTOM -> intervalMinutes % 60
                else -> 60
            },
            customHours = when (mode) {
                RecurringMode.CUSTOM -> intervalMinutes / 60
                else -> 0
            },
            specificTime = specificTime,
            triggerMode = triggerMode
        )
    }
}
