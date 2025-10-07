package com.example.questflow.presentation.components

import androidx.compose.foundation.background
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

    // Date and time state - START datetime
    var selectedYear by remember { mutableStateOf(initialDateTime.year) }
    var selectedMonth by remember { mutableStateOf(initialDateTime.monthValue) }
    var selectedDay by remember { mutableStateOf(initialDateTime.dayOfMonth) }
    var selectedHour by remember { mutableStateOf(initialDateTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialDateTime.minute) }

    // END datetime state (default: 1 hour after start)
    val defaultEndDateTime = initialDateTime.plusHours(1)
    var endYear by remember { mutableStateOf(defaultEndDateTime.year) }
    var endMonth by remember { mutableStateOf(defaultEndDateTime.monthValue) }
    var endDay by remember { mutableStateOf(defaultEndDateTime.dayOfMonth) }
    var endHour by remember { mutableStateOf(defaultEndDateTime.hour) }
    var endMinute by remember { mutableStateOf(defaultEndDateTime.minute) }

    val dateTimeText = "$selectedDay.$selectedMonth.$selectedYear $selectedHour:${String.format("%02d", selectedMinute)}"
    val endDateTimeText = "$endDay.$endMonth.$endYear $endHour:${String.format("%02d", endMinute)}"

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
                        var categoryExpanded by remember { mutableStateOf(false) }
                        var categorySearchQuery by remember { mutableStateOf("") }
                        val categoryFocusRequester = remember { FocusRequester() }

                        OutlinedTextField(
                            value = taskCategory?.name ?: "Allgemein",
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { categoryExpanded = !categoryExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
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

                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            // Search field
                            OutlinedTextField(
                                value = categorySearchQuery,
                                onValueChange = { categorySearchQuery = it },
                                placeholder = { Text("Suchen...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .focusRequester(categoryFocusRequester),
                                singleLine = true
                            )

                            // Auto-focus when dropdown opens
                            LaunchedEffect(categoryExpanded) {
                                if (categoryExpanded) {
                                    kotlinx.coroutines.delay(100)
                                    categoryFocusRequester.requestFocus()
                                }
                            }

                            // Filter categories
                            val filteredCategories = categories.filter { category ->
                                category.name.contains(categorySearchQuery, ignoreCase = true) ||
                                category.emoji.contains(categorySearchQuery, ignoreCase = true)
                            }

                            if (filteredCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Keine Ergebnisse", style = MaterialTheme.typography.bodySmall) },
                                    onClick = { },
                                    enabled = false
                                )
                            } else {
                                filteredCategories.forEach { category ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        try {
                                                            Color(android.graphics.Color.parseColor(category.color))
                                                        } catch (e: Exception) {
                                                            MaterialTheme.colorScheme.primary
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = category.emoji,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        },
                                        text = { Text(category.name) },
                                        onClick = {
                                            taskCategory = category
                                            categoryExpanded = false
                                            categorySearchQuery = ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Parent Task Selection (Subtask)
                    if (availableTasks.isNotEmpty()) {
                        item {
                            Text("Übergeordneter Task (optional):", style = MaterialTheme.typography.labelMedium)
                        }

                        item {
                            var parentExpanded by remember { mutableStateOf(false) }
                            var parentSearchQuery by remember { mutableStateOf("") }
                            val parentFocusRequester = remember { FocusRequester() }

                            OutlinedTextField(
                                value = selectedParentTask?.title ?: "Kein (Haupt-Task)",
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { parentExpanded = !parentExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            DropdownMenu(
                                expanded = parentExpanded,
                                onDismissRequest = { parentExpanded = false }
                            ) {
                                // Search field
                                OutlinedTextField(
                                    value = parentSearchQuery,
                                    onValueChange = { parentSearchQuery = it },
                                    placeholder = { Text("Suchen...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .focusRequester(parentFocusRequester),
                                    singleLine = true
                                )

                                // Auto-focus when dropdown opens
                                LaunchedEffect(parentExpanded) {
                                    if (parentExpanded) {
                                        kotlinx.coroutines.delay(100)
                                        parentFocusRequester.requestFocus()
                                    }
                                }

                                // Option: No parent (main task) - always visible
                                DropdownMenuItem(
                                    text = { Text("Kein (Haupt-Task)") },
                                    onClick = {
                                        selectedParentTask = null
                                        parentExpanded = false
                                        parentSearchQuery = ""
                                    }
                                )

                                // Filter tasks
                                val filteredTasks = availableTasks.filter { it.id != task?.id }.filter { parentTask ->
                                    parentTask.title.contains(parentSearchQuery, ignoreCase = true) ||
                                    parentTask.description.contains(parentSearchQuery, ignoreCase = true)
                                }

                                if (filteredTasks.isEmpty() && parentSearchQuery.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Keine Ergebnisse", style = MaterialTheme.typography.bodySmall) },
                                        onClick = { },
                                        enabled = false
                                    )
                                } else {
                                    // Available tasks as parent options
                                    filteredTasks.forEach { parentTask ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = parentTask.title,
                                                    maxLines = 1
                                                )
                                            },
                                            onClick = {
                                                selectedParentTask = parentTask
                                                parentExpanded = false
                                                parentSearchQuery = ""
                                            }
                                        )
                                    }
                                }
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

                    // Date & Time Selection - START
                    item {
                        Text("Start:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "📅 $dateTimeText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val picker = DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            selectedYear = year
                                            selectedMonth = month + 1
                                            selectedDay = dayOfMonth
                                        },
                                        selectedYear,
                                        selectedMonth - 1,
                                        selectedDay
                                    )
                                    picker.datePicker.minDate = 0
                                    picker.datePicker.maxDate = Long.MAX_VALUE
                                    picker.show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📅 Datum")
                            }

                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            selectedHour = hourOfDay
                                            selectedMinute = minute
                                        },
                                        selectedHour,
                                        selectedMinute,
                                        true
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🕐 Uhrzeit")
                            }
                        }
                    }

                    // Date & Time Selection - END
                    item {
                        Text("Ende:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "📅 $endDateTimeText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val picker = DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            endYear = year
                                            endMonth = month + 1
                                            endDay = dayOfMonth
                                        },
                                        endYear,
                                        endMonth - 1,
                                        endDay
                                    )
                                    picker.datePicker.minDate = 0
                                    picker.datePicker.maxDate = Long.MAX_VALUE
                                    picker.show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📅 Datum")
                            }

                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            endHour = hourOfDay
                                            endMinute = minute
                                        },
                                        endHour,
                                        endMinute,
                                        true
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🕐 Uhrzeit")
                            }
                        }
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
                        val startDateTime = LocalDateTime.of(
                            selectedYear, selectedMonth, selectedDay,
                            selectedHour, selectedMinute
                        )
                        val endDateTime = LocalDateTime.of(
                            endYear, endMonth, endDay,
                            endHour, endMinute
                        )
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