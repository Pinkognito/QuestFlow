package com.example.questflow.presentation.screens.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    var showSelectionBoxDialog by remember { mutableStateOf(false) }
    var showBatchOperationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TimelineTopBar(
                focusedTask = uiState.focusedTask,
                selectionCount = uiState.selectedTaskIds.size,
                selectionBox = uiState.selectionBox,
                onBackClick = { navController.navigateUp() },
                onSettingsClick = { viewModel.toggleSettings() },
                onSelectionClick = { viewModel.toggleSelectionList() }
            )
        },
        floatingActionButton = {
            // FAB shows different content based on SelectionBox state
            if (uiState.selectionBox != null) {
                // SelectionBox active → Show expandable menu with actions
                SelectionBoxFAB(
                    selectionBox = uiState.selectionBox!!,
                    onSelectAllInRange = { viewModel.selectAllInRange() },
                    onInsertIntoRange = { showBatchOperationDialog = true },
                    onEdit = { showSelectionBoxDialog = true },
                    onDismiss = { viewModel.clearSelectionBox() }
                )
            } else {
                // No SelectionBox → Show create time range button
                FloatingActionButton(
                    onClick = { showSelectionBoxDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Zeitbereich festlegen"
                    )
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Calculate pixelsPerMinute based on available screen height (minus header!)
            val headerHeightDp = 48f // Must match TimelineGrid header height
            val availableHeightDp = maxHeight.value - headerHeightDp

            android.util.Log.d("TimelineScreen", "📐 BoxWithConstraints: maxHeight=${maxHeight.value}dp, header=${headerHeightDp}dp, available=${availableHeightDp}dp")

            LaunchedEffect(availableHeightDp, uiState.visibleHours) {
                android.util.Log.d("TimelineScreen", "🔄 LaunchedEffect triggered: updating screenHeight to ${availableHeightDp}dp")
                viewModel.updateScreenHeight(availableHeightDp)
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        TimelineGrid(
                            uiState = uiState,
                            onTaskClick = { task ->
                                viewModel.onTaskClick(task)
                                viewModel.setFocusedTask(task)
                            },
                            onTaskLongPress = { task ->
                                // Long-press on task toggles selection
                                viewModel.toggleTaskSelection(task.id)
                            },
                            onLoadMore = { direction ->
                                viewModel.loadMore(direction)
                            },
                            onDayWindowShift = { direction ->
                                viewModel.shiftDayWindow(direction)
                            },
                            viewModel = viewModel
                        )

                        // DEBUG OVERLAY - ALWAYS VISIBLE for debugging
                        uiState.gestureDebugInfo?.let { debugInfo ->
                            GestureDebugOverlay(debugInfo = debugInfo)
                        }

                        // RADIAL JOYSTICK MENU - Press button and swipe to select
                        uiState.contextMenu?.let { contextMenuState ->
                            RadialJoystickMenu(
                                state = contextMenuState,
                                onActionSelected = { actionId ->
                                    viewModel.executeContextMenuAction(actionId)
                                },
                                onDismiss = {
                                    viewModel.dismissContextMenu()
                                }
                            )
                        }
                    }
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

    // Selection list sheet
    if (uiState.showSelectionList && uiState.selectedTaskIds.isNotEmpty()) {
        SelectionListSheet(
            selectedTasks = uiState.getSelectedTasksOrdered(),
            onDismiss = { viewModel.toggleSelectionList() },
            onRemoveTask = { taskId -> viewModel.removeFromSelection(taskId) },
            onReorder = { newOrder -> viewModel.setCustomTaskOrder(newOrder) },
            onClearAll = {
                viewModel.clearSelection()
                viewModel.toggleSelectionList()
            }
        )
    }

    // SelectionBox creation dialog
    if (showSelectionBoxDialog) {
        SelectionBoxDialog(
            initialStart = uiState.selectionBox?.startTime,
            initialEnd = uiState.selectionBox?.endTime,
            onDismiss = { showSelectionBoxDialog = false },
            onConfirm = { start, end ->
                viewModel.setSelectionBox(start, end)
                showSelectionBoxDialog = false
            }
        )
    }

    // Batch operation dialog
    if (showBatchOperationDialog) {
        BatchOperationDialog(
            taskCount = uiState.selectedTaskIds.size,
            onDismiss = { showBatchOperationDialog = false },
            onConfirm = { sortOption ->
                viewModel.insertSelectedIntoRange(sortOption)
                showBatchOperationDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        val tasksInBox = uiState.getTasksInSelectionBox()
        DeleteConfirmationDialog(
            taskCount = tasksInBox.size,
            onConfirm = {
                viewModel.deleteTasksInSelectionBox()
                viewModel.showDeleteConfirmation(false)
            },
            onDismiss = {
                viewModel.showDeleteConfirmation(false)
            }
        )
    }

    // Time adjustment dialog
    if (uiState.showTimeAdjustmentDialog) {
        SelectionBoxDialog(
            initialStart = uiState.selectionBox?.startTime,
            initialEnd = uiState.selectionBox?.endTime,
            onDismiss = {
                viewModel.showTimeAdjustmentDialog(false)
            },
            onConfirm = { start, end ->
                viewModel.setSelectionBox(start, end)
                viewModel.showTimeAdjustmentDialog(false)
            }
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

/**
 * Expandable FAB for SelectionBox actions
 */
@Composable
private fun SelectionBoxFAB(
    selectionBox: com.example.questflow.presentation.screens.timeline.model.SelectionBox,
    onSelectAllInRange: () -> Unit,
    onInsertIntoRange: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Action buttons (visible when expanded)
        if (expanded) {
            // Dismiss button
            SmallFloatingActionButton(
                onClick = {
                    expanded = false
                    onDismiss()
                },
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Schließen",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // Edit time button
            SmallFloatingActionButton(
                onClick = {
                    expanded = false
                    onEdit()
                }
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Zeit bearbeiten")
            }

            // Insert selected tasks button
            SmallFloatingActionButton(
                onClick = {
                    expanded = false
                    onInsertIntoRange()
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Einfügen",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Select all in range button
            SmallFloatingActionButton(
                onClick = {
                    onSelectAllInRange()
                    // Keep expanded to allow multiple selections
                }
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Alles auswählen")
            }
        }

        // Main FAB (always visible)
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.MoreVert,
                contentDescription = if (expanded) "Einklappen" else "Aktionen"
            )
        }
    }
}

/**
 * Debug overlay showing gesture information
 */
@Composable
private fun GestureDebugOverlay(
    debugInfo: com.example.questflow.presentation.screens.timeline.model.GestureDebugInfo
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (debugInfo.gestureType) {
                    "WAITING" -> MaterialTheme.colorScheme.surfaceVariant
                    "VERTICAL" -> MaterialTheme.colorScheme.tertiaryContainer
                    "HORIZONTAL", "HORIZONTAL_DRAG" -> MaterialTheme.colorScheme.secondaryContainer
                    "LONG_PRESS" -> MaterialTheme.colorScheme.primaryContainer
                    "DRAGGING" -> MaterialTheme.colorScheme.primary
                    "DAY_SHIFT" -> MaterialTheme.colorScheme.secondary
                    "TASK_HIT" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = debugInfo.gestureType,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = when (debugInfo.gestureType) {
                        "DRAGGING" -> MaterialTheme.colorScheme.onPrimary
                        "TASK_HIT" -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = debugInfo.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (debugInfo.gestureType) {
                        "DRAGGING" -> MaterialTheme.colorScheme.onPrimary
                        "TASK_HIT" -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (debugInfo.elapsedMs > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${debugInfo.elapsedMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (debugInfo.gestureType) {
                            "DRAGGING" -> MaterialTheme.colorScheme.onPrimary
                            "TASK_HIT" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (debugInfo.dragX != 0f || debugInfo.dragY != 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "X: ${debugInfo.dragX.toInt()}px  Y: ${debugInfo.dragY.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (debugInfo.gestureType) {
                            "DRAGGING" -> MaterialTheme.colorScheme.onPrimary
                            "TASK_HIT" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Show gesture history
                if (debugInfo.history.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = when (debugInfo.gestureType) {
                            "DRAGGING" -> MaterialTheme.colorScheme.onPrimary
                            "TASK_HIT" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    debugInfo.history.forEach { historyEntry ->
                        Text(
                            text = historyEntry,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (debugInfo.gestureType) {
                                "DRAGGING" -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                "TASK_HIT" -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                }
            }
        }
    }
}
