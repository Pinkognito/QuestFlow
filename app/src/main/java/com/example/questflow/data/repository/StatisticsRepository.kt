package com.example.questflow.data.repository

import com.example.questflow.data.database.TaskDao
import com.example.questflow.data.database.dao.CategoryDao
import com.example.questflow.data.database.dao.StatisticsDao
import com.example.questflow.data.database.dao.XpTransactionDao
import com.example.questflow.data.database.entity.StatisticsConfigEntity
import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val xpTransactionDao: XpTransactionDao,
    private val categoryDao: CategoryDao,
    private val statisticsDao: StatisticsDao
) {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // Configuration
    fun getConfig(): Flow<StatisticsConfigEntity?> = statisticsDao.getConfig()

    suspend fun getConfigOnce(): StatisticsConfigEntity? = statisticsDao.getConfigOnce()

    suspend fun saveConfig(config: StatisticsConfigEntity) = statisticsDao.saveConfig(config)

    suspend fun updateConfig(config: StatisticsConfigEntity) = statisticsDao.updateConfig(config)

    // Task Statistics
    suspend fun getTaskStatistics(filter: StatisticsFilter): TaskStatistics {
        val completedTasks = taskDao.getRecentCompletedTasks()

        // Filter by date range - compare dates without time
        val filteredByDate = completedTasks.filter { task ->
            task.completedAt?.let { completedAt ->
                val completedDate = completedAt.toLocalDate()
                val startDate = filter.startDate.toLocalDate()
                val endDate = filter.endDate.toLocalDate()

                !completedDate.isBefore(startDate) && !completedDate.isAfter(endDate)
            } ?: false
        }

        // Apply additional filters
        val filteredCompleted = applyTaskFilters(filteredByDate, filter)

        val totalCompleted = filteredCompleted.size
        val totalCreated = totalCompleted // Simplified for now
        val completionRate = if (totalCreated > 0) 100f else 0f

        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(filter.startDate, filter.endDate).toInt() + 1
        val avgTasksPerDay = if (daysDiff > 0) totalCompleted.toFloat() / daysDiff else 0f

        val streak = calculateStreak(filteredCompleted)
        val longestStreak = calculateLongestStreak(filteredCompleted)

        return TaskStatistics(
            totalTasksCreated = totalCreated,
            totalTasksCompleted = totalCompleted,
            completionRate = completionRate,
            averageTasksPerDay = avgTasksPerDay,
            currentStreak = streak,
            longestStreak = longestStreak,
            pendingTasks = 0
        )
    }

    // XP Statistics
    suspend fun getXpStatistics(filter: StatisticsFilter): XpStatistics {
        val transactions = xpTransactionDao.getRecentTransactions()

        // Filter by date range - compare dates without time
        val filteredTransactions = transactions.filter { transaction ->
            val transactionDate = transaction.timestamp.toLocalDate()
            val startDate = filter.startDate.toLocalDate()
            val endDate = filter.endDate.toLocalDate()
            !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)
        }

        val totalXp = filteredTransactions.sumOf { it.amount.toLong() }

        val completedTasks = taskDao.getRecentCompletedTasks()
        val filteredCompleted = completedTasks.filter { task ->
            task.completedAt?.let {
                val completedDate = it.toLocalDate()
                val startDate = filter.startDate.toLocalDate()
                val endDate = filter.endDate.toLocalDate()
                !completedDate.isBefore(startDate) && !completedDate.isAfter(endDate)
            } ?: false
        }

        val avgXpPerTask = if (filteredCompleted.isNotEmpty()) {
            totalXp.toFloat() / filteredCompleted.size
        } else 0f

        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(filter.startDate, filter.endDate).toInt() + 1
        val avgXpPerDay = if (daysDiff > 0) totalXp.toFloat() / daysDiff else 0f

        val xpBySource = mutableMapOf<String, Long>()
        XpSource.values().forEach { source ->
            val xp = xpTransactionDao.getTotalXpBySource(source.name) ?: 0L
            xpBySource[source.name] = xp
        }

        return XpStatistics(
            totalXpEarned = totalXp,
            averageXpPerTask = avgXpPerTask,
            averageXpPerDay = avgXpPerDay,
            bestXpDay = null,
            xpBySource = xpBySource
        )
    }

    // Productivity Statistics
    suspend fun getProductivityStatistics(filter: StatisticsFilter): ProductivityStats {
        val completedTasks = taskDao.getRecentCompletedTasks()
        val filteredCompleted = completedTasks.filter { task ->
            task.completedAt?.let {
                val completedDate = it.toLocalDate()
                val startDate = filter.startDate.toLocalDate()
                val endDate = filter.endDate.toLocalDate()
                !completedDate.isBefore(startDate) && !completedDate.isAfter(endDate)
            } ?: false
        }

        val dayOfWeekCounts = mutableMapOf<Int, Int>()
        val hourCounts = mutableMapOf<Int, Int>()
        val heatmapData = mutableMapOf<Int, MutableMap<Int, Int>>()
        val activeDays = mutableSetOf<String>()

        filteredCompleted.forEach { task ->
            task.completedAt?.let { completedAt ->
                val dayOfWeek = completedAt.dayOfWeek.value
                val hour = completedAt.hour
                val dateKey = completedAt.toLocalDate().toString()

                dayOfWeekCounts[dayOfWeek] = (dayOfWeekCounts[dayOfWeek] ?: 0) + 1
                hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
                activeDays.add(dateKey)

                if (!heatmapData.containsKey(dayOfWeek)) {
                    heatmapData[dayOfWeek] = mutableMapOf()
                }
                heatmapData[dayOfWeek]!![hour] = (heatmapData[dayOfWeek]!![hour] ?: 0) + 1
            }
        }

        val mostProductiveDay = dayOfWeekCounts.maxByOrNull { it.value }?.key ?: 1
        val mostProductiveHour = hourCounts.maxByOrNull { it.value }?.key ?: 9

        return ProductivityStats(
            mostProductiveDayOfWeek = mostProductiveDay,
            mostProductiveHour = mostProductiveHour,
            totalActiveDays = activeDays.size,
            heatmapData = heatmapData
        )
    }

    // Category Statistics
    suspend fun getCategoryStatistics(filter: StatisticsFilter): CategoryStats {
        val completedTasks = taskDao.getRecentCompletedTasks()
        val filteredCompleted = completedTasks.filter { task ->
            task.completedAt?.let {
                val completedDate = it.toLocalDate()
                val startDate = filter.startDate.toLocalDate()
                val endDate = filter.endDate.toLocalDate()
                !completedDate.isBefore(startDate) && !completedDate.isAfter(endDate)
            } ?: false
        }

        val categories = categoryDao.getAllCategoriesOnce()
        val categoryDistribution = mutableMapOf<Long, CategoryDistribution>()

        categories.forEach { category ->
            val categoryTasks = filteredCompleted.filter { it.categoryId == category.id }
            val completedCount = categoryTasks.count { it.isCompleted }
            val totalXp = categoryTasks.sumOf { it.xpReward.toLong() }

            if (categoryTasks.isNotEmpty()) {
                categoryDistribution[category.id] = CategoryDistribution(
                    categoryId = category.id,
                    categoryName = category.name,
                    taskCount = categoryTasks.size,
                    completedCount = completedCount,
                    totalXp = totalXp
                )
            }
        }

        val mostActive = categoryDistribution.maxByOrNull { it.value.taskCount }?.value?.categoryName

        return CategoryStats(
            mostActiveCategory = mostActive,
            categoryDistribution = categoryDistribution
        )
    }

    // Difficulty Distribution
    suspend fun getDifficultyDistribution(filter: StatisticsFilter): Map<Int, DifficultyDistribution> {
        val completedTasks = taskDao.getRecentCompletedTasks()
        val filteredCompleted = completedTasks.filter { task ->
            task.completedAt?.let {
                val completedDate = it.toLocalDate()
                val startDate = filter.startDate.toLocalDate()
                val endDate = filter.endDate.toLocalDate()
                !completedDate.isBefore(startDate) && !completedDate.isAfter(endDate)
            } ?: false
        }

        val distribution = mutableMapOf<Int, DifficultyDistribution>()
        val xpPercentages = listOf(20, 40, 60, 80, 100)

        xpPercentages.forEach { percentage ->
            val tasks = filteredCompleted.filter { it.xpPercentage == percentage }
            distribution[percentage] = DifficultyDistribution(
                xpPercentage = percentage,
                taskCount = tasks.size,
                completedCount = tasks.count { it.isCompleted }
            )
        }

        return distribution
    }

    // Priority Distribution
    suspend fun getPriorityDistribution(filter: StatisticsFilter): Map<String, PriorityDistribution> {
        val completedTasks = taskDao.getRecentCompletedTasks()
        val filteredCompleted = completedTasks.filter { task ->
            task.completedAt?.let {
                val completedDate = it.toLocalDate()
                val startDate = filter.startDate.toLocalDate()
                val endDate = filter.endDate.toLocalDate()
                !completedDate.isBefore(startDate) && !completedDate.isAfter(endDate)
            } ?: false
        }

        val distribution = mutableMapOf<String, PriorityDistribution>()
        val priorities = listOf("LOW", "MEDIUM", "HIGH")

        priorities.forEach { priority ->
            val tasks = filteredCompleted.filter { it.priority == priority }
            distribution[priority] = PriorityDistribution(
                priority = priority,
                taskCount = tasks.size,
                completedCount = tasks.count { it.isCompleted }
            )
        }

        return distribution
    }

    // XP Trend Data (for line chart)
    suspend fun getXpTrendData(filter: StatisticsFilter): List<ChartDataPoint> {
        val transactions = xpTransactionDao.getRecentTransactions()

        val filteredTransactions = transactions.filter { transaction ->
            val transactionDate = transaction.timestamp.toLocalDate()
            val startDate = filter.startDate.toLocalDate()
            val endDate = filter.endDate.toLocalDate()
            !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)
        }

        // Group by date
        val xpByDate = filteredTransactions
            .groupBy { it.timestamp.toLocalDate() }
            .map { (date, transactions) ->
                ChartDataPoint(
                    date = date.atStartOfDay(),
                    value = transactions.sumOf { it.amount }.toFloat(),
                    label = date.toString()
                )
            }
            .sortedBy { it.date }

        return xpByDate
    }

    // Task Completion Trend Data (for bar chart)
    suspend fun getTaskCompletionTrendData(filter: StatisticsFilter): List<ChartDataPoint> {
        val completedTasks = taskDao.getRecentCompletedTasks()

        val filteredTasks = completedTasks.filter { task ->
            task.completedAt?.let {
                val completedDate = it.toLocalDate()
                val startDate = filter.startDate.toLocalDate()
                val endDate = filter.endDate.toLocalDate()
                !completedDate.isBefore(startDate) && !completedDate.isAfter(endDate)
            } ?: false
        }

        // Group by date
        val tasksByDate = filteredTasks
            .mapNotNull { task -> task.completedAt?.toLocalDate()?.let { it to task } }
            .groupBy { it.first }
            .map { (date, tasks) ->
                ChartDataPoint(
                    date = date.atStartOfDay(),
                    value = tasks.size.toFloat(),
                    label = date.toString()
                )
            }
            .sortedBy { it.date }

        return tasksByDate
    }

    // Helper: Apply additional filters to tasks
    private fun applyTaskFilters(
        tasks: List<com.example.questflow.data.database.TaskEntity>,
        filter: StatisticsFilter
    ): List<com.example.questflow.data.database.TaskEntity> {
        var filtered = tasks

        filter.categoryId?.let { catId ->
            filtered = filtered.filter { it.categoryId == catId }
        }

        filter.difficultyFilter?.let { difficulties ->
            filtered = filtered.filter { it.xpPercentage in difficulties }
        }

        filter.priorityFilter?.let { priorities ->
            filtered = filtered.filter { it.priority in priorities }
        }

        if (filter.showCompletedOnly) {
            filtered = filtered.filter { it.isCompleted }
        }

        return filtered
    }

    // Helper: Calculate current streak
    private fun calculateStreak(tasks: List<com.example.questflow.data.database.TaskEntity>): Int {
        if (tasks.isEmpty()) return 0

        val sortedDates = tasks
            .mapNotNull { it.completedAt?.toLocalDate() }
            .distinct()
            .sortedDescending()

        if (sortedDates.isEmpty()) return 0

        var streak = 0
        var currentDate = LocalDateTime.now().toLocalDate()

        for (date in sortedDates) {
            if (date == currentDate || date == currentDate.minusDays(1)) {
                streak++
                currentDate = date.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    // Helper: Calculate longest streak
    private fun calculateLongestStreak(tasks: List<com.example.questflow.data.database.TaskEntity>): Int {
        if (tasks.isEmpty()) return 0

        val sortedDates = tasks
            .mapNotNull { it.completedAt?.toLocalDate() }
            .distinct()
            .sorted()

        if (sortedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] == sortedDates[i - 1].plusDays(1)) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return maxStreak
    }
}

// Extension to get first value from Flow (for pending tasks count)
private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.first(): T {
    var result: T? = null
    this.collect { value ->
        result = value
        return@collect
    }
    return result ?: throw NoSuchElementException("Flow is empty")
}
