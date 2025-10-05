package com.example.questflow.domain.model

import java.time.LocalDateTime

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val xpReward: Int = 10,
    val xpPercentage: Int = 60, // Percentage of level requirement (20, 40, 60, 80, 100)
    val categoryId: Long? = null,
    val calendarEventId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    // Recurring task fields
    val isRecurring: Boolean = false,
    val recurringType: String? = null,
    val recurringInterval: Int? = null,
    val recurringDays: String? = null,
    val triggerMode: String? = null,
    val lastCompletedAt: LocalDateTime? = null,
    val nextDueDate: LocalDateTime? = null,
    val isEditable: Boolean = true,
    val parentTaskId: Long? = null,
    val autoCompleteParent: Boolean = false
)

enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}