package com.example.questflow.domain.model

// Chart Builder - Dynamic Chart Configuration System

// Chart Types
enum class ChartType {
    BAR_CHART,      // Balkendiagramm
    LINE_CHART,     // Liniendiagramm
    PIE_CHART,      // Tortendiagramm
    TABLE,          // Tabelle
    SCATTER_PLOT,   // Streudiagramm
    AREA_CHART      // Flächendiagramm
}

// Data Sources
enum class DataSource {
    TASKS,              // Task-Daten
    XP_TRANSACTIONS,    // XP-Transaktionen
    CATEGORIES,         // Kategorien
    CALENDAR_EVENTS     // Kalender-Events
}

// Available Fields per Data Source
sealed class DataField(val id: String, val label: String, val dataType: FieldDataType) {

    // Task Fields
    object TaskTitle : DataField("title", "Titel", FieldDataType.TEXT)
    object TaskCategory : DataField("categoryName", "Kategorie", FieldDataType.CATEGORY)
    object TaskPriority : DataField("priority", "Priorität", FieldDataType.CATEGORY)
    object TaskDifficulty : DataField("xpPercentage", "Schwierigkeit", FieldDataType.NUMBER)
    object TaskXpReward : DataField("xpReward", "XP-Belohnung", FieldDataType.NUMBER)
    object TaskIsCompleted : DataField("isCompleted", "Abgeschlossen", FieldDataType.BOOLEAN)
    object TaskIsOverdue : DataField("isOverdue", "Überfällig", FieldDataType.BOOLEAN)
    object TaskCompletedAt : DataField("completedAt", "Abschlussdatum", FieldDataType.DATE)
    object TaskCreatedAt : DataField("createdAt", "Erstelldatum", FieldDataType.DATE)
    object TaskDueDate : DataField("dueDate", "Fälligkeitsdatum", FieldDataType.DATE)
    object TaskEstimatedMinutes : DataField("estimatedMinutes", "Geschätzte Minuten", FieldDataType.NUMBER)

    // XP Transaction Fields
    object XpAmount : DataField("amount", "XP-Menge", FieldDataType.NUMBER)
    object XpSource : DataField("source", "XP-Quelle", FieldDataType.CATEGORY)
    object XpTimestamp : DataField("timestamp", "Zeitstempel", FieldDataType.DATE)

    // Category Fields
    object CategoryName : DataField("name", "Kategorie-Name", FieldDataType.TEXT)
    object CategoryLevel : DataField("level", "Level", FieldDataType.NUMBER)
    object CategoryXp : DataField("xp", "XP", FieldDataType.NUMBER)

    companion object {
        fun getFieldsForDataSource(source: DataSource): List<DataField> = when (source) {
            DataSource.TASKS -> listOf(
                TaskTitle, TaskCategory, TaskPriority, TaskDifficulty,
                TaskXpReward, TaskIsCompleted, TaskIsOverdue, TaskCompletedAt, TaskCreatedAt,
                TaskDueDate, TaskEstimatedMinutes
            )
            DataSource.XP_TRANSACTIONS -> listOf(
                XpAmount, XpSource, XpTimestamp
            )
            DataSource.CATEGORIES -> listOf(
                CategoryName, CategoryLevel, CategoryXp
            )
            DataSource.CALENDAR_EVENTS -> emptyList() // TODO
        }
    }
}

// Field Data Types
enum class FieldDataType {
    TEXT,       // String
    NUMBER,     // Int/Long/Float
    DATE,       // LocalDateTime
    BOOLEAN,    // Boolean
    CATEGORY    // Enum/Category
}

// Aggregation Functions
enum class AggregationFunction(val label: String) {
    COUNT("Anzahl"),
    SUM("Summe"),
    AVERAGE("Durchschnitt"),
    MIN("Minimum"),
    MAX("Maximum"),
    FIRST("Erster Wert"),
    LAST("Letzter Wert")
}

// Sort Direction
enum class SortDirection {
    ASC,    // Aufsteigend
    DESC    // Absteigend
}

// Chart Configuration
data class DynamicChartConfig(
    val id: Long = 0,
    val title: String,
    val chartType: ChartType,
    val dataSource: DataSource,

    // Axis Configuration
    val xAxisField: String,              // Field ID for X-axis
    val yAxisField: String?,             // Field ID for Y-axis (null for pie charts)
    val yAxisAggregation: AggregationFunction,

    // Grouping (for X-axis categories)
    val xAxisGrouping: GroupingConfig? = null,

    // Filtering
    val filters: List<DataFilter> = emptyList(),
    val timeRange: TimeRangeFilter? = null,  // NEW: Time-based filtering

    // Sorting
    val sortBy: String? = null,          // Field ID to sort by
    val sortDirection: SortDirection = SortDirection.ASC,

    // Display Options
    val showLegend: Boolean = true,
    val showValues: Boolean = true,
    val showAxisLabels: Boolean = true,  // NEW: Show axis labels
    val colorScheme: String? = null,

    // Category & Template
    val categoryId: Long? = null,        // NEW: Category-specific chart
    val isTemplate: Boolean = false,     // NEW: Save as template

    // Size & Position
    val position: Int = 0,
    val height: ChartHeight = ChartHeight.MEDIUM
)

// Grouping Configuration for X-Axis
data class GroupingConfig(
    val type: GroupingType,
    val dateInterval: DateInterval? = null  // Only for date grouping
)

enum class GroupingType {
    NONE,           // No grouping (use raw values)
    BY_CATEGORY,    // Group by category field
    BY_DATE         // Group by date with interval
}

enum class DateInterval {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

// Time Range Filter
data class TimeRangeFilter(
    val type: TimeRangeType,
    val customStart: java.time.LocalDateTime? = null,
    val customEnd: java.time.LocalDateTime? = null
)

enum class TimeRangeType {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    LAST_YEAR,
    LAST_2_YEARS,
    LAST_3_YEARS,
    ALL_TIME,
    CUSTOM
}

// Data Filters
data class DataFilter(
    val fieldId: String,
    val operator: FilterOperator,
    val value: String
)

enum class FilterOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    CONTAINS,
    IN_LIST
}

// Chart Height Options
enum class ChartHeight(val dp: Int) {
    SMALL(200),
    MEDIUM(300),
    LARGE(400),
    EXTRA_LARGE(500)
}

// Chart Data Result
data class ChartDataResult(
    val labels: List<String>,           // X-axis labels
    val values: List<Float>,            // Y-axis values
    val metadata: Map<String, Any>? = null
)

// Field Compatibility - which fields work on which axes for which chart types
object ChartFieldCompatibility {

    fun isValidXAxis(chartType: ChartType, field: DataField): Boolean = when (chartType) {
        ChartType.BAR_CHART -> field.dataType in listOf(
            FieldDataType.CATEGORY, FieldDataType.DATE, FieldDataType.TEXT
        )
        ChartType.LINE_CHART -> field.dataType == FieldDataType.DATE
        ChartType.PIE_CHART -> field.dataType in listOf(
            FieldDataType.CATEGORY, FieldDataType.TEXT, FieldDataType.BOOLEAN, FieldDataType.DATE
        )
        ChartType.TABLE -> true  // All fields valid
        ChartType.SCATTER_PLOT -> field.dataType == FieldDataType.NUMBER
        ChartType.AREA_CHART -> field.dataType == FieldDataType.DATE
    }

    fun isValidYAxis(chartType: ChartType, field: DataField): Boolean = when (chartType) {
        ChartType.BAR_CHART, ChartType.LINE_CHART, ChartType.AREA_CHART, ChartType.SCATTER_PLOT ->
            field.dataType == FieldDataType.NUMBER
        ChartType.PIE_CHART -> field.dataType == FieldDataType.NUMBER
        ChartType.TABLE -> true  // All fields valid
    }

    fun getAvailableAggregations(field: DataField): List<AggregationFunction> = when (field.dataType) {
        FieldDataType.NUMBER -> AggregationFunction.entries
        FieldDataType.DATE, FieldDataType.TEXT, FieldDataType.CATEGORY, FieldDataType.BOOLEAN ->
            listOf(AggregationFunction.COUNT)
    }

    // Check if Y-axis is required for chart type
    fun requiresYAxis(chartType: ChartType): Boolean = when (chartType) {
        ChartType.PIE_CHART, ChartType.TABLE -> false
        else -> true
    }

    // Check if aggregation makes sense for combination
    fun isValidAggregation(chartType: ChartType, xField: DataField?, yField: DataField?, agg: AggregationFunction): Boolean {
        // COUNT is always valid
        if (agg == AggregationFunction.COUNT) return true

        // For numeric aggregations, we need a numeric Y field
        if (agg in listOf(AggregationFunction.SUM, AggregationFunction.AVERAGE, AggregationFunction.MIN, AggregationFunction.MAX)) {
            return yField?.dataType == FieldDataType.NUMBER
        }

        return true
    }
}

