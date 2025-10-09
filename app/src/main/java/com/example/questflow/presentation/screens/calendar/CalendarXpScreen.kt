package com.example.questflow.presentation.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.data.preferences.CalendarFilterSettings
import com.example.questflow.data.preferences.DateFilterType
import com.example.questflow.presentation.components.XpBurstAnimation
import com.example.questflow.presentation.components.XpLevelBadge
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.AppViewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import com.example.questflow.presentation.viewmodels.TodayViewModel
import com.example.questflow.presentation.components.AddTaskDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarXpScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: CalendarXpViewModel = hiltViewModel(),
    todayViewModel: TodayViewModel = hiltViewModel(),
    deepLinkTaskId: Long? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()
    val showFilterDialog by viewModel.showFilterDialog.collectAsState()
    val filterSettings by viewModel.filterSettings.collectAsState()
    val availableTasks by viewModel.getAvailableTasksFlow().collectAsState(initial = emptyList())
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedEditLink by remember { mutableStateOf<CalendarEventLinkEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var includeSubtasks by remember { mutableStateOf(false) }

    // Track previous XP for animation
    var previousXp by remember { mutableStateOf(globalStats?.xp ?: 0L) }
    LaunchedEffect(globalStats?.xp) {
        globalStats?.xp?.let { currentXp ->
            if (currentXp != previousXp) {
                previousXp = currentXp
            }
        }
    }

    // Sync category with viewmodel
    LaunchedEffect(selectedCategory) {
        viewModel.updateSelectedCategory(selectedCategory?.id)
    }

    // Handle deep link - open edit dialog for specific task
    LaunchedEffect(deepLinkTaskId) {
        deepLinkTaskId?.let { taskId ->
            android.util.Log.d("CalendarXpScreen", "Deep link received for taskId: $taskId")
            viewModel.openTaskFromDeepLink(taskId) { link ->
                android.util.Log.d("CalendarXpScreen", "Callback received, link: ${link != null}")
                if (link != null) {
                    android.util.Log.d("CalendarXpScreen", "Setting selectedEditLink to: ${link.title}")
                    selectedEditLink = link
                } else {
                    android.util.Log.w("CalendarXpScreen", "No link returned from viewModel")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            },
            topBar = {
                QuestFlowTopBar(
                    title = "Tasks",
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelected = appViewModel::selectCategory,
                    onManageCategoriesClick = {
                        navController.navigate("categories")
                    },
                    level = globalStats?.level ?: 1,
                    totalXp = globalStats?.xp ?: 0,
                    previousXp = previousXp,
                    actions = {
                        IconButton(onClick = { viewModel.toggleFilterDialog() }) {
                            Badge(
                                containerColor = if (filterSettings.isActive())
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.surface
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Filter",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (filterSettings.isActive())
                                        MaterialTheme.colorScheme.onTertiary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Filter links based on search query
            val filteredLinks = if (searchQuery.isEmpty()) {
                uiState.links
            } else {
                uiState.links.filter { link ->
                    // Search in title
                    link.title.contains(searchQuery, ignoreCase = true) ||
                    // Search in category name
                    (link.categoryId != null && categories.find { it.id == link.categoryId }?.name?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Suche nach Name, Metadaten, Tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Suchen") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Löschen")
                            }
                        }
                    },
                    singleLine = true
                )

                if (filteredLinks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isEmpty()) "No calendar events yet" else "Keine Ergebnisse gefunden",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLinks) { link ->
                    val isExpired = link.endsAt < java.time.LocalDateTime.now()
                    val isClaimed = link.rewarded || link.status == "CLAIMED"

                    // Check if this task is a parent or subtask
                    val taskData = availableTasks.find { it.id == link.taskId }
                    val isParentTask = taskData != null && availableTasks.any { it.parentTaskId == taskData.id }
                    val isSubtask = taskData?.parentTaskId != null
                    val parentTask = if (isSubtask) {
                        availableTasks.find { it.id == taskData?.parentTaskId }
                    } else null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Allow editing for ALL tasks
                                selectedEditLink = link
                                android.util.Log.d("CalendarXpScreen", "Task clicked: ${link.title}, taskId: ${link.taskId}, status: ${link.status}, expired: $isExpired")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isClaimed -> MaterialTheme.colorScheme.surfaceVariant
                                isExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        ListItem(
                            headlineContent = {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Show full path for subtasks: "ParentName / SubtaskName"
                                        if (isSubtask && parentTask != null) {
                                            Text(
                                                "${parentTask.title} / ",
                                                fontWeight = FontWeight.Normal,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                            )
                                        }

                                        // Visual indicator for parent/subtask
                                        if (isParentTask) {
                                            Text("📁 ", style = MaterialTheme.typography.bodyLarge)
                                        }

                                        Text(
                                            link.title,
                                            fontWeight = if (isParentTask) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isExpired && !isClaimed)
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isExpired && !isClaimed) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ) {
                                                Text("Abgelaufen", fontSize = 10.sp)
                                            }
                                        }
                                        if (isParentTask) {
                                            val subtaskCount = availableTasks.count { it.parentTaskId == taskData?.id }
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text("$subtaskCount", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            },
                            supportingContent = {
                                Column {
                                    Text(
                                        "Starts: ${link.startsAt.format(dateFormatter)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val difficultyText = when (link.xpPercentage) {
                                        20 -> "Trivial"
                                        40 -> "Einfach"
                                        60 -> "Mittel"
                                        80 -> "Schwer"
                                        100 -> "Episch"
                                        else -> "Mittel"
                                    }
                                    Text(
                                        "Schwierigkeit: $difficultyText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isExpired && !isClaimed)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            trailingContent = {
                                // Check if XP has been claimed (rewarded flag)
                                if (link.rewarded || link.status == "CLAIMED") {
                                    Text(
                                        "Erhalten",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    // Show claim button for all unclaimed tasks (including expired)
                                    val isExpired = link.endsAt < java.time.LocalDateTime.now()
                                    Button(
                                        onClick = {
                                            viewModel.claimXp(link.id) {
                                                // Refresh stats after claiming
                                                appViewModel.refreshStats()
                                            }
                                        },
                                        colors = if (isExpired) {
                                            ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        } else {
                                            ButtonDefaults.buttonColors()
                                        }
                                    ) {
                                        Text(if (isExpired) "Claim (Verspätet)" else "Claim XP")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
            }
        }

        // Show XP animation overlay
        uiState.xpAnimationData?.let { animationData ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                XpBurstAnimation(
                    xpAmount = animationData.xpAmount,
                    leveledUp = animationData.leveledUp,
                    newLevel = animationData.newLevel,
                    onAnimationEnd = {
                        viewModel.clearXpAnimation()
                    }
                )
            }
        }

        // Show snackbar for notifications
        uiState.notification?.let { message ->
            // In production, you'd show a Snackbar here
        }

        // Filter Dialog
        if (showFilterDialog) {
            CalendarFilterDialog(
                filterSettings = filterSettings,
                onDismiss = { viewModel.toggleFilterDialog() },
                onApply = { settings ->
                    viewModel.updateFilterSettings(settings)
                    viewModel.toggleFilterDialog()
                }
            )
        }
    }

    // Add Task Dialog using the unified Today dialog
    if (showAddTaskDialog) {
        // Sync category with Today ViewModel
        LaunchedEffect(selectedCategory) {
            todayViewModel.syncSelectedCategory(selectedCategory)
        }

        AddTaskDialog(
            viewModel = todayViewModel,
            onDismiss = {
                showAddTaskDialog = false
                // Refresh calendar links after creating
                viewModel.loadCalendarLinks()
            },
            isCalendarMode = true  // Enable calendar-specific features
        )
    }

    // Edit Task Dialog - works with or without taskId
    // CRITICAL FIX: Use key() to force dialog recreation when navigating between tasks
    selectedEditLink?.let { link ->
        key(link.id, link.taskId) {
            EditCalendarTaskDialog(
                calendarLink = link,
                viewModel = todayViewModel,
                calendarViewModel = viewModel,
                onDismiss = {
                    selectedEditLink = null
                    viewModel.loadCalendarLinks()
                },
                onNavigateToTask = { newLink ->
                    selectedEditLink = newLink
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCalendarTaskDialog(
    calendarLink: CalendarEventLinkEntity,
    viewModel: TodayViewModel,
    calendarViewModel: CalendarXpViewModel,
    onDismiss: () -> Unit,
    onNavigateToTask: (CalendarEventLinkEntity) -> Unit = {}
) {
    val availableTasks by calendarViewModel.getAvailableTasksFlow().collectAsState(initial = emptyList())
    val context = LocalContext.current
    var taskTitle by remember { mutableStateOf(calendarLink.title) }
    var taskDescription by remember { mutableStateOf("") }
    var selectedPercentage by remember { mutableStateOf(calendarLink.xpPercentage) }
    val categories by viewModel.categories.collectAsState()
    var taskCategory by remember(categories) {
        mutableStateOf(categories.find { it.id == calendarLink.categoryId })
    }
    var shouldReactivate by remember { mutableStateOf(false) }

    // Contact selection state
    val availableContacts by viewModel.contacts.collectAsState()
    var selectedContactIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }

    // Fullscreen selection dialogs state
    var showCategorySelectionDialog by remember { mutableStateOf(false) }
    var showParentSelectionDialog by remember { mutableStateOf(false) }

    // Tag-Related State for Contact Selection
    val tagViewModel: com.example.questflow.presentation.viewmodels.TagViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    var usedContactTags by remember { mutableStateOf<List<com.example.questflow.data.database.entity.MetadataTagEntity>>(emptyList()) }
    var contactTagsMap by remember { mutableStateOf<Map<Long, List<com.example.questflow.data.database.entity.MetadataTagEntity>>>(emptyMap()) }
    var taskContactTagsMap by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }

    // Load text templates for actions
    val textTemplates by viewModel.getTextTemplates().collectAsState(initial = emptyList())

    // Load tags and contact-tag mappings
    LaunchedEffect(availableContacts) {
        if (availableContacts.isNotEmpty()) {
            usedContactTags = tagViewModel.getUsedContactTags()
            contactTagsMap = tagViewModel.getContactTagsMap(availableContacts.map { it.id })
        }
    }

    // Load existing contact links for this task
    LaunchedEffect(calendarLink.taskId) {
        calendarLink.taskId?.let { taskId ->
            // CRITICAL: Launch separate coroutine for collect to avoid blocking
            launch {
                viewModel.getTaskContactIds(taskId).collect { contactIds ->
                    selectedContactIds = contactIds
                }
            }

            // Load task-specific contact tags
            android.util.Log.d("TaskTags", "LOADING task-tags from DB for task $taskId...")
            taskContactTagsMap = calendarViewModel.loadTaskContactTags(taskId)
            android.util.Log.d("TaskTags", "LOADED task-tags from DB: $taskContactTagsMap")
        }
    }

    // Calendar integration options - initialize once per dialog instance
    // WICHTIG: Keine calendarLink.deleteOnClaim/deleteOnExpiry als Keys verwenden!
    // Sonst wird State zurückgesetzt wenn DB sich durch Background-Updates ändert
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsState()

    // addToCalendar: Wenn irgendeine Kalender-Option gesetzt ist ODER Event existiert
    // → User-Intention, nicht aktueller Event-Status!
    var addToCalendar by remember(calendarLink.id) {
        mutableStateOf(
            calendarLink.calendarEventId != 0L ||
            calendarLink.deleteOnClaim ||
            calendarLink.deleteOnExpiry
        )
    }
    var deleteOnClaim by remember(calendarLink.id) {
        mutableStateOf(calendarLink.deleteOnClaim)
    }
    var deleteOnExpiry by remember(calendarLink.id) {
        mutableStateOf(calendarLink.deleteOnExpiry)
    }

    // Recurring task options
    var isRecurring by remember { mutableStateOf(calendarLink.isRecurring) }
    var recurringConfig by remember {
        mutableStateOf(com.example.questflow.presentation.components.RecurringConfig())
    }
    var showRecurringDialog by remember { mutableStateOf(false) }

    // Subtask options
    val currentTask = remember(availableTasks, calendarLink.taskId) {
        availableTasks.find { it.id == calendarLink.taskId }
    }
    var selectedParentTask by remember(availableTasks, currentTask) {
        mutableStateOf(
            currentTask?.parentTaskId?.let { parentId ->
                availableTasks.find { it.id == parentId }
            }
        )
    }
    var autoCompleteParent by remember(currentTask) {
        mutableStateOf(currentTask?.autoCompleteParent ?: false)
    }

    // Task family section collapse state - persistent
    val uiSettings by calendarViewModel.uiSettings.collectAsState()

    // Date and time state - using QuickDateTimePicker-friendly state
    var startDateTime by remember { mutableStateOf(calendarLink.startsAt) }
    var endDateTime by remember { mutableStateOf(calendarLink.endsAt) }

    // Track whether task is claimable
    val isClaimable = !calendarLink.rewarded && calendarLink.status != "CLAIMED" && calendarLink.status != "EXPIRED"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aufgabe bearbeiten")
                if (isClaimable) {
                    FilledTonalButton(
                        onClick = {
                            android.util.Log.d("QuickClaim", "Quick claim for linkId: ${calendarLink.id}")
                            calendarViewModel.claimXp(calendarLink.id) {
                                android.util.Log.d("QuickClaim", "Claim successful")
                                onDismiss()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Claim", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Claimen", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
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
                        OutlinedTextField(
                            value = taskCategory?.name ?: "Allgemein",
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showCategorySelectionDialog = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { showCategorySelectionDialog = true },
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
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Parent Task Selection (Subtask) - only if we have a task
                    if (availableTasks.isNotEmpty() && calendarLink.taskId != null) {
                        item {
                            Text("Übergeordneter Task (optional):", style = MaterialTheme.typography.labelMedium)
                        }

                        item {
                            OutlinedTextField(
                                value = selectedParentTask?.title ?: "Kein (Haupt-Task)",
                                onValueChange = { },
                                readOnly = true,
                                leadingIcon = if (selectedParentTask != null) {
                                    {
                                        IconButton(onClick = {
                                            // Navigate to parent task
                                            val parentLink = calendarViewModel.uiState.value.links.find { it.taskId == selectedParentTask?.id }
                                            if (parentLink != null) {
                                                onDismiss()
                                                onNavigateToTask(parentLink)
                                            }
                                        }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Zum Parent", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                } else null,
                                trailingIcon = {
                                    IconButton(onClick = { showParentSelectionDialog = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { showParentSelectionDialog = true }
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
                                        onCheckedChange = { autoCompleteParent = it }
                                    )
                                }
                            }
                        }
                    }

                    // Task Family Navigation - ALWAYS show both parent AND subtasks
                    // This allows jumping between any related tasks regardless of which one is currently open
                    if (calendarLink.taskId != null) {
                        // Determine the parent task and all siblings
                        val parentTask = if (currentTask?.parentTaskId != null) {
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
                                            calendarViewModel.updateUISettings(
                                                uiSettings.copy(taskFamilyExpanded = !uiSettings.taskFamilyExpanded)
                                            )
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
                                        if (uiSettings.taskFamilyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (uiSettings.taskFamilyExpanded) "Zuklappen" else "Aufklappen",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Show parent and subtasks only when expanded
                            if (uiSettings.taskFamilyExpanded) {
                                // ALWAYS show parent task card
                                item {
                                    val parentLink = calendarViewModel.uiState.value.links.find { it.taskId == parentTask.id }
                                    val isCurrentTask = parentTask.id == currentTask?.id

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (parentLink != null && !isCurrentTask) {
                                                    onDismiss()
                                                    onNavigateToTask(parentLink)
                                                }
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
                                    val subtaskLink = calendarViewModel.uiState.value.links.find { it.taskId == subtask.id }
                                    val isCurrentTask = subtask.id == currentTask?.id

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .clickable {
                                                if (subtaskLink != null && !isCurrentTask) {
                                                    onDismiss()
                                                    onNavigateToTask(subtaskLink)
                                                }
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

                    // Date and Time Selection - START (ModernDateTimePicker)
                    item {
                        com.example.questflow.presentation.components.ModernDateTimePicker(
                            label = "Start",
                            dateTime = startDateTime,
                            onDateTimeChange = { startDateTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Date and Time Selection - END (ModernDateTimePicker)
                    item {
                        com.example.questflow.presentation.components.ModernDateTimePicker(
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
                                        "Kalenderintegration verwenden",
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
                                    Text(com.example.questflow.presentation.components.getRecurringSummary(recurringConfig))
                                }
                            }
                        }
                    }

                    // Contact & Action Section
                    if (calendarLink.taskId != null) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Kontakte & Aktionen",
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
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { showContactDialog = true },
                                        enabled = availableContacts.isNotEmpty(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Task Contacts",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            android.util.Log.d("CalendarXpScreen", "=== AKTIONEN BUTTON CLICKED ===")
                                            android.util.Log.d("CalendarXpScreen", "selectedContactIds: $selectedContactIds")
                                            android.util.Log.d("CalendarXpScreen", "taskId: ${calendarLink.taskId}")
                                            android.util.Log.d("CalendarXpScreen", "Setting showActionDialog = true")
                                            showActionDialog = true
                                            android.util.Log.d("CalendarXpScreen", "showActionDialog is now: $showActionDialog")
                                        },
                                        enabled = selectedContactIds.isNotEmpty(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Aktionen",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Show selected contacts with tags
                            if (selectedContactIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
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
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        contact.displayName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    // Show task-tags for this contact
                                                    val contactTags = taskContactTagsMap[contact.id] ?: emptyList()
                                                    if (contactTags.isNotEmpty()) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        ) {
                                                            contactTags.take(3).forEach { tag ->
                                                                AssistChip(
                                                                    onClick = { },
                                                                    label = {
                                                                        Text(
                                                                            tag,
                                                                            style = MaterialTheme.typography.labelSmall
                                                                        )
                                                                    },
                                                                    modifier = Modifier.height(24.dp)
                                                                )
                                                            }
                                                            if (contactTags.size > 3) {
                                                                Text(
                                                                    "+${contactTags.size - 3}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        }
                    }

                    // Show reactivation option only for claimed tasks
                    val isExpiredTask = calendarLink.endsAt < java.time.LocalDateTime.now()
                    val isClaimedTask = calendarLink.rewarded || calendarLink.status == "CLAIMED"

                    if (isClaimedTask) {
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
                        // Check if we need to reactivate the task (only for claimed tasks)
                        val isReactivating = shouldReactivate &&
                            (calendarLink.rewarded || calendarLink.status == "CLAIMED")

                        // UNIFIED: Always use calendarViewModel.updateCalendarTask()
                        // Works for both tasks (with taskId) and calendar-only events (taskId = null)
                        calendarViewModel.updateCalendarTask(
                            linkId = calendarLink.id,
                            taskId = calendarLink.taskId, // Can be null
                            title = taskTitle,
                            description = taskDescription,
                            xpPercentage = selectedPercentage,
                            startDateTime = startDateTime,
                            endDateTime = endDateTime,
                            categoryId = taskCategory?.id,
                            shouldReactivate = isReactivating,
                            addToCalendar = addToCalendar, // User's calendar integration checkbox
                            deleteOnClaim = deleteOnClaim,
                            deleteOnExpiry = deleteOnExpiry,
                            isRecurring = isRecurring,
                            recurringConfig = if (isRecurring) recurringConfig else null,
                            parentTaskId = selectedParentTask?.id,
                            autoCompleteParent = autoCompleteParent
                        )

                        // Update contact links and task-tags if task has an ID
                        calendarLink.taskId?.let { taskId ->
                            viewModel.saveTaskContactLinks(taskId, selectedContactIds)
                            android.util.Log.d("TaskTags", "FINAL SAVE on dialog close - task $taskId: $taskContactTagsMap")
                            calendarViewModel.saveTaskContactTags(taskId, taskContactTagsMap)
                            android.util.Log.d("TaskTags", "FINAL SAVE completed")
                        }

                        onDismiss()
                    }
                }
            ) {
                Text("Speichern")
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
        com.example.questflow.presentation.components.RecurringConfigDialog(
            initialConfig = recurringConfig,
            onDismiss = { showRecurringDialog = false },
            onConfirm = { config ->
                recurringConfig = config
                showRecurringDialog = false
            }
        )
    }

    // Show contact-tag selection dialog
    if (showContactDialog && calendarLink.taskId != null) {
        val coroutineScope = rememberCoroutineScope()

        com.example.questflow.presentation.components.TaskContactSelectionDialog(
            allContacts = availableContacts,
            contactTags = contactTagsMap,
            availableTags = usedContactTags,
            initialSelectedContactIds = selectedContactIds,
            onDismiss = { showContactDialog = false },
            onConfirm = { newContactIds ->
                selectedContactIds = newContactIds

                // Save contacts to database
                calendarLink.taskId?.let { taskId ->
                    coroutineScope.launch {
                        viewModel.saveTaskContactLinks(taskId, newContactIds)
                    }
                }

                showContactDialog = false
            }
        )
    }

    // Action Dialog
    android.util.Log.d("CalendarXpScreen", "ACTION DIALOG CHECK: showActionDialog=$showActionDialog, taskId=${calendarLink.taskId}")
    if (showActionDialog && calendarLink.taskId != null) {
        android.util.Log.d("CalendarXpScreen", "=== RENDERING TaskContactActionsDialog ===")
        val taskLinkedContacts = availableContacts.filter { it.id in selectedContactIds }
        android.util.Log.d("CalendarXpScreen", "taskLinkedContacts count: ${taskLinkedContacts.size}")
        val coroutineScope = rememberCoroutineScope()

        com.example.questflow.presentation.components.TaskContactActionsDialog(
            taskId = calendarLink.taskId ?: 0L,
            taskLinkedContacts = taskLinkedContacts,
            contactTags = contactTagsMap,
            availableTags = usedContactTags,
            taskContactTags = taskContactTagsMap,
            textTemplates = textTemplates,
            actionExecutor = viewModel.actionExecutor,
            placeholderResolver = viewModel.placeholderResolver,
            multiContactActionManager = viewModel.multiContactActionManager,
            onDismiss = { showActionDialog = false },
            onSaveTaskTags = { updatedTaskTags ->
                // Save task-specific tags
                android.util.Log.d("TaskTags", "onSaveTaskTags called - before merge: $taskContactTagsMap")
                android.util.Log.d("TaskTags", "onSaveTaskTags - updated tags: $updatedTaskTags")

                // CRITICAL: Merge instead of replace to preserve tags of non-selected contacts
                taskContactTagsMap = taskContactTagsMap + updatedTaskTags

                android.util.Log.d("TaskTags", "onSaveTaskTags - after merge: $taskContactTagsMap")

                calendarLink.taskId?.let { taskId ->
                    coroutineScope.launch {
                        android.util.Log.d("TaskTags", "Saving merged tags to DB for task $taskId: $taskContactTagsMap")
                        calendarViewModel.saveTaskContactTags(taskId, taskContactTagsMap)
                        android.util.Log.d("TaskTags", "Tags saved successfully")
                    }
                }
                showActionDialog = false
            }
        )
    }

    // Category Selection Fullscreen Dialog
    if (showCategorySelectionDialog) {
        com.example.questflow.presentation.components.FullscreenSelectionDialog(
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
        val availableParents = availableTasks.filter { it.id != calendarLink.taskId }
        com.example.questflow.presentation.components.FullscreenSelectionDialog(
            title = "Parent Task wählen",
            items = availableParents,
            selectedItem = selectedParentTask,
            onItemSelected = { parent ->
                selectedParentTask = parent
            },
            onDismiss = { showParentSelectionDialog = false },
            itemLabel = { it.title },
            itemDescription = { task ->
                val hasSubtasks = availableTasks.any { it.parentTaskId == task.id }
                if (hasSubtasks) "📁 ${task.title}" else task.title
            },
            allowNone = true,
            noneLabel = "Kein (Haupt-Task)",
            searchPlaceholder = "Task suchen..."
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterDialog(
    filterSettings: CalendarFilterSettings,
    onDismiss: () -> Unit,
    onApply: (CalendarFilterSettings) -> Unit
) {
    val context = LocalContext.current
    var showCompleted by remember { mutableStateOf(filterSettings.showCompleted) }
    var showOpen by remember { mutableStateOf(filterSettings.showOpen) }
    var showExpired by remember { mutableStateOf(filterSettings.showExpired) }
    var filterByCategory by remember { mutableStateOf(filterSettings.filterByCategory) }
    var dateFilterType by remember { mutableStateOf(filterSettings.dateFilterType) }

    // Custom date range state
    var customStartDateTime by remember {
        mutableStateOf(
            if (filterSettings.customRangeStart > 0)
                java.time.LocalDateTime.ofEpochSecond(filterSettings.customRangeStart, 0, java.time.ZoneOffset.UTC)
            else
                java.time.LocalDateTime.now()
        )
    }
    var customEndDateTime by remember {
        mutableStateOf(
            if (filterSettings.customRangeEnd > 0)
                java.time.LocalDateTime.ofEpochSecond(filterSettings.customRangeEnd, 0, java.time.ZoneOffset.UTC)
            else
                java.time.LocalDateTime.now().plusDays(7)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Calendar Events") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Filter Section
                Text(
                    "Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showOpen,
                        onClick = { showOpen = !showOpen },
                        label = { Text("Offen") },
                        leadingIcon = if (showOpen) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = showCompleted,
                        onClick = { showCompleted = !showCompleted },
                        label = { Text("Erhalten") },
                        leadingIcon = if (showCompleted) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = showExpired,
                        onClick = { showExpired = !showExpired },
                        label = { Text("Abgelaufen") },
                        leadingIcon = if (showExpired) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                Divider()

                // Category Filter Section
                Text(
                    "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = filterByCategory,
                        onCheckedChange = { filterByCategory = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (filterByCategory) "Show only selected category" else "Show all categories",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Divider()

                // Date Filter Section
                Text(
                    "Date Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DateFilterType.values().forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dateFilterType = type }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = dateFilterType == type,
                                onClick = { dateFilterType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (type) {
                                    DateFilterType.ALL -> "All dates"
                                    DateFilterType.TODAY -> "Today"
                                    DateFilterType.THIS_WEEK -> "This week"
                                    DateFilterType.THIS_MONTH -> "This month"
                                    DateFilterType.CUSTOM_RANGE -> "Custom range"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Custom Range Date Pickers (only show when CUSTOM_RANGE is selected)
                if (dateFilterType == DateFilterType.CUSTOM_RANGE) {
                    Divider()

                    Text(
                        "Custom Range Start",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${customStartDateTime.dayOfMonth}.${customStartDateTime.monthValue}.${customStartDateTime.year} ${customStartDateTime.hour}:${String.format("%02d", customStartDateTime.minute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
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
                                        customStartDateTime = customStartDateTime
                                            .withYear(year)
                                            .withMonth(month + 1)
                                            .withDayOfMonth(dayOfMonth)
                                    },
                                    customStartDateTime.year,
                                    customStartDateTime.monthValue - 1,
                                    customStartDateTime.dayOfMonth
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
                                        customStartDateTime = customStartDateTime
                                            .withHour(hourOfDay)
                                            .withMinute(minute)
                                    },
                                    customStartDateTime.hour,
                                    customStartDateTime.minute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🕐 Uhrzeit")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Custom Range End",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${customEndDateTime.dayOfMonth}.${customEndDateTime.monthValue}.${customEndDateTime.year} ${customEndDateTime.hour}:${String.format("%02d", customEndDateTime.minute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
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
                                        customEndDateTime = customEndDateTime
                                            .withYear(year)
                                            .withMonth(month + 1)
                                            .withDayOfMonth(dayOfMonth)
                                    },
                                    customEndDateTime.year,
                                    customEndDateTime.monthValue - 1,
                                    customEndDateTime.dayOfMonth
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
                                        customEndDateTime = customEndDateTime
                                            .withHour(hourOfDay)
                                            .withMinute(minute)
                                    },
                                    customEndDateTime.hour,
                                    customEndDateTime.minute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🕐 Uhrzeit")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        filterSettings.copy(
                            showCompleted = showCompleted,
                            showOpen = showOpen,
                            showExpired = showExpired,
                            filterByCategory = filterByCategory,
                            dateFilterType = dateFilterType,
                            customRangeStart = customStartDateTime.toEpochSecond(java.time.ZoneOffset.UTC),
                            customRangeEnd = customEndDateTime.toEpochSecond(java.time.ZoneOffset.UTC)
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}