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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.TaskHistoryEntity
import com.example.questflow.domain.model.HistoryEventType
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.presentation.viewmodels.HistoryConfigViewModel
import java.time.format.DateTimeFormatter

/**
 * Expandable section showing task history events
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistorySection(
    taskHistory: List<TaskHistoryEntity>,
    modifier: Modifier = Modifier,
    viewModel: HistoryConfigViewModel = hiltViewModel()
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

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

                Row {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "History-Einstellungen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Zuklappen" else "Aufklappen"
                        )
                    }
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

    // History Configuration Dialog
    if (showConfigDialog) {
        val config by viewModel.config.collectAsState()
        
        HistoryConfigDialog(
            currentConfig = config,
            onDismiss = { showConfigDialog = false },
            onConfigChange = { newConfig ->
                viewModel.updateConfig(newConfig)
            }
        )
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

    // Get event type metadata from enum
    val eventType = HistoryEventType.fromString(entry.eventType)
    val (eventIcon, eventText, eventColor) = if (eventType != null) {
        val color = when (eventType.priority) {
            com.example.questflow.domain.model.HistoryPriority.CRITICAL -> MaterialTheme.colorScheme.error
            com.example.questflow.domain.model.HistoryPriority.HIGH -> MaterialTheme.colorScheme.tertiary
            com.example.questflow.domain.model.HistoryPriority.MEDIUM -> MaterialTheme.colorScheme.secondary
            com.example.questflow.domain.model.HistoryPriority.LOW -> MaterialTheme.colorScheme.primary
        }
        Triple(eventType.icon, eventType.displayName, color)
    } else {
        // Fallback for unknown event types
        Triple("ℹ️", entry.eventType, MaterialTheme.colorScheme.onSurfaceVariant)
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

                // Show old→new value for property changes
                if (entry.oldValue != null && entry.newValue != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Format values based on event type
                        val (formattedOld, formattedNew) = formatHistoryValues(
                            eventType = entry.eventType,
                            oldValue = entry.oldValue,
                            newValue = entry.newValue
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alt:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedOld,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Neu:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedNew,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

}

/**
 * Format history values based on event type for better readability
 */
private fun formatHistoryValues(
    eventType: String,
    oldValue: String,
    newValue: String
): Pair<String, String> {
    return when (eventType) {
        "DUE_DATE_CHANGED" -> {
            // Parse ISO datetime and format as "dd.MM.yy HH:mm"
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
            val oldFormatted = try {
                java.time.LocalDateTime.parse(oldValue).format(formatter)
            } catch (e: Exception) {
                oldValue
            }
            val newFormatted = try {
                java.time.LocalDateTime.parse(newValue).format(formatter)
            } catch (e: Exception) {
                newValue
            }
            Pair(oldFormatted, newFormatted)
        }
        "DIFFICULTY_CHANGED" -> {
            // Add % sign to difficulty values
            Pair("$oldValue%", "$newValue%")
        }
        "CATEGORY_CHANGED" -> {
            // Category IDs - could be improved with category names lookup
            Pair("Kategorie #$oldValue", "Kategorie #$newValue")
        }
        else -> {
            // Default: return as-is
            Pair(oldValue, newValue)
        }
    }
}
