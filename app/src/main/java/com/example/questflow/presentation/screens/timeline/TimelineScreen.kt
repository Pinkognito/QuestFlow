package com.example.questflow.presentation.screens.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.questflow.presentation.screens.timeline.components.*

/**
 * Timeline Screen - Interactive multi-day timeline view with vertical time axis.
 *
 * UPDATED Features:
 * - Vertical time axis (00:00-23:59)
 * - Horizontal day scrolling with infinite loading
 * - Synchronized vertical scrolling across all columns
 * - Drag & drop task rescheduling (vertical drag)
 * - Color-coded conflict detection
 * - 0-24h tolerance range
 */
@Composable
fun TimelineScreen(
    navController: NavHostController,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TimelineTopBar(
                focusedTask = uiState.focusedTask,
                onBackClick = { navController.navigateUp() },
                onSettingsClick = { viewModel.toggleSettings() }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Calculate pixelsPerMinute based on available screen height
            val screenHeightDp = maxHeight.value
            LaunchedEffect(screenHeightDp, uiState.visibleHours) {
                viewModel.updateScreenHeight(screenHeightDp)
            }

            when {
                // Loading state
                uiState.isLoading -> {
                    LoadingState()
                }

                // Error state
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.refresh() },
                        onDismiss = { viewModel.clearError() }
                    )
                }

                // Empty state
                uiState.days.isEmpty() -> {
                    EmptyState()
                }

                // Content
                else -> {
                    TimelineGrid(
                        uiState = uiState,
                        onTaskClick = { task ->
                            viewModel.onTaskClick(task)
                            viewModel.setFocusedTask(task)
                        },
                        onTaskLongPress = { task ->
                            viewModel.onTaskDragStart(task)
                        },
                        onLoadMore = { direction ->
                            viewModel.loadMore(direction)
                        },
                        onDayWindowShift = { direction ->
                            viewModel.shiftDayWindow(direction)
                        }
                    )
                }
            }
        }
    }

    // Task detail bottom sheet
    if (uiState.selectedTask != null) {
        TaskDetailBottomSheet(
            task = uiState.selectedTask!!,
            onDismiss = { viewModel.onTaskClick(null) }
        )
    }

    // Settings dialog
    if (uiState.showSettings) {
        TimelineSettingsDialog(
            currentTolerance = uiState.toleranceMinutes,
            currentVisibleHours = uiState.visibleHours,
            onDismiss = { viewModel.toggleSettings() },
            onToleranceChange = { viewModel.updateTolerance(it) },
            onVisibleHoursChange = { viewModel.updateVisibleHours(it) }
        )
    }
}

/**
 * Loading indicator
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Timeline wird geladen...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state with retry option
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displayMedium
                )

                Text(
                    text = "Fehler beim Laden",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Schließen")
                    }

                    Button(onClick = onRetry) {
                        Text("Erneut versuchen")
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no tasks exist
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "📅",
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = "Keine Tasks gefunden",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Erstelle Tasks im Tasks-Tab, um sie hier in der Timeline zu sehen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}
