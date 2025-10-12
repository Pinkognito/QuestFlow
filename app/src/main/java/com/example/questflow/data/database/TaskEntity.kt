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
    val xpReward: Int = 10,
    val xpPercentage: Int = 40, // Store XP as percentage of level requirement (20, 40, 60, 80, 100)
    val categoryId: Long? = null, // Associated category
    val calendarEventId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    // Statistics metadata
    val estimatedMinutes: Int? = null, // Estimated duration in minutes
    val tags: String? = null, // Comma-separated tags
    // Recurring task fields
    val isRecurring: Boolean = false,
    val recurringType: String? = null, // DAILY, WEEKLY, MONTHLY, CUSTOM
    val recurringInterval: Int? = null, // Minutes for CUSTOM
    val recurringDays: String? = null, // Comma-separated weekdays for WEEKLY (MONDAY,FRIDAY)
    val specificTime: String? = null, // Specific time for DAILY/WEEKLY/MONTHLY (HH:mm format)
    val lastCompletedAt: LocalDateTime? = null,
    val nextDueDate: LocalDateTime? = null,
    val triggerMode: String? = null, // FIXED_INTERVAL, AFTER_COMPLETION, AFTER_EXPIRY
    // Task editing fields
    val isEditable: Boolean = true,
    val parentTaskId: Long? = null, // Reference to parent task for subtasks
    val autoCompleteParent: Boolean = false // Auto-complete parent when all subtasks done
)

enum class RecurringType {
    DAILY,      // Every X days
    WEEKLY,     // Specific weekdays
    MONTHLY,    // Every X months on specific day
    CUSTOM      // Every X minutes/hours
}