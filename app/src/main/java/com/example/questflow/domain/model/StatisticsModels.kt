package com.example.questflow.domain.model

import java.time.LocalDateTime

data class TaskStatistics(
    val totalTasksCreated: Int,
    val totalTasksCompleted: Int,
    val completionRate: Float,
    val averageTasksPerDay: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val pendingTasks: Int
)

data class XpStatistics(
    val totalXpEarned: Long,
    val averageXpPerTask: Float,
    val averageXpPerDay: Float,
    val bestXpDay: DayXpRecord?,
    val xpBySource: Map<String, Long>
)

data class DayXpRecord(
    val date: LocalDateTime,
    val xp: Long
)

data class ProductivityStats(
    val mostProductiveDayOfWeek: Int, // 1-7 (Monday-Sunday)
    val mostProductiveHour: Int, // 0-23
    val totalActiveDays: Int,
    val heatmapData: Map<Int, Map<Int, Int>> // DayOfWeek -> Hour -> TaskCount
)

data class CategoryStats(
    val mostActiveCategory: String?,
    val categoryDistribution: Map<Long, CategoryDistribution>
)

data class CategoryDistribution(
    val categoryId: Long,
    val categoryName: String,
    val taskCount: Int,
    val completedCount: Int,
    val totalXp: Long
)

data class DifficultyDistribution(
    val xpPercentage: Int,
    val taskCount: Int,
    val completedCount: Int
)

data class PriorityDistribution(
    val priority: String,
    val taskCount: Int,
    val completedCount: Int
)

data class ChartDataPoint(
    val date: LocalDateTime,
    val value: Float,
    val label: String = ""
)

data class StatisticsFilter(
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val categoryId: Long? = null,
    val difficultyFilter: List<Int>? = null, // XP percentages: 20, 40, 60, 80, 100
    val priorityFilter: List<String>? = null, // LOW, MEDIUM, HIGH
    val showCompletedOnly: Boolean = false
)
