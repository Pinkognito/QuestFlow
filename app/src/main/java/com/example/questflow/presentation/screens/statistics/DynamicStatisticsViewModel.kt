package com.example.questflow.presentation.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.repository.DynamicChartRepository
import com.example.questflow.domain.model.*
import com.example.questflow.domain.usecase.ChartDataAggregationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DynamicStatisticsViewModel @Inject constructor(
    private val chartRepository: DynamicChartRepository,
    private val chartDataAggregationUseCase: ChartDataAggregationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DynamicStatisticsUiState())
    val uiState: StateFlow<DynamicStatisticsUiState> = _uiState.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)

    val charts: StateFlow<List<DynamicChartConfig>> = chartRepository.getAllCharts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chartData = MutableStateFlow<Map<Long, ChartDataResult>>(emptyMap())
    val chartData: StateFlow<Map<Long, ChartDataResult>> = _chartData.asStateFlow()

    init {
        observeCharts()
        observeCategoryChanges()
    }

    fun updateSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    private fun observeCategoryChanges() {
        viewModelScope.launch {
            _selectedCategoryId.collect { categoryId ->
                // Reload all charts when category changes
                charts.value.forEach { chart ->
                    loadChartData(chart)
                }
            }
        }
    }

    private fun observeCharts() {
        viewModelScope.launch {
            charts.collect { chartList ->
                chartList.forEach { chart ->
                    loadChartData(chart)
                }
            }
        }
    }

    fun loadChartData(chart: DynamicChartConfig) {
        viewModelScope.launch {
            try {
                val data = chartDataAggregationUseCase(chart, _selectedCategoryId.value)
                _chartData.value = _chartData.value + (chart.id to data)
            } catch (e: Exception) {
                // Handle error
                android.util.Log.e("DynamicStatisticsVM", "Error loading chart data", e)
            }
        }
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditMode = !_uiState.value.isEditMode)
    }

    fun showChartBuilder(template: ChartTemplate? = null, existingChart: DynamicChartConfig? = null) {
        _uiState.value = _uiState.value.copy(
            showChartBuilder = true,
            editingChart = existingChart,
            selectedTemplate = template
        )
    }

    fun hideChartBuilder() {
        _uiState.value = _uiState.value.copy(
            showChartBuilder = false,
            editingChart = null,
            selectedTemplate = null
        )
    }

    fun showTemplateDialog() {
        _uiState.value = _uiState.value.copy(showTemplateDialog = true)
    }

    fun hideTemplateDialog() {
        _uiState.value = _uiState.value.copy(showTemplateDialog = false)
    }

    fun addChart(config: DynamicChartConfig) {
        viewModelScope.launch {
            val maxPosition = charts.value.maxOfOrNull { it.position } ?: -1
            val newConfig = config.copy(position = maxPosition + 1)
            chartRepository.addChart(newConfig)
        }
    }

    fun updateChart(config: DynamicChartConfig) {
        viewModelScope.launch {
            chartRepository.updateChart(config)
            loadChartData(config)  // Reload data
        }
    }

    fun deleteChart(config: DynamicChartConfig) {
        viewModelScope.launch {
            chartRepository.deleteChart(config)
        }
    }

    fun reorderCharts(chartIds: List<Long>) {
        viewModelScope.launch {
            chartRepository.reorderCharts(chartIds)
        }
    }
}

data class DynamicStatisticsUiState(
    val isEditMode: Boolean = false,
    val showChartBuilder: Boolean = false,
    val showTemplateDialog: Boolean = false,
    val editingChart: DynamicChartConfig? = null,
    val selectedTemplate: ChartTemplate? = null
)
