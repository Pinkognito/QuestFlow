package com.example.questflow.presentation.screens.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.questflow.data.database.entity.TimeRange
import com.example.questflow.domain.model.*
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.AnimatedXpLevelBadge
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()

    // Reload statistics when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiken") },
                actions = {
                    globalStats?.let { stats ->
                        AnimatedXpLevelBadge(
                            level = stats.level,
                            xp = stats.xp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Time Range Selector
                item {
                    TimeRangeSelector(
                        selectedTimeRange = uiState.selectedTimeRange,
                        onTimeRangeSelected = { viewModel.setTimeRange(it) }
                    )
                }

                // Filter Section
                item {
                    FilterSection(
                        isExpanded = uiState.isFilterExpanded,
                        onToggleExpanded = { viewModel.toggleFilterExpanded() },
                        selectedCategoryFilter = uiState.selectedCategoryFilter,
                        selectedDifficultyFilter = uiState.selectedDifficultyFilter,
                        selectedPriorityFilter = uiState.selectedPriorityFilter,
                        showCompletedOnly = uiState.showCompletedOnly,
                        onCategoryFilterChanged = { viewModel.setCategoryFilter(it) },
                        onDifficultyFilterChanged = { viewModel.setDifficultyFilter(it) },
                        onPriorityFilterChanged = { viewModel.setPriorityFilter(it) },
                        onShowCompletedOnlyChanged = { viewModel.setShowCompletedOnly(it) }
                    )
                }

                // Statistics Summary Cards
                item {
                    StatisticsSummaryCards(
                        taskStatistics = uiState.taskStatistics,
                        xpStatistics = uiState.xpStatistics,
                        productivityStats = uiState.productivityStats
                    )
                }

                // Charts (ordered according to config)
                items(uiState.chartOrder.filter { it in uiState.visibleCharts }) { chartId ->
                    when (chartId) {
                        "xp_trend" -> {
                            uiState.xpTrendData?.let { data ->
                                XpTrendChart(data = data)
                            }
                        }
                        "task_completion" -> {
                            uiState.taskCompletionTrendData?.let { data ->
                                TaskCompletionChart(data = data)
                            }
                        }
                        "category_distribution" -> {
                            uiState.categoryStats?.let { stats ->
                                CategoryDistributionCard(categoryStats = stats)
                            }
                        }
                        "difficulty_distribution" -> {
                            uiState.difficultyDistribution?.let { distribution ->
                                DifficultyDistributionCard(distribution = distribution)
                            }
                        }
                        "priority_distribution" -> {
                            uiState.priorityDistribution?.let { distribution ->
                                PriorityDistributionCard(distribution = distribution)
                            }
                        }
                        "productivity_heatmap" -> {
                            uiState.productivityStats?.let { stats ->
                                ProductivityHeatmapCard(productivityStats = stats)
                            }
                        }
                        "xp_sources" -> {
                            uiState.xpStatistics?.let { stats ->
                                XpSourcesCard(xpStatistics = stats)
                            }
                        }
                        "streak_calendar" -> {
                            uiState.taskStatistics?.let { stats ->
                                StreakCard(taskStatistics = stats)
                            }
                        }
                    }
                }

                // Chart Configuration Section
                item {
                    ChartConfigSection(
                        isExpanded = uiState.isConfigExpanded,
                        onToggleExpanded = { viewModel.toggleConfigExpanded() },
                        visibleCharts = uiState.visibleCharts,
                        onToggleChartVisibility = { viewModel.toggleChartVisibility(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Zeitraum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.values().forEach { timeRange ->
                    if (timeRange != TimeRange.CUSTOM) {
                        FilterChip(
                            selected = selectedTimeRange == timeRange,
                            onClick = { onTimeRangeSelected(timeRange) },
                            label = {
                                Text(
                                    text = when (timeRange) {
                                        TimeRange.TODAY -> "Heute"
                                        TimeRange.WEEK -> "Woche"
                                        TimeRange.MONTH -> "Monat"
                                        TimeRange.YEAR -> "Jahr"
                                        TimeRange.LAST_7_DAYS -> "7 Tage"
                                        TimeRange.LAST_30_DAYS -> "30 Tage"
                                        else -> ""
                                    },
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedCategoryFilter: Long?,
    selectedDifficultyFilter: List<Int>?,
    selectedPriorityFilter: List<String>?,
    showCompletedOnly: Boolean,
    onCategoryFilterChanged: (Long?) -> Unit,
    onDifficultyFilterChanged: (List<Int>?) -> Unit,
    onPriorityFilterChanged: (List<String>?) -> Unit,
    onShowCompletedOnlyChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showCompletedOnly,
                            onCheckedChange = onShowCompletedOnlyChanged
                        )
                        Text("Nur abgeschlossene Tasks")
                    }

                    Text(
                        text = "Weitere Filter können hier hinzugefügt werden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsSummaryCards(
    taskStatistics: TaskStatistics?,
    xpStatistics: XpStatistics?,
    productivityStats: ProductivityStats?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        taskStatistics?.let { stats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Abgeschlossen",
                    value = stats.totalTasksCompleted.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Erfolgsrate",
                    value = "${stats.completionRate.toInt()}%",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Streak",
                    value = "${stats.currentStreak}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        xpStatistics?.let { stats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Gesamt XP",
                    value = NumberFormat.getNumberInstance(Locale.GERMANY).format(stats.totalXpEarned),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Ø XP/Task",
                    value = "${stats.averageXpPerTask.toInt()}",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Ø XP/Tag",
                    value = "${stats.averageXpPerDay.toInt()}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun XpTrendChart(data: List<ChartDataPoint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "XP-Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (data.isNotEmpty()) {
                val entries = data.mapIndexed { index, point ->
                    entryOf(index.toFloat(), point.value)
                }
                val chartEntryModelProducer = remember(data) {
                    ChartEntryModelProducer(entries)
                }
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
}

@Composable
fun TaskCompletionChart(data: List<ChartDataPoint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Task-Abschlüsse",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (data.isNotEmpty()) {
                val entries = data.mapIndexed { index, point ->
                    entryOf(index.toFloat(), point.value)
                }
                val chartEntryModelProducer = remember(data) {
                    ChartEntryModelProducer(entries)
                }
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
}

@Composable
fun CategoryDistributionCard(categoryStats: CategoryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Kategorie-Verteilung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (categoryStats.categoryDistribution.isNotEmpty()) {
                categoryStats.categoryDistribution.values.forEach { category ->
                    CategoryDistributionItem(category)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "Keine Kategorien verfügbar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryDistributionItem(category: CategoryDistribution) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${category.completedCount}/${category.taskCount} Tasks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${NumberFormat.getNumberInstance(Locale.GERMANY).format(category.totalXp)} XP",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DifficultyDistributionCard(distribution: Map<Int, DifficultyDistribution>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Schwierigkeits-Verteilung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            listOf(20, 40, 60, 80, 100).forEach { percentage ->
                distribution[percentage]?.let { dist ->
                    DifficultyDistributionItem(
                        name = when (percentage) {
                            20 -> "Trivial"
                            40 -> "Einfach"
                            60 -> "Mittel"
                            80 -> "Schwer"
                            100 -> "Episch"
                            else -> "Unbekannt"
                        },
                        distribution = dist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun DifficultyDistributionItem(name: String, distribution: DifficultyDistribution) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${distribution.completedCount}/${distribution.taskCount}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PriorityDistributionCard(distribution: Map<String, PriorityDistribution>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Prioritäts-Verteilung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            listOf("HIGH", "MEDIUM", "LOW").forEach { priority ->
                distribution[priority]?.let { dist ->
                    PriorityDistributionItem(
                        name = when (priority) {
                            "HIGH" -> "Hoch"
                            "MEDIUM" -> "Mittel"
                            "LOW" -> "Niedrig"
                            else -> priority
                        },
                        distribution = dist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PriorityDistributionItem(name: String, distribution: PriorityDistribution) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${distribution.completedCount}/${distribution.taskCount}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProductivityHeatmapCard(productivityStats: ProductivityStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Produktivität",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Produktivster Tag: ${getDayName(productivityStats.mostProductiveDayOfWeek)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Produktivste Stunde: ${productivityStats.mostProductiveHour}:00 Uhr",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Aktive Tage: ${productivityStats.totalActiveDays}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun XpSourcesCard(xpStatistics: XpStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "XP-Quellen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            xpStatistics.xpBySource.entries.sortedByDescending { it.value }.forEach { (source, xp) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (source) {
                            "TASK" -> "Tasks"
                            "CALENDAR" -> "Kalender"
                            "SUBTASK" -> "Subtasks"
                            "SYSTEM" -> "System"
                            else -> source
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.GERMANY).format(xp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun StreakCard(taskStatistics: TaskStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${taskStatistics.currentStreak}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Aktuell",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${taskStatistics.longestStreak}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Längste",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ChartConfigSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    visibleCharts: Set<String>,
    onToggleChartVisibility: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chart-Konfiguration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chartNames = mapOf(
                        "xp_trend" to "XP-Trend",
                        "task_completion" to "Task-Abschlüsse",
                        "category_distribution" to "Kategorie-Verteilung",
                        "difficulty_distribution" to "Schwierigkeits-Verteilung",
                        "priority_distribution" to "Prioritäts-Verteilung",
                        "productivity_heatmap" to "Produktivität",
                        "xp_sources" to "XP-Quellen",
                        "streak_calendar" to "Streak"
                    )

                    chartNames.forEach { (chartId, chartName) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = chartId in visibleCharts,
                                onCheckedChange = { onToggleChartVisibility(chartId) }
                            )
                            Text(chartName)
                        }
                    }
                }
            }
        }
    }
}

fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Montag"
        2 -> "Dienstag"
        3 -> "Mittwoch"
        4 -> "Donnerstag"
        5 -> "Freitag"
        6 -> "Samstag"
        7 -> "Sonntag"
        else -> "Unbekannt"
    }
}
