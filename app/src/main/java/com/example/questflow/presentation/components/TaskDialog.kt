package com.example.questflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.domain.model.Task
import com.example.questflow.domain.model.TaskMetadataItem
import com.example.questflow.presentation.components.metadata.LinkMetadataDialog
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    isEditMode: Boolean = false,
    initialTitle: String = "",
    initialDescription: String = "",
    initialPercentage: Int = 60,
    initialDateTime: LocalDateTime = LocalDateTime.now(),
    initialCategoryId: Long? = null,
    initialAddToCalendar: Boolean = true,
    initialDeleteOnClaim: Boolean = false,
    initialDeleteOnExpiry: Boolean = false,
    initialIsRecurring: Boolean = false,
    initialRecurringConfig: RecurringConfig? = null,
    initialParentTaskId: Long? = null,
    initialAutoCompleteParent: Boolean = false,
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity? = null,
    hasCalendarPermission: Boolean,
    calendarLink: CalendarEventLinkEntity? = null, // For edit mode
    task: Task? = null, // For edit mode from Today screen
    availableTasks: List<Task> = emptyList(), // For parent task selection
    linkedMetadata: List<TaskMetadataItem> = emptyList(), // Existing linked metadata
    onMetadataLinked: (TaskMetadataItem) -> Unit = {}, // Callback for metadata linking
    availableContacts: List<MetadataContactEntity> = emptyList(), // For contact selection
    initialContactIds: Set<Long> = emptySet(), // Initial selected contacts
    onTaskClick: (Task) -> Unit = {}, // NEW: Callback to open another task dialog
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        xpPercentage: Int,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        categoryId: Long?,
        addToCalendar: Boolean,
        deleteOnClaim: Boolean,
        deleteOnExpiry: Boolean,
        isRecurring: Boolean,
        recurringConfig: RecurringConfig?,
        shouldReactivate: Boolean,
        parentTaskId: Long?,
        autoCompleteParent: Boolean,
        contactIds: Set<Long>
    ) -> Unit,
    onCreateSubtask: (parentTaskId: Long) -> Unit = {}, // NEW: Callback to create subtask
    getXpForPercentage: (Int) -> String
) {
    val context = LocalContext.current

    // State management
    var taskTitle by remember { mutableStateOf(initialTitle) }
    var taskDescription by remember { mutableStateOf(initialDescription) }
    var selectedPercentage by remember { mutableStateOf(initialPercentage) }

    // Find initial category from the provided categories list
    var taskCategory by remember(categories, initialCategoryId) {
        mutableStateOf(
            categories.find { it.id == initialCategoryId } ?: selectedCategory
        )
    }

    // Calendar integration state
    var addToCalendar by remember { mutableStateOf(initialAddToCalendar) }
    var deleteOnClaim by remember { mutableStateOf(initialDeleteOnClaim) }
    var deleteOnExpiry by remember { mutableStateOf(initialDeleteOnExpiry) }

    // Recurring task state
    var isRecurring by remember { mutableStateOf(initialIsRecurring) }
    var recurringConfig by remember {
        mutableStateOf(initialRecurringConfig ?: RecurringConfig())
    }
    var showRecurringDialog by remember { mutableStateOf(false) }

    // Subtask state
    var selectedParentTask by remember(availableTasks, initialParentTaskId) {
        mutableStateOf(availableTasks.find { it.id == initialParentTaskId })
    }
    var autoCompleteParent by remember { mutableStateOf(initialAutoCompleteParent) }

    // Reactivation state (only for edit mode)
    var shouldReactivate by remember { mutableStateOf(false) }

    // Metadata linking state
    var showLinkMetadataDialog by remember { mutableStateOf(false) }

    // Contact selection state
    var selectedContactIds by remember { mutableStateOf(initialContactIds) }
    var showTaskContactsDialog by remember { mutableStateOf(false) }
    var showActionsDialog by remember { mutableStateOf(false) }

    // Tag-Related State for Contact Selection
    val tagViewModel: com.example.questflow.presentation.viewmodels.TagViewModel = hiltViewModel()

    // Lade nur verwendete CONTACT-Tags (die mindestens einem Kontakt zugewiesen sind)
    var usedContactTags by remember { mutableStateOf<List<com.example.questflow.data.database.entity.MetadataTagEntity>>(emptyList()) }
    var contactTagsMap by remember { mutableStateOf<Map<Long, List<com.example.questflow.data.database.entity.MetadataTagEntity>>>(emptyMap()) }

    // Task-spezifische Contact-Tags State
    var taskContactTagsMap by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }

    // Lade Tags beim Start
    LaunchedEffect(availableContacts) {
        if (availableContacts.isNotEmpty()) {
            // Lade verwendete Tags
            usedContactTags = tagViewModel.getUsedContactTags()

            // Lade Contact-Tags Map
            contactTagsMap = tagViewModel.getContactTagsMap(availableContacts.map { it.id })
        }
    }

    // Lade Task-Contact-Tags wenn im Edit-Modus
    LaunchedEffect(task?.id) {
        task?.id?.let { taskId ->
            // Hier würden wir Task-Contact-Tags laden
            // TODO: Implement TaskContactTagViewModel
        }
    }

    // Date and time state - using QuickDateTimePicker-friendly state
    var startDateTime by remember { mutableStateOf(initialDateTime) }
    var endDateTime by remember { mutableStateOf(initialDateTime.plusHours(1)) }

    // Check if task is claimed (for showing reactivation option)
    val isClaimedTask = calendarLink?.let {
        it.rewarded || it.status == "CLAIMED"
    } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditMode) "Aufgabe bearbeiten" else "Neue Aufgabe erstellen")
        },
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
                    // Title input
                    item {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Titel") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Description input
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
                        var showCategoryDialog by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = taskCategory?.name ?: "Allgemein",
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showCategoryDialog = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDialog = true },
                            leadingIcon = {
                                taskCategory?.let { cat ->
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

                        if (showCategoryDialog) {
                            FullscreenSelectionDialog(
                                title = "Kategorie wählen",
                                items = categories,
                                selectedItem = taskCategory,
                                onItemSelected = { category ->
                                    taskCategory = category
                                },
                                onDismiss = { showCategoryDialog = false },
                                itemLabel = { it.name },
                                itemDescription = null,
                                allowNone = true,
                                noneLabel = "Allgemein",
                                searchPlaceholder = "Kategorie suchen..."
                            )
                        }
                    }

                    // Parent Task Selection (Subtask)
                    if (availableTasks.isNotEmpty()) {
                        item {
                            Text("Übergeordneter Task (optional):", style = MaterialTheme.typography.labelMedium)
                        }

                        item {
                            var showParentDialog by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = selectedParentTask?.title ?: "Kein (Haupt-Task)",
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showParentDialog = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showParentDialog = true }
                            )

                            if (showParentDialog) {
                                // Filter out current task to prevent self-parenting
                                val availableParentTasks = availableTasks.filter { it.id != task?.id }

                                FullscreenSelectionDialog(
                                    title = "Übergeordneter Task wählen",
                                    items = availableParentTasks,
                                    selectedItem = selectedParentTask,
                                    onItemSelected = { parentTask ->
                                        selectedParentTask = parentTask
                                    },
                                    onDismiss = { showParentDialog = false },
                                    itemLabel = { it.title },
                                    itemDescription = null,
                                    allowNone = true,
                                    noneLabel = "Kein (Haupt-Task)",
                                    searchPlaceholder = "Task suchen..."
                                )
                            }
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
                                        onCheckedChange = { autoCompleteParent = it }
                                    )
                                }
                            }
                        }
                    }

                    // Show Parent Task if current task is a subtask
                    if (isEditMode && task != null && task.parentTaskId != null) {
                        android.util.Log.d("TaskDialog", "=== PARENT TASK SECTION ===")
                        android.util.Log.d("TaskDialog", "isEditMode: $isEditMode")
                        android.util.Log.d("TaskDialog", "task.id: ${task.id}")
                        android.util.Log.d("TaskDialog", "task.title: ${task.title}")
                        android.util.Log.d("TaskDialog", "task.parentTaskId: ${task.parentTaskId}")
                        android.util.Log.d("TaskDialog", "availableTasks.size: ${availableTasks.size}")

                        val parentTask = availableTasks.find { it.id == task.parentTaskId }
                        android.util.Log.d("TaskDialog", "parentTask found: ${parentTask != null}")
                        if (parentTask != null) {
                            android.util.Log.d("TaskDialog", "parentTask.id: ${parentTask.id}")
                            android.util.Log.d("TaskDialog", "parentTask.title: ${parentTask.title}")

                            item {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            item {
                                Text(
                                    "Parent Task",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            android.util.Log.d("TaskDialog", "Parent Task Card clicked: ${parentTask.title}")
                                            onTaskClick(parentTask)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                                                parentTask.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (parentTask.description.isNotEmpty()) {
                                                Text(
                                                    parentTask.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1
                                                )
                                            }
                                        }
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
                    }

                    // Show Subtasks if current task is a parent
                    if (isEditMode && task != null) {
                        android.util.Log.d("TaskDialog", "=== SUBTASKS SECTION ===")
                        val subtasks = availableTasks.filter { it.parentTaskId == task.id }
                        android.util.Log.d("TaskDialog", "Subtasks found: ${subtasks.size}")
                        subtasks.forEachIndexed { idx, st ->
                            android.util.Log.d("TaskDialog", "  [$idx] Subtask: ${st.title} (id=${st.id})")
                        }

                        if (subtasks.isNotEmpty() || task.parentTaskId == null) {
                            // Only show section if has subtasks OR is a main task (not a subtask itself)
                            item {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Subtasks (${subtasks.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Create Subtask Button
                                    OutlinedButton(
                                        onClick = {
                                            android.util.Log.d("TaskDialog", "Create Subtask button clicked for parent: ${task.id}")
                                            onCreateSubtask(task.id)
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Neu", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // Render each subtask as separate item - CRITICAL FIX
                            if (subtasks.isNotEmpty()) {
                                android.util.Log.d("TaskDialog", "Rendering ${subtasks.size} subtask items")
                                items(subtasks.size) { index ->
                                    val subtask = subtasks[index]
                                    android.util.Log.d("TaskDialog", "Rendering subtask item [$index]: ${subtask.title}")
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .clickable {
                                                android.util.Log.d("TaskDialog", "Subtask Card clicked [$index]: ${subtask.title} (id=${subtask.id})")
                                                onTaskClick(subtask)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                                    subtask.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (subtask.description.isNotEmpty()) {
                                                    Text(
                                                        subtask.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Zu Subtask",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        "Keine Subtasks. Klicke 'Neu' um einen zu erstellen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Difficulty Selection
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
                            }

                            // XP Preview
                            Text(
                                text = "XP Belohnung: ${getXpForPercentage(selectedPercentage)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Metadata Linking
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Metadaten",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (linkedMetadata.isEmpty()) "Keine verknüpft"
                                    else "${linkedMetadata.size} verknüpft",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    // Only allow linking for edit mode with task ID
                                    if (isEditMode && task != null) {
                                        showLinkMetadataDialog = true
                                    }
                                },
                                enabled = isEditMode && task != null
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verknüpfen")
                            }
                        }

                        if (!isEditMode || task == null) {
                            Text(
                                "Speichern Sie die Aufgabe, um Metadaten zu verknüpfen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Contact Selection
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Kontakte & Aktionen",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (selectedContactIds.isEmpty()) "Keine verknüpft"
                                else "${selectedContactIds.size} verknüpft",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Zwei Buttons nebeneinander
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Button 1: Task Contacts (Alle Kontakte mit Tag-Filter)
                                OutlinedButton(
                                    onClick = {
                                        showTaskContactsDialog = true
                                    },
                                    enabled = availableContacts.isNotEmpty(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("Task Contacts", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                // Button 2: Aktionen (nur Task-Kontakte)
                                OutlinedButton(
                                    onClick = {
                                        showActionsDialog = true
                                    },
                                    enabled = selectedContactIds.isNotEmpty(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("Aktionen", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Date & Time Selection - START (ModernDateTimePicker)
                    item {
                        ModernDateTimePicker(
                            label = "Start",
                            dateTime = startDateTime,
                            onDateTimeChange = { startDateTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Date & Time Selection - END (ModernDateTimePicker)
                    item {
                        ModernDateTimePicker(
                            label = "Ende",
                            dateTime = endDateTime,
                            onDateTimeChange = { endDateTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Calendar Integration Options
                    item {
                        if (hasCalendarPermission) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                "Kalender-Optionen",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

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
                                        if (isEditMode) "Kalenderintegration verwenden"
                                        else "Erstellt einen Kalendereintrag mit Erinnerung",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

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
                                    Text(getRecurringSummary(recurringConfig))
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

                    // Reactivation option for claimed tasks (edit mode only)
                    if (isEditMode && isClaimedTask) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Reaktivieren",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "XP wurden bereits erhalten",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Aktiviere um XP erneut zu claimen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Switch(
                                        checked = shouldReactivate,
                                        onCheckedChange = { shouldReactivate = it }
                                    )
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
                    if (taskTitle.isNotBlank()) {
                        onConfirm(
                            taskTitle,
                            taskDescription,
                            selectedPercentage,
                            startDateTime,
                            endDateTime,
                            taskCategory?.id,
                            addToCalendar,
                            deleteOnClaim,
                            deleteOnExpiry,
                            isRecurring,
                            if (isRecurring) recurringConfig else null,
                            shouldReactivate && isClaimedTask,
                            selectedParentTask?.id,
                            autoCompleteParent,
                            selectedContactIds
                        )
                        onDismiss()
                    }
                }
            ) {
                Text(if (isEditMode) "Speichern" else "Erstellen")
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

    // Show metadata linking dialog
    if (showLinkMetadataDialog && task != null) {
        LinkMetadataDialog(
            taskId = task.id,
            onDismiss = { showLinkMetadataDialog = false },
            onMetadataLinked = { metadata ->
                onMetadataLinked(metadata)
                showLinkMetadataDialog = false
            }
        )
    }

    // Show Task Contacts Dialog (alle Kontakte mit Tag-Filter)
    if (showTaskContactsDialog) {
        TaskContactSelectionDialog(
            allContacts = availableContacts,
            contactTags = contactTagsMap,
            availableTags = usedContactTags,
            initialSelectedContactIds = selectedContactIds,
            onDismiss = { showTaskContactsDialog = false },
            onConfirm = { newContactIds ->
                selectedContactIds = newContactIds
                showTaskContactsDialog = false
            }
        )
    }

    // Show Actions Dialog (nur Task-Kontakte)
    if (showActionsDialog) {
        val taskLinkedContacts = availableContacts.filter { it.id in selectedContactIds }

        TaskContactActionsDialog(
            taskLinkedContacts = taskLinkedContacts,
            contactTags = contactTagsMap,
            availableTags = usedContactTags,
            taskContactTags = taskContactTagsMap,
            onDismiss = { showActionsDialog = false },
            onSaveTaskTags = { updatedTaskTags ->
                taskContactTagsMap = updatedTaskTags
                // TODO: Persist to database
                showActionsDialog = false
            }
        )
    }
}