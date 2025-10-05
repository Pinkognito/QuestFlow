package com.example.questflow.presentation.screens.statistics.chartbuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun DynamicChartCard(
    config: DynamicChartConfig,
    data: ChartDataResult,
    isEditMode: Boolean = false,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(config.height.dp.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Axis Labels Info
                if (config.showAxisLabels && config.chartType != ChartType.PIE_CHART) {
                    Text(
                        text = buildAxisLabel(config),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Chart Content
                when (config.chartType) {
                    ChartType.BAR_CHART -> BarChartRenderer(data, config)
                    ChartType.LINE_CHART -> LineChartRenderer(data, config)
                    ChartType.PIE_CHART -> PieChartRenderer(data, config)
                    ChartType.TABLE -> TableRenderer(data, config)
                    ChartType.SCATTER_PLOT -> ScatterPlotRenderer(data, config)
                    ChartType.AREA_CHART -> AreaChartRenderer(data, config)
                }
            }

            // Edit Mode Controls
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Bearbeiten",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
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

@Composable
fun BarChartRenderer(
    data: ChartDataResult,
    config: DynamicChartConfig
) {
    if (data.values.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    val chartEntryModelProducer = ChartEntryModelProducer(
        data.values.mapIndexed { index, value ->
            entryOf(index.toFloat(), value)
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

        // Legend
        if (config.showLegend && data.labels.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(data.labels.take(5)) { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LineChartRenderer(
    data: ChartDataResult,
    config: DynamicChartConfig
) {
    if (data.values.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    val chartEntryModelProducer = ChartEntryModelProducer(
        data.values.mapIndexed { index, value ->
            entryOf(index.toFloat(), value)
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
}

@Composable
fun PieChartRenderer(
    data: ChartDataResult,
    config: DynamicChartConfig
) {
    if (data.values.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    val total = data.values.sum()
    val colors = listOf(
        androidx.compose.ui.graphics.Color(0xFF6200EE),
        androidx.compose.ui.graphics.Color(0xFF03DAC5),
        androidx.compose.ui.graphics.Color(0xFFBB86FC),
        androidx.compose.ui.graphics.Color(0xFF018786),
        androidx.compose.ui.graphics.Color(0xFFCF6679),
        androidx.compose.ui.graphics.Color(0xFF3700B3)
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Pie Chart
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(16.dp)
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                var startAngle = -90f
                data.values.forEachIndexed { index, value ->
                    val sweepAngle = (value / total) * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
        }

        // Legend
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.labels.zip(data.values).forEachIndexed { index, (label, value) ->
                val percentage = if (total > 0) (value / total * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size])
                    )
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableRenderer(
    data: ChartDataResult,
    config: DynamicChartConfig
) {
    if (data.values.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = config.xAxisField,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = config.yAxisAggregation.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider()
        }

        // Rows
        items(data.labels.zip(data.values)) { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = NumberFormat.getNumberInstance(Locale.GERMAN).format(value),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun ScatterPlotRenderer(
    data: ChartDataResult,
    config: DynamicChartConfig
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Streudiagramm - Coming Soon",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AreaChartRenderer(
    data: ChartDataResult,
    config: DynamicChartConfig
) {
    // Use line chart as fallback for now
    LineChartRenderer(data, config)
}

@Composable
fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Keine Daten verfügbar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Passe die Filter oder den Zeitraum an",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildAxisLabel(config: DynamicChartConfig): String {
    val xLabel = DataField.getFieldsForDataSource(config.dataSource)
        .find { it.id == config.xAxisField }?.label ?: config.xAxisField

    val yLabel = config.yAxisField?.let { yField ->
        DataField.getFieldsForDataSource(config.dataSource)
            .find { it.id == yField }?.label ?: yField
    }

    return if (yLabel != null) {
        "X: $xLabel  •  Y: ${config.yAxisAggregation.label} ($yLabel)"
    } else {
        "X: $xLabel  •  Y: ${config.yAxisAggregation.label}"
    }
}
