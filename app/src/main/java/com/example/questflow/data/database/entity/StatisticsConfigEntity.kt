package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics_config")
data class StatisticsConfigEntity(
    @PrimaryKey
    val id: Int = 1, // Single row config
    val visibleCharts: String = "xp_trend,task_completion,category_distribution,difficulty_distribution", // Comma-separated chart IDs
    val defaultTimeRange: String = "WEEK", // TODAY, WEEK, MONTH, YEAR, LAST_7_DAYS, LAST_30_DAYS, CUSTOM
    val chartOrder: String = "xp_trend,task_completion,category_distribution,difficulty_distribution,productivity_heatmap,priority_distribution,xp_sources,streak_calendar", // Comma-separated in order
    val aggregationLevel: String = "DAILY", // DAILY, WEEKLY, MONTHLY
    val selectedCategoryFilter: Long? = null, // null = all
    val selectedDifficultyFilter: String? = null, // null = all, otherwise comma-separated: "20,40,60,80,100"
    val selectedPriorityFilter: String? = null, // null = all, otherwise comma-separated: "LOW,MEDIUM,HIGH"
    val showCompletedOnly: Boolean = false
)

enum class TimeRange {
    TODAY,
    WEEK,
    MONTH,
    YEAR,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM
}

enum class AggregationLevel {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class ChartType {
    XP_TREND,
    TASK_COMPLETION,
    CATEGORY_DISTRIBUTION,
    DIFFICULTY_DISTRIBUTION,
    PRODUCTIVITY_HEATMAP,
    PRIORITY_DISTRIBUTION,
    XP_SOURCES,
    STREAK_CALENDAR
}
