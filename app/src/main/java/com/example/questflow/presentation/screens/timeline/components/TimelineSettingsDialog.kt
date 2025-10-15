package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings dialog for timeline view.
 * UPDATED: Supports tolerance, zoom, and long-press delay configuration.
 */
@Composable
fun TimelineSettingsDialog(
    currentTolerance: Int, // In minutes (0-1440)
    currentVisibleHours: Float, // Zoom level (2-24 hours)
    currentLongPressDelayMs: Long = 250L, // Long-press delay in ms
    onDismiss: () -> Unit,
    onToleranceChange: (Int) -> Unit,
    onVisibleHoursChange: (Float) -> Unit,
    onLongPressDelayChange: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var toleranceHours by remember { mutableStateOf(currentTolerance / 60f) }
    var visibleHours by remember { mutableStateOf(currentVisibleHours) }
    var longPressDelayMs by remember { mutableStateOf(currentLongPressDelayMs.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timeline Einstellungen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Zoom setting (visible hours)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hours = visibleHours.toInt()
                    val minutes = ((visibleHours - hours) * 60).toInt()

                    Text(
                        text = if (minutes > 0) {
                            "Zoom: ${hours}h ${minutes}min sichtbar"
                        } else {
                            "Zoom: ${hours}h sichtbar"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = "Wie viele Stunden auf dem Bildschirm angezeigt werden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = visibleHours,
                        onValueChange = { visibleHours = it },
                        valueRange = 2f..24f,
                        steps = 43, // 0.5 hour intervals (22 steps)
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2h (max zoom)", style = MaterialTheme.typography.labelSmall)
                        Text("24h (volle Übersicht)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Tolerance setting (0-24 hours)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hours = toleranceHours.toInt()
                    val minutes = ((toleranceHours - hours) * 60).toInt()

                    Text(
                        text = if (hours > 0 && minutes > 0) {
                            "Toleranz: ${hours}h ${minutes}min"
                        } else if (hours > 0) {
                            "Toleranz: ${hours}h"
                        } else {
                            "Toleranz: ${minutes}min"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = "Minimaler Zeitabstand zwischen Tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = toleranceHours,
                        onValueChange = { toleranceHours = it },
                        valueRange = 0f..24f,
                        steps = 47, // 0.5 hour intervals
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0h", style = MaterialTheme.typography.labelSmall)
                        Text("24h", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Long-Press Delay setting (50-1000ms)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Long-Press Verzögerung: ${longPressDelayMs.toInt()}ms",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = "Zeit bis Drag-Selektion oder Task-Toggle startet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = longPressDelayMs,
                        onValueChange = { longPressDelayMs = it },
                        valueRange = 50f..1000f,
                        steps = 18, // 50ms steps
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("50ms (sehr schnell)", style = MaterialTheme.typography.labelSmall)
                        Text("1000ms (langsam)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Info text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "ℹ️ Hinweis",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Die Timeline lädt automatisch weitere Tage beim Scrollen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalMinutes = (toleranceHours * 60).toInt()
                    onToleranceChange(totalMinutes)
                    onVisibleHoursChange(visibleHours)
                    onLongPressDelayChange(longPressDelayMs.toLong())
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
