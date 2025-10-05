package com.example.questflow.domain.model

import com.example.questflow.data.database.entity.WidgetConfigEntity

// Widget Types
enum class WidgetType {
    SUMMARY_CARD,   // Single KPI card
    LINE_CHART,     // Trend line chart
    BAR_CHART,      // Bar chart
    PIE_CHART,      // Pie chart for distributions
    HEATMAP,        // Productivity heatmap
    DATA_TABLE,     // Tabular data
    PROGRESS_BAR    // Progress indicator
}

// Data Sources for widgets
enum class WidgetDataSource {
    // Task-related
    TASKS_COMPLETED,
    TASKS_PENDING,
    TASKS_CREATED,
    TASK_COMPLETION,      // For charts
    COMPLETION_RATE,

    // XP-related
    XP_TOTAL,
    XP_AVERAGE_PER_TASK,
    XP_AVERAGE_PER_DAY,
    XP_TREND,             // For charts
    XP_BY_SOURCE,         // For pie charts

    // Category-related
    CATEGORY_DISTRIBUTION,  // For pie charts
    CATEGORY_MOST_ACTIVE,

    // Difficulty & Priority
    DIFFICULTY_DISTRIBUTION,  // For pie charts
    PRIORITY_DISTRIBUTION,    // For pie charts

    // Productivity
    PRODUCTIVITY_HEATMAP,
    PRODUCTIVE_DAY,
    PRODUCTIVE_HOUR,
    ACTIVE_DAYS,

    // Streaks
    STREAK_CURRENT,
    STREAK_LONGEST,

    // Combined metrics
    TASKS_PER_DAY_AVERAGE
}

// Widget Size
enum class WidgetSize {
    SMALL,   // 1/2 width on tablet, full on phone
    MEDIUM,  // Full width, medium height
    LARGE    // Full width, large height
}

// Widget time range (reuse from StatisticsFilter but add more options)
enum class WidgetTimeRange {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

// Widget configuration domain model
data class WidgetConfig(
    val id: Long = 0,
    val widgetType: WidgetType,
    val position: Int,
    val title: String,
    val dataSource: WidgetDataSource,
    val timeRange: WidgetTimeRange,
    val customStartDate: String? = null,
    val customEndDate: String? = null,
    val categoryFilter: Long? = null,
    val difficultyFilter: List<Int>? = null,  // [20, 40, 60, 80, 100]
    val priorityFilter: List<String>? = null, // ["LOW", "MEDIUM", "HIGH"]
    val chartStyle: Map<String, String>? = null,
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val isVisible: Boolean = true
)

// Extension functions for conversion
fun WidgetConfigEntity.toDomainModel(): WidgetConfig = WidgetConfig(
    id = id,
    widgetType = WidgetType.valueOf(widgetType),
    position = position,
    title = title,
    dataSource = WidgetDataSource.valueOf(dataSource),
    timeRange = WidgetTimeRange.valueOf(timeRange),
    customStartDate = customStartDate,
    customEndDate = customEndDate,
    categoryFilter = categoryFilter,
    difficultyFilter = difficultyFilter?.split(",")?.map { it.toInt() },
    priorityFilter = priorityFilter?.split(","),
    chartStyle = null, // TODO: Parse JSON if needed
    widgetSize = WidgetSize.valueOf(widgetSize),
    isVisible = isVisible
)

fun WidgetConfig.toEntity(): WidgetConfigEntity = WidgetConfigEntity(
    id = id,
    widgetType = widgetType.name,
    position = position,
    title = title,
    dataSource = dataSource.name,
    timeRange = timeRange.name,
    customStartDate = customStartDate,
    customEndDate = customEndDate,
    categoryFilter = categoryFilter,
    difficultyFilter = difficultyFilter?.joinToString(","),
    priorityFilter = priorityFilter?.joinToString(","),
    chartStyle = null, // TODO: Serialize to JSON if needed
    widgetSize = widgetSize.name,
    isVisible = isVisible
)

// Widget template for picker
data class WidgetTemplate(
    val widgetType: WidgetType,
    val dataSource: WidgetDataSource,
    val defaultTitle: String,
    val description: String,
    val icon: String, // Material icon name
    val defaultSize: WidgetSize = WidgetSize.MEDIUM
)

// Predefined widget templates
object WidgetTemplates {
    val templates = listOf(
        // Summary Cards
        WidgetTemplate(
            WidgetType.SUMMARY_CARD,
            WidgetDataSource.TASKS_COMPLETED,
            "Abgeschlossene Tasks",
            "Anzahl abgeschlossener Tasks",
            "check_circle",
            WidgetSize.SMALL
        ),
        WidgetTemplate(
            WidgetType.SUMMARY_CARD,
            WidgetDataSource.XP_TOTAL,
            "Gesamtes XP",
            "Gesammeltes XP",
            "star",
            WidgetSize.SMALL
        ),
        WidgetTemplate(
            WidgetType.SUMMARY_CARD,
            WidgetDataSource.STREAK_CURRENT,
            "Aktueller Streak",
            "Tage in Folge mit Tasks",
            "local_fire_department",
            WidgetSize.SMALL
        ),
        WidgetTemplate(
            WidgetType.SUMMARY_CARD,
            WidgetDataSource.COMPLETION_RATE,
            "Erfolgsrate",
            "Prozentsatz abgeschlossener Tasks",
            "trending_up",
            WidgetSize.SMALL
        ),
        WidgetTemplate(
            WidgetType.SUMMARY_CARD,
            WidgetDataSource.TASKS_PER_DAY_AVERAGE,
            "Ø Tasks pro Tag",
            "Durchschnittliche Tasks pro Tag",
            "calendar_today",
            WidgetSize.SMALL
        ),
        WidgetTemplate(
            WidgetType.SUMMARY_CARD,
            WidgetDataSource.XP_AVERAGE_PER_TASK,
            "Ø XP pro Task",
            "Durchschnittliches XP pro Task",
            "grade",
            WidgetSize.SMALL
        ),

        // Charts
        WidgetTemplate(
            WidgetType.LINE_CHART,
            WidgetDataSource.XP_TREND,
            "XP-Verlauf",
            "Entwicklung deines XP über Zeit",
            "show_chart",
            WidgetSize.LARGE
        ),
        WidgetTemplate(
            WidgetType.BAR_CHART,
            WidgetDataSource.TASK_COMPLETION,
            "Tasks pro Tag",
            "Abgeschlossene Tasks pro Tag",
            "bar_chart",
            WidgetSize.LARGE
        ),
        WidgetTemplate(
            WidgetType.PIE_CHART,
            WidgetDataSource.CATEGORY_DISTRIBUTION,
            "Kategorie-Verteilung",
            "Tasks nach Kategorie",
            "pie_chart",
            WidgetSize.MEDIUM
        ),
        WidgetTemplate(
            WidgetType.PIE_CHART,
            WidgetDataSource.DIFFICULTY_DISTRIBUTION,
            "Schwierigkeitsgrad",
            "Tasks nach Schwierigkeit",
            "speed",
            WidgetSize.MEDIUM
        ),
        WidgetTemplate(
            WidgetType.PIE_CHART,
            WidgetDataSource.PRIORITY_DISTRIBUTION,
            "Prioritäten-Verteilung",
            "Tasks nach Priorität",
            "priority_high",
            WidgetSize.MEDIUM
        ),
        WidgetTemplate(
            WidgetType.PIE_CHART,
            WidgetDataSource.XP_BY_SOURCE,
            "XP-Quellen",
            "XP nach Quelle",
            "source",
            WidgetSize.MEDIUM
        ),

        // Heatmap
        WidgetTemplate(
            WidgetType.HEATMAP,
            WidgetDataSource.PRODUCTIVITY_HEATMAP,
            "Produktivitäts-Heatmap",
            "Aktivität nach Wochentag und Stunde",
            "grid_on",
            WidgetSize.LARGE
        ),

        // Progress Bars
        WidgetTemplate(
            WidgetType.PROGRESS_BAR,
            WidgetDataSource.COMPLETION_RATE,
            "Fortschritt",
            "Visueller Fortschrittsbalken",
            "linear_scale",
            WidgetSize.SMALL
        )
    )
}
