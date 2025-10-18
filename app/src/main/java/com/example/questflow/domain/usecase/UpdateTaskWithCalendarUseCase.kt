package com.example.questflow.domain.usecase

import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.domain.model.Priority
import com.example.questflow.presentation.components.RecurringConfig
import com.example.questflow.presentation.components.RecurringMode
import kotlinx.coroutines.flow.first
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
 * Ersetzt die duplizierten Update-Methoden in TodayViewModel und TasksViewModel.
 */
@Singleton
class UpdateTaskWithCalendarUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarLinkRepository: CalendarLinkRepository,
    private val calendarManager: CalendarManager,
    private val categoryRepository: CategoryRepository,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase,
    private val notificationScheduler: com.example.questflow.domain.notification.TaskNotificationScheduler,
    private val placeholderResolver: com.example.questflow.domain.placeholder.PlaceholderResolver,
    private val taskContactLinkDao: com.example.questflow.data.database.dao.TaskContactLinkDao
) {
    data class UpdateParams(
        val taskId: Long?,
        val linkId: Long,
        val title: String,
        val description: String,
        val xpPercentage: Int,
        val startDateTime: LocalDateTime,
        val endDateTime: LocalDateTime,
        val categoryId: Long?,
        val shouldReactivate: Boolean = false,
        val addToCalendar: Boolean = true, // User wants calendar integration
        val deleteOnClaim: Boolean = false,
        val deleteOnExpiry: Boolean = false,
        val isRecurring: Boolean = false,
        val recurringConfig: RecurringConfig? = null,
        val parentTaskId: Long? = null,
        val autoCompleteParent: Boolean = false,
        val calendarEventCustomTitle: String? = null,
        val calendarEventCustomDescription: String? = null
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

        // 1.5. Resolve placeholders in title, description, and calendar fields
        val contactId: Long? = if (params.taskId != null) {
            try {
                val contacts = taskContactLinkDao.getContactsByTaskId(params.taskId).first()
                contacts.firstOrNull()?.id
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Could not get contacts for taskId ${params.taskId}", e)
                null
            }
        } else {
            null
        }

        val resolvedTitle = if (params.title.contains("{") && params.taskId != null && contactId != null) {
            placeholderResolver.resolve(params.title, params.taskId, contactId)
        } else {
            params.title
        }

        val resolvedDescription = if (params.description.contains("{") && params.taskId != null && contactId != null) {
            placeholderResolver.resolve(params.description, params.taskId, contactId)
        } else {
            params.description
        }

        val resolvedCalendarTitle = if (params.calendarEventCustomTitle != null && params.calendarEventCustomTitle.contains("{") && params.taskId != null && contactId != null) {
            placeholderResolver.resolve(params.calendarEventCustomTitle, params.taskId, contactId)
        } else {
            params.calendarEventCustomTitle
        }

        val resolvedCalendarDescription = if (params.calendarEventCustomDescription != null && params.calendarEventCustomDescription.contains("{") && params.taskId != null && contactId != null) {
            placeholderResolver.resolve(params.calendarEventCustomDescription, params.taskId, contactId)
        } else {
            params.calendarEventCustomDescription
        }

        android.util.Log.d(TAG, "Resolved placeholders in UPDATE (contactId=$contactId):")
        android.util.Log.d(TAG, "  Title: '${params.title}' -> '$resolvedTitle'")
        android.util.Log.d(TAG, "  Description: '${params.description}' -> '$resolvedDescription'")
        android.util.Log.d(TAG, "  Calendar Title: '${params.calendarEventCustomTitle}' -> '$resolvedCalendarTitle'")
        android.util.Log.d(TAG, "  Calendar Description: '${params.calendarEventCustomDescription}' -> '$resolvedCalendarDescription'")

        // 2. Berechne XP und Priority
        val currentLevel = params.categoryId?.let {
            categoryRepository.getCategoryById(it)?.currentLevel
        } ?: 1
        val xpReward = calculateXpRewardUseCase(params.xpPercentage, currentLevel)
        val priority = mapPercentageToPriority(params.xpPercentage)

        // 3. Bestimme Calendar Event Operation
        val now = LocalDateTime.now()
        val isExpiredNow = params.endDateTime <= now
        val wasExpiredByStatus = existingLink.status == "EXPIRED"
        val hadCalendarEvent = existingLink.calendarEventId > 0

        val calendarOp = determineCalendarOperation(
            addToCalendar = params.addToCalendar,
            isExpiredNow = isExpiredNow,
            wasExpiredByStatus = wasExpiredByStatus,
            hadCalendarEvent = hadCalendarEvent,
            deleteOnExpiry = params.deleteOnExpiry,
            deleteOnClaim = params.deleteOnClaim,
            shouldReactivate = params.shouldReactivate,
            existingLink = existingLink
        )

        // 4. Führe Calendar-Operation aus (vor DB-Update!)
        val newCalendarEventId = executeCalendarOperation(
            operation = calendarOp,
            eventId = existingLink.calendarEventId,
            title = resolvedTitle,
            description = resolvedDescription,
            startDateTime = params.startDateTime,
            endDateTime = params.endDateTime,
            xpReward = xpReward,
            xpPercentage = params.xpPercentage,
            categoryId = params.categoryId,
            taskId = params.taskId,
            customTitle = resolvedCalendarTitle,
            customDescription = resolvedCalendarDescription
        )

        // 5. Update Task in DB (falls vorhanden)
        android.util.Log.d(TAG, "🔍 existingTask is ${if (existingTask == null) "NULL" else "present (id=${existingTask.id})"}")
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
                    it.weeklyDays.map { day -> day.name }.joinToString(",")
                } else null
            }

            val specificTime = params.recurringConfig?.specificTime?.let {
                val formatted = it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                android.util.Log.d(TAG, "Saving specificTime: LocalTime=$it, formatted='$formatted'")
                formatted
            }

            android.util.Log.d(TAG, "Updating task - recurringType=$recurringType, recurringInterval=$recurringInterval, recurringDays=$recurringDays, specificTime=$specificTime, triggerMode=${params.recurringConfig?.triggerMode?.name}")
            android.util.Log.d(TAG, "🔍 Updating task description: '${params.description}' -> '$resolvedDescription' (was: '${task.description}')")

            val updatedTask = task.copy(
                title = resolvedTitle,
                description = resolvedDescription,
                xpPercentage = params.xpPercentage,
                xpReward = xpReward,
                dueDate = params.startDateTime,
                priority = priority,
                categoryId = params.categoryId,
                isRecurring = params.isRecurring,
                recurringType = recurringType,
                recurringInterval = recurringInterval,
                recurringDays = recurringDays,
                specificTime = specificTime,
                triggerMode = params.recurringConfig?.triggerMode?.name,
                isCompleted = if (params.shouldReactivate) false else task.isCompleted,
                calendarEventId = newCalendarEventId ?: task.calendarEventId,
                parentTaskId = params.parentTaskId,
                autoCompleteParent = params.autoCompleteParent,
                calendarEventCustomTitle = resolvedCalendarTitle,
                calendarEventCustomDescription = resolvedCalendarDescription
            )
            android.util.Log.d("DescriptionFlow-UseCase", "📝 CALLING repository.updateTask() with description='${updatedTask.description}'")

            taskRepository.updateTask(updatedTask)
            android.util.Log.d("DescriptionFlow-UseCase", "✅ repository.updateTask() completed for taskId=${task.id}")
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
            title = resolvedTitle,
            startsAt = params.startDateTime,
            endsAt = params.endDateTime,
            xpPercentage = params.xpPercentage,
            categoryId = params.categoryId,
            deleteOnClaim = params.deleteOnClaim,
            deleteOnExpiry = params.deleteOnExpiry,
            isRecurring = params.isRecurring,
            recurringTaskId = if (params.isRecurring && params.taskId != null) params.taskId else existingLink.recurringTaskId,
            status = newStatus,
            rewarded = if (params.shouldReactivate) false else existingLink.rewarded,
            calendarEventId = finalCalendarEventId
        )
        calendarLinkRepository.updateLink(updatedLink)

        // 7. Reschedule notification if task has changed and is in future
        params.taskId?.let { taskId ->
            val now = LocalDateTime.now()
            if (params.startDateTime.isAfter(now) && params.addToCalendar) {
                android.util.Log.d(TAG, "Rescheduling notification for task $taskId at ${params.startDateTime}")
                notificationScheduler.rescheduleNotification(
                    taskId = taskId,
                    title = resolvedTitle,
                    description = resolvedDescription,
                    xpReward = xpReward,
                    notificationTime = params.startDateTime
                )
            } else if (!params.startDateTime.isAfter(now)) {
                // Cancel notification if task is now in the past
                android.util.Log.d(TAG, "Cancelling notification for task $taskId (now in past)")
                notificationScheduler.cancelNotification(taskId)
            }
        }

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
        deleteOnClaim: Boolean,
        shouldReactivate: Boolean,
        existingLink: CalendarEventLinkEntity
    ): CalendarOperation {
        val wasClaimed = existingLink.rewarded

        android.util.Log.d(TAG, "=== Calendar Operation Decision ===")
        android.util.Log.d(TAG, "  addToCalendar=$addToCalendar, isExpiredNow=$isExpiredNow")
        android.util.Log.d(TAG, "  wasExpiredByStatus=$wasExpiredByStatus, hadCalendarEvent=$hadCalendarEvent")
        android.util.Log.d(TAG, "  deleteOnExpiry=$deleteOnExpiry, deleteOnClaim=$deleteOnClaim, shouldReactivate=$shouldReactivate")
        android.util.Log.d(TAG, "  existingLink.deleteOnExpiry=${existingLink.deleteOnExpiry}, existingLink.deleteOnClaim=${existingLink.deleteOnClaim}")
        android.util.Log.d(TAG, "  wasClaimed=$wasClaimed")

        // Wenn User Calendar-Integration NICHT will → DELETE falls Event existiert
        if (!addToCalendar && hadCalendarEvent) {
            android.util.Log.d(TAG, "  Decision: DELETE (user disabled calendar)")
            return CalendarOperation.DELETE
        }

        // Wenn User Calendar-Integration NICHT will und kein Event → NONE
        if (!addToCalendar && !hadCalendarEvent) {
            android.util.Log.d(TAG, "  Decision: NONE (user disabled, no event)")
            return CalendarOperation.NONE
        }

        // Ab hier: addToCalendar = true (User WILL Calendar-Integration)
        val operation = when {
            // === ABSOLUTE HIGHEST PRIORITY: REACTIVATION (überschreibt alles!) ===

            // Case 0: Reactivation → CREATE wenn kein Event (wichtig für deleteOnClaim!)
            shouldReactivate && !hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: CREATE (REACTIVATION, no event)")
                CalendarOperation.CREATE
            }

            // Case 0b: Reactivation → UPDATE wenn Event existiert
            shouldReactivate && hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: UPDATE (REACTIVATION, has event)")
                CalendarOperation.UPDATE
            }

            // === HIGHEST PRIORITY: DELETE WINS (wenn irgendeine Delete-Bedingung erfüllt ist) ===

            // Case PRIO-1: Task ist geclaimed UND deleteOnClaim ist ON → DELETE (egal ob expired oder nicht!)
            wasClaimed && deleteOnClaim && hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: DELETE PRIORITY (claimed with deleteOnClaim ON)")
                CalendarOperation.DELETE
            }

            // Case PRIO-2: Task ist expired UND deleteOnExpiry ist ON → DELETE
            isExpiredNow && deleteOnExpiry && hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: DELETE PRIORITY (expired with deleteOnExpiry ON)")
                CalendarOperation.DELETE
            }

            // === TOGGLE-ÄNDERUNGEN ===

            // Case 0c: deleteOnClaim wurde OFF geschaltet, Task IST geclaimed, kein Event → CREATE
            wasClaimed && !deleteOnClaim && existingLink.deleteOnClaim && !hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: CREATE (deleteOnClaim toggled off, was claimed)")
                CalendarOperation.CREATE
            }

            // === EXPIRY CASES ===

            // Case 3: War expired, jetzt future → REPLACE (altes löschen, neues erstellen)
            wasExpiredByStatus && !isExpiredNow && hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: REPLACE (was expired, now future)")
                CalendarOperation.REPLACE
            }

            // Case 4: War expired, jetzt future, kein Event → CREATE
            wasExpiredByStatus && !isExpiredNow && !hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: CREATE (was expired, now future, no event)")
                CalendarOperation.CREATE
            }

            // Case 5: deleteOnExpiry war ON, jetzt OFF → CREATE wenn kein Event
            isExpiredNow && !deleteOnExpiry && existingLink.deleteOnExpiry && !hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: CREATE (deleteOnExpiry toggled off)")
                CalendarOperation.CREATE
            }

            // Case 6: Normal update für NICHT expired tasks (nur wenn Event existiert!)
            !isExpiredNow && hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: UPDATE (not expired, has event)")
                CalendarOperation.UPDATE
            }

            // Case 7: Nicht expired, aber kein Event → CREATE
            !isExpiredNow && !hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: CREATE (not expired, no event)")
                CalendarOperation.CREATE
            }

            // Case 8: Expired, deleteOnExpiry OFF, HAT Event → UPDATE (!!!)
            isExpiredNow && !deleteOnExpiry && hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: UPDATE (expired but deleteOnExpiry OFF, has event)")
                CalendarOperation.UPDATE
            }

            // Case 9: Expired, deleteOnExpiry OFF, kein Event → CREATE
            isExpiredNow && !deleteOnExpiry && !hadCalendarEvent -> {
                android.util.Log.d(TAG, "  Decision: CREATE (expired, deleteOnExpiry OFF, no event)")
                CalendarOperation.CREATE
            }

            else -> {
                android.util.Log.d(TAG, "  Decision: NONE (no matching case)")
                CalendarOperation.NONE
            }
        }

        android.util.Log.d(TAG, "=== Final Operation: $operation ===")
        return operation
    }

    // === HELPER: Execute Calendar Operation ===

    private suspend fun executeCalendarOperation(
        operation: CalendarOperation,
        eventId: Long,
        title: String,
        description: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        xpReward: Int,
        xpPercentage: Int,
        categoryId: Long?,
        taskId: Long?,
        customTitle: String? = null,
        customDescription: String? = null
    ): Long? {
        android.util.Log.d(TAG, "=== Executing Calendar Operation: $operation ===")

        if (!calendarManager.hasCalendarPermission()) {
            android.util.Log.w(TAG, "  No calendar permission, skipping operation")
            return null
        }

        val result = when (operation) {
            CalendarOperation.DELETE -> {
                android.util.Log.d(TAG, "  Deleting event ID: $eventId")
                calendarManager.deleteEvent(eventId)
                null // Kein Event mehr
            }

            CalendarOperation.CREATE -> {
                android.util.Log.d(TAG, "  Creating event: title=$title, start=$startDateTime, end=$endDateTime")
                val newEventId = createCalendarEvent(
                    title, description, startDateTime, endDateTime, xpReward, xpPercentage, categoryId, taskId, customTitle, customDescription
                )
                android.util.Log.d(TAG, "  Created event with ID: $newEventId")
                newEventId
            }

            CalendarOperation.UPDATE -> {
                android.util.Log.d(TAG, "  Updating event ID: $eventId")
                val (eventTitle, _) = buildEventTitle(title, categoryId)

                val updateSuccess = calendarManager.updateTaskEvent(
                    eventId = eventId,
                    taskTitle = eventTitle,
                    taskDescription = description,
                    startTime = startDateTime,
                    endTime = endDateTime
                )

                if (!updateSuccess) {
                    // Event wurde gelöscht (z.B. von Google Calendar weil expired) → CREATE fallback!
                    android.util.Log.w(TAG, "  Update failed, falling back to CREATE")
                    val newEventId = createCalendarEvent(
                        title, description, startDateTime, endDateTime, xpReward, xpPercentage, categoryId, taskId, customTitle, customDescription
                    )
                    android.util.Log.d(TAG, "  Created new event with ID: $newEventId")
                    newEventId
                } else {
                    android.util.Log.d(TAG, "  Updated event ID: $eventId")
                    eventId // Gleiche ID
                }
            }

            CalendarOperation.REPLACE -> {
                android.util.Log.d(TAG, "  Replacing event ID: $eventId")
                calendarManager.deleteEvent(eventId)
                val newEventId = createCalendarEvent(
                    title, description, startDateTime, endDateTime, xpReward, xpPercentage, categoryId, taskId, customTitle, customDescription
                )
                android.util.Log.d(TAG, "  Replaced with new event ID: $newEventId")
                newEventId
            }

            CalendarOperation.NONE -> {
                android.util.Log.d(TAG, "  No operation needed, keeping event ID: $eventId")
                eventId // Keine Änderung
            }
        }

        android.util.Log.d(TAG, "=== Calendar Operation Complete, returning ID: $result ===")
        return result
    }

    private suspend fun buildEventTitle(title: String, categoryId: Long?): Pair<String, com.example.questflow.data.database.entity.CategoryEntity?> {
        val category = categoryId?.let { categoryRepository.getCategoryById(it) }
        val eventTitle = if (category != null) {
            "${category.emoji} $title"
        } else {
            "🎯 $title"
        }
        return Pair(eventTitle, category)
    }

    private suspend fun createCalendarEvent(
        title: String,
        description: String,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        xpReward: Int,
        xpPercentage: Int,
        categoryId: Long?,
        taskId: Long? = null,
        customTitle: String? = null,
        customDescription: String? = null
    ): Long? {
        val (eventTitle, category) = buildEventTitle(title, categoryId)
        return calendarManager.createTaskEvent(
            taskTitle = eventTitle,
            taskDescription = description,
            startTime = startDateTime,
            endTime = endDateTime,
            xpReward = xpReward,
            xpPercentage = xpPercentage,
            categoryColor = category?.color,
            taskId = taskId,
            customTitle = customTitle,
            customDescription = customDescription
        )
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