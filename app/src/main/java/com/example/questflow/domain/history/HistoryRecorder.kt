package com.example.questflow.domain.history

import com.example.questflow.data.database.dao.TaskHistoryDao
import com.example.questflow.data.database.entity.TaskHistoryEntity
import com.example.questflow.data.preferences.HistoryPreferences
import com.example.questflow.domain.model.HistoryEventType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for recording task history events
 *
 * Automatically checks user preferences before recording events.
 * Provides convenient methods for common event types.
 */
@Singleton
class HistoryRecorder @Inject constructor(
    private val taskHistoryDao: TaskHistoryDao,
    private val historyPreferences: HistoryPreferences
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Record a simple event without values
     */
    suspend fun recordEvent(
        taskId: Long,
        eventType: HistoryEventType,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        android.util.Log.d("HistoryRecorder", "🔍 recordEvent called: taskId=$taskId, eventType=${eventType.name}")

        val shouldRecord = historyPreferences.shouldRecordEvent(eventType)
        android.util.Log.d("HistoryRecorder", "🔍 shouldRecordEvent($eventType) = $shouldRecord")

        if (!shouldRecord) {
            android.util.Log.w("HistoryRecorder", "⚠️ Event ${eventType.name} is DISABLED in preferences - skipping")
            return
        }

        android.util.Log.d("HistoryRecorder", "✅ Recording event: ${eventType.name} for task $taskId")
        taskHistoryDao.insertIfNotExists(
            TaskHistoryEntity(
                taskId = taskId,
                eventType = eventType.name,
                timestamp = timestamp
            )
        )
        android.util.Log.d("HistoryRecorder", "✅ Event recorded successfully: ${eventType.name}")
    }

    /**
     * Record an event with a new due date (for recurring/rescheduled tasks)
     */
    suspend fun recordEventWithNewDueDate(
        taskId: Long,
        eventType: HistoryEventType,
        newDueDate: LocalDateTime,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        if (!historyPreferences.shouldRecordEvent(eventType)) return

        taskHistoryDao.insertIfNotExists(
            TaskHistoryEntity(
                taskId = taskId,
                eventType = eventType.name,
                timestamp = timestamp,
                newDueDate = newDueDate
            )
        )
    }

    /**
     * Record a property change event with old and new values
     */
    suspend fun recordPropertyChange(
        taskId: Long,
        eventType: HistoryEventType,
        oldValue: String?,
        newValue: String?,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        android.util.Log.d("HistoryRecorder", "🔍 recordPropertyChange called: taskId=$taskId, eventType=${eventType.name}, oldValue='$oldValue', newValue='$newValue'")

        val shouldRecord = historyPreferences.shouldRecordEvent(eventType)
        android.util.Log.d("HistoryRecorder", "🔍 shouldRecordEvent($eventType) = $shouldRecord")

        if (!shouldRecord) {
            android.util.Log.w("HistoryRecorder", "⚠️ Event ${eventType.name} is DISABLED in preferences - skipping")
            return
        }

        if (oldValue == newValue) {
            android.util.Log.d("HistoryRecorder", "⚠️ No change detected (oldValue == newValue) - skipping")
            return
        }

        android.util.Log.d("HistoryRecorder", "✅ Recording property change: ${eventType.name} for task $taskId")
        taskHistoryDao.insertIfNotExists(
            TaskHistoryEntity(
                taskId = taskId,
                eventType = eventType.name,
                timestamp = timestamp,
                oldValue = oldValue,
                newValue = newValue
            )
        )
        android.util.Log.d("HistoryRecorder", "✅ Property change recorded successfully: ${eventType.name}")
    }

    /**
     * Record priority change
     */
    suspend fun recordPriorityChange(
        taskId: Long,
        oldPriority: String?,
        newPriority: String?
    ) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.PRIORITY_CHANGED,
            oldValue = oldPriority,
            newValue = newPriority
        )
    }

    /**
     * Record difficulty (XP percentage) change
     */
    suspend fun recordDifficultyChange(
        taskId: Long,
        oldPercentage: Int?,
        newPercentage: Int?
    ) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.DIFFICULTY_CHANGED,
            oldValue = oldPercentage?.toString(),
            newValue = newPercentage?.toString()
        )
    }

    /**
     * Record category change
     */
    suspend fun recordCategoryChange(
        taskId: Long,
        oldCategoryId: Long?,
        newCategoryId: Long?
    ) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.CATEGORY_CHANGED,
            oldValue = oldCategoryId?.toString(),
            newValue = newCategoryId?.toString()
        )
    }

    /**
     * Record title change
     */
    suspend fun recordTitleChange(
        taskId: Long,
        oldTitle: String?,
        newTitle: String?
    ) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.TITLE_CHANGED,
            oldValue = oldTitle,
            newValue = newTitle
        )
    }

    /**
     * Record description change
     */
    suspend fun recordDescriptionChange(
        taskId: Long,
        oldDescription: String?,
        newDescription: String?
    ) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.DESCRIPTION_CHANGED,
            oldValue = oldDescription,
            newValue = newDescription
        )
    }

    /**
     * Record due date change
     */
    suspend fun recordDueDateChange(
        taskId: Long,
        oldDueDate: LocalDateTime?,
        newDueDate: LocalDateTime?
    ) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.DUE_DATE_CHANGED,
            oldValue = oldDueDate?.format(dateFormatter),
            newValue = newDueDate?.format(dateFormatter)
        )
    }

    /**
     * Record task completion
     */
    suspend fun recordCompletion(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.COMPLETED
        )
    }

    /**
     * Record task uncompletion (claim reverted)
     */
    suspend fun recordUncompletion(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.UNCOMPLETED
        )
    }

    /**
     * Record XP claim
     */
    suspend fun recordXpClaimed(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.CLAIMED
        )
    }

    /**
     * Record XP reclaim (after reactivation)
     */
    suspend fun recordXpReclaimed(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.RECLAIMED
        )
    }

    /**
     * Record XP becoming reclaimable (uncomplete)
     */
    suspend fun recordXpReclaimable(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.XP_RECLAIMABLE
        )
    }

    /**
     * Record task expiration
     */
    suspend fun recordExpired(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.EXPIRED
        )
    }

    /**
     * Record task deletion
     */
    suspend fun recordDeleted(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.DELETED
        )
    }

    /**
     * Record task restoration from trash
     */
    suspend fun recordRestored(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.RESTORED
        )
    }

    /**
     * Record parent task assignment
     */
    suspend fun recordParentAssigned(taskId: Long, parentId: Long) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.PARENT_ASSIGNED,
            oldValue = null,
            newValue = parentId.toString()
        )
    }

    /**
     * Record parent task removal
     */
    suspend fun recordParentRemoved(taskId: Long, oldParentId: Long) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.PARENT_REMOVED,
            oldValue = oldParentId.toString(),
            newValue = null
        )
    }

    /**
     * Record calendar event added
     */
    suspend fun recordCalendarAdded(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.CALENDAR_ADDED
        )
    }

    /**
     * Record calendar event removed
     */
    suspend fun recordCalendarRemoved(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.CALENDAR_REMOVED
        )
    }

    /**
     * Record recurring enabled
     */
    suspend fun recordRecurringEnabled(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.RECURRING_ENABLED
        )
    }

    /**
     * Record recurring disabled
     */
    suspend fun recordRecurringDisabled(taskId: Long) {
        recordEvent(
            taskId = taskId,
            eventType = HistoryEventType.RECURRING_DISABLED
        )
    }

    /**
     * Record recurring instance created
     */
    suspend fun recordRecurringCreated(taskId: Long, newDueDate: LocalDateTime) {
        recordEventWithNewDueDate(
            taskId = taskId,
            eventType = HistoryEventType.RECURRING_CREATED,
            newDueDate = newDueDate
        )
    }

    /**
     * Record task rescheduled
     */
    suspend fun recordRescheduled(taskId: Long, newDueDate: LocalDateTime) {
        recordEventWithNewDueDate(
            taskId = taskId,
            eventType = HistoryEventType.RESCHEDULED,
            newDueDate = newDueDate
        )
    }

    /**
     * Record recurring config changed
     */
    suspend fun recordRecurringConfigChanged(
        taskId: Long,
        oldConfig: String?,
        newConfig: String?
    ) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.RECURRING_CONFIG_CHANGED,
            oldValue = oldConfig,
            newValue = newConfig
        )
    }

    /**
     * Record contact added
     */
    suspend fun recordContactAdded(taskId: Long, contactId: Long) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.CONTACT_ADDED,
            oldValue = null,
            newValue = contactId.toString()
        )
    }

    /**
     * Record contact removed
     */
    suspend fun recordContactRemoved(taskId: Long, contactId: Long) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.CONTACT_REMOVED,
            oldValue = contactId.toString(),
            newValue = null
        )
    }

    /**
     * Record action executed
     */
    suspend fun recordActionExecuted(taskId: Long, actionType: String) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.ACTION_EXECUTED,
            oldValue = null,
            newValue = actionType
        )
    }

    /**
     * Record tag added
     */
    suspend fun recordTagAdded(taskId: Long, tagName: String) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.TAG_ADDED,
            oldValue = null,
            newValue = tagName
        )
    }

    /**
     * Record tag removed
     */
    suspend fun recordTagRemoved(taskId: Long, tagName: String) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.TAG_REMOVED,
            oldValue = tagName,
            newValue = null
        )
    }

    /**
     * Record note added
     */
    suspend fun recordNoteAdded(taskId: Long, notePreview: String) {
        recordPropertyChange(
            taskId = taskId,
            eventType = HistoryEventType.NOTE_ADDED,
            oldValue = null,
            newValue = notePreview.take(50) // Limit preview length
        )
    }
}
