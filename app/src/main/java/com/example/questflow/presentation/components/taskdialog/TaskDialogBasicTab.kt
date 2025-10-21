package com.example.questflow.presentation.components.taskdialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.domain.model.Task

/**
 * TAB 1: Basis
 * - Titel (mit Template & Placeholder)
 * - Beschreibung (mit Template & Placeholder)
 * - Kategorie
 * - Schwierigkeit (5 Stufen)
 * - Parent Task (optional)
 * - Verbundene Tasks (Parent & Subtasks Navigation)
 */
@Composable
fun TaskDialogBasicTab(
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    taskDescription: String,
    onTaskDescriptionChange: (String) -> Unit,
    selectedPercentage: Int,
    onPercentageChange: (Int) -> Unit,
    taskCategory: CategoryEntity?,
    onCategoryClick: () -> Unit,
    selectedParentTask: Task?,
    onParentTaskClick: () -> Unit,
    autoCompleteParent: Boolean,
    onAutoCompleteParentChange: (Boolean) -> Unit,
    hasTextTemplates: Boolean,
    onTitleTemplateClick: () -> Unit,
    onTitlePlaceholderClick: () -> Unit,
    onDescriptionTemplateClick: () -> Unit,
    onDescriptionPlaceholderClick: () -> Unit,
    showParentSection: Boolean = true,
    // Task Family Navigation
    currentTask: Task?,
    availableTasks: List<Task>,
    taskFamilyExpanded: Boolean,
    onTaskFamilyExpandedChange: (Boolean) -> Unit,
    onNavigateToTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Task Title with Template and Placeholder Support
        item {
            Text("Titel:", style = MaterialTheme.typography.labelMedium)
        }
        item {
            OutlinedButton(
                onClick = onTitleTemplateClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasTextTemplates
            ) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasTextTemplates) "Textbaustein auswählen" else "Keine Templates verfügbar")
            }
        }
        item {
            OutlinedTextField(
                value = taskTitle,
                onValueChange = onTaskTitleChange,
                label = { Text("Titel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = onTitlePlaceholderClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Platzhalter einfügen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }

        // Task Description with Template and Placeholder Support
        item {
            Text("Beschreibung:", style = MaterialTheme.typography.labelMedium)
        }
        item {
            OutlinedButton(
                onClick = onDescriptionTemplateClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasTextTemplates
            ) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasTextTemplates) "Textbaustein auswählen" else "Keine Templates verfügbar")
            }
        }
        item {
            OutlinedTextField(
                value = taskDescription,
                onValueChange = onTaskDescriptionChange,
                label = { Text("Beschreibung (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = onDescriptionPlaceholderClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Platzhalter einfügen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }

        // Category Selection
        item {
            Text("Kategorie:", style = MaterialTheme.typography.labelMedium)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick() },
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        taskCategory?.let { cat ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(cat.color))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat.emoji,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Text(
                            text = taskCategory?.name ?: "Allgemein",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Kategorie wählen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Difficulty Selection with 5 levels
        item {
            Text("Schwierigkeit:", style = MaterialTheme.typography.labelMedium)
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: First 3 difficulties
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = selectedPercentage == 20,
                        onClick = { onPercentageChange(20) },
                        label = {
                            Text("Trivial", style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                    FilterChip(
                        selected = selectedPercentage == 40,
                        onClick = { onPercentageChange(40) },
                        label = {
                            Text("Einfach", style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                    FilterChip(
                        selected = selectedPercentage == 60,
                        onClick = { onPercentageChange(60) },
                        label = {
                            Text("Mittel", style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                }

                // Row 2: Last 2 difficulties
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = selectedPercentage == 80,
                        onClick = { onPercentageChange(80) },
                        label = {
                            Text("Schwer", style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                    FilterChip(
                        selected = selectedPercentage == 100,
                        onClick = { onPercentageChange(100) },
                        label = {
                            Text("Episch", style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f)) // Empty space for layout balance
                }
            }
        }

        // Parent Task Selection (optional)
        if (showParentSection) {
            item {
                Text("Übergeordneter Task (optional):", style = MaterialTheme.typography.labelMedium)
            }

            item {
                OutlinedTextField(
                    value = selectedParentTask?.title ?: "Kein (Haupt-Task)",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = onParentTaskClick) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onParentTaskClick() }
                )
            }

            // Auto-complete parent option (only if parent is selected)
            if (selectedParentTask != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Parent automatisch abschließen",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Wenn alle Subtasks fertig sind",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoCompleteParent,
                            onCheckedChange = onAutoCompleteParentChange
                        )
                    }
                }
            }
        }

        // Task Family Navigation - Parent AND Subtasks (from HistoryTab)
        if (currentTask != null) {
            // Determine the parent task and all siblings
            val parentTask = if (currentTask.parentTaskId != null) {
                // Current task IS a subtask - find its parent
                availableTasks.find { it.id == currentTask.parentTaskId }
            } else {
                // Current task IS the parent - use it
                currentTask
            }

            val allSubtasks = if (parentTask != null) {
                availableTasks.filter { it.parentTaskId == parentTask.id }
            } else {
                emptyList()
            }

            // Show this section only if we have a task family (parent + subtasks)
            if (parentTask != null && allSubtasks.isNotEmpty()) {
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Collapsible header for Task Family section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTaskFamilyExpandedChange(!taskFamilyExpanded)
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Verbundene Tasks",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text("${allSubtasks.size + 1}", fontSize = 10.sp)
                            }
                        }
                        Icon(
                            if (taskFamilyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (taskFamilyExpanded) "Zuklappen" else "Aufklappen",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Show parent and subtasks only when expanded
                if (taskFamilyExpanded) {
                    // ALWAYS show parent task card
                    item {
                        val isCurrentTask = parentTask.id == currentTask.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isCurrentTask) {
                                    onNavigateToTask(parentTask)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentTask)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.primaryContainer
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("📁 ", style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            parentTask.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isCurrentTask) {
                                            Text(
                                                "(aktuell)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    if (parentTask.description.isNotEmpty()) {
                                        Text(
                                            parentTask.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (!isCurrentTask) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Zum Parent Task",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // ALWAYS show all subtasks
                    item {
                        Text(
                            "Subtasks (${allSubtasks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(allSubtasks.size) { index ->
                        val subtask = allSubtasks[index]
                        val isCurrentTask = subtask.id == currentTask?.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable(enabled = !isCurrentTask) {
                                    onNavigateToTask(subtask)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentTask)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            subtask.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (isCurrentTask) {
                                            Text(
                                                "(aktuell)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    if (subtask.description.isNotEmpty()) {
                                        Text(
                                            subtask.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (!isCurrentTask) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Zu Subtask",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
