package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val widgetType: String, // SUMMARY_CARD, LINE_CHART, BAR_CHART, PIE_CHART, HEATMAP, DATA_TABLE, PROGRESS_BAR
    val position: Int, // For ordering
    val title: String, // Custom widget title
    val dataSource: String, // TASKS, XP, CATEGORIES, PRODUCTIVITY, STREAKS, etc.
    val timeRange: String, // TODAY, WEEK, MONTH, YEAR, CUSTOM, ALL_TIME
    val customStartDate: String? = null, // ISO date if CUSTOM
    val customEndDate: String? = null, // ISO date if CUSTOM
    val categoryFilter: Long? = null, // Filter by category ID
    val difficultyFilter: String? = null, // Comma-separated: "20,40,60"
    val priorityFilter: String? = null, // Comma-separated: "LOW,MEDIUM,HIGH"
    val chartStyle: String? = null, // Additional chart-specific settings as JSON
    val widgetSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val isVisible: Boolean = true
)
