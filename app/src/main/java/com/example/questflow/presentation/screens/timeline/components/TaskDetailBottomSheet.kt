package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.TimelineTask
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Bottom sheet showing task details in timeline view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(
    task: TimelineTask,
    onDismiss: () -> Unit,
    onEdit: ((TimelineTask) -> Unit)? = null,
    onDelete: ((TimelineTask) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Schließen")
                }
            }

            Divider()

            // Task info
            TaskInfoSection(task)

            // Conflict state indicator
            ConflictStateIndicator(task)

            // Description (if not empty)
            if (task.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Beschreibung",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Divider()

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onEdit != null) {
                    Button(
                        onClick = { onEdit(task) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bearbeiten")
                    }
                }

                if (onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(task) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Löschen")
                    }
                }
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Task information section
 */
@Composable
private fun TaskInfoSection(task: TimelineTask) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Date and time
        InfoRow(
            label = "📅 Datum",
            value = task.startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        )

        InfoRow(
            label = "🕐 Zeit",
            value = "${task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${task.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        )

        InfoRow(
            label = "⏱️ Dauer",
            value = formatDuration(task.durationMinutes())
        )

        // Category (if available)
        if (task.categoryEmoji != null) {
            InfoRow(
                label = "📁 Kategorie",
                value = "${task.categoryEmoji} Kategorie"
            )
        }

        // Difficulty
        val difficultyLabel = when (task.xpPercentage) {
            in 0..25 -> "Trivial (20%)"
            in 26..45 -> "Einfach (40%)"
            in 46..70 -> "Mittel (60%)"
            in 71..90 -> "Schwer (80%)"
            else -> "Episch (100%)"
        }

        InfoRow(
            label = "⚡ Schwierigkeit",
            value = difficultyLabel
        )

        // Completion status
        if (task.isCompleted) {
            InfoRow(
                label = "✅ Status",
                value = "Abgeschlossen"
            )
        }
    }
}

/**
 * Conflict state indicator card
 */
@Composable
private fun ConflictStateIndicator(task: TimelineTask) {
    val (icon, label, color) = when (task.conflictState) {
        com.example.questflow.domain.model.ConflictState.OVERLAP -> Triple(
            "⚠️",
            "Überschneidung mit anderen Tasks",
            MaterialTheme.colorScheme.error
        )
        com.example.questflow.domain.model.ConflictState.TOLERANCE_WARNING -> Triple(
            "ℹ️",
            "Zeitpuffer zu anderen Tasks unterschritten",
            MaterialTheme.colorScheme.primary
        )
        com.example.questflow.domain.model.ConflictState.NO_CONFLICT -> Triple(
            "✅",
            "Keine Konflikte",
            MaterialTheme.colorScheme.tertiary
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

/**
 * Information row with label and value
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Format duration in a human-readable way
 */
private fun formatDuration(minutes: Long): String {
    return when {
        minutes < 60 -> "$minutes Minuten"
        minutes % 60 == 0L -> "${minutes / 60} Stunden"
        else -> "${minutes / 60} Std ${minutes % 60} Min"
    }
}