// Chart Builder Templates (Quick Start)
data class ChartTemplate(
    val name: String,
    val description: String,
    val config: DynamicChartConfig
)

// Conversion functions
fun DynamicChartConfig.toEntity(): com.example.questflow.data.database.entity.DynamicChartEntity {
    return com.example.questflow.data.database.entity.DynamicChartEntity(
        id = id,
        title = title,
        chartType = chartType.name,
        dataSource = dataSource.name,
        xAxisField = xAxisField,
        yAxisField = yAxisField,
        yAxisAggregation = yAxisAggregation.name,
        groupingType = xAxisGrouping?.type?.name,
        dateInterval = xAxisGrouping?.dateInterval?.name,
        filters = null, // TODO: Serialize filters to JSON
        timeRangeType = timeRange?.type?.name,
        sortBy = sortBy,
        sortDirection = sortDirection.name,
        showLegend = showLegend,
        showValues = showValues,
        showAxisLabels = showAxisLabels,
        colorScheme = colorScheme,
        categoryId = categoryId,
        isTemplate = isTemplate,
        position = position,
        height = height.dp,
        isVisible = true
    )
}

fun com.example.questflow.data.database.entity.DynamicChartEntity.toDomainModel(): DynamicChartConfig {
    return DynamicChartConfig(
        id = id,
        title = title,
        chartType = ChartType.valueOf(chartType),
        dataSource = DataSource.valueOf(dataSource),
        xAxisField = xAxisField,
        yAxisField = yAxisField,
        yAxisAggregation = AggregationFunction.valueOf(yAxisAggregation),
        xAxisGrouping = if (groupingType != null) {
            GroupingConfig(
                type = GroupingType.valueOf(groupingType),
                dateInterval = dateInterval?.let { DateInterval.valueOf(it) }
            )
        } else null,
        filters = emptyList(), // TODO: Deserialize from JSON
        timeRange = timeRangeType?.let { TimeRangeFilter(TimeRangeType.valueOf(it)) },
        sortBy = sortBy,
        sortDirection = SortDirection.valueOf(sortDirection),
        showLegend = showLegend,
        showValues = showValues,
        showAxisLabels = showAxisLabels,
        colorScheme = colorScheme,
        categoryId = categoryId,
        isTemplate = isTemplate,
        position = position,
        height = ChartHeight.entries.find { it.dp == height } ?: ChartHeight.MEDIUM
    )
}

object ChartTemplates {
    val templates = listOf(
        ChartTemplate(
            name = "Tasks nach Kategorie",
            description = "Balkendiagramm: Anzahl Tasks pro Kategorie",
            config = DynamicChartConfig(
                title = "Tasks nach Kategorie",
                chartType = ChartType.BAR_CHART,
                dataSource = DataSource.TASKS,
                xAxisField = DataField.TaskCategory.id,
                yAxisField = DataField.TaskTitle.id,
                yAxisAggregation = AggregationFunction.COUNT,
                xAxisGrouping = GroupingConfig(GroupingType.BY_CATEGORY)
            )
        ),
        ChartTemplate(
            name = "XP-Verlauf",
            description = "Liniendiagramm: XP über Zeit",
            config = DynamicChartConfig(
                title = "XP-Verlauf",
                chartType = ChartType.LINE_CHART,
                dataSource = DataSource.XP_TRANSACTIONS,
                xAxisField = DataField.XpTimestamp.id,
                yAxisField = DataField.XpAmount.id,
                yAxisAggregation = AggregationFunction.SUM,
                xAxisGrouping = GroupingConfig(GroupingType.BY_DATE, DateInterval.DAY)
            )
        ),
        ChartTemplate(
            name = "Prioritäten-Verteilung",
            description = "Tortendiagramm: Tasks nach Priorität",
            config = DynamicChartConfig(
                title = "Prioritäten-Verteilung",
                chartType = ChartType.PIE_CHART,
                dataSource = DataSource.TASKS,
                xAxisField = DataField.TaskPriority.id,
                yAxisField = DataField.TaskTitle.id,
                yAxisAggregation = AggregationFunction.COUNT
            )
        ),
        ChartTemplate(
            name = "Task-Übersicht",
            description = "Tabelle: Alle Tasks mit Details",
            config = DynamicChartConfig(
                title = "Task-Übersicht",
                chartType = ChartType.TABLE,
                dataSource = DataSource.TASKS,
                xAxisField = DataField.TaskTitle.id,
                yAxisField = null,
                yAxisAggregation = AggregationFunction.COUNT
            )
        )
    )
}
