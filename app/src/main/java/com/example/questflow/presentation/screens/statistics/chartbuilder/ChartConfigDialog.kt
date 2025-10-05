package com.example.questflow.presentation.screens.statistics.chartbuilder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartConfigDialog(
    onDismiss: () -> Unit,
    onSave: (DynamicChartConfig) -> Unit,
    existingConfig: DynamicChartConfig? = null
) {
    var title by remember { mutableStateOf(existingConfig?.title ?: "") }
    var chartType by remember { mutableStateOf(existingConfig?.chartType ?: ChartType.BAR_CHART) }
    var dataSource by remember { mutableStateOf(existingConfig?.dataSource ?: DataSource.TASKS) }
    var xAxisField by remember { mutableStateOf<DataField?>(
        existingConfig?.let { config ->
            DataField.getFieldsForDataSource(config.dataSource)
                .find { it.id == config.xAxisField }
        }
    ) }
    var yAxisField by remember { mutableStateOf<DataField?>(
        existingConfig?.yAxisField?.let { yField ->
            DataField.getFieldsForDataSource(existingConfig.dataSource)
                .find { it.id == yField }
        }
    ) }
    var yAxisAggregation by remember { mutableStateOf(existingConfig?.yAxisAggregation ?: AggregationFunction.COUNT) }
    var groupingType by remember { mutableStateOf(existingConfig?.xAxisGrouping?.type ?: GroupingType.BY_CATEGORY) }
    var dateInterval by remember { mutableStateOf(existingConfig?.xAxisGrouping?.dateInterval ?: DateInterval.DAY) }
    var chartHeight by remember { mutableStateOf(existingConfig?.height ?: ChartHeight.MEDIUM) }
    var timeRangeType by remember { mutableStateOf(existingConfig?.timeRange?.type ?: TimeRangeType.ALL_TIME) }

    var expandedChartType by remember { mutableStateOf(false) }
    var expandedDataSource by remember { mutableStateOf(false) }
    var expandedXAxis by remember { mutableStateOf(false) }
    var expandedYAxis by remember { mutableStateOf(false) }
    var expandedAggregation by remember { mutableStateOf(false) }
    var expandedGrouping by remember { mutableStateOf(false) }
    var expandedInterval by remember { mutableStateOf(false) }
    var expandedHeight by remember { mutableStateOf(false) }
    var expandedTimeRange by remember { mutableStateOf(false) }

    // Update fields when data source changes
    LaunchedEffect(dataSource) {
        xAxisField = null
        yAxisField = null
    }

    // Auto-set grouping when X-axis is a date field
    LaunchedEffect(xAxisField) {
        if (xAxisField?.dataType == FieldDataType.DATE) {
            groupingType = GroupingType.BY_DATE
            // Ensure dateInterval has a default value
            dateInterval = DateInterval.DAY
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = if (existingConfig != null) "Chart bearbeiten" else "Neues Chart",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Chart Type
                ExposedDropdownMenuBox(
                    expanded = expandedChartType,
                    onExpandedChange = { expandedChartType = it }
                ) {
                    OutlinedTextField(
                        value = getChartTypeName(chartType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Diagramm-Typ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedChartType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedChartType,
                        onDismissRequest = { expandedChartType = false }
                    ) {
                        ChartType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getChartTypeName(type)) },
                                onClick = {
                                    chartType = type
                                    expandedChartType = false
                                }
                            )
                        }
                    }
                }

                // Data Source
                ExposedDropdownMenuBox(
                    expanded = expandedDataSource,
                    onExpandedChange = { expandedDataSource = it }
                ) {
                    OutlinedTextField(
                        value = getDataSourceName(dataSource),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Datenquelle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDataSource) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDataSource,
                        onDismissRequest = { expandedDataSource = false }
                    ) {
                        DataSource.entries.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(getDataSourceName(source)) },
                                onClick = {
                                    dataSource = source
                                    expandedDataSource = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Helper Text for Chart Configuration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = getChartTypeHint(chartType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // X-Axis
                val xAxisFields = DataField.getFieldsForDataSource(dataSource)
                    .filter { ChartFieldCompatibility.isValidXAxis(chartType, it) }

                ExposedDropdownMenuBox(
                    expanded = expandedXAxis,
                    onExpandedChange = { expandedXAxis = it }
                ) {
                    OutlinedTextField(
                        value = xAxisField?.label ?: "Auswählen...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(getXAxisLabel(chartType)) },
                        supportingText = { Text(getXAxisHint(chartType)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedXAxis) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedXAxis,
                        onDismissRequest = { expandedXAxis = false }
                    ) {
                        xAxisFields.forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.label) },
                                onClick = {
                                    xAxisField = field
                                    expandedXAxis = false
                                }
                            )
                        }
                    }
                }

                // What to measure (simplified)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Was möchtest du messen?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getWhatToMeasureExample(chartType, xAxisField),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Aggregation - Simplified approach
                val availableAggregations = if (ChartFieldCompatibility.requiresYAxis(chartType) && yAxisField != null) {
                    ChartFieldCompatibility.getAvailableAggregations(yAxisField!!)
                } else {
                    listOf(AggregationFunction.COUNT)
                }

                ExposedDropdownMenuBox(
                    expanded = expandedAggregation,
                    onExpandedChange = { expandedAggregation = it }
                ) {
                    OutlinedTextField(
                        value = getAggregationDisplayName(yAxisAggregation, yAxisField),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Messwert") },
                        supportingText = { Text(getAggregationExample(yAxisAggregation, xAxisField, yAxisField)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedAggregation) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAggregation,
                        onDismissRequest = { expandedAggregation = false }
                    ) {
                        availableAggregations.forEach { agg ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = getAggregationDisplayName(agg, yAxisField),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = getAggregationShortExample(agg, yAxisField),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    yAxisAggregation = agg
                                    expandedAggregation = false
                                }
                            )
                        }
                    }
                }

                // Y-Axis field (only for numeric aggregations)
                if (ChartFieldCompatibility.requiresYAxis(chartType) &&
                    yAxisAggregation in listOf(AggregationFunction.SUM, AggregationFunction.AVERAGE,
                                              AggregationFunction.MIN, AggregationFunction.MAX)) {
                    val yAxisFields = DataField.getFieldsForDataSource(dataSource)
                        .filter { ChartFieldCompatibility.isValidYAxis(chartType, it) }

                    ExposedDropdownMenuBox(
                        expanded = expandedYAxis,
                        onExpandedChange = { expandedYAxis = it }
                    ) {
                        OutlinedTextField(
                            value = yAxisField?.label ?: "Auswählen...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Welches Feld?") },
                            supportingText = { Text("Zahlenfeld für ${yAxisAggregation.label}") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedYAxis) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedYAxis,
                            onDismissRequest = { expandedYAxis = false }
                        ) {
                            yAxisFields.forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.label) },
                                    onClick = {
                                        yAxisField = field
                                        expandedYAxis = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Date Interval - ALWAYS show when X-axis is a date
                if (xAxisField?.dataType == FieldDataType.DATE) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "📅 Zeitbasiertes Diagramm - Wähle das Intervall",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedInterval,
                        onExpandedChange = { expandedInterval = it }
                    ) {
                        OutlinedTextField(
                            value = getDateIntervalName(dateInterval),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pro ...") },
                            supportingText = { Text("Ein Balken/Punkt pro ${getDateIntervalName(dateInterval)}") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedInterval) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedInterval,
                            onDismissRequest = { expandedInterval = false }
                        ) {
                            DateInterval.entries.forEach { interval ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Pro ${getDateIntervalName(interval)}",
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = getIntervalExample(interval, timeRangeType),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        dateInterval = interval
                                        expandedInterval = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Grouping (only for non-date fields)
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
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedGrouping,
                            onDismissRequest = { expandedGrouping = false }
                        ) {
                            GroupingType.entries.filter { it != GroupingType.BY_DATE }.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(getGroupingTypeName(type)) },
                                    onClick = {
                                        groupingType = type
                                        expandedGrouping = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Time Range Filter
                ExposedDropdownMenuBox(
                    expanded = expandedTimeRange,
                    onExpandedChange = { expandedTimeRange = it }
                ) {
                    OutlinedTextField(
                        value = getTimeRangeName(timeRangeType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Zeitraum") },
                        supportingText = { Text("Filtere Daten nach Zeitraum") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTimeRange) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTimeRange,
                        onDismissRequest = { expandedTimeRange = false }
                    ) {
                        TimeRangeType.entries.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(getTimeRangeName(range)) },
                                onClick = {
                                    timeRangeType = range
                                    expandedTimeRange = false
                                }
                            )
                        }
                    }
                }

                // Chart Height
                ExposedDropdownMenuBox(
                    expanded = expandedHeight,
                    onExpandedChange = { expandedHeight = it }
                ) {
                    OutlinedTextField(
                        value = getChartHeightName(chartHeight),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Höhe") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedHeight) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedHeight,
                        onDismissRequest = { expandedHeight = false }
                    ) {
                        ChartHeight.entries.forEach { height ->
                            DropdownMenuItem(
                                text = { Text(getChartHeightName(height)) },
                                onClick = {
                                    chartHeight = height
                                    expandedHeight = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Y-Achse nur erforderlich bei numerischen Aggregationen (SUM, AVG, MIN, MAX)
            val needsYField = yAxisAggregation in listOf(
                AggregationFunction.SUM, AggregationFunction.AVERAGE,
                AggregationFunction.MIN, AggregationFunction.MAX
            )

            val isValid = xAxisField != null &&
                         title.isNotBlank() &&
                         (!needsYField || yAxisField != null)

            Button(
                onClick = {
                    if (isValid) {
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
                            },
                            timeRange = if (timeRangeType != TimeRangeType.ALL_TIME) {
                                TimeRangeFilter(timeRangeType)
                            } else null,
                            height = chartHeight
                        )
                        onSave(config)
                    }
                },
                enabled = isValid
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
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

private fun getChartHeightName(height: ChartHeight) = when (height) {
    ChartHeight.SMALL -> "Klein (200dp)"
    ChartHeight.MEDIUM -> "Mittel (300dp)"
    ChartHeight.LARGE -> "Groß (400dp)"
    ChartHeight.EXTRA_LARGE -> "Extra Groß (500dp)"
}

private fun getChartTypeHint(chartType: ChartType) = when (chartType) {
    ChartType.BAR_CHART -> "📊 Balkendiagramm: Vergleiche Werte über Kategorien oder Zeiträume"
    ChartType.LINE_CHART -> "📈 Liniendiagramm: Zeige Trends über die Zeit"
    ChartType.PIE_CHART -> "🥧 Tortendiagramm: Zeige Verteilung/Anteile (z.B. Tasks pro Kategorie oder Datum)"
    ChartType.TABLE -> "📋 Tabelle: Strukturierte Datenansicht"
    ChartType.SCATTER_PLOT -> "📍 Streudiagramm: Zeige Korrelationen zwischen zwei Werten"
    ChartType.AREA_CHART -> "📊 Flächendiagramm: Zeige kumulierte Werte über Zeit"
}

private fun getXAxisLabel(chartType: ChartType) = when (chartType) {
    ChartType.PIE_CHART -> "Gruppierung (Segmente)"
    ChartType.TABLE -> "Erste Spalte"
    ChartType.LINE_CHART, ChartType.AREA_CHART -> "X-Achse (Zeit)"
    else -> "X-Achse (Kategorie)"
}

private fun getXAxisHint(chartType: ChartType) = when (chartType) {
    ChartType.PIE_CHART -> "Wonach sollen die Segmente gruppiert werden?"
    ChartType.LINE_CHART -> "Zeitfeld (z.B. Abschlussdatum)"
    ChartType.BAR_CHART -> "Kategorie oder Zeitfeld"
    ChartType.TABLE -> "Hauptfeld für die Tabelle"
    else -> "Feld für die X-Achse"
}

private fun getAggregationHint(chartType: ChartType) = when (chartType) {
    ChartType.PIE_CHART -> "Wie soll gezählt/berechnet werden?"
    ChartType.BAR_CHART -> "Wie sollen Werte zusammengefasst werden?"
    ChartType.LINE_CHART -> "Wie sollen Werte pro Zeitpunkt berechnet werden?"
    else -> "Berechnungsmethode"
}

private fun getTimeRangeName(type: TimeRangeType) = when (type) {
    TimeRangeType.LAST_7_DAYS -> "Letzte 7 Tage"
    TimeRangeType.LAST_30_DAYS -> "Letzte 30 Tage"
    TimeRangeType.LAST_3_MONTHS -> "Letzte 3 Monate"
    TimeRangeType.LAST_6_MONTHS -> "Letzte 6 Monate"
    TimeRangeType.LAST_YEAR -> "Letztes Jahr"
    TimeRangeType.LAST_2_YEARS -> "Letzte 2 Jahre"
    TimeRangeType.LAST_3_YEARS -> "Letzte 3 Jahre"
    TimeRangeType.ALL_TIME -> "Alle Zeit"
    TimeRangeType.CUSTOM -> "Benutzerdefiniert"
}

private fun getWhatToMeasureExample(chartType: ChartType, xField: DataField?) = when {
    chartType == ChartType.PIE_CHART -> "Beispiel: Zeige wie viele Tasks pro ${xField?.label ?: "Kategorie"}"
    chartType == ChartType.BAR_CHART && xField?.dataType == FieldDataType.DATE ->
        "Beispiel: Zeige Anzahl Tasks pro Monat/Tag"
    chartType == ChartType.LINE_CHART -> "Beispiel: Zeige XP-Entwicklung über Zeit"
    else -> "Wähle was gemessen werden soll (z.B. Anzahl, Summe, Durchschnitt)"
}

private fun getAggregationDisplayName(agg: AggregationFunction, yField: DataField?) = when (agg) {
    AggregationFunction.COUNT -> "Wie viele?"
    AggregationFunction.SUM -> "Gesamt${if (yField != null) " (${yField.label})" else ""}"
    AggregationFunction.AVERAGE -> "Durchschnitt${if (yField != null) " (${yField.label})" else ""}"
    AggregationFunction.MIN -> "Minimum${if (yField != null) " (${yField.label})" else ""}"
    AggregationFunction.MAX -> "Maximum${if (yField != null) " (${yField.label})" else ""}"
    AggregationFunction.FIRST -> "Erster Wert"
    AggregationFunction.LAST -> "Letzter Wert"
}

private fun getAggregationShortExample(agg: AggregationFunction, yField: DataField?) = when (agg) {
    AggregationFunction.COUNT -> "Zähle Einträge"
    AggregationFunction.SUM -> "Addiere ${yField?.label ?: "Werte"}"
    AggregationFunction.AVERAGE -> "Durchschnitt von ${yField?.label ?: "Werten"}"
    AggregationFunction.MIN -> "Kleinster Wert"
    AggregationFunction.MAX -> "Größter Wert"
    AggregationFunction.FIRST -> "Erster Eintrag"
    AggregationFunction.LAST -> "Letzter Eintrag"
}

private fun getAggregationExample(agg: AggregationFunction, xField: DataField?, yField: DataField?) = when (agg) {
    AggregationFunction.COUNT -> "Beispiel: Wie viele Tasks pro ${xField?.label ?: "Gruppe"}?"
    AggregationFunction.SUM -> "Beispiel: Gesamt-${yField?.label ?: "XP"} pro ${xField?.label ?: "Monat"}"
    AggregationFunction.AVERAGE -> "Beispiel: Durchschnittliche ${yField?.label ?: "XP"} pro ${xField?.label ?: "Monat"}"
    AggregationFunction.MIN -> "Beispiel: Niedrigster Wert"
    AggregationFunction.MAX -> "Beispiel: Höchster Wert"
    else -> "Wähle die Berechnungsart"
}

private fun getIntervalExample(interval: DateInterval, timeRange: TimeRangeType) = when (interval) {
    DateInterval.DAY -> when (timeRange) {
        TimeRangeType.LAST_7_DAYS -> "= 7 Balken"
        TimeRangeType.LAST_30_DAYS -> "= 30 Balken"
        TimeRangeType.LAST_3_MONTHS -> "= ~90 Balken"
        else -> "Ein Balken pro Tag"
    }
    DateInterval.WEEK -> "Eine Woche pro Balken"
    DateInterval.MONTH -> when (timeRange) {
        TimeRangeType.LAST_YEAR -> "= 12 Balken"
        TimeRangeType.LAST_3_YEARS -> "= 36 Balken"
        else -> "Ein Balken pro Monat"
    }
    DateInterval.YEAR -> "Ein Balken pro Jahr"
}
