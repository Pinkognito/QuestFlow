package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
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
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.CategoryEntity
import java.time.LocalDateTime

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

/**
 * Batch Edit Task Dialog - Comprehensive editing for multiple tasks
 * Works like normal edit dialog but applies changes to all selected tasks
 * Only modifies fields that are explicitly enabled via checkbox
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditTaskDialog(
    selectedLinks: List<CalendarEventLinkEntity>,
    availableTasks: List<com.example.questflow.domain.model.Task>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onApplyChanges: (BatchEditChanges) -> Unit
) {
    // Enable/disable flags for each field
    var enableDifficulty by remember { mutableStateOf(false) }
    var enableCategory by remember { mutableStateOf(false) }
    var enableParent by remember { mutableStateOf(false) }
    var enableDeleteOnClaim by remember { mutableStateOf(false) }
    var enableDeleteOnExpiry by remember { mutableStateOf(false) }

    // Field values
    var selectedPercentage by remember { mutableStateOf(60) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedParent by remember { mutableStateOf<com.example.questflow.domain.model.Task?>(null) }
    var deleteOnClaim by remember { mutableStateOf(false) }
    var deleteOnExpiry by remember { mutableStateOf(false) }

    // Fullscreen dialogs state
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showParentDialog by remember { mutableStateOf(false) }

    // Reset function
    fun resetAll() {
        enableDifficulty = false
        enableCategory = false
        enableParent = false
        enableDeleteOnClaim = false
        enableDeleteOnExpiry = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${selectedLinks.size} Tasks bearbeiten")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Wähle die Felder aus, die du für alle ausgewählten Tasks ändern möchtest:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Difficulty Field
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (enableDifficulty)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = enableDifficulty,
                                        onCheckedChange = { enableDifficulty = it }
                                    )
                                    Text(
                                        "Schwierigkeit ändern",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                if (enableDifficulty) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            FilterChip(
                                                selected = selectedPercentage == 20,
                                                onClick = { selectedPercentage = 20 },
                                                label = { Text("Trivial", style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                            )
                                            FilterChip(
                                                selected = selectedPercentage == 40,
                                                onClick = { selectedPercentage = 40 },
                                                label = { Text("Einfach", style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                            )
                                            FilterChip(
                                                selected = selectedPercentage == 60,
                                                onClick = { selectedPercentage = 60 },
                                                label = { Text("Mittel", style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            FilterChip(
                                                selected = selectedPercentage == 80,
                                                onClick = { selectedPercentage = 80 },
                                                label = { Text("Schwer", style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                            )
                                            FilterChip(
                                                selected = selectedPercentage == 100,
                                                onClick = { selectedPercentage = 100 },
                                                label = { Text("Episch", style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Category Field
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (enableCategory)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = enableCategory,
                                        onCheckedChange = { enableCategory = it }
                                    )
                                    Text(
                                        "Kategorie ändern",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                if (enableCategory) {
                                    OutlinedTextField(
                                        value = selectedCategory?.name ?: "Keine",
                                        onValueChange = { },
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { showCategoryDialog = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp)
                                            .clickable { showCategoryDialog = true },
                                        leadingIcon = {
                                            selectedCategory?.let { cat ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
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
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Parent Task Field
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (enableParent)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = enableParent,
                                        onCheckedChange = { enableParent = it }
                                    )
                                    Text(
                                        "Parent Task ändern",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                if (enableParent) {
                                    OutlinedTextField(
                                        value = selectedParent?.title ?: "Kein (Haupt-Task)",
                                        onValueChange = { },
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { showParentDialog = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp)
                                            .clickable { showParentDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // Delete Options
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Kalender-Optionen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                // Delete on claim
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = enableDeleteOnClaim,
                                        onCheckedChange = { enableDeleteOnClaim = it }
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                        Text(
                                            "Nach XP-Claim löschen",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (enableDeleteOnClaim) {
                                            Switch(
                                                checked = deleteOnClaim,
                                                onCheckedChange = { deleteOnClaim = it },
                                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Delete on expiry
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = enableDeleteOnExpiry,
                                        onCheckedChange = { enableDeleteOnExpiry = it }
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                        Text(
                                            "Nach Ablauf löschen",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (enableDeleteOnExpiry) {
                                            Switch(
                                                checked = deleteOnExpiry,
                                                onCheckedChange = { deleteOnExpiry = it },
                                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Build changes object with only enabled fields
                    val changes = BatchEditChanges(
                        xpPercentage = if (enableDifficulty) selectedPercentage else null,
                        categoryId = if (enableCategory) selectedCategory?.id else null,
                        parentTaskId = if (enableParent) selectedParent?.id else null,
                        deleteOnClaim = if (enableDeleteOnClaim) deleteOnClaim else null,
                        deleteOnExpiry = if (enableDeleteOnExpiry) deleteOnExpiry else null
                    )
                    onApplyChanges(changes)
                    onDismiss()
                }
            ) {
                Text("Änderungen anwenden")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { resetAll() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Zurücksetzen")
            }
        }
    )

    // Category Selection Dialog
    if (showCategoryDialog) {
        FullscreenSelectionDialog(
            title = "Kategorie wählen",
            items = categories,
            selectedItem = selectedCategory,
            onItemSelected = { category ->
                selectedCategory = category
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false },
            itemLabel = { it.name },
            itemDescription = { "${it.emoji} ${it.name}" },
            allowNone = true,
            noneLabel = "Keine"
        )
    }

    // Parent Task Selection Dialog
    if (showParentDialog) {
        FullscreenSelectionDialog(
            title = "Parent Task wählen",
            items = availableTasks,
            selectedItem = selectedParent,
            onItemSelected = { parent ->
                selectedParent = parent
                showParentDialog = false
            },
            onDismiss = { showParentDialog = false },
            itemLabel = { it.title },
            itemDescription = { it.title },
            allowNone = true,
            noneLabel = "Kein (Haupt-Task)",
            searchPlaceholder = "Task suchen..."
        )
    }
}

/**
 * Data class to hold batch edit changes
 * Only non-null fields will be applied to tasks
 */
data class BatchEditChanges(
    val xpPercentage: Int? = null,
    val categoryId: Long? = null,
    val parentTaskId: Long? = null,
    val deleteOnClaim: Boolean? = null,
    val deleteOnExpiry: Boolean? = null
)
