package com.example.questflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.calendar.CalendarEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Dialog showing detailed information about scheduling conflicts
 * Displays all conflicting events/tasks/TimeBlocks with their overlap times
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictDetailsDialog(
    conflicts: List<CalendarEvent>,
    taskStart: LocalDateTime,
    taskEnd: LocalDateTime,
    onDismiss: () -> Unit,
    onNavigateToTask: ((Long) -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
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
                        text = "Scheduling-Konflikte",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Task time info
                Text(
                    text = "Geplante Task-Zeit:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatDateTime(taskStart)} - ${formatTime(taskEnd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Conflicts list
                Text(
                    text = "${conflicts.size} ${if (conflicts.size == 1) "Konflikt gefunden" else "Konflikte gefunden"}:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(conflicts) { conflict ->
                        ConflictItem(
                            conflict = conflict,
                            taskStart = taskStart,
                            taskEnd = taskEnd,
                            onNavigateToTask = onNavigateToTask
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Schließen")
                }
            }
        }
    }
}

@Composable
private fun ConflictItem(
    conflict: CalendarEvent,
    taskStart: LocalDateTime,
    taskEnd: LocalDateTime,
    onNavigateToTask: ((Long) -> Unit)?
) {
    val conflictType = when {
        conflict.id < 0 -> ConflictType.TIME_BLOCK
        conflict.isExternal -> ConflictType.EXTERNAL_EVENT
        else -> ConflictType.TASK
    }

    // Calculate overlap
    val overlapStart = maxOf(taskStart, conflict.startTime)
    val overlapEnd = minOf(taskEnd, conflict.endTime)
    val overlapMinutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd)

    val backgroundColor = when (conflictType) {
        ConflictType.TIME_BLOCK -> Color(0xFFFF9800).copy(alpha = 0.1f) // Orange
        ConflictType.EXTERNAL_EVENT -> Color(0xFF2196F3).copy(alpha = 0.1f) // Blue
        ConflictType.TASK -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }

    val borderColor = when (conflictType) {
        ConflictType.TIME_BLOCK -> Color(0xFFFF9800)
        ConflictType.EXTERNAL_EVENT -> Color(0xFF2196F3)
        ConflictType.TASK -> MaterialTheme.colorScheme.primary
    }

    val icon = when (conflictType) {
        ConflictType.TIME_BLOCK -> Icons.Default.Lock
        ConflictType.EXTERNAL_EVENT -> Icons.Default.DateRange
        ConflictType.TASK -> Icons.Default.CheckCircle
    }

    val typeLabel = when (conflictType) {
        ConflictType.TIME_BLOCK -> "Zeitblockierung"
        ConflictType.EXTERNAL_EVENT -> conflict.calendarName.ifEmpty { "Externer Termin" }
        ConflictType.TASK -> "QuestFlow Task"
    }

    val isClickable = conflictType == ConflictType.TASK && onNavigateToTask != null && conflict.id > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) {
                    Modifier.clickable { onNavigateToTask?.invoke(conflict.id) }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Type + Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = borderColor,
                    fontWeight = FontWeight.Bold
                )
                if (isClickable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Zur Task navigieren",
                        tint = borderColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = conflict.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            // Description (if present)
            if (conflict.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conflict.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatDateTime(conflict.startTime)} - ${formatTime(conflict.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Overlap info with warning icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Überschneidung:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${formatDateTime(overlapStart)} - ${formatTime(overlapEnd)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "($overlapMinutes Min.)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

private enum class ConflictType {
    TIME_BLOCK,
    EXTERNAL_EVENT,
    TASK
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    return dateTime.format(formatter)
}

private fun formatTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return dateTime.format(formatter)
}
