package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.questflow.data.preferences.TimeRange

/**
 * Settings dialog for timeline view.
 * Allows user to configure tolerance, time range, and display options.
 */
@Composable
fun TimelineSettingsDialog(
    currentTolerance: Int,
    currentTimeRange: TimeRange,
    onDismiss: () -> Unit,
    onToleranceChange: (Int) -> Unit,
    onTimeRangeChange: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var tolerance by remember { mutableStateOf(currentTolerance.toFloat()) }
    var timeRange by remember { mutableStateOf(currentTimeRange) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timeline Einstellungen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Tolerance setting
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Toleranz: ${tolerance.toInt()} Minuten",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Minimaler Zeitabstand zwischen Tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = tolerance,
                        onValueChange = { tolerance = it },
                        valueRange = 0f..120f,
                        steps = 23, // 5-minute intervals
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 Min", style = MaterialTheme.typography.labelSmall)
                        Text("120 Min", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Divider()

                // Time range setting
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zeitbereich",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Anzahl der angezeigten Tage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TimeRange.values().forEach { range ->
                            TimeRangeOption(
                                range = range,
                                isSelected = timeRange == range,
                                onSelect = { timeRange = range }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onToleranceChange(tolerance.toInt())
                    onTimeRangeChange(timeRange)
                    onDismiss()
                }
            ) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        modifier = modifier
    )
}

/**
 * Single time range option chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeOption(
    range: TimeRange,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = { Text(range.displayName) },
        modifier = Modifier.fillMaxWidth()
    )
}
