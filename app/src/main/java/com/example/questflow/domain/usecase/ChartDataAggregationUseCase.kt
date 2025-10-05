package com.example.questflow.domain.usecase

import com.example.questflow.data.database.TaskDao
import com.example.questflow.data.database.dao.CategoryDao
import com.example.questflow.data.database.dao.XpTransactionDao
import com.example.questflow.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ChartDataAggregationUseCase @Inject constructor(
    private val taskDao: TaskDao,
    private val xpTransactionDao: XpTransactionDao,
    private val categoryDao: CategoryDao
) {

    suspend operator fun invoke(config: DynamicChartConfig, categoryId: Long? = null): ChartDataResult {
        // 1. Load raw data based on data source
        val rawData = loadRawData(config.dataSource, config.filters, categoryId)

        // 2. Apply filters
        val filteredData = applyFilters(rawData, config.filters)

        // 3. Apply time range filter
        val timeFilteredData = applyTimeRangeFilter(filteredData, config.timeRange, config)

        // 4. Group by X-axis
        val groupedData = groupByXAxis(timeFilteredData, config)

        // 5. Aggregate Y-axis values
        val aggregatedData = aggregateYAxis(groupedData, config)

        // 6. Sort results
        val sortedData = sortResults(aggregatedData, config)

        // 7. Format for chart
        return formatChartData(sortedData, config)
    }

    private fun applyTimeRangeFilter(
        data: List<Map<String, Any?>>,
        timeRange: TimeRangeFilter?,
        config: DynamicChartConfig
    ): List<Map<String, Any?>> {
        if (timeRange == null) return data

        val now = LocalDateTime.now()
        val startDate = when (timeRange.type) {
            TimeRangeType.LAST_7_DAYS -> now.minusDays(7)
            TimeRangeType.LAST_30_DAYS -> now.minusDays(30)
            TimeRangeType.LAST_3_MONTHS -> now.minusMonths(3)
            TimeRangeType.LAST_6_MONTHS -> now.minusMonths(6)
            TimeRangeType.LAST_YEAR -> now.minusYears(1)
            TimeRangeType.LAST_2_YEARS -> now.minusYears(2)
            TimeRangeType.LAST_3_YEARS -> now.minusYears(3)
            TimeRangeType.ALL_TIME -> null
            TimeRangeType.CUSTOM -> timeRange.customStart
        }

        val endDate = when (timeRange.type) {
            TimeRangeType.CUSTOM -> timeRange.customEnd
            else -> now
        }

        if (startDate == null) return data

        // Filter based on the appropriate date field
        val dateField = when {
            config.xAxisField == DataField.TaskCompletedAt.id -> DataField.TaskCompletedAt.id
            config.xAxisField == DataField.TaskCreatedAt.id -> DataField.TaskCreatedAt.id
            config.xAxisField == DataField.TaskDueDate.id -> DataField.TaskDueDate.id
            config.xAxisField == DataField.XpTimestamp.id -> DataField.XpTimestamp.id
            else -> config.xAxisField
        }

        return data.filter { row ->
            val date = row[dateField] as? LocalDateTime
            date != null && date.isAfter(startDate) && (endDate == null || date.isBefore(endDate))
        }
    }

    private suspend fun loadRawData(
        source: DataSource,
        filters: List<DataFilter>,
        categoryId: Long? = null
    ): List<Map<String, Any?>> {
        return when (source) {
            DataSource.TASKS -> {
                val tasks = if (categoryId != null) {
                    taskDao.getRecentCompletedTasks().filter { it.categoryId == categoryId }
                } else {
                    taskDao.getRecentCompletedTasks()
                }
                val categories = categoryDao.getAllCategoriesOnce()
                val categoryMap = categories.associateBy { it.id }

                tasks.map { task ->
                    val isOverdue = task.dueDate?.let { dueDate ->
                        !task.isCompleted && dueDate.isBefore(LocalDateTime.now())
                    } ?: false

                    mapOf(
                        "id" to task.id,
                        "title" to task.title,
                        "categoryName" to (categoryMap[task.categoryId]?.name ?: "Keine Kategorie"),
                        "categoryId" to task.categoryId,
                        "priority" to task.priority,
                        "xpPercentage" to task.xpPercentage,
                        "xpReward" to task.xpReward,
                        "isCompleted" to task.isCompleted,
                        "isOverdue" to isOverdue,
                        "completedAt" to task.completedAt,
                        "createdAt" to task.createdAt,
                        "dueDate" to task.dueDate,
                        "estimatedMinutes" to task.estimatedMinutes
                    )
                }
            }
            DataSource.XP_TRANSACTIONS -> {
                val transactions = xpTransactionDao.getRecentTransactions()
                // TODO: Filter by category via referenceId -> Task lookup
                transactions.map { tx ->
                    mapOf(
                        "id" to tx.id,
                        "amount" to tx.amount,
                        "source" to tx.source.name,
                        "timestamp" to tx.timestamp
                    )
                }
            }
            DataSource.CATEGORIES -> {
                val categories = categoryDao.getAllCategoriesOnce()
                categories.map { cat ->
                    mapOf(
                        "id" to cat.id,
                        "name" to cat.name,
                        "level" to cat.currentLevel,
                        "xp" to cat.currentXp
                    )
                }
            }
            DataSource.CALENDAR_EVENTS -> emptyList() // TODO
        }
    }

    private fun applyFilters(data: List<Map<String, Any?>>, filters: List<DataFilter>): List<Map<String, Any?>> {
        var filtered = data
        filters.forEach { filter ->
            filtered = filtered.filter { row ->
                val value = row[filter.fieldId]
                when (filter.operator) {
                    FilterOperator.EQUALS -> value?.toString() == filter.value
                    FilterOperator.NOT_EQUALS -> value?.toString() != filter.value
                    FilterOperator.GREATER_THAN -> (value as? Number)?.toDouble() ?: 0.0 > filter.value.toDoubleOrNull() ?: 0.0
                    FilterOperator.LESS_THAN -> (value as? Number)?.toDouble() ?: 0.0 < filter.value.toDoubleOrNull() ?: 0.0
                    FilterOperator.CONTAINS -> value?.toString()?.contains(filter.value, ignoreCase = true) == true
                    FilterOperator.IN_LIST -> filter.value.split(",").contains(value?.toString())
                }
            }
        }
        return filtered
    }

    private fun groupByXAxis(
        data: List<Map<String, Any?>>,
        config: DynamicChartConfig
    ): Map<String, List<Map<String, Any?>>> {
        val grouped = when (config.xAxisGrouping?.type) {
            GroupingType.BY_DATE -> {
                data.groupBy { row ->
                    val date = row[config.xAxisField] as? LocalDateTime
                    date?.let { formatDateByInterval(it, config.xAxisGrouping.dateInterval ?: DateInterval.DAY) } ?: "Unknown"
                }
            }
            GroupingType.BY_CATEGORY, GroupingType.NONE, null -> {
                data.groupBy { row ->
                    row[config.xAxisField]?.toString() ?: "Unknown"
                }
            }
        }

        // Fill missing dates with empty lists for date-based grouping
        if (config.xAxisGrouping?.type == GroupingType.BY_DATE && config.timeRange != null) {
            return fillMissingDates(grouped, config)
        }

        return grouped
    }

    private fun fillMissingDates(
        grouped: Map<String, List<Map<String, Any?>>>,
        config: DynamicChartConfig
    ): Map<String, List<Map<String, Any?>>> {
        val timeRange = config.timeRange ?: return grouped
        val interval = config.xAxisGrouping?.dateInterval ?: DateInterval.DAY

        val now = LocalDateTime.now()
        val startDate = when (timeRange.type) {
            TimeRangeType.LAST_7_DAYS -> now.minusDays(7)
            TimeRangeType.LAST_30_DAYS -> now.minusDays(30)
            TimeRangeType.LAST_3_MONTHS -> now.minusMonths(3)
            TimeRangeType.LAST_6_MONTHS -> now.minusMonths(6)
            TimeRangeType.LAST_YEAR -> now.minusYears(1)
            TimeRangeType.LAST_2_YEARS -> now.minusYears(2)
            TimeRangeType.LAST_3_YEARS -> now.minusYears(3)
            TimeRangeType.ALL_TIME -> return grouped // Don't fill for all time
            TimeRangeType.CUSTOM -> timeRange.customStart ?: return grouped
        } ?: return grouped

        // Generate all date labels in the range
        val allDates = mutableMapOf<String, List<Map<String, Any?>>>()
        var currentDate = startDate

        while (currentDate.isBefore(now) || currentDate.isEqual(now)) {
            val label = formatDateByInterval(currentDate, interval)
            allDates[label] = grouped[label] ?: emptyList()

            currentDate = when (interval) {
                DateInterval.DAY -> currentDate.plusDays(1)
                DateInterval.WEEK -> currentDate.plusWeeks(1)
                DateInterval.MONTH -> currentDate.plusMonths(1)
                DateInterval.YEAR -> currentDate.plusYears(1)
            }

            // Safety: prevent infinite loops
            if (allDates.size > 1000) break
        }

        return allDates
    }

    private fun aggregateYAxis(
        groupedData: Map<String, List<Map<String, Any?>>>,
        config: DynamicChartConfig
    ): Map<String, Float> {
        return groupedData.mapValues { (_, rows) ->
            when (config.yAxisAggregation) {
                AggregationFunction.COUNT -> rows.size.toFloat()
                AggregationFunction.SUM -> rows.sumOf {
                    (it[config.yAxisField] as? Number)?.toDouble() ?: 0.0
                }.toFloat()
                AggregationFunction.AVERAGE -> {
                    val sum = rows.sumOf { (it[config.yAxisField] as? Number)?.toDouble() ?: 0.0 }
                    (sum / rows.size.coerceAtLeast(1)).toFloat()
                }
                AggregationFunction.MIN -> rows.minOfOrNull {
                    (it[config.yAxisField] as? Number)?.toFloat() ?: Float.MAX_VALUE
                } ?: 0f
                AggregationFunction.MAX -> rows.maxOfOrNull {
                    (it[config.yAxisField] as? Number)?.toFloat() ?: Float.MIN_VALUE
                } ?: 0f
                AggregationFunction.FIRST -> (rows.firstOrNull()?.get(config.yAxisField) as? Number)?.toFloat() ?: 0f
                AggregationFunction.LAST -> (rows.lastOrNull()?.get(config.yAxisField) as? Number)?.toFloat() ?: 0f
            }
        }
    }

    private fun sortResults(
        data: Map<String, Float>,
        config: DynamicChartConfig
    ): List<Pair<String, Float>> {
        val sorted = data.toList()

        // For date-based grouping, sort chronologically instead of by value
        if (config.xAxisGrouping?.type == GroupingType.BY_DATE) {
            return sorted  // Already in chronological order from fillMissingDates
        }

        return when (config.sortDirection) {
            SortDirection.ASC -> sorted.sortedBy { it.second }
            SortDirection.DESC -> sorted.sortedByDescending { it.second }
        }
    }

    private fun formatChartData(
        data: List<Pair<String, Float>>,
        config: DynamicChartConfig
    ): ChartDataResult {
        return ChartDataResult(
            labels = data.map { it.first },
            values = data.map { it.second },
            metadata = mapOf(
                "chartType" to config.chartType.name,
                "dataSource" to config.dataSource.name
            )
        )
    }

    private fun formatDateByInterval(date: LocalDateTime, interval: DateInterval): String {
        return when (interval) {
            DateInterval.DAY -> date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            DateInterval.WEEK -> {
                val weekStart = date.minusDays(date.dayOfWeek.value.toLong() - 1)
                "KW ${weekStart.format(DateTimeFormatter.ofPattern("ww/yyyy"))}"
            }
            DateInterval.MONTH -> date.format(DateTimeFormatter.ofPattern("MM/yyyy"))
            DateInterval.YEAR -> date.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
}
