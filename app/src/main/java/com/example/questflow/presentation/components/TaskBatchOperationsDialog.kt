package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.CategoryEntity

/**
 * Batch Operations Dialog for Multi-Selected Tasks
 * Allows bulk operations on multiple selected tasks
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBatchOperationsDialog(
    selectedTasks: List<CalendarEventLinkEntity>,
    availableTasks: List<com.example.questflow.domain.model.Task>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onBatchDelete: (List<Long>) -> Unit,
    onBatchSetCategory: (List<Long>, Long?) -> Unit,
    onBatchSetDifficulty: (List<Long>, Int) -> Unit,
    onBatchSetParent: (List<Long>, Long?) -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDifficultyPicker by remember { mutableStateOf(false) }
    var showParentPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${selectedTasks.size} Tasks ausgewählt")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Wähle eine Aktion für alle ausgewählten Tasks:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Delete All
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showConfirmDelete = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    "Alle löschen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${selectedTasks.size} Tasks werden gelöscht",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Set Category
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showCategoryPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null
                            )
                            Column {
                                Text(
                                    "Kategorie ändern",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Allen Tasks eine Kategorie zuweisen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Set Difficulty
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showDifficultyPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null
                            )
                            Column {
                                Text(
                                    "Schwierigkeit ändern",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Allen Tasks einen Schwierigkeitsgrad zuweisen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Set Parent Task
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showParentPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null
                            )
                            Column {
                                Text(
                                    "Parent Task zuweisen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Alle zu Subtasks eines Parents machen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )

    // Confirm Delete Dialog
    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Löschen bestätigen") },
            text = {
                Text("Möchtest du wirklich ${selectedTasks.size} Tasks löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBatchDelete(selectedTasks.mapNotNull { it.taskId })
                        showConfirmDelete = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Category Picker Dialog
    if (showCategoryPicker) {
        FullscreenSelectionDialog(
            title = "Kategorie wählen",
            items = categories,
            selectedItem = null,
            onItemSelected = { category ->
                onBatchSetCategory(selectedTasks.mapNotNull { it.taskId }, category?.id)
                showCategoryPicker = false
                onDismiss()
            },
            onDismiss = { showCategoryPicker = false },
            itemLabel = { it.name },
            itemDescription = { "${it.emoji} ${it.name}" },
            allowNone = true
        )
    }

    // Difficulty Picker Dialog
    if (showDifficultyPicker) {
        val difficulties = listOf(
            20 to "Trivial",
            40 to "Einfach",
            60 to "Mittel",
            80 to "Schwer",
            100 to "Episch"
        )

        AlertDialog(
            onDismissRequest = { showDifficultyPicker = false },
            title = { Text("Schwierigkeit wählen") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    difficulties.forEach { (percentage, name) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onBatchSetDifficulty(selectedTasks.mapNotNull { it.taskId }, percentage)
                                showDifficultyPicker = false
                                onDismiss()
                            }
                        ) {
                            Text(
                                name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDifficultyPicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Parent Task Picker Dialog
    if (showParentPicker) {
        FullscreenSelectionDialog(
            title = "Parent Task wählen",
            items = availableTasks,
            selectedItem = null,
            onItemSelected = { parentTask ->
                onBatchSetParent(selectedTasks.mapNotNull { it.taskId }, parentTask?.id)
                showParentPicker = false
                onDismiss()
            },
            onDismiss = { showParentPicker = false },
            itemLabel = { it.title },
            itemDescription = { it.title },
            allowNone = true,
            noneLabel = "Kein (Haupt-Task)",
            searchPlaceholder = "Task suchen..."
        )
    }
}
