package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.*

/**
 * Advanced dialog for configuring task card layout
 * Allows users to enable/disable elements, set column, and adjust priority
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLayoutConfigDialog(
    layoutConfig: List<TaskDisplayElementConfig>,
    onDismiss: () -> Unit,
    onConfigChange: (List<TaskDisplayElementConfig>) -> Unit,
    onResetToDefaults: () -> Unit
) {
    // Local state - changes only saved on confirm
    var currentConfig by remember { mutableStateOf(layoutConfig) }
    var hasChanges by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (hasChanges) {
                // Save changes before closing
                onConfigChange(currentConfig)
            }
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Layout Konfiguration")
                IconButton(onClick = {
                    onResetToDefaults()
                    currentConfig = getDefaultDisplayConfig()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Zurücksetzen")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Konfiguriere welche Elemente angezeigt werden, in welcher Spalte sie erscheinen und ihre Reihenfolge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Group by column for better overview
                Text(
                    "Linke Spalte (Hauptinhalt - 2/3)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                val leftElements = currentConfig
                    .filter { it.column == DisplayColumn.LEFT }
                    .sortedBy { it.priority }

                leftElements.forEach { element ->
                    LayoutElementConfigItem(
                        config = element,
                        onEnabledChange = { enabled ->
                            currentConfig = TaskDisplayLayoutHelper.updateElement(
                                currentConfig, element.type, enabled = enabled
                            )
                            hasChanges = true
                        },
                        onColumnChange = { column ->
                            currentConfig = TaskDisplayLayoutHelper.updateElement(
                                currentConfig, element.type, column = column
                            )
                            hasChanges = true
                        },
                        onPriorityChange = { priority ->
                            currentConfig = TaskDisplayLayoutHelper.updateElement(
                                currentConfig, element.type, priority = priority
                            )
                            hasChanges = true
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Rechte Spalte (Metadaten - 1/3)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                val rightElements = currentConfig
                    .filter { it.column == DisplayColumn.RIGHT }
                    .sortedBy { it.priority }

                rightElements.forEach { element ->
                    LayoutElementConfigItem(
                        config = element,
                        onEnabledChange = { enabled ->
                            currentConfig = TaskDisplayLayoutHelper.updateElement(
                                currentConfig, element.type, enabled = enabled
                            )
                            hasChanges = true
                        },
                        onColumnChange = { column ->
                            currentConfig = TaskDisplayLayoutHelper.updateElement(
                                currentConfig, element.type, column = column
                            )
                            hasChanges = true
                        },
                        onPriorityChange = { priority ->
                            currentConfig = TaskDisplayLayoutHelper.updateElement(
                                currentConfig, element.type, priority = priority
                            )
                            hasChanges = true
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (hasChanges) {
                    onConfigChange(currentConfig)
                }
                onDismiss()
            }) {
                Text("Fertig")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun LayoutElementConfigItem(
    config: TaskDisplayElementConfig,
    onEnabledChange: (Boolean) -> Unit,
    onColumnChange: (DisplayColumn) -> Unit,
    onPriorityChange: (Int) -> Unit
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = if (config.enabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.type.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (config.enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "Priorität: ${config.priority}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = config.enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }

    // Detail dialog for column and priority
    if (showDetailDialog) {
        var priorityText by remember { mutableStateOf(config.priority.toString()) }
        var showPriorityDialog by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text(config.type.getDisplayName()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Column selector
                    Text(
                        "Spalte",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = config.column == DisplayColumn.LEFT,
                            onClick = { onColumnChange(DisplayColumn.LEFT) },
                            label = { Text("Links (2/3)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = config.column == DisplayColumn.RIGHT,
                            onClick = { onColumnChange(DisplayColumn.RIGHT) },
                            label = { Text("Rechts (1/3)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider()

                    // Priority selector
                    Text(
                        "Priorität (Reihenfolge)",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedButton(
                        onClick = {
                            priorityText = config.priority.toString()
                            showPriorityDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Position: ${config.priority}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Text(
                        "Niedrigere Zahl = weiter oben. Alle anderen werden automatisch angepasst.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = false }) {
                    Text("Fertig")
                }
            }
        )

        // Priority input dialog
        if (showPriorityDialog) {
            AlertDialog(
                onDismissRequest = { showPriorityDialog = false },
                title = { Text("Position eingeben") },
                text = {
                    OutlinedTextField(
                        value = priorityText,
                        onValueChange = { priorityText = it.filter { c -> c.isDigit() } },
                        label = { Text("Position (1, 2, 3, ...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newPriority = priorityText.toIntOrNull()
                            if (newPriority != null && newPriority > 0) {
                                onPriorityChange(newPriority)
                            }
                            showPriorityDialog = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPriorityDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    }
}
