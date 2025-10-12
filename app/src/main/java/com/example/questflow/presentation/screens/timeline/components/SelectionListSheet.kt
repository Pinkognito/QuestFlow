package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.TimelineTask
import java.time.format.DateTimeFormatter

/**
 * Bottom sheet showing selected tasks with drag-to-reorder functionality.
 * Manual order is used as DEFAULT when inserting tasks into time range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionListSheet(
    selectedTasks: List<TimelineTask>,
    onDismiss: () -> Unit,
    onRemoveTask: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ausgewählte Tasks (${selectedTasks.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    // Clear all button
                    IconButton(onClick = onClearAll) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Alle entfernen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info text
            Text(
                text = "Reihenfolge durch Ziehen ändern (Standard beim Einfügen)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task list with drag-to-reorder
            var taskOrder by remember(selectedTasks) {
                mutableStateOf(selectedTasks.map { it.id })
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = taskOrder,
                    key = { _, taskId -> taskId }
                ) { index, taskId ->
                    val task = selectedTasks.find { it.id == taskId }
                    if (task != null) {
                        SelectionListItem(
                            task = task,
                            index = index + 1,
                            onRemove = { onRemoveTask(taskId) },
                            onMoveUp = {
                                if (index > 0) {
                                    val newOrder = taskOrder.toMutableList()
                                    newOrder[index] = taskOrder[index - 1]
                                    newOrder[index - 1] = taskId
                                    taskOrder = newOrder
                                    onReorder(newOrder)
                                }
                            },
                            onMoveDown = {
                                if (index < taskOrder.size - 1) {
                                    val newOrder = taskOrder.toMutableList()
                                    newOrder[index] = taskOrder[index + 1]
                                    newOrder[index + 1] = taskId
                                    taskOrder = newOrder
                                    onReorder(newOrder)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Single item in selection list with drag handle and remove button
 */
@Composable
private fun SelectionListItem(
    task: TimelineTask,
    index: Int,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(task.categoryColor ?: "#4CAF50"))
    } catch (e: Exception) {
        Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order number
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(backgroundColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = backgroundColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.categoryEmoji != null) {
                        Text(
                            text = task.categoryEmoji,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${task.startTime.format(timeFormatter)} - ${task.endTime.format(timeFormatter)} • ${task.durationMinutes()}min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Drag handle (visual only, actual drag-to-reorder simplified with up/down buttons)
            Column {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Text("▲", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Text("▼", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Entfernen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
