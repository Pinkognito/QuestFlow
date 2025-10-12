package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.TimelineTask
import java.time.format.DateTimeFormatter

/**
 * Top bar for timeline screen.
 * UPDATED: Removed time range selector (infinite scroll now).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTopBar(
    focusedTask: TimelineTask?,
    selectionCount: Int = 0,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSelectionClick: () -> Unit = {},
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
