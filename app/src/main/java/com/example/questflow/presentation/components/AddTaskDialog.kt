package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import com.example.questflow.presentation.viewmodels.TodayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    viewModel: TodayViewModel,
    onDismiss: () -> Unit,
    isCalendarMode: Boolean = false,  // Add parameter to distinguish context
    preSelectedParentId: Long? = null,  // Pre-select parent task (for sub-task creation)
    inheritFromTask: com.example.questflow.domain.model.Task? = null,  // Inherit category from parent
    inheritFromCalendarLink: com.example.questflow.data.database.entity.CalendarEventLinkEntity? = null  // Inherit time from parent
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedPercentage by remember { mutableStateOf(60) } // Default to 60%
    var addToCalendar by remember { mutableStateOf(true) } // Default to true
    var deleteOnClaim by remember { mutableStateOf(isCalendarMode) } // Default true for calendar mode
    var deleteOnExpiry by remember { mutableStateOf(false) } // Delete on expiry
    var isRecurring by remember { mutableStateOf(false) }
    var recurringConfig by remember { mutableStateOf(RecurringConfig()) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val availableTasks by viewModel.uiState.collectAsState()

    // Inherit category from parent task if provided
    val inheritedCategory = remember(inheritFromTask, categories) {
        inheritFromTask?.categoryId?.let { categoryId ->
            categories.find { it.id == categoryId }
        } ?: selectedCategory
    }
    var taskCategory by remember(inheritedCategory) { mutableStateOf(inheritedCategory) }

    // Pre-select parent task if provided - use inheritFromTask directly if available
    var selectedParentTask by remember(preSelectedParentId, inheritFromTask) {
        // First try to use inheritFromTask directly (most reliable)
        val found = inheritFromTask ?: preSelectedParentId?.let { parentId ->
            availableTasks.tasks.find { it.id == parentId }
        }
        mutableStateOf(found)
    }

    // Update selectedParentTask when tasks are loaded (race condition fix)
    LaunchedEffect(availableTasks.tasks, preSelectedParentId, inheritFromTask) {
        if (preSelectedParentId != null && selectedParentTask == null && availableTasks.tasks.isNotEmpty()) {
            val found = inheritFromTask ?: availableTasks.tasks.find { it.id == preSelectedParentId }
            selectedParentTask = found
        }
    }

    var autoCompleteParent by remember { mutableStateOf(false) }

    // Contact selection state
    val availableContacts by viewModel.contacts.collectAsState()
    var selectedContactIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showContactDialog by remember { mutableStateOf(false) }

    // Fullscreen selection dialogs state
    var showCategorySelectionDialog by remember { mutableStateOf(false) }
    var showParentSelectionDialog by remember { mutableStateOf(false) }

    // Date and time state - START (inherit from parent calendar link if provided)
    val currentDateTime = remember {
        inheritFromCalendarLink?.startsAt ?: java.time.LocalDateTime.now()
    }

    // Date and time state using ModernDateTimePicker
    var startDateTime by remember {
        mutableStateOf(inheritFromCalendarLink?.startsAt ?: java.time.LocalDateTime.now())
    }

    // Date and time state - END (inherit from parent calendar link if provided, otherwise 1 hour after start)
    val defaultEndDateTime = inheritFromCalendarLink?.endsAt ?: currentDateTime.plusHours(1)
    var endDateTime by remember {
        mutableStateOf(defaultEndDateTime)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Aufgabe erstellen") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Titel") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = taskDescription,
                            onValueChange = { taskDescription = it },
                            label = { Text("Beschreibung (optional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
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
                                .clickable { showCategorySelectionDialog = true },
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

                    // Parent Task Selection
                    item {
                        Text("Parent Task (optional):", style = MaterialTheme.typography.labelMedium)
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showParentSelectionDialog = true },
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
                                    Text(
                                        text = if (selectedParentTask != null) "📁" else "🎯",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        text = selectedParentTask?.title ?: "Kein (Haupt-Task)",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Parent Task wählen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Show auto-complete parent option only if a parent is selected
                    if (selectedParentTask != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = autoCompleteParent,
                                    onCheckedChange = { autoCompleteParent = it }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        "Parent automatisch abschließen",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Wenn alle Subtasks erledigt sind, wird der Parent-Task automatisch abgeschlossen",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                                    onClick = { selectedPercentage = 20 },
                                    label = {
                                        Text("Trivial", style = MaterialTheme.typography.labelSmall)
                                    },
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                )
                                FilterChip(
                                    selected = selectedPercentage == 40,
                                    onClick = { selectedPercentage = 40 },
                                    label = {
                                        Text("Einfach", style = MaterialTheme.typography.labelSmall)
                                    },
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                )
                                FilterChip(
                                    selected = selectedPercentage == 60,
                                    onClick = { selectedPercentage = 60 },
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
                                    onClick = { selectedPercentage = 80 },
                                    label = {
                                        Text("Schwer", style = MaterialTheme.typography.labelSmall)
                                    },
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                )
                                FilterChip(
                                    selected = selectedPercentage == 100,
                                    onClick = { selectedPercentage = 100 },
                                    label = {
                                        Text("Episch", style = MaterialTheme.typography.labelSmall)
                                    },
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f)) // Empty space for layout balance
                            }
                        }
                    }

                    // Contact Selection
                    item {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Kontakte",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (selectedContactIds.isEmpty()) "Keine verknüpft"
                                    else "${selectedContactIds.size} verknüpft",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { showContactDialog = true },
                                enabled = availableContacts.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auswählen")
                            }
                        }

                        // Show selected contacts with photos - ALWAYS EXPANDED
                        if (selectedContactIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Header
                            Text(
                                "Verknüpfte Kontakte (${selectedContactIds.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Contact List - always visible
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val selectedContacts = availableContacts.filter { it.id in selectedContactIds }
                                    selectedContacts.forEach { contact ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Contact photo or icon
                                            if (contact.photoUri != null) {
                                                coil.compose.AsyncImage(
                                                    model = android.net.Uri.parse(contact.photoUri),
                                                    contentDescription = "Kontaktfoto",
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "Kein Foto",
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                                        .padding(6.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }

                                            Text(
                                                text = contact.displayName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // Date and Time Selection - START with ModernDateTimePicker
                    item {
                        ModernDateTimePicker(
                            label = "Start",
                            dateTime = startDateTime,
                            onDateTimeChange = { startDateTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Date and Time Selection - END with ModernDateTimePicker
                    item {
                        ModernDateTimePicker(
                            label = "Ende",
                            dateTime = endDateTime,
                            onDateTimeChange = { endDateTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // Calendar Integration (only show if permission granted)
                    item {
                        if (hasCalendarPermission) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = addToCalendar,
                                    onCheckedChange = { addToCalendar = it }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        "Google Kalender Integration",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Erstellt einen Kalendereintrag mit Erinnerung",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Show deleteOnClaim option if calendar is enabled
                            if (addToCalendar) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = deleteOnClaim,
                                        onCheckedChange = { deleteOnClaim = it }
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(
                                            "Nach XP-Claim löschen",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "Kalendereintrag wird nach XP-Erhalt entfernt",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = deleteOnExpiry,
                                        onCheckedChange = { deleteOnExpiry = it }
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(
                                            "Nach Ablauf löschen",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "Kalendereintrag wird automatisch entfernt wenn abgelaufen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Recurring task option
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        "Wiederkehrende Aufgabe",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Aufgabe wiederholt sich automatisch",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Recurring configuration button
                            if (isRecurring) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showRecurringDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(getRecurringButtonText(recurringConfig))
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚠️ Kalenderberechtigung erforderlich für Kalenderintegration",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Auto-generate title if empty: yyMMddHHmmss
                    val finalTitle = if (taskTitle.isBlank()) {
                        val now = java.time.LocalDateTime.now()
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss")
                        now.format(formatter)
                    } else {
                        taskTitle
                    }

                    // Debug log
                    android.util.Log.d("AddTaskDialog", "Creating task with:")
                    android.util.Log.d("AddTaskDialog", "  isRecurring: $isRecurring")
                    android.util.Log.d("AddTaskDialog", "  recurringConfig: $recurringConfig")
                    android.util.Log.d("AddTaskDialog", "  recurringConfig.mode: ${recurringConfig.mode}")
                    android.util.Log.d("AddTaskDialog", "  recurringConfig.triggerMode: ${recurringConfig.triggerMode}")

                    viewModel.createTaskWithCalendar(
                        title = finalTitle,
                        description = taskDescription,
                        xpPercentage = selectedPercentage,
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        addToCalendar = addToCalendar,
                        categoryId = taskCategory?.id,
                        deleteOnClaim = deleteOnClaim,
                        deleteOnExpiry = deleteOnExpiry,
                        isRecurring = isRecurring,
                        recurringConfig = if (isRecurring) recurringConfig else null,
                        parentTaskId = selectedParentTask?.id,
                        autoCompleteParent = autoCompleteParent,
                        contactIds = selectedContactIds
                    )
                    onDismiss()
                }
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    // Show recurring configuration dialog
    if (showRecurringDialog) {
        RecurringConfigDialog(
            initialConfig = recurringConfig,
            onDismiss = { showRecurringDialog = false },
            onConfirm = { config ->
                recurringConfig = config
                showRecurringDialog = false
            }
        )
    }

    // Show contact selection dialog
    if (showContactDialog) {
        SelectContactsDialog(
            contacts = availableContacts,
            selectedContactIds = selectedContactIds,
            onDismiss = { showContactDialog = false },
            onConfirm = { newContactIds ->
                selectedContactIds = newContactIds
                showContactDialog = false
            }
        )
    }

    // Category Selection Fullscreen Dialog
    if (showCategorySelectionDialog) {
        FullscreenSelectionDialog(
            title = "Kategorie wählen",
            items = categories,
            selectedItem = taskCategory,
            onItemSelected = { category ->
                taskCategory = category
            },
            onDismiss = { showCategorySelectionDialog = false },
            itemLabel = { it.name },
            itemDescription = { "${it.emoji} ${it.name}" },
            allowNone = false
        )
    }

    // Parent Task Selection Fullscreen Dialog
    if (showParentSelectionDialog) {
        FullscreenSelectionDialog(
            title = "Parent Task wählen",
            items = availableTasks.tasks,
            selectedItem = selectedParentTask,
            onItemSelected = { parent ->
                selectedParentTask = parent
            },
            onDismiss = { showParentSelectionDialog = false },
            itemLabel = { it.title },
            itemDescription = { task ->
                val hasSubtasks = availableTasks.tasks.any { it.parentTaskId == task.id }
                if (hasSubtasks) "📁 ${task.title}" else task.title
            },
            allowNone = true,
            noneLabel = "Kein (Haupt-Task)",
            searchPlaceholder = "Task suchen..."
        )
    }
}

// Helper function to get button text for recurring configuration - now uses the shared function
private fun getRecurringButtonText(config: RecurringConfig): String {
    return getRecurringSummary(config)
}
