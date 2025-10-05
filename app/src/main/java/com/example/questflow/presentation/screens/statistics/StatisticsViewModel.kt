package com.example.questflow.presentation.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.StatisticsConfigEntity
import com.example.questflow.data.database.entity.TimeRange
import com.example.questflow.domain.model.*
import com.example.questflow.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getTaskStatisticsUseCase: GetTaskStatisticsUseCase,
    private val getXpStatisticsUseCase: GetXpStatisticsUseCase,
    private val getProductivityStatisticsUseCase: GetProductivityStatisticsUseCase,
    private val getCategoryStatisticsUseCase: GetCategoryStatisticsUseCase,
    private val getDifficultyDistributionUseCase: GetDifficultyDistributionUseCase,
    private val getPriorityDistributionUseCase: GetPriorityDistributionUseCase,
    private val getXpTrendDataUseCase: GetXpTrendDataUseCase,
    private val getTaskCompletionTrendDataUseCase: GetTaskCompletionTrendDataUseCase,
    private val getStatisticsConfigUseCase: GetStatisticsConfigUseCase,
    private val saveStatisticsConfigUseCase: SaveStatisticsConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _config = MutableStateFlow<StatisticsConfigEntity?>(null)
    val config: StateFlow<StatisticsConfigEntity?> = _config.asStateFlow()

    init {
        loadConfig()
        loadStatistics()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            getStatisticsConfigUseCase().collect { config ->
                _config.value = config
                config?.let {
                    applyConfigToUiState(it)
                }
            }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val filter = createFilterFromUiState()

                // Load all statistics in parallel
                val taskStats = getTaskStatisticsUseCase(filter)
                val xpStats = getXpStatisticsUseCase(filter)
                val productivityStats = getProductivityStatisticsUseCase(filter)
                val categoryStats = getCategoryStatisticsUseCase(filter)
                val difficultyDist = getDifficultyDistributionUseCase(filter)
                val priorityDist = getPriorityDistributionUseCase(filter)
                val xpTrend = getXpTrendDataUseCase(filter)
                val taskCompletionTrend = getTaskCompletionTrendDataUseCase(filter)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    taskStatistics = taskStats,
                    xpStatistics = xpStats,
                    productivityStats = productivityStats,
                    categoryStats = categoryStats,
                    difficultyDistribution = difficultyDist,
                    priorityDistribution = priorityDist,
                    xpTrendData = xpTrend,
                    taskCompletionTrendData = taskCompletionTrend
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setTimeRange(timeRange: TimeRange) {
        val (start, end) = calculateDateRangeFromTimeRange(timeRange)
        _uiState.value = _uiState.value.copy(
            selectedTimeRange = timeRange,
            startDate = start,
            endDate = end
        )
        loadStatistics()
    }

    fun setCustomDateRange(start: LocalDateTime, end: LocalDateTime) {
        _uiState.value = _uiState.value.copy(
            selectedTimeRange = TimeRange.CUSTOM,
            startDate = start,
            endDate = end
        )
        loadStatistics()
    }

    fun setCategoryFilter(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = categoryId)
        loadStatistics()
    }

    fun setDifficultyFilter(difficulties: List<Int>?) {
        _uiState.value = _uiState.value.copy(selectedDifficultyFilter = difficulties)
        loadStatistics()
    }

    fun setPriorityFilter(priorities: List<String>?) {
        _uiState.value = _uiState.value.copy(selectedPriorityFilter = priorities)
        loadStatistics()
    }

    fun setShowCompletedOnly(showCompletedOnly: Boolean) {
        _uiState.value = _uiState.value.copy(showCompletedOnly = showCompletedOnly)
        loadStatistics()
    }

    fun toggleChartVisibility(chartId: String) {
        val currentVisible = _uiState.value.visibleCharts.toMutableSet()
        if (currentVisible.contains(chartId)) {
            currentVisible.remove(chartId)
        } else {
            currentVisible.add(chartId)
        }
        _uiState.value = _uiState.value.copy(visibleCharts = currentVisible)
        saveConfigToDatabase()
    }

    fun setChartOrder(chartOrder: List<String>) {
        _uiState.value = _uiState.value.copy(chartOrder = chartOrder)
        saveConfigToDatabase()
    }

    fun toggleFilterExpanded() {
        _uiState.value = _uiState.value.copy(isFilterExpanded = !_uiState.value.isFilterExpanded)
    }

    fun toggleConfigExpanded() {
        _uiState.value = _uiState.value.copy(isConfigExpanded = !_uiState.value.isConfigExpanded)
    }

    private fun createFilterFromUiState(): StatisticsFilter {
        val state = _uiState.value
        return StatisticsFilter(
            startDate = state.startDate,
            endDate = state.endDate,
            categoryId = state.selectedCategoryFilter,
            difficultyFilter = state.selectedDifficultyFilter,
            priorityFilter = state.selectedPriorityFilter,
            showCompletedOnly = state.showCompletedOnly
        )
    }

    private fun applyConfigToUiState(config: StatisticsConfigEntity) {
        val visibleCharts = config.visibleCharts.split(",").toSet()
        val chartOrder = config.chartOrder.split(",")
        val timeRange = TimeRange.valueOf(config.defaultTimeRange)
        val (start, end) = calculateDateRangeFromTimeRange(timeRange)

        _uiState.value = _uiState.value.copy(
            visibleCharts = visibleCharts,
            chartOrder = chartOrder,
            selectedTimeRange = timeRange,
            startDate = start,
            endDate = end,
            selectedCategoryFilter = config.selectedCategoryFilter,
            selectedDifficultyFilter = config.selectedDifficultyFilter?.split(",")?.mapNotNull { it.toIntOrNull() },
            selectedPriorityFilter = config.selectedPriorityFilter?.split(","),
            showCompletedOnly = config.showCompletedOnly
        )
    }

    private fun saveConfigToDatabase() {
        viewModelScope.launch {
            val state = _uiState.value
            val config = StatisticsConfigEntity(
                id = 1,
                visibleCharts = state.visibleCharts.joinToString(","),
                defaultTimeRange = state.selectedTimeRange.name,
                chartOrder = state.chartOrder.joinToString(","),
                aggregationLevel = "DAILY",
                selectedCategoryFilter = state.selectedCategoryFilter,
                selectedDifficultyFilter = state.selectedDifficultyFilter?.joinToString(","),
                selectedPriorityFilter = state.selectedPriorityFilter?.joinToString(","),
                showCompletedOnly = state.showCompletedOnly
            )
            saveStatisticsConfigUseCase(config)
        }
    }

    private fun calculateDateRangeFromTimeRange(timeRange: TimeRange): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDateTime.now()
        return when (timeRange) {
            TimeRange.TODAY -> {
                val start = now.truncatedTo(ChronoUnit.DAYS)
                val end = start.plusDays(1).minusSeconds(1)
                start to end
            }
            TimeRange.WEEK -> {
                val start = now.minusDays(now.dayOfWeek.value.toLong() - 1).truncatedTo(ChronoUnit.DAYS)
                val end = start.plusDays(7).minusSeconds(1)
                start to end
            }
            TimeRange.MONTH -> {
                val start = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                val end = start.plusMonths(1).minusSeconds(1)
                start to end
            }
            TimeRange.YEAR -> {
                val start = now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS)
                val end = start.plusYears(1).minusSeconds(1)
                start to end
            }
            TimeRange.LAST_7_DAYS -> {
                val start = now.minusDays(7).truncatedTo(ChronoUnit.DAYS)
                val end = now
                start to end
            }
            TimeRange.LAST_30_DAYS -> {
                val start = now.minusDays(30).truncatedTo(ChronoUnit.DAYS)
                val end = now
                start to end
            }
            TimeRange.CUSTOM -> {
                _uiState.value.startDate to _uiState.value.endDate
            }
        }
    }
}

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Filter state
    val selectedTimeRange: TimeRange = TimeRange.WEEK,
    val startDate: LocalDateTime = LocalDateTime.now().minusDays(7),
    val endDate: LocalDateTime = LocalDateTime.now(),
    val selectedCategoryFilter: Long? = null,
    val selectedDifficultyFilter: List<Int>? = null,
    val selectedPriorityFilter: List<String>? = null,
    val showCompletedOnly: Boolean = false,

    // Configuration state
    val visibleCharts: Set<String> = setOf("xp_trend", "task_completion", "category_distribution", "difficulty_distribution"),
    val chartOrder: List<String> = listOf("xp_trend", "task_completion", "category_distribution", "difficulty_distribution", "productivity_heatmap", "priority_distribution", "xp_sources", "streak_calendar"),

    // UI state
    val isFilterExpanded: Boolean = false,
    val isConfigExpanded: Boolean = false,

    // Statistics data
    val taskStatistics: TaskStatistics? = null,
    val xpStatistics: XpStatistics? = null,
    val productivityStats: ProductivityStats? = null,
    val categoryStats: CategoryStats? = null,
    val difficultyDistribution: Map<Int, DifficultyDistribution>? = null,
    val priorityDistribution: Map<String, PriorityDistribution>? = null,
    val xpTrendData: List<ChartDataPoint>? = null,
    val taskCompletionTrendData: List<ChartDataPoint>? = null
)
