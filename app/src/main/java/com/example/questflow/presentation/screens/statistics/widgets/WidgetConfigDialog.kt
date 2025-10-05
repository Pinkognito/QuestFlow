package com.example.questflow.presentation.screens.statistics.widgets

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
fun WidgetConfigDialog(
    widget: WidgetConfig,
    onDismiss: () -> Unit,
    onSave: (WidgetConfig) -> Unit
) {
    var title by remember { mutableStateOf(widget.title) }
    var timeRange by remember { mutableStateOf(widget.timeRange) }
    var widgetSize by remember { mutableStateOf(widget.widgetSize) }
    var expandedTimeRange by remember { mutableStateOf(false) }
    var expandedSize by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Widget konfigurieren",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Time Range Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedTimeRange,
                    onExpandedChange = { expandedTimeRange = it }
                ) {
                    OutlinedTextField(
                        value = getTimeRangeLabel(timeRange),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Zeitbereich") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTimeRange) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTimeRange,
                        onDismissRequest = { expandedTimeRange = false }
                    ) {
                        WidgetTimeRange.entries.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(getTimeRangeLabel(range)) },
                                onClick = {
                                    timeRange = range
                                    expandedTimeRange = false
                                }
                            )
                        }
                    }
                }

                // Widget Size Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedSize,
                    onExpandedChange = { expandedSize = it }
                ) {
                    OutlinedTextField(
                        value = getSizeLabel(widgetSize),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Größe") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSize) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSize,
                        onDismissRequest = { expandedSize = false }
                    ) {
                        WidgetSize.entries.forEach { size ->
                            DropdownMenuItem(
                                text = { Text(getSizeLabel(size)) },
                                onClick = {
                                    widgetSize = size
                                    expandedSize = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        widget.copy(
                            title = title,
                            timeRange = timeRange,
                            widgetSize = widgetSize
                        )
                    )
                    onDismiss()
                }
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

private fun getTimeRangeLabel(range: WidgetTimeRange): String = when (range) {
    WidgetTimeRange.TODAY -> "Heute"
    WidgetTimeRange.YESTERDAY -> "Gestern"
    WidgetTimeRange.LAST_7_DAYS -> "Letzte 7 Tage"
    WidgetTimeRange.LAST_30_DAYS -> "Letzte 30 Tage"
    WidgetTimeRange.THIS_WEEK -> "Diese Woche"
    WidgetTimeRange.THIS_MONTH -> "Dieser Monat"
    WidgetTimeRange.THIS_YEAR -> "Dieses Jahr"
    WidgetTimeRange.ALL_TIME -> "Alle Zeit"
    WidgetTimeRange.CUSTOM -> "Benutzerdefiniert"
}

private fun getSizeLabel(size: WidgetSize): String = when (size) {
    WidgetSize.SMALL -> "Klein"
    WidgetSize.MEDIUM -> "Mittel"
    WidgetSize.LARGE -> "Groß"
}
