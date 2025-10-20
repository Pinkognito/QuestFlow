package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import com.example.questflow.presentation.viewmodels.TodayViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    viewModel: TodayViewModel,
    onDismiss: () -> Unit,
    isCalendarMode: Boolean = false,  // Add parameter to distinguish context
    preSelectedParentId: Long? = null,  // Pre-select parent task (for sub-task creation)
    inheritFromTask: com.example.questflow.domain.model.Task? = null,  // Inherit category from parent
    inheritFromCalendarLink: com.example.questflow.data.database.entity.CalendarEventLinkEntity? = null,  // Inherit time from parent
    occupancyCalculator: com.example.questflow.domain.usecase.DayOccupancyCalculator? = null  // For month view occupancy
) {
    val uiSettings by viewModel.uiSettings.collectAsState()

    // Calendar events for month view occupancy (get 2 months range)
    val currentMonth = remember { java.time.YearMonth.now() }
    val monthViewEvents by viewModel.getCalendarEventsInRange(
        startDate = currentMonth.minusMonths(1).atDay(1),
        endDate = currentMonth.plusMonths(2).atDay(1)
    ).collectAsState(initial = emptyList())
    
    var taskTitleField by remember { mutableStateOf(TextFieldValue("")) }
    var taskDescriptionField by remember { mutableStateOf(TextFieldValue("")) }
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

    // Placeholder dialogs for task fields
    var showTaskTitlePlaceholderDialog by remember { mutableStateOf(false) }
    var showTaskDescriptionPlaceholderDialog by remember { mutableStateOf(false) }
    var showTaskTitleTemplateDialog by remember { mutableStateOf(false) }
    var showTaskDescriptionTemplateDialog by remember { mutableStateOf(false) }

    // Calendar Event Custom Fields
    var calendarEventSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var calendarEventCustomTitleField by remember { mutableStateOf(TextFieldValue("")) }
    var calendarEventCustomDescriptionField by remember { mutableStateOf(TextFieldValue("")) }
    val textTemplates by viewModel.textTemplates.collectAsState(initial = emptyList())
    var showCalendarTitleTemplateDialog by remember { mutableStateOf(false) }
    var showCalendarDescriptionTemplateDialog by remember { mutableStateOf(false) }
    var showCalendarTitlePlaceholderDialog by remember { mutableStateOf(false) }
    var showCalendarDescriptionPlaceholderDialog by remember { mutableStateOf(false) }

    // Load default CALENDAR template on first composition
    LaunchedEffect(Unit) {
        viewModel.getDefaultCalendarTemplate()?.let { defaultTemplate ->
            calendarEventCustomTitleField = TextFieldValue(defaultTemplate.subject ?: defaultTemplate.content)
            calendarEventCustomDescriptionField = TextFieldValue(defaultTemplate.content)
        }
    }

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

    // Smart scheduling state
    val coroutineScope = rememberCoroutineScope()
    var showTimeSlotDialog by remember { mutableStateOf(false) }
    var scheduleConflicts by remember { mutableStateOf<List<com.example.questflow.data.calendar.CalendarEvent>>(emptyList()) }
    var dailyFreeTime by remember { mutableStateOf<List<com.example.questflow.domain.usecase.FindFreeTimeSlotsUseCase.DailyFreeTime>>(emptyList()) }
    var timeSlotSuggestions by remember { mutableStateOf<List<com.example.questflow.domain.usecase.FindFreeTimeSlotsUseCase.FreeSlot>>(emptyList()) }

    // Check for conflicts whenever date/time changes
    LaunchedEffect(startDateTime, endDateTime) {
        if (hasCalendarPermission) {
            scheduleConflicts = viewModel.checkScheduleConflicts(
                startTime = startDateTime,
                endTime = endDateTime,
                excludeEventId = null
            )
        }
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
                    // Task Title with Template and Placeholder Support
                    item {
                        Text("Titel:", style = MaterialTheme.typography.labelMedium)
                    }
                    item {
                        OutlinedButton(
                            onClick = { showTaskTitleTemplateDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = textTemplates.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (textTemplates.isEmpty()) "Keine Templates verfügbar"
                                else "Textbaustein auswählen"
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = taskTitleField,
                            onValueChange = { taskTitleField = it },
                            label = { Text("Titel") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showTaskTitlePlaceholderDialog = true }) {
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
                            onClick = { showTaskDescriptionTemplateDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = textTemplates.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (textTemplates.isEmpty()) "Keine Templates verfügbar"
                                else "Textbaustein auswählen"
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = taskDescriptionField,
                            onValueChange = { taskDescriptionField = it },
                            label = { Text("Beschreibung (optional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showTaskDescriptionPlaceholderDialog = true }) {
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
                            modifier = Modifier.fillMaxWidth(),
                            events = monthViewEvents,
                            occupancyCalculator = occupancyCalculator,
                            currentTaskId = inheritFromTask?.id,
                            currentCategoryId = taskCategory?.id
                        )
                    }

                    // Date and Time Selection - END with ModernDateTimePicker
                    item {
                        ModernDateTimePicker(
                            label = "Ende",
                            dateTime = endDateTime,
                            onDateTimeChange = { endDateTime = it },
                            modifier = Modifier.fillMaxWidth(),
                            events = monthViewEvents,
                            occupancyCalculator = occupancyCalculator,
                            currentTaskId = inheritFromTask?.id,
                            currentCategoryId = taskCategory?.id
                        )
                    }

                    // Conflict warning and smart scheduling
                    if (hasCalendarPermission && scheduleConflicts.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Column {
                                            Text(
                                                text = "⚠️ Zeitkonflikt",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = if (scheduleConflicts.size == 1) {
                                                    "Zur gewählten Zeit ist bereits 1 Termin eingetragen"
                                                } else {
                                                    "Zur gewählten Zeit sind bereits ${scheduleConflicts.size} Termine eingetragen"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }

                                    // Smart scheduling button
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val durationMinutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes()

                                                // Load free time slots
                                                val startDate = startDateTime.toLocalDate()
                                                val endDate = startDate.plusDays(30)
                                                dailyFreeTime = viewModel.findFreeTimeSlots(
                                                    startDate = startDate,
                                                    endDate = endDate,
                                                    minDurationMinutes = durationMinutes
                                                )

                                                // Get suggestions
                                                timeSlotSuggestions = viewModel.suggestTimeSlots(
                                                    requiredDurationMinutes = durationMinutes,
                                                    startSearchFrom = startDateTime,
                                                    maxSuggestions = 5
                                                )

                                                showTimeSlotDialog = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Freie Zeiten finden")
                                    }
                                }
                            }
                        }
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
                            // Recurring configuration button (FIX P1-002: Always show, not just when isRecurring=true)
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

                            // Calendar Event Configuration Section (Expandable)
                            if (addToCalendar) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Header (clickable to expand/collapse)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { calendarEventSectionExpanded = !calendarEventSectionExpanded }
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Column {
                                                    Text(
                                                        "Kalender-Event anpassen",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        "Titel & Beschreibung mit Platzhaltern",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = if (calendarEventSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (calendarEventSectionExpanded) "Einklappen" else "Ausklappen",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        // Expandable Content
                                        if (calendarEventSectionExpanded) {
                                            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Title Section
                                                Text(
                                                    "Betreff:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )

                                                // Template Dropdown Button for Title
                                                OutlinedButton(
                                                    onClick = { showCalendarTitleTemplateDialog = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = textTemplates.isNotEmpty()
                                                ) {
                                                    Icon(
                                                        Icons.Default.List,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        if (textTemplates.isEmpty()) "Keine Templates verfügbar"
                                                        else "Textbaustein auswählen"
                                                    )
                                                }

                                                // Title TextField
                                                OutlinedTextField(
                                                    value = calendarEventCustomTitleField,
                                                    onValueChange = { calendarEventCustomTitleField = it },
                                                    label = { Text("Event-Titel (optional)") },
                                                    placeholder = { Text("z.B. Meeting mit {kontakt.name}") },
                                                    maxLines = 2,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    trailingIcon = {
                                                        IconButton(onClick = { showCalendarTitlePlaceholderDialog = true }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Platzhalter einfügen",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Description Section
                                                Text(
                                                    "Beschreibung:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )

                                                // Template Dropdown Button for Description
                                                OutlinedButton(
                                                    onClick = { showCalendarDescriptionTemplateDialog = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = textTemplates.isNotEmpty()
                                                ) {
                                                    Icon(
                                                        Icons.Default.List,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        if (textTemplates.isEmpty()) "Keine Templates verfügbar"
                                                        else "Textbaustein auswählen"
                                                    )
                                                }

                                                // Description TextField
                                                OutlinedTextField(
                                                    value = calendarEventCustomDescriptionField,
                                                    onValueChange = { calendarEventCustomDescriptionField = it },
                                                    label = { Text("Event-Beschreibung (optional)") },
                                                    placeholder = { Text("z.B. Aufgabe: {task.title}\nKontakt: {kontakt.name}") },
                                                    maxLines = 4,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    trailingIcon = {
                                                        IconButton(onClick = { showCalendarDescriptionPlaceholderDialog = true }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Platzhalter einfügen",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                )

                                                // Info Card
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Info,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            "Platzhalter: {task.title}, {task.description}, {kontakt.name}, {kontakt.name.all}, {standort.name}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
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
                    val taskTitleText = taskTitleField.text
                    val finalTitle = if (taskTitleText.isBlank()) {
                        val now = java.time.LocalDateTime.now()
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss")
                        now.format(formatter)
                    } else {
                        taskTitleText
                    }

                    // Debug log
                    android.util.Log.d("AddTaskDialog", "Creating task with:")
                    android.util.Log.d("AddTaskDialog", "  isRecurring: $isRecurring")
                    android.util.Log.d("AddTaskDialog", "  recurringConfig: $recurringConfig")
                    android.util.Log.d("AddTaskDialog", "  recurringConfig.mode: ${recurringConfig.mode}")
                    android.util.Log.d("AddTaskDialog", "  recurringConfig.triggerMode: ${recurringConfig.triggerMode}")

                    viewModel.createTaskWithCalendar(
                        title = finalTitle,
                        description = taskDescriptionField.text,
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
                        contactIds = selectedContactIds,
                        calendarEventCustomTitle = calendarEventCustomTitleField.text.ifBlank { null },
                        calendarEventCustomDescription = calendarEventCustomDescriptionField.text.ifBlank { null }
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
            minuteIncrement = uiSettings.recurringTimeMinuteIncrement,
            onMinuteIncrementChange = { newValue ->
                viewModel.updateUISettings(uiSettings.copy(recurringTimeMinuteIncrement = newValue))
            },
            onDismiss = { showRecurringDialog = false },
            onConfirm = { config ->
                recurringConfig = config
                isRecurring = true  // FIX P1-002: Auto-activate isRecurring when config is saved
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

    // Time Slot Suggestion Dialog
    if (showTimeSlotDialog) {
        TimeSlotSuggestionDialog(
            currentStartTime = startDateTime,
            currentEndTime = endDateTime,
            dailyFreeTime = dailyFreeTime,
            suggestions = timeSlotSuggestions,
            hasConflict = scheduleConflicts.isNotEmpty(),
            conflictCount = scheduleConflicts.size,
            onDismiss = { showTimeSlotDialog = false },
            onTimeSlotSelected = { newStart, newEnd ->
                startDateTime = newStart
                endDateTime = newEnd
            }
        )
    }

    // Template Selection Dialogs for Task Title
    if (showTaskTitleTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTaskTitleTemplateDialog = false },
            title = { Text("Textbaustein für Titel wählen") },
            text = {
                LazyColumn {
                    items(textTemplates.size) { index ->
                        val template = textTemplates[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    taskTitleField = TextFieldValue(template.subject ?: template.content)
                                    showTaskTitleTemplateDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                template.description?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                                Text(text = template.subject ?: template.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), maxLines = 2)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTaskTitleTemplateDialog = false }) { Text("Abbrechen") } }
        )
    }

    if (showTaskDescriptionTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTaskDescriptionTemplateDialog = false },
            title = { Text("Textbaustein für Beschreibung wählen") },
            text = {
                LazyColumn {
                    items(textTemplates.size) { index ->
                        val template = textTemplates[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                taskDescriptionField = TextFieldValue(template.content)
                                showTaskDescriptionTemplateDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                template.description?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                                Text(text = template.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), maxLines = 3)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTaskDescriptionTemplateDialog = false }) { Text("Abbrechen") } }
        )
    }

    if (showCalendarTitleTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarTitleTemplateDialog = false },
            title = { Text("Textbaustein für Calendar-Titel wählen") },
            text = {
                LazyColumn {
                    items(textTemplates.size) { index ->
                        val template = textTemplates[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                calendarEventCustomTitleField = TextFieldValue(template.subject ?: template.content)
                                showCalendarTitleTemplateDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                template.description?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                                Text(text = template.subject ?: template.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), maxLines = 2)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCalendarTitleTemplateDialog = false }) { Text("Abbrechen") } }
        )
    }

    if (showCalendarDescriptionTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarDescriptionTemplateDialog = false },
            title = { Text("Textbaustein für Calendar-Beschreibung wählen") },
            text = {
                LazyColumn {
                    items(textTemplates.size) { index ->
                        val template = textTemplates[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                calendarEventCustomDescriptionField = TextFieldValue(template.content)
                                showCalendarDescriptionTemplateDialog = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                template.description?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                                Text(text = template.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp), maxLines = 3)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCalendarDescriptionTemplateDialog = false }) { Text("Abbrechen") } }
        )
    }

    // Placeholder dialogs
    if (showTaskTitlePlaceholderDialog) {
        PlaceholderSelectorDialog(
            onDismiss = { showTaskTitlePlaceholderDialog = false },
            onPlaceholderSelected = { placeholder ->
                val currentText = taskTitleField.text
                val selection = taskTitleField.selection
                val newText = currentText.substring(0, selection.start) + placeholder + currentText.substring(selection.end)
                taskTitleField = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(selection.start + placeholder.length))
            }
        )
    }

    if (showTaskDescriptionPlaceholderDialog) {
        PlaceholderSelectorDialog(
            onDismiss = { showTaskDescriptionPlaceholderDialog = false },
            onPlaceholderSelected = { placeholder ->
                val currentText = taskDescriptionField.text
                val selection = taskDescriptionField.selection
                val newText = currentText.substring(0, selection.start) + placeholder + currentText.substring(selection.end)
                taskDescriptionField = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(selection.start + placeholder.length))
            }
        )
    }

    if (showCalendarTitlePlaceholderDialog) {
        PlaceholderSelectorDialog(
            onDismiss = { showCalendarTitlePlaceholderDialog = false },
            onPlaceholderSelected = { placeholder ->
                val currentText = calendarEventCustomTitleField.text
                val selection = calendarEventCustomTitleField.selection
                val newText = currentText.substring(0, selection.start) + placeholder + currentText.substring(selection.end)
                calendarEventCustomTitleField = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(selection.start + placeholder.length))
            }
        )
    }

    if (showCalendarDescriptionPlaceholderDialog) {
        PlaceholderSelectorDialog(
            onDismiss = { showCalendarDescriptionPlaceholderDialog = false },
            onPlaceholderSelected = { placeholder ->
                val currentText = calendarEventCustomDescriptionField.text
                val selection = calendarEventCustomDescriptionField.selection
                val newText = currentText.substring(0, selection.start) + placeholder + currentText.substring(selection.end)
                calendarEventCustomDescriptionField = TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(selection.start + placeholder.length))
            }
        )
    }
}

// Helper function to get button text for recurring configuration - now uses the shared function
private fun getRecurringButtonText(config: RecurringConfig): String {
    return getRecurringSummary(config)
}
