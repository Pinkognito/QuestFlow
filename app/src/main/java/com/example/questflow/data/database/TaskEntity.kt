package com.example.questflow.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM",
    val dueDate: LocalDateTime? = null,
    @Deprecated("Unused field, replaced by xpPercentage. Will be removed in future version.")
    val difficulty: String = "MEDIUM",
    @Deprecated("Unused field, replaced by xpPercentage. Will be removed in future version.")
    val xpOverride: Int? = null,
    val xpReward: Int = 10,
    val xpPercentage: Int = 40, // Store XP as percentage of level requirement (20, 40, 60, 80, 100)
    val categoryId: Long? = null, // Associated category
    val calendarEventId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    // Recurring task fields
    val isRecurring: Boolean = false,
    val recurringType: String? = null, // DAILY, WEEKLY, MONTHLY, CUSTOM
    val recurringInterval: Int? = null, // Minutes for CUSTOM
    val recurringDays: String? = null, // JSON array of weekdays for WEEKLY
    val lastCompletedAt: LocalDateTime? = null,
    val nextDueDate: LocalDateTime? = null,
    val triggerMode: String? = null, // FIXED_INTERVAL, AFTER_COMPLETION, AFTER_EXPIRY
    // Task editing fields
    val isEditable: Boolean = true,
    val parentTaskId: Long? = null // Reference to parent recurring task
)

enum class RecurringType {
    DAILY,      // Every X days
    WEEKLY,     // Specific weekdays
    MONTHLY,    // Every X months on specific day
    CUSTOM      // Every X minutes/hours
}