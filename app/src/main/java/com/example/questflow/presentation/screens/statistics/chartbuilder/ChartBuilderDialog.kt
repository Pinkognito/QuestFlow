package com.example.questflow.presentation.screens.statistics.chartbuilder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartBuilderDialog(
    onDismiss: () -> Unit,
    onSave: (DynamicChartConfig) -> Unit,
    existingConfig: DynamicChartConfig? = null
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf(existingConfig?.title ?: "") }
    var chartType by remember { mutableStateOf(existingConfig?.chartType ?: ChartType.BAR_CHART) }
    var dataSource by remember { mutableStateOf(existingConfig?.dataSource ?: DataSource.TASKS) }
    var xAxisField by remember { mutableStateOf<DataField?>(null) }
    var yAxisField by remember { mutableStateOf<DataField?>(null) }
    var yAxisAggregation by remember { mutableStateOf(AggregationFunction.COUNT) }
    var groupingType by remember { mutableStateOf(GroupingType.BY_CATEGORY) }
    var dateInterval by remember { mutableStateOf(DateInterval.DAY) }

    val steps = listOf(
        "Diagramm-Typ",
        "Datenquelle",
        "X-Achse",
        "Y-Achse & Aggregation",
        "Optionen"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Chart erstellen",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Schritt ${currentStep + 1} von ${steps.size}: ${steps[currentStep]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Box(modifier = Modifier.height(400.dp)) {
                when (currentStep) {
                    0 -> ChartTypeStep(chartType) { chartType = it }
                    1 -> DataSourceStep(dataSource) { dataSource = it }
                    2 -> XAxisStep(dataSource, chartType, xAxisField) { xAxisField = it }
                    3 -> YAxisStep(dataSource, chartType, yAxisField, yAxisAggregation,
                        onFieldChange = { yAxisField = it },
                        onAggregationChange = { yAxisAggregation = it }
                    )
                    4 -> OptionsStep(
                        title = title,
                        groupingType = groupingType,
                        dateInterval = dateInterval,
                        onTitleChange = { title = it },
                        onGroupingChange = { groupingType = it },
                        onDateIntervalChange = { dateInterval = it }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zurück")
                    }
                }
                if (currentStep < steps.size - 1) {
                    Button(
                        onClick = { currentStep++ },
                        enabled = when (currentStep) {
                            2 -> xAxisField != null
                            3 -> yAxisField != null || chartType == ChartType.PIE_CHART
                            else -> true
                        }
                    ) {
                        Text("Weiter")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = {
                            if (xAxisField != null && title.isNotBlank()) {
                                val config = DynamicChartConfig(
                                    id = existingConfig?.id ?: 0,
                                    title = title,
                                    chartType = chartType,
                                    dataSource = dataSource,
                                    xAxisField = xAxisField!!.id,
                                    yAxisField = yAxisField?.id,
                                    yAxisAggregation = yAxisAggregation,
                                    xAxisGrouping = when (groupingType) {
                                        GroupingType.BY_DATE -> GroupingConfig(groupingType, dateInterval)
                                        else -> GroupingConfig(groupingType)
                                    }
                                )
                                onSave(config)
                                onDismiss()
                            }
                        },
                        enabled = xAxisField != null && title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Speichern")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun ChartTypeStep(
    selected: ChartType,
    onSelect: (ChartType) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ChartType.entries) { type ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == type)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { onSelect(type) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getChartTypeName(type),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getChartTypeDescription(type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected == type) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DataSourceStep(
    selected: DataSource,
    onSelect: (DataSource) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DataSource.entries) { source ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == source)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { onSelect(source) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getDataSourceName(source),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selected == source) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun XAxisStep(
    dataSource: DataSource,
    chartType: ChartType,
    selected: DataField?,
    onSelect: (DataField) -> Unit
) {
    val availableFields = DataField.getFieldsForDataSource(dataSource)
        .filter { ChartFieldCompatibility.isValidXAxis(chartType, it) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableFields) { field ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == field)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { onSelect(field) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = field.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = field.dataType.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected == field) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YAxisStep(
    dataSource: DataSource,
    chartType: ChartType,
    selectedField: DataField?,
    selectedAggregation: AggregationFunction,
    onFieldChange: (DataField) -> Unit,
    onAggregationChange: (AggregationFunction) -> Unit
) {
    val availableFields = DataField.getFieldsForDataSource(dataSource)
        .filter { ChartFieldCompatibility.isValidYAxis(chartType, it) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Feld auswählen:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        availableFields.forEach { field ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedField == field)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                onClick = { onFieldChange(field) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = field.label)
                    if (selectedField == field) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (selectedField != null) {
            HorizontalDivider()
            Text(
                text = "Aggregation:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ChartFieldCompatibility.getAvailableAggregations(selectedField).forEach { agg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAggregation == agg)
                            MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    onClick = { onAggregationChange(agg) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = agg.label)
                        if (selectedAggregation == agg) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsStep(
    title: String,
    groupingType: GroupingType,
    dateInterval: DateInterval,
    onTitleChange: (String) -> Unit,
    onGroupingChange: (GroupingType) -> Unit,
    onDateIntervalChange: (DateInterval) -> Unit
) {
    var expandedGrouping by remember { mutableStateOf(false) }
    var expandedInterval by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Chart-Titel") },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = expandedGrouping,
            onExpandedChange = { expandedGrouping = it }
        ) {
            OutlinedTextField(
                value = getGroupingTypeName(groupingType),
                onValueChange = {},
                readOnly = true,
                label = { Text("Gruppierung") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedGrouping) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedGrouping,
                onDismissRequest = { expandedGrouping = false }
            ) {
                GroupingType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(getGroupingTypeName(type)) },
                        onClick = {
                            onGroupingChange(type)
                            expandedGrouping = false
                        }
                    )
                }
            }
        }

        if (groupingType == GroupingType.BY_DATE) {
            ExposedDropdownMenuBox(
                expanded = expandedInterval,
                onExpandedChange = { expandedInterval = it }
            ) {
                OutlinedTextField(
                    value = getDateIntervalName(dateInterval),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Zeitintervall") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedInterval) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedInterval,
                    onDismissRequest = { expandedInterval = false }
                ) {
                    DateInterval.entries.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(getDateIntervalName(interval)) },
                            onClick = {
                                onDateIntervalChange(interval)
                                expandedInterval = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// Helper functions
private fun getChartTypeName(type: ChartType) = when (type) {
    ChartType.BAR_CHART -> "Balkendiagramm"
    ChartType.LINE_CHART -> "Liniendiagramm"
    ChartType.PIE_CHART -> "Tortendiagramm"
    ChartType.TABLE -> "Tabelle"
    ChartType.SCATTER_PLOT -> "Streudiagramm"
    ChartType.AREA_CHART -> "Flächendiagramm"
}

private fun getChartTypeDescription(type: ChartType) = when (type) {
    ChartType.BAR_CHART -> "Vergleiche Werte in Kategorien"
    ChartType.LINE_CHART -> "Zeige Trends über Zeit"
    ChartType.PIE_CHART -> "Zeige Anteile am Ganzen"
    ChartType.TABLE -> "Zeige detaillierte Daten in Tabellenform"
    ChartType.SCATTER_PLOT -> "Zeige Korrelationen zwischen zwei Werten"
    ChartType.AREA_CHART -> "Zeige kumulative Werte über Zeit"
}

private fun getDataSourceName(source: DataSource) = when (source) {
    DataSource.TASKS -> "Tasks"
    DataSource.XP_TRANSACTIONS -> "XP-Transaktionen"
    DataSource.CATEGORIES -> "Kategorien"
    DataSource.CALENDAR_EVENTS -> "Kalender-Events"
}

private fun getGroupingTypeName(type: GroupingType) = when (type) {
    GroupingType.NONE -> "Keine Gruppierung"
    GroupingType.BY_CATEGORY -> "Nach Kategorie"
    GroupingType.BY_DATE -> "Nach Datum"
}

private fun getDateIntervalName(interval: DateInterval) = when (interval) {
    DateInterval.DAY -> "Tag"
    DateInterval.WEEK -> "Woche"
    DateInterval.MONTH -> "Monat"
    DateInterval.YEAR -> "Jahr"
}
