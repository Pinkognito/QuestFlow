package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.TimelineTask
import java.time.format.DateTimeFormatter

/**
 * Top bar for timeline screen.
 * Shows title, focused task time (if any), and action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTopBar(
    focusedTask: TimelineTask?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTimeRangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Show focused task time if available
                if (focusedTask != null) {
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val timeText = "${focusedTask.startTime.format(timeFormatter)} - ${focusedTask.endTime.format(timeFormatter)}"

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück"
                )
            }
        },
        actions = {
            // Time range selector
            IconButton(onClick = onTimeRangeClick) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Zeitbereich wählen"
                )
            }

            // Settings
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Einstellungen"
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Quick time range selector dialog
 */
@Composable
fun TimeRangeSelectorDialog(
    currentRange: com.example.questflow.data.preferences.TimeRange,
    onDismiss: () -> Unit,
    onRangeSelected: (com.example.questflow.data.preferences.TimeRange) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zeitbereich wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.questflow.data.preferences.TimeRange.values().forEach { range ->
                    TimeRangeButton(
                        range = range,
                        isSelected = range == currentRange,
                        onClick = {
                            onRangeSelected(range)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Time range selection button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeButton(
    range: com.example.questflow.data.preferences.TimeRange,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(range.displayName) },
        modifier = Modifier.fillMaxWidth()
    )
}
