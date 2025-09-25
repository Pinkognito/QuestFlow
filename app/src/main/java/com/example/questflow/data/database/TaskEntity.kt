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
    val difficulty: String = "MEDIUM",
    val xpOverride: Int? = null,
    val xpReward: Int = 10,
    val xpPercentage: Int = 40, // Store XP as percentage of level requirement (20, 40, 60, 80, 100)
    val categoryId: Long? = null, // Associated category
    val calendarEventId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
)

enum class TaskDifficulty {
    TRIVIAL,    // 20% of level XP requirement
    EASY,       // 40% of level XP requirement
    MEDIUM,     // 60% of level XP requirement
    HARD,       // 80% of level XP requirement
    EPIC        // 100% of level XP requirement
}