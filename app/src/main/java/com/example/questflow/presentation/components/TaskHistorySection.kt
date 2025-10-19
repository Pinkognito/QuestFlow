package com.example.questflow.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.TaskHistoryEntity
import java.time.format.DateTimeFormatter

/**
 * Expandable section showing task history events
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistorySection(
    taskHistory: List<TaskHistoryEntity>,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Historie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${taskHistory.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Zuklappen" else "Aufklappen"
                    )
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (taskHistory.isEmpty()) {
                        Text(
                            text = "Keine Historie verfügbar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        taskHistory.forEach { entry ->
                            TaskHistoryItem(entry)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual task history item
 */
@Composable
private fun TaskHistoryItem(
    entry: TaskHistoryEntity
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    val (eventIcon, eventText, eventColor) = when (entry.eventType) {
        "EXPIRED" -> Triple("⏰", "Abgelaufen", MaterialTheme.colorScheme.error)
        "CLAIMED" -> Triple("✅", "XP beansprucht", MaterialTheme.colorScheme.primary)
        "RECLAIMED" -> Triple("🔄", "XP erneut beansprucht", MaterialTheme.colorScheme.tertiary)
        "RECURRING_CREATED" -> Triple("🔁", "Wiederkehrend erstellt", MaterialTheme.colorScheme.secondary)
        "RESCHEDULED" -> Triple("📅", "Neu geplant", MaterialTheme.colorScheme.secondary)
        else -> Triple("ℹ️", entry.eventType, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = eventIcon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = eventText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = eventColor
                    )
                }

                Text(
                    text = entry.timestamp.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show new due date for recurring/rescheduled events
                entry.newDueDate?.let { newDate ->
                    Text(
                        text = "→ ${newDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
