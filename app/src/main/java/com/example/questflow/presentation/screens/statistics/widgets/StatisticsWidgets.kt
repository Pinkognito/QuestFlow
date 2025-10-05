package com.example.questflow.presentation.screens.statistics.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.NumberFormat
import java.util.Locale

// Main widget container that routes to specific widget types
@Composable
fun StatisticsWidget(
    widget: WidgetConfig,
    statistics: StatisticsData,
    onEditClick: (WidgetConfig) -> Unit,
    onDeleteClick: (WidgetConfig) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (widget.widgetSize) {
                    WidgetSize.SMALL -> Modifier.height(120.dp)
                    WidgetSize.MEDIUM -> Modifier.height(250.dp)
                    WidgetSize.LARGE -> Modifier.height(350.dp)
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Widget content
            when (widget.widgetType) {
                WidgetType.SUMMARY_CARD -> SummaryCardWidget(widget, statistics)
                WidgetType.LINE_CHART -> LineChartWidget(widget, statistics)
                WidgetType.BAR_CHART -> BarChartWidget(widget, statistics)
                WidgetType.PIE_CHART -> PieChartWidget(widget, statistics)
                WidgetType.HEATMAP -> HeatmapWidget(widget, statistics)
                WidgetType.DATA_TABLE -> DataTableWidget(widget, statistics)
                WidgetType.PROGRESS_BAR -> ProgressBarWidget(widget, statistics)
            }

            // Edit mode overlay
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onEditClick(widget) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Bearbeiten",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(widget) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// Summary Card Widget - displays single KPI
@Composable
fun SummaryCardWidget(
    widget: WidgetConfig,
    statistics: StatisticsData
) {
    val (value, unit) = when (widget.dataSource) {
        WidgetDataSource.TASKS_COMPLETED -> statistics.taskStats.totalTasksCompleted.toString() to "Tasks"
        WidgetDataSource.XP_TOTAL -> NumberFormat.getNumberInstance(Locale.GERMAN).format(statistics.xpStats.totalXpEarned) to "XP"
        WidgetDataSource.STREAK_CURRENT -> statistics.taskStats.currentStreak.toString() to "Tage"
        WidgetDataSource.COMPLETION_RATE -> "${statistics.taskStats.completionRate.toInt()}%" to ""
        WidgetDataSource.TASKS_PER_DAY_AVERAGE -> String.format("%.1f", statistics.taskStats.averageTasksPerDay) to "Tasks/Tag"
        WidgetDataSource.XP_AVERAGE_PER_TASK -> NumberFormat.getNumberInstance(Locale.GERMAN).format(statistics.xpStats.averageXpPerTask.toInt()) to "XP/Task"
        else -> "N/A" to ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = widget.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Line Chart Widget
@Composable
fun LineChartWidget(
    widget: WidgetConfig,
    statistics: StatisticsData
) {
    val chartData = when (widget.dataSource) {
        WidgetDataSource.XP_TREND -> statistics.xpTrendData
        WidgetDataSource.TASK_COMPLETION -> statistics.taskCompletionTrendData
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = widget.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (chartData.isNotEmpty()) {
            val chartEntryModelProducer = ChartEntryModelProducer(
                chartData.mapIndexed { index, point ->
                    entryOf(index.toFloat(), point.value)
                }
            )

            val chartModel = chartEntryModelProducer.getModel()
            if (chartModel != null) {
                ProvideChartStyle(m3ChartStyle()) {
                    Chart(
                        chart = lineChart(),
                        model = chartModel,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Daten verfügbar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Bar Chart Widget
@Composable
fun BarChartWidget(
    widget: WidgetConfig,
    statistics: StatisticsData
) {
    val chartData = when (widget.dataSource) {
        WidgetDataSource.TASK_COMPLETION -> statistics.taskCompletionTrendData
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = widget.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (chartData.isNotEmpty()) {
            val chartEntryModelProducer = ChartEntryModelProducer(
                chartData.mapIndexed { index, point ->
                    entryOf(index.toFloat(), point.value)
                }
            )

            val chartModel = chartEntryModelProducer.getModel()
            if (chartModel != null) {
                ProvideChartStyle(m3ChartStyle()) {
                    Chart(
                        chart = columnChart(),
                        model = chartModel,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Daten verfügbar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Pie Chart Widget (simplified - shows distribution)
@Composable
fun PieChartWidget(
    widget: WidgetConfig,
    statistics: StatisticsData
) {
    val distributionData = when (widget.dataSource) {
        WidgetDataSource.CATEGORY_DISTRIBUTION -> statistics.categoryStats.categoryDistribution.values.map { it.categoryName to it.taskCount }
        WidgetDataSource.DIFFICULTY_DISTRIBUTION -> statistics.difficultyDistribution.values.map { "${it.xpPercentage}%" to it.taskCount }
        WidgetDataSource.PRIORITY_DISTRIBUTION -> statistics.priorityDistribution.values.map { it.priority to it.taskCount }
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = widget.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (distributionData.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                distributionData.forEach { (label, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Daten verfügbar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Heatmap Widget (simplified grid view)
@Composable
fun HeatmapWidget(
    widget: WidgetConfig,
    statistics: StatisticsData
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = widget.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Heatmap - Coming Soon",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center
        )
    }
}

// Data Table Widget
@Composable
fun DataTableWidget(
    widget: WidgetConfig,
    statistics: StatisticsData
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = widget.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Data Table - Coming Soon",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center
        )
    }
}

// Progress Bar Widget
@Composable
fun ProgressBarWidget(
    widget: WidgetConfig,
    statistics: StatisticsData
) {
    val (progress, label) = when (widget.dataSource) {
        WidgetDataSource.COMPLETION_RATE -> statistics.taskStats.completionRate / 100f to "${statistics.taskStats.completionRate.toInt()}%"
        else -> 0f to "0%"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = widget.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

// Container for all statistics data needed by widgets
data class StatisticsData(
    val taskStats: TaskStatistics,
    val xpStats: XpStatistics,
    val categoryStats: CategoryStats,
    val productivityStats: ProductivityStats,
    val difficultyDistribution: Map<Int, DifficultyDistribution>,
    val priorityDistribution: Map<String, PriorityDistribution>,
    val xpTrendData: List<ChartDataPoint>,
    val taskCompletionTrendData: List<ChartDataPoint>
)
