package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.domain.model.Priority
import com.example.questflow.presentation.components.RecurringConfig
import com.example.questflow.presentation.components.RecurringMode
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zentrale UseCase für alle Task-Updates mit Calendar-Integration.
 *
 * Verantwortlich für:
 * - Atomare Updates von Task, CalendarLink und Google Calendar Event
 * - Konsistente Status-Verwaltung
 * - Deterministische Calendar-Operations (DELETE/CREATE/UPDATE/NONE)
 *
 * Ersetzt die duplizierten Update-Methoden in TodayViewModel und CalendarXpViewModel.
 */
@Singleton
class UpdateTaskWithCalendarUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarLinkRepository: CalendarLinkRepository,
    private val calendarManager: CalendarManager,
    private val categoryRepository: CategoryRepository,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase
) {
    data class UpdateParams(
        val taskId: Long?,
        val linkId: Long,
        val title: String,
        val description: String,
        val xpPercentage: Int,
        val dateTime: LocalDateTime,
        val categoryId: Long?,
        val shouldReactivate: Boolean = false,
        val addToCalendar: Boolean = true, // User wants calendar integration
        val deleteOnClaim: Boolean = false,
        val deleteOnExpiry: Boolean = false,
        val isRecurring: Boolean = false,
        val recurringConfig: RecurringConfig? = null
    )

    sealed class UpdateResult {
        data class Success(val updatedTaskId: Long?, val updatedLinkId: Long) : UpdateResult()
        data class Error(val message: String, val throwable: Throwable? = null) : UpdateResult()
    }

    suspend operator fun invoke(params: UpdateParams): UpdateResult {
        return try {
            performAtomicUpdate(params)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Update fehlgeschlagen", e)
            UpdateResult.Error("Update fehlgeschlagen: ${e.message}", e)
        }
    }

    private suspend fun performAtomicUpdate(params: UpdateParams): UpdateResult {
        // 1. Hole existierende Entities
        val existingLink = calendarLinkRepository.getLinkById(params.linkId)
            ?: return UpdateResult.Error("Link nicht gefunden")

        val existingTask = params.taskId?.let { taskRepository.getTaskById(it) }

        // 2. Berechne XP und Priority
        val currentLevel = params.categoryId?.let {
            categoryRepository.getCategoryById(it)?.currentLevel
        } ?: 1
        val xpReward = calculateXpRewardUseCase(params.xpPercentage, currentLevel)
        val priority = mapPercentageToPriority(params.xpPercentage)

        // 3. Bestimme Calendar Event Operation
        val now = LocalDateTime.now()
        val isExpiredNow = params.dateTime.plusHours(1) <= now
        val wasExpiredByStatus = existingLink.status == "EXPIRED"
        val hadCalendarEvent = existingLink.calendarEventId > 0

        val calendarOp = determineCalendarOperation(
            addToCalendar = params.addToCalendar,
            isExpiredNow = isExpiredNow,
            wasExpiredByStatus = wasExpiredByStatus,
            hadCalendarEvent = hadCalendarEvent,
            deleteOnExpiry = params.deleteOnExpiry,
            existingLink = existingLink
        )

        // 4. Führe Calendar-Operation aus (vor DB-Update!)
        val newCalendarEventId = executeCalendarOperation(
            operation = calendarOp,
            eventId = existingLink.calendarEventId,
            title = params.title,
            description = params.description,
            dateTime = params.dateTime,
            xpReward = xpReward,
            xpPercentage = params.xpPercentage,
            categoryId = params.categoryId
        )

        // 5. Update Task in DB (falls vorhanden)
        existingTask?.let { task ->
            val recurringType = params.recurringConfig?.let {
                when (it.mode) {
                    RecurringMode.DAILY -> "DAILY"
                    RecurringMode.WEEKLY -> "WEEKLY"
                    RecurringMode.MONTHLY -> "MONTHLY"
                    RecurringMode.CUSTOM -> "CUSTOM"
                }
            }

            val recurringInterval = params.recurringConfig?.let {
                when (it.mode) {
                    RecurringMode.DAILY -> it.dailyInterval * 24 * 60
                    RecurringMode.WEEKLY -> 7 * 24 * 60
                    RecurringMode.MONTHLY -> it.monthlyDay * 24 * 60
                    RecurringMode.CUSTOM -> it.customHours * 60 + it.customMinutes
                }
            }

            val recurringDays = params.recurringConfig?.let {
                if (it.mode == RecurringMode.WEEKLY) {
                    it.weeklyDays.map { day -> day.value }.joinToString(",")
                } else null
            }

            val updatedTask = task.copy(
                title = params.title,
                description = params.description,
                xpPercentage = params.xpPercentage,
                xpReward = xpReward,
                dueDate = params.dateTime,
                priority = priority,
                categoryId = params.categoryId,
                isRecurring = params.isRecurring,
                recurringType = recurringType,
                recurringInterval = recurringInterval,
                recurringDays = recurringDays,
                triggerMode = params.recurringConfig?.triggerMode?.name,
                isCompleted = if (params.shouldReactivate) false else task.isCompleted,
                calendarEventId = newCalendarEventId ?: task.calendarEventId
            )

            taskRepository.updateTask(updatedTask)
        }

        // 6. Update Link in DB
        val newStatus = determineNewStatus(
            shouldReactivate = params.shouldReactivate,
            isExpiredNow = isExpiredNow,
            existingLink = existingLink
        )

        val finalCalendarEventId = when {
            newCalendarEventId != null -> newCalendarEventId
            calendarOp == CalendarOperation.DELETE -> 0L
            else -> existingLink.calendarEventId
        }

        val updatedLink = existingLink.copy(
            title = params.title,
            startsAt = params.dateTime,
            endsAt = params.dateTime.plusHours(1),
            xpPercentage = params.xpPercentage,
            categoryId = params.categoryId,
            deleteOnClaim = params.deleteOnClaim,
            deleteOnExpiry = params.deleteOnExpiry,
            isRecurring = params.isRecurring,
            status = newStatus,
            rewarded = if (params.shouldReactivate) false else existingLink.rewarded,
            calendarEventId = finalCalendarEventId
        )
        calendarLinkRepository.updateLink(updatedLink)

        return UpdateResult.Success(
            updatedTaskId = params.taskId,
            updatedLinkId = params.linkId
        )
    }

    // === HELPER: Calendar Operation Determination ===

    private enum class CalendarOperation {
        DELETE, CREATE, UPDATE, REPLACE, NONE
    }

    private fun determineCalendarOperation(
        addToCalendar: Boolean,
        isExpiredNow: Boolean,
        wasExpiredByStatus: Boolean,
        hadCalendarEvent: Boolean,
        deleteOnExpiry: Boolean,
        existingLink: CalendarEventLinkEntity
    ): CalendarOperation {
        // Wenn User Calendar-Integration NICHT will → DELETE falls Event existiert
        if (!addToCalendar && hadCalendarEvent) {
            return CalendarOperation.DELETE
        }

        // Wenn User Calendar-Integration NICHT will und kein Event → NONE
        if (!addToCalendar && !hadCalendarEvent) {
            return CalendarOperation.NONE
        }

        // Ab hier: addToCalendar = true (User WILL Calendar-Integration)
        return when {
            // Case 1: Expired + deleteOnExpiry ON → DELETE
            isExpiredNow && deleteOnExpiry && hadCalendarEvent -> CalendarOperation.DELETE

            // Case 2: War expired, jetzt future → REPLACE (altes löschen, neues erstellen)
            wasExpiredByStatus && !isExpiredNow && hadCalendarEvent -> CalendarOperation.REPLACE

            // Case 2b: War expired, jetzt future, kein Event → CREATE
            wasExpiredByStatus && !isExpiredNow && !hadCalendarEvent -> CalendarOperation.CREATE

            // Case 3: deleteOnExpiry war ON, jetzt OFF → CREATE wenn kein Event
            isExpiredNow && !deleteOnExpiry && existingLink.deleteOnExpiry && !hadCalendarEvent -> CalendarOperation.CREATE

            // Case 4: Normal update (nur wenn Event existiert!)
            !isExpiredNow && hadCalendarEvent -> CalendarOperation.UPDATE

            // Case 5: Nicht expired, aber kein Event → CREATE
            !isExpiredNow && !hadCalendarEvent -> CalendarOperation.CREATE

            // Case 6: Expired, deleteOnExpiry OFF, aber kein Event → CREATE
            isExpiredNow && !deleteOnExpiry && !hadCalendarEvent -> CalendarOperation.CREATE

            else -> CalendarOperation.NONE
        }
    }

    // === HELPER: Execute Calendar Operation ===

    private suspend fun executeCalendarOperation(
        operation: CalendarOperation,
        eventId: Long,
        title: String,
        description: String,
        dateTime: LocalDateTime,
        xpReward: Int,
        xpPercentage: Int,
        categoryId: Long?
    ): Long? {
        if (!calendarManager.hasCalendarPermission()) {
            android.util.Log.w(TAG, "  No calendar permission, skipping operation")
            return null
        }

        return when (operation) {
            CalendarOperation.DELETE -> {
                calendarManager.deleteEvent(eventId)
                null // Kein Event mehr
            }

            CalendarOperation.CREATE -> {
                val category = categoryId?.let { categoryRepository.getCategoryById(it) }
                val eventTitle = if (category != null) {
                    "${category.emoji} $title"
                } else {
                    "🎯 $title"
                }

                calendarManager.createTaskEvent(
                    taskTitle = eventTitle,
                    taskDescription = description,
                    startTime = dateTime,
                    endTime = dateTime.plusHours(1),
                    xpReward = xpReward,
                    xpPercentage = xpPercentage,
                    categoryColor = category?.color
                )
            }

            CalendarOperation.UPDATE -> {
                val category = categoryId?.let { categoryRepository.getCategoryById(it) }
                val eventTitle = if (category != null) {
                    "${category.emoji} $title"
                } else {
                    "🎯 $title"
                }

                calendarManager.updateTaskEvent(
                    eventId = eventId,
                    taskTitle = eventTitle,
                    taskDescription = description,
                    startTime = dateTime,
                    endTime = dateTime.plusHours(1)
                )
                eventId // Gleiche ID
            }

            CalendarOperation.REPLACE -> {
                // 1. Delete old event
                calendarManager.deleteEvent(eventId)

                // 2. Create new event
                val category = categoryId?.let { categoryRepository.getCategoryById(it) }
                val eventTitle = if (category != null) {
                    "${category.emoji} $title"
                } else {
                    "🎯 $title"
                }

                calendarManager.createTaskEvent(
                    taskTitle = eventTitle,
                    taskDescription = description,
                    startTime = dateTime,
                    endTime = dateTime.plusHours(1),
                    xpReward = xpReward,
                    xpPercentage = xpPercentage,
                    categoryColor = category?.color
                )
            }

            CalendarOperation.NONE -> {
                eventId // Keine Änderung
            }
        }
    }

    // === HELPER: Status & Priority ===

    private fun mapPercentageToPriority(percentage: Int): Priority {
        return when (percentage) {
            20, 40 -> Priority.LOW
            60 -> Priority.MEDIUM
            80, 100 -> Priority.HIGH
            else -> Priority.MEDIUM
        }
    }

    private fun determineNewStatus(
        shouldReactivate: Boolean,
        isExpiredNow: Boolean,
        existingLink: CalendarEventLinkEntity
    ): String {
        return when {
            shouldReactivate -> "PENDING"
            isExpiredNow && !existingLink.rewarded -> "EXPIRED"
            !isExpiredNow -> "PENDING"
            else -> existingLink.status
        }
    }

    companion object {
        private const val TAG = "UpdateTaskWithCalendarUseCase"
    }
}