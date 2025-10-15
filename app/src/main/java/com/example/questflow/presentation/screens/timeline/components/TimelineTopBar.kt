package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.presentation.screens.timeline.model.SelectionBox
import java.time.format.DateTimeFormatter

/**
 * Top bar for timeline screen.
 * UPDATED: Shows SelectionBox time range next to title when active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTopBar(
    focusedTask: TimelineTask?,
    selectionCount: Int = 0,
    selectionBox: SelectionBox? = null,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSelectionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Show focused task time if available (when NOT in selection mode)
                    if (focusedTask != null && selectionBox == null) {
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val timeText = "${focusedTask.startTime.format(timeFormatter)} - ${focusedTask.endTime.format(timeFormatter)}"

                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Show SelectionBox time range (when active)
                if (selectionBox != null) {
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")

                    // Format time range
                    val startTimeText = selectionBox.startTime.format(timeFormatter)
                    val endTimeText = selectionBox.endTime.format(timeFormatter)
                    val startDateText = selectionBox.startTime.format(dateFormatter)
                    val endDateText = selectionBox.endTime.format(dateFormatter)

                    // Show date if different days
                    val timeRangeText = if (startDateText == endDateText) {
                        "$startTimeText-$endTimeText"
                    } else {
                        "$startDateText $startTimeText - $endDateText $endTimeText"
                    }

                    val durationText = "${selectionBox.durationMinutes()}min"

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⏱️ $timeRangeText",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück"
                )
            }
        },
        actions = {
            // Selection button
            SelectionButton(
                selectionCount = selectionCount,
                onClick = onSelectionClick
            )

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
