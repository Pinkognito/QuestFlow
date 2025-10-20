package com.example.questflow.presentation.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
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
import com.example.questflow.data.preferences.TaskFilterSettings
import com.example.questflow.data.preferences.DateFilterType
import com.example.questflow.presentation.components.XpBurstAnimation
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.AppViewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import com.example.questflow.presentation.viewmodels.TodayViewModel
import com.example.questflow.presentation.components.AddTaskDialog
import com.example.questflow.presentation.components.RecurringConfig
import com.example.questflow.presentation.components.AdvancedTaskFilterDialog
import com.example.questflow.presentation.components.TaskHistorySection
import com.example.questflow.domain.model.getDisplayText
import com.example.questflow.domain.model.SortOption
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
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: TasksViewModel = hiltViewModel(),
    todayViewModel: TodayViewModel = hiltViewModel(),
    deepLinkTaskId: Long? = null
) {
    // Occupancy calculator for month view date picker
    val occupancyCalculator = remember { com.example.questflow.domain.usecase.DayOccupancyCalculator() }

    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }
    val globalStats by appViewModel.globalStats.collectAsState()
    val showFilterDialog by viewModel.showFilterDialog.collectAsState()
    val showAdvancedFilterDialog by viewModel.showAdvancedFilterDialog.collectAsState()
    val currentAdvancedFilter by viewModel.currentAdvancedFilter.collectAsState()
    val filterPresets by viewModel.filterPresets.collectAsState()
    val filterSettings by viewModel.filterSettings.collectAsState()
    val availableTasks by viewModel.getAvailableTasksFlow().collectAsState(initial = emptyList())
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedEditLink by remember { mutableStateOf<CalendarEventLinkEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var includeSubtasks by remember { mutableStateOf(false) }

    // Multi-Select State
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedTaskLinks by remember { mutableStateOf<Set<CalendarEventLinkEntity>>(emptySet()) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Display Settings Dialog State
    var showDisplaySettingsDialog by remember { mutableStateOf(false) }

    // Filter Info Expanded State
    var filterInfoExpanded by remember { mutableStateOf(false) }

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
            android.util.Log.d("TasksScreen", "Deep link received for taskId: $taskId")
            viewModel.openTaskFromDeepLink(taskId) { link ->
                android.util.Log.d("TasksScreen", "Callback received, link: ${link != null}")
                if (link != null) {
                    android.util.Log.d("TasksScreen", "Setting selectedEditLink to: ${link.title}")
                    selectedEditLink = link
                } else {
                    android.util.Log.w("TasksScreen", "No link returned from viewModel")
                }
            }
        }
    }

    // Load search filter settings to trigger re-search when they change
    val searchFilterSettings by viewModel.getSearchFilterSettings().collectAsState(
        initial = com.example.questflow.data.database.entity.TaskSearchFilterSettingsEntity()
    )

    // Load display settings to control what's shown in task cards
    val displaySettings by viewModel.getDisplaySettings().collectAsState(
        initial = com.example.questflow.data.database.entity.TaskDisplaySettingsEntity()
    )

    // Load layout configuration for 2-column display
    var layoutConfig by remember { mutableStateOf<List<com.example.questflow.domain.model.TaskDisplayElementConfig>>(emptyList()) }
    LaunchedEffect(displaySettings) {
        layoutConfig = viewModel.getLayoutConfig()
    }

    // Filter tasks based on search query with configurable filters
    // Returns TaskSearchResult with match information
    val filteredTasksWithMatches = if (searchQuery.isEmpty()) {
        uiState.tasks.map { com.example.questflow.domain.model.TaskSearchResult(it, emptyList()) }
    } else {
        // Use coroutine to call suspend function
        var searchResult by remember { mutableStateOf<List<com.example.questflow.domain.model.TaskSearchResult>>(emptyList()) }
        LaunchedEffect(searchQuery, uiState.tasks, searchFilterSettings) {
            searchResult = viewModel.searchTasksWithMatchInfo(uiState.tasks, searchQuery)
        }
        searchResult
    }

    // Apply advanced filter if active, otherwise use all links
    val filteredResult = uiState.filteredLinksResult
    val baseLinks = if (filteredResult != null) {
        android.util.Log.d("TasksScreen", "=== USING FILTERED RESULT ===")
        android.util.Log.d("TasksScreen", "Filtered count: ${filteredResult.filteredCount}/${filteredResult.totalCount}")
        android.util.Log.d("TasksScreen", "allTasks.size: ${filteredResult.allTasks.size}")
        filteredResult.allTasks
    } else {
        android.util.Log.d("TasksScreen", "=== NO FILTER ACTIVE, using all links ===")
        android.util.Log.d("TasksScreen", "uiState.links.size: ${uiState.links.size}")
        uiState.links
    }

    // Also apply text search on top of advanced filter
    // Need to check both link fields AND task fields (especially description)
    val filteredLinks = if (searchQuery.isEmpty()) {
        android.util.Log.d("TasksScreen", "No search query, using baseLinks: ${baseLinks.size}")
        baseLinks
    } else {
        // Create a map of taskId -> Task for quick lookup
        val taskMap = filteredTasksWithMatches.associateBy { it.task.id }

        val searchFiltered = baseLinks.filter { link ->
            // Check link title
            val matchesTitle = link.title.contains(searchQuery, ignoreCase = true)

            // Check category name
            val matchesCategory = link.categoryId != null &&
                categories.find { it.id == link.categoryId }?.name?.contains(searchQuery, ignoreCase = true) == true

            // Check task description (if link has a taskId)
            val matchesTaskDescription = link.taskId != null &&
                taskMap[link.taskId]?.task?.description?.contains(searchQuery, ignoreCase = true) == true

            // Check if the task itself was found by the advanced search
            val taskWasFound = link.taskId != null && taskMap.containsKey(link.taskId)

            matchesTitle || matchesCategory || matchesTaskDescription || taskWasFound
        }
        android.util.Log.d("TasksScreen", "Search filtered: ${searchFiltered.size}/${baseLinks.size}")
        searchFiltered
    }

    android.util.Log.d("TasksScreen", "=== FINAL filteredLinks.size: ${filteredLinks.size} ===")
    if (filteredLinks.isNotEmpty()) {
        android.util.Log.d("TasksScreen", "First 3 tasks: ${filteredLinks.take(3).map { it.title }}")
    }

    // CRITICAL: Filter AND SORT tasks based on filtered links
    // If advanced filter is active, only show tasks that have a corresponding filtered link
    // AND maintain the sort order from the filtered links
    val finalFilteredTasks = if (filteredResult != null) {
        android.util.Log.d("TasksScreen", "=== FILTERING & SORTING TASKS by filtered links ===")
        android.util.Log.d("TasksScreen", "Filtered links count: ${filteredLinks.size}")
        android.util.Log.d("TasksScreen", "Original filteredTasksWithMatches: ${filteredTasksWithMatches.size}")

        // Create a map of taskId -> TaskSearchResult for quick lookup
        val taskMap = filteredTasksWithMatches.associateBy { it.task.id }

        // Create ordered list following the link order (sorted by UseCase)
        val sortedTasks = filteredLinks.mapNotNull { link ->
            link.taskId?.let { taskId ->
                taskMap[taskId]
            }
        }

        android.util.Log.d("TasksScreen", "Tasks after link filter & sort: ${sortedTasks.size}")
        if (sortedTasks.isNotEmpty()) {
            android.util.Log.d("TasksScreen", "First 5 sorted task titles: ${sortedTasks.take(5).map { it.task.title }}")
        }
        sortedTasks
    } else {
        android.util.Log.d("TasksScreen", "No advanced filter, using all filteredTasksWithMatches: ${filteredTasksWithMatches.size}")
        filteredTasksWithMatches
    }

    // Extract just the tasks for backwards compatibility
    val filteredTasks = finalFilteredTasks.map { it.task }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                if (multiSelectMode) {
                    // Multi-Select: Two action buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Delete button
                        FloatingActionButton(
                            onClick = { showBatchDeleteDialog = true },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        }

                        // Edit button with count
                        ExtendedFloatingActionButton(
                            onClick = { showBatchEditDialog = true },
                            text = { Text("${selectedTaskLinks.size} bearbeiten") },
                            icon = { Icon(Icons.Default.Edit, contentDescription = "Bearbeiten") }
                        )
                    }
                } else {
                    // Normal: All action buttons in horizontal row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timeline button
                        SmallFloatingActionButton(
                            onClick = { navController.navigate("timeline") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Timeline",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Display Settings button
                        SmallFloatingActionButton(
                            onClick = { showDisplaySettingsDialog = true },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Anzeige",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Advanced Filter button with badge
                        SmallFloatingActionButton(
                            onClick = { viewModel.toggleAdvancedFilterDialog() },
                            containerColor = if (currentAdvancedFilter.isActive())
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (currentAdvancedFilter.isActive())
                                MaterialTheme.colorScheme.onTertiary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Filter",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Add Task FAB
                        FloatingActionButton(
                            onClick = { showAddTaskDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Auftrag erstellen")
                        }
                    }
                }
            },
            topBar = {
                if (multiSelectMode) {
                    // Multi-Select TopBar
                    TopAppBar(
                        title = { Text("${selectedTaskLinks.size} ausgewählt") },
                        navigationIcon = {
                            IconButton(onClick = {
                                multiSelectMode = false
                                selectedTaskLinks = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Exit Multi-Select")
                            }
                        },
                        actions = {
                            // Select All button
                            IconButton(onClick = {
                                selectedTaskLinks = if (selectedTaskLinks.size == filteredLinks.size) {
                                    emptySet()
                                } else {
                                    filteredLinks.toSet()
                                }
                            }) {
                                Icon(
                                    if (selectedTaskLinks.size == filteredLinks.size)
                                        Icons.Default.Clear
                                    else
                                        Icons.Default.CheckCircle,
                                    contentDescription = if (selectedTaskLinks.size == filteredLinks.size)
                                        "Deselect All"
                                    else
                                        "Select All"
                                )
                            }
                        }
                    )
                } else {
                    // Normal TopBar
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
                        previousXp = previousXp
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search bar with filter settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Suche...") },
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

                    // Filter settings button
                    var showSearchFilterDialog by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showSearchFilterDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Such-Filter Einstellungen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Filter settings dialog
                    if (showSearchFilterDialog) {
                        com.example.questflow.presentation.components.TaskSearchFilterDialog(
                            currentSettings = searchFilterSettings,
                            onDismiss = { showSearchFilterDialog = false },
                            onSettingsChange = { settings ->
                                viewModel.updateSearchFilterSettings(settings)
                            },
                            onResetToDefaults = {
                                viewModel.resetSearchFilterSettings()
                            }
                        )
                    }
                }

                // Show task count with filter/search info (collapsible)
                if (searchQuery.isNotEmpty() || filteredResult != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { filterInfoExpanded = !filterInfoExpanded },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Main count (always visible)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "${filteredTasks.size} ${if (filteredTasks.size == 1) "Task" else "Tasks"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Icon(
                                    if (filterInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (filterInfoExpanded) "Einklappen" else "Ausklappen",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // Expanded details
                            if (filterInfoExpanded) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                                )

                                // Show filter info if advanced filter is active
                                if (filteredResult != null) {
                                    Text(
                                        text = "Gefiltert: ${filteredResult.filteredCount}/${filteredResult.totalCount} Links",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )

                                    // Show active filter count
                                    val activeFilterCount = currentAdvancedFilter.getActiveFilterCount()
                                    if (activeFilterCount > 0) {
                                        Text(
                                            text = "$activeFilterCount ${if (activeFilterCount == 1) "Filter" else "Filter"} aktiv",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Show sort info
                                    if (currentAdvancedFilter.sortOptions.isNotEmpty() &&
                                        currentAdvancedFilter.sortOptions.first() != SortOption.DEFAULT) {
                                        Text(
                                            text = "Sortiert: ${currentAdvancedFilter.sortOptions.first().displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                // Show search info
                                if (searchQuery.isNotEmpty()) {
                                    Text(
                                        text = "Suche: \"$searchQuery\"",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isEmpty()) "Keine Aufgaben vorhanden" else "Keine Ergebnisse gefunden",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(finalFilteredTasks, key = { it.task.id }) { taskSearchResult ->
                    val task = taskSearchResult.task
                    val matchedFilters = taskSearchResult.matchedFilters

                    // Find corresponding calendar link if exists
                    val link = uiState.links.find { it.taskId == task.id }

                    val now = java.time.LocalDateTime.now()

                    // FIX P1-001: Use link.endsAt (if available) instead of task.dueDate for expiry check
                    // task.dueDate is the START time, not END time!
                    val isExpired = if (link != null) {
                        // Use calendar link's end time (correct!)
                        val expired = link.endsAt < now
                        android.util.Log.d("QuestFlow_TaskStatus", "=== TASK STATUS CHECK (with link) ===")
                        android.util.Log.d("QuestFlow_TaskStatus", "Task: ${task.title}")
                        android.util.Log.d("QuestFlow_TaskStatus", "Link Start: ${link.startsAt}")
                        android.util.Log.d("QuestFlow_TaskStatus", "Link End: ${link.endsAt}")
                        android.util.Log.d("QuestFlow_TaskStatus", "Current Time: $now")
                        android.util.Log.d("QuestFlow_TaskStatus", "Is Expired: $expired (endsAt < now)")
                        android.util.Log.d("QuestFlow_TaskStatus", "Link Status: ${link.status}")
                        expired
                    } else {
                        // Fallback: Use task.dueDate if no calendar link exists
                        val expired = task.dueDate?.let { it < now } ?: false
                        android.util.Log.d("QuestFlow_TaskStatus", "=== TASK STATUS CHECK (no link) ===")
                        android.util.Log.d("QuestFlow_TaskStatus", "Task: ${task.title}")
                        android.util.Log.d("QuestFlow_TaskStatus", "Task DueDate: ${task.dueDate}")
                        android.util.Log.d("QuestFlow_TaskStatus", "Current Time: $now")
                        android.util.Log.d("QuestFlow_TaskStatus", "Is Expired (fallback): $expired")
                        expired
                    }

                    val isClaimed = task.isCompleted

                    // Check if this task is a parent or subtask
                    val isParentTask = availableTasks.any { it.parentTaskId == task.id }
                    val isSubtask = task.parentTaskId != null
                    val parentTask = if (isSubtask) {
                        availableTasks.find { it.id == task.parentTaskId }
                    } else null

                    // Track if this task is selected in multi-select mode (using link if exists)
                    val isSelected = link?.let { selectedTaskLinks.contains(it) } ?: false

                    com.example.questflow.presentation.components.TaskCardV2(
                        task = task,
                        layoutConfig = layoutConfig,
                        matchedFilters = matchedFilters,
                        availableTasks = availableTasks,
                        categoriesMap = categoriesMap,
                        isExpired = isExpired,
                        isClaimed = isClaimed,
                        isSelected = isSelected,
                        searchQuery = searchQuery,
                        dateFormatter = dateFormatter,
                        onClaimClick = if (link != null && !link.rewarded && link.status != "CLAIMED") {
                            {
                                viewModel.claimXp(link.id) {
                                    appViewModel.refreshStats()
                                }
                            }
                        } else null,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (multiSelectMode) {
                                    // Toggle selection (only if link exists)
                                    link?.let {
                                        selectedTaskLinks = if (isSelected) {
                                            selectedTaskLinks - it
                                        } else {
                                            selectedTaskLinks + it
                                        }
                                    }
                                } else {
                                    // Normal: Open task for editing
                                    // Create temp link if not exists
                                    val editLink = link ?: CalendarEventLinkEntity(
                                        id = 0,
                                        calendarEventId = task.calendarEventId ?: 0,
                                        title = task.title,
                                        startsAt = task.dueDate ?: now,
                                        endsAt = (task.dueDate ?: now).plusHours(1),
                                        xp = task.xpReward,
                                        xpPercentage = task.xpPercentage ?: 60,
                                        categoryId = task.categoryId,
                                        taskId = task.id
                                    )
                                    selectedEditLink = editLink
                                }
                            },
                            onLongClick = {
                                if (!multiSelectMode && link != null) {
                                    // Enter multi-select mode and select this task (only if link exists)
                                    multiSelectMode = true
                                    selectedTaskLinks = setOf(link)
                                }
                            }
                        )
                    )

                    // Multi-select checkbox overlay
                    if (multiSelectMode) {
                        Box(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
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

        // Advanced Filter Dialog
        if (showAdvancedFilterDialog) {
            AdvancedTaskFilterDialog(
                currentFilter = currentAdvancedFilter,
                categories = categories,
                presets = filterPresets,
                onDismiss = {
                    viewModel.toggleAdvancedFilterDialog()
                },
                onApply = { filter ->
                    // Only apply filter, don't close dialog here
                    // Dialog closes itself via onDismiss
                    viewModel.applyAdvancedFilter(filter)
                },
                onSavePreset = { filter, name, desc ->
                    viewModel.saveFilterAsPreset(filter, name, desc)
                },
                onLoadPreset = { presetId ->
                    viewModel.loadFilterPreset(presetId)
                },
                onDeletePreset = { presetId ->
                    viewModel.deleteFilterPreset(presetId)
                }
            )
        }
    }

    // Layout Config Dialog (V2 - Advanced)
    if (showDisplaySettingsDialog) {
        com.example.questflow.presentation.components.TaskLayoutConfigDialog(
            layoutConfig = layoutConfig,
            onDismiss = { showDisplaySettingsDialog = false },
            onConfigChange = { config ->
                viewModel.updateLayoutConfig(config)
            },
            onResetToDefaults = {
                viewModel.resetLayoutToDefaults()
            }
        )
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
            isCalendarMode = true,  // Enable calendar-specific features
            occupancyCalculator = occupancyCalculator
        )
    }

    // Edit Task Dialog - works with or without taskId
    // CRITICAL FIX: Use key() to force dialog recreation when navigating between tasks
    selectedEditLink?.let { link ->
        key(link.id, link.taskId) {
            EditCalendarTaskDialog(
                calendarLink = link,
                viewModel = todayViewModel,
                tasksViewModel = viewModel,
                onDismiss = {
                    selectedEditLink = null
                    viewModel.loadCalendarLinks()
                },
                onNavigateToTask = { newLink ->
                    selectedEditLink = newLink
                },
                occupancyCalculator = occupancyCalculator
            )
        }
    }

    // Batch Delete Dialog with confirmation
    if (showBatchDeleteDialog && selectedTaskLinks.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("${selectedTaskLinks.size} Tasks löschen?") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Möchtest du wirklich ${selectedTaskLinks.size} Tasks löschen?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Diese Aktion kann nicht rückgängig gemacht werden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Delete all selected tasks via ViewModel
                        val taskIds = selectedTaskLinks.mapNotNull { it.taskId }
                        android.util.Log.d("BatchDelete", "Deleting ${taskIds.size} tasks: $taskIds")

                        viewModel.deleteTasks(taskIds) {
                            android.util.Log.d("BatchDelete", "Batch delete completed successfully")
                        }

                        // Exit multi-select mode
                        multiSelectMode = false
                        selectedTaskLinks = emptySet()
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Batch Edit Dialog - Opens the normal edit dialog with multiple tasks
    if (showBatchEditDialog && selectedTaskLinks.isNotEmpty()) {
        // Use the normal EditCalendarTaskDialog but in multi-edit mode
        EditCalendarTaskDialog(
            calendarLink = selectedTaskLinks.first(), // Use first as reference
            viewModel = todayViewModel,
            tasksViewModel = viewModel,
            onDismiss = {
                showBatchEditDialog = false
                multiSelectMode = false
                selectedTaskLinks = emptySet()
                viewModel.loadCalendarLinks()
            },
            onNavigateToTask = { }, // Disabled in batch mode
            batchEditLinks = selectedTaskLinks.toList(), // Pass all selected tasks
            occupancyCalculator = occupancyCalculator
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCalendarTaskDialog(
    calendarLink: CalendarEventLinkEntity,
    viewModel: TodayViewModel,
    tasksViewModel: TasksViewModel,
    onDismiss: () -> Unit,
    onNavigateToTask: (CalendarEventLinkEntity) -> Unit = {},
    batchEditLinks: List<CalendarEventLinkEntity>? = null, // NULL = single edit, NON-NULL = batch edit
    occupancyCalculator: com.example.questflow.domain.usecase.DayOccupancyCalculator? = null
) {
    val availableTasks by tasksViewModel.getAvailableTasksFlow().collectAsState(initial = emptyList())
    val context = LocalContext.current

    // Calendar events for month view occupancy (get 3 months range)
    val currentMonth = remember { java.time.YearMonth.now() }
    val monthViewEvents by viewModel.getCalendarEventsInRange(
        startDate = currentMonth.minusMonths(1).atDay(1),
        endDate = currentMonth.plusMonths(2).atDay(1)
    ).collectAsState(initial = emptyList())

    // Tasks for month view occupancy (get 3 months range)
    val monthViewTasks by viewModel.getTasksInRange(
        startDate = currentMonth.minusMonths(1).atDay(1),
        endDate = currentMonth.plusMonths(2).atDay(1)
    ).collectAsState(initial = emptyList())

    // Batch edit mode detection
    val isBatchEdit = batchEditLinks != null && batchEditLinks.size > 1
    val allLinks = batchEditLinks ?: listOf(calendarLink)

    // Auto-Save Feature: Snapshot initial state for reset functionality
    data class TaskSnapshot(
        val title: String,
        val description: String,
        val xpPercentage: Int,
        val categoryId: Long?,
        val startDateTime: LocalDateTime,
        val endDateTime: LocalDateTime,
        val addToCalendar: Boolean,
        val deleteOnClaim: Boolean,
        val deleteOnExpiry: Boolean,
        val isRecurring: Boolean,
        val recurringConfig: RecurringConfig,
        val parentTaskId: Long?,
        val autoCompleteParent: Boolean,
        val shouldReactivate: Boolean
    )

    // Helper: Find common value across all tasks, or null if different
    fun <T> findCommonValue(selector: (CalendarEventLinkEntity) -> T): T? {
        if (!isBatchEdit) return selector(calendarLink)
        val values = allLinks.map(selector).distinct()
        return if (values.size == 1) values.first() else null
    }

    fun <T> findCommonTaskValue(selector: (com.example.questflow.domain.model.Task) -> T): T? {
        if (!isBatchEdit) {
            val task = availableTasks.find { it.id == calendarLink.taskId }
            val result = task?.let(selector)
            android.util.Log.d("TaskDialog-Description", "🔎 findCommonTaskValue: taskId=${calendarLink.taskId}, task found=${task != null}, description='${if (selector == com.example.questflow.domain.model.Task::description) result else "N/A"}'")
            return result
        }
        val taskValues = allLinks.mapNotNull { link ->
            availableTasks.find { it.id == link.taskId }?.let(selector)
        }.distinct()
        return if (taskValues.size == 1) taskValues.first() else null
    }

    // Load current task data
    val currentTaskData = remember(availableTasks, calendarLink.taskId) {
        availableTasks.find { it.id == calendarLink.taskId }
    }

    // Calculate common values across all selected tasks
    val commonTitle = remember(allLinks) { findCommonValue { it.title } }
    val commonDescription = remember(allLinks, availableTasks) { findCommonTaskValue { it.description } }
    val commonPercentage = remember(allLinks) { findCommonValue { it.xpPercentage } }
    val commonCategoryId = remember(allLinks) { findCommonValue { it.categoryId } }
    val commonStartDateTime = remember(allLinks) { findCommonValue { it.startsAt } }
    val commonEndDateTime = remember(allLinks) { findCommonValue { it.endsAt } }
    val commonDeleteOnClaim = remember(allLinks) { findCommonValue { it.deleteOnClaim } }
    val commonDeleteOnExpiry = remember(allLinks) { findCommonValue { it.deleteOnExpiry } }
    val commonParentTaskId = remember(allLinks, availableTasks) { findCommonTaskValue { it.parentTaskId } }
    val commonAutoCompleteParent = remember(allLinks, availableTasks) { findCommonTaskValue { it.autoCompleteParent } }

    // Recurring task common values
    val commonIsRecurring = remember(allLinks) { findCommonValue { it.isRecurring } }
    val commonRecurringConfig = remember(allLinks, availableTasks) {
        if (!isBatchEdit) null
        else {
            // Get all tasks' recurring configs
            val configs = allLinks.mapNotNull { link ->
                availableTasks.find { it.id == link.taskId }?.let { task ->
                    taskToRecurringConfig(task)
                }
            }
            // Check if all configs are identical
            if (configs.distinct().size == 1) configs.firstOrNull() else null
        }
    }

    // IMPORTANT: initialSnapshot should be created ONCE when dialog opens
    // Remove currentTaskData from remember keys to prevent reset on task updates
    val initialSnapshot = remember(calendarLink.id, isBatchEdit) {
        TaskSnapshot(
            title = commonTitle ?: "",
            description = commonDescription ?: "",
            xpPercentage = commonPercentage ?: 60,
            categoryId = commonCategoryId,
            startDateTime = commonStartDateTime ?: calendarLink.startsAt,
            endDateTime = commonEndDateTime ?: calendarLink.endsAt,
            addToCalendar = calendarLink.calendarEventId != 0L || calendarLink.deleteOnClaim || calendarLink.deleteOnExpiry,
            deleteOnClaim = commonDeleteOnClaim ?: false,
            deleteOnExpiry = commonDeleteOnExpiry ?: false,
            isRecurring = calendarLink.isRecurring,
            recurringConfig = com.example.questflow.presentation.components.RecurringConfig(),  // Not used - dummy value
            parentTaskId = commonParentTaskId,
            autoCompleteParent = commonAutoCompleteParent ?: false,
            shouldReactivate = false
        )
    }

    // Track initial values to detect changes - STABLE keys to prevent reset on recomposition
    var taskTitle by remember(calendarLink.id, isBatchEdit) { mutableStateOf(commonTitle ?: "") }

    // Keep task description stable - Initialize from task, but persist user edits
    // The description is stored in the Task entity, so we need to wait for it to load
    var taskDescription by remember(calendarLink.id, isBatchEdit) { mutableStateOf("") }

    var selectedPercentage by remember(calendarLink.id, isBatchEdit) { mutableStateOf(commonPercentage ?: 60) }
    val categories by viewModel.categories.collectAsState()
    var taskCategory by remember(categories, isBatchEdit) {
        mutableStateOf(
            if (isBatchEdit) {
                commonCategoryId?.let { id -> categories.find { it.id == id } }
            } else {
                categories.find { it.id == calendarLink.categoryId }
            }
        )
    }
    var shouldReactivate by remember(calendarLink.id, isBatchEdit) { mutableStateOf(false) }

    // Contact selection state
    val availableContacts by viewModel.contacts.collectAsState()
    var selectedContactIds by remember(calendarLink.id, isBatchEdit) { mutableStateOf<Set<Long>>(emptySet()) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }

    // Media Library support for contact photos
    val mediaLibraryViewModel: com.example.questflow.presentation.screens.medialibrary.MediaLibraryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val allMedia by mediaLibraryViewModel.getAllMedia().collectAsState(initial = emptyList())

    // Fullscreen selection dialogs state
    var showCategorySelectionDialog by remember { mutableStateOf(false) }
    var showParentSelectionDialog by remember { mutableStateOf(false) }
    var showCreateSubTaskDialog by remember { mutableStateOf(false) }

    // Template and Placeholder dialogs for Edit mode
    var showTaskTitleTemplateDialog by remember { mutableStateOf(false) }
    var showTaskDescriptionTemplateDialog by remember { mutableStateOf(false) }
    var showTaskTitlePlaceholderDialog by remember { mutableStateOf(false) }
    var showTaskDescriptionPlaceholderDialog by remember { mutableStateOf(false) }

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
            taskContactTagsMap = tasksViewModel.loadTaskContactTags(taskId)
            android.util.Log.d("TaskTags", "LOADED task-tags from DB: $taskContactTagsMap")
        }
    }

    // Calendar integration options - initialize once per dialog instance
    // WICHTIG: Keine calendarLink.deleteOnClaim/deleteOnExpiry als Keys verwenden!
    // Sonst wird State zurückgesetzt wenn DB sich durch Background-Updates ändert
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsState()

    // addToCalendar: Wenn irgendeine Kalender-Option gesetzt ist ODER Event existiert
    // → User-Intention, nicht aktueller Event-Status!
    var addToCalendar by remember(calendarLink.id, isBatchEdit) {
        mutableStateOf(
            if (isBatchEdit) {
                // Batch: addToCalendar wenn IRGENDEINE Task das hat
                allLinks.any { it.calendarEventId != 0L || it.deleteOnClaim || it.deleteOnExpiry }
            } else {
                calendarLink.calendarEventId != 0L ||
                calendarLink.deleteOnClaim ||
                calendarLink.deleteOnExpiry
            }
        )
    }
    var deleteOnClaim by remember(calendarLink.id, isBatchEdit) {
        mutableStateOf(commonDeleteOnClaim ?: false)
    }
    var deleteOnExpiry by remember(calendarLink.id, isBatchEdit) {
        mutableStateOf(commonDeleteOnExpiry ?: false)
    }

    // Subtask options - MOVED UP to be available for recurringConfig
    // IMPORTANT: Use derived state instead of remember() so it updates when availableTasks changes!
    val currentTask = availableTasks.find { it.id == calendarLink.taskId }

    // Recurring task options - Load from currentTask or common value in batch mode
    var isRecurring by remember(calendarLink.id, isBatchEdit) {
        mutableStateOf(
            if (isBatchEdit) (commonIsRecurring ?: false)
            else calendarLink.isRecurring
        )
    }
    // Initialize recurringConfig ONCE when dialog opens, don't reset on task updates
    val initialRecurringConfig = remember(calendarLink.id, isBatchEdit) {
        if (isBatchEdit) {
            commonRecurringConfig ?: com.example.questflow.presentation.components.RecurringConfig()
        } else {
            currentTask?.let { taskToRecurringConfig(it) }
                ?: com.example.questflow.presentation.components.RecurringConfig()
        }
    }
    var recurringConfig by remember(calendarLink.id, isBatchEdit) {
        mutableStateOf(initialRecurringConfig)
    }


    // Task History - Collect history for the task
    val taskHistory by remember(calendarLink.taskId) {
        if (calendarLink.taskId != null) {
            viewModel.getTaskHistory(calendarLink.taskId!!)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    // Track if initial data has been loaded to prevent premature Auto-Save
    var isDataLoaded by remember(calendarLink.id, isBatchEdit) { mutableStateOf(false) }

    // Update recurringConfig and description when currentTask loads (race condition fix)
    // LOAD DIRECTLY FROM DATABASE - Don't rely on availableTasks Flow!
    LaunchedEffect(calendarLink.id, isBatchEdit) {
        android.util.Log.d("TaskDialog-Description", "🚀 LaunchedEffect START: isBatchEdit=$isBatchEdit, taskId=${calendarLink.taskId}, linkId=${calendarLink.id}")
        // Load task data DIRECTLY from repository in single mode
        if (!isBatchEdit && calendarLink.taskId != null) {
            android.util.Log.d("TaskDialog-Description", "🔍 Loading task DIRECTLY from DB: taskId=${calendarLink.taskId}")
            val loadedTask = tasksViewModel.findTaskById(calendarLink.taskId)
            android.util.Log.d("TaskDialog-Description", "📖 Task loaded from DB: ${if (loadedTask == null) "NULL" else "present (id=${loadedTask.id})"}")

            if (loadedTask != null) {
                recurringConfig = taskToRecurringConfig(loadedTask)
                // Load description from task - ALWAYS set it, even if empty!
                val loadedDescription = loadedTask.description
                android.util.Log.d("TaskDialog-Description", "🔤 Loading description from task: '$loadedDescription' for linkId=${calendarLink.id}")
                taskDescription = loadedDescription  // Set description ALWAYS (even if empty) to prevent Auto-Save from overwriting!
                android.util.Log.d("TaskDialog-Description", "✅ Description set to: '$taskDescription'")
            } else {
                android.util.Log.d("TaskDialog-Description", "❌ Task is NULL after DB load, can't load description")
            }
        } else {
            android.util.Log.d("TaskDialog-Description", "⏭️ Batch edit mode or no taskId, skipping description load")
        }
        isDataLoaded = true
    }

    var showRecurringDialog by remember { mutableStateOf(false) }
    var selectedParentTask by remember(calendarLink.id, isBatchEdit) {
        mutableStateOf<com.example.questflow.domain.model.Task?>(null)
    }
    var autoCompleteParent by remember(calendarLink.id, isBatchEdit) {
        mutableStateOf(false)
    }

    // CRITICAL FIX: Load parent task and autoCompleteParent when task data is available
    LaunchedEffect(currentTask, availableTasks) {
        if (!isBatchEdit && currentTask != null) {
            android.util.Log.d("TaskDialog-Parent", "🔍 Loading parent task: parentTaskId=${currentTask.parentTaskId}")
            selectedParentTask = currentTask.parentTaskId?.let { parentId ->
                availableTasks.find { it.id == parentId }
            }
            autoCompleteParent = currentTask.autoCompleteParent
            android.util.Log.d("TaskDialog-Parent", "✅ Parent task loaded: ${selectedParentTask?.title}, autoComplete=$autoCompleteParent")
        }
    }

    // Task family section collapse state - persistent
    val uiSettings by tasksViewModel.uiSettings.collectAsState()

    // Date and time state - using QuickDateTimePicker-friendly state
    var startDateTime by remember(isBatchEdit) {
        mutableStateOf(commonStartDateTime ?: calendarLink.startsAt)
    }
    var endDateTime by remember(isBatchEdit) {
        mutableStateOf(commonEndDateTime ?: calendarLink.endsAt)
    }

    // DateTime Validation: End must be >= Start
    LaunchedEffect(startDateTime, endDateTime) {
        if (endDateTime < startDateTime) {
            // Auto-correct: set end to start + 1 hour
            endDateTime = startDateTime.plusHours(1)
            android.util.Log.d("TaskDialog", "Auto-corrected endDateTime: end was before start")
        }
    }

    // Smart scheduling state
    val coroutineScope = rememberCoroutineScope()
    var showTimeSlotDialog by remember { mutableStateOf(false) }
    var scheduleConflicts by remember { mutableStateOf<List<com.example.questflow.data.calendar.CalendarEvent>>(emptyList()) }
    var dailyFreeTime by remember { mutableStateOf<List<com.example.questflow.domain.usecase.FindFreeTimeSlotsUseCase.DailyFreeTime>>(emptyList()) }
    var timeSlotSuggestions by remember { mutableStateOf<List<com.example.questflow.domain.usecase.FindFreeTimeSlotsUseCase.FreeSlot>>(emptyList()) }

    // Check for conflicts whenever date/time changes (exclude current event)
    LaunchedEffect(startDateTime, endDateTime) {
        val hasPermission = viewModel.hasCalendarPermission.value
        if (hasPermission) {
            scheduleConflicts = viewModel.checkScheduleConflicts(
                startTime = startDateTime,
                endTime = endDateTime,
                excludeEventId = calendarLink.calendarEventId
            )
        }
    }

    // Track whether task is claimable - also depends on deleteOnClaim setting
    val isClaimable = deleteOnClaim && !calendarLink.rewarded && calendarLink.status != "CLAIMED" && calendarLink.status != "EXPIRED"

    // Debug: Track claim button visibility
    LaunchedEffect(isClaimable, deleteOnClaim, calendarLink.rewarded, calendarLink.status) {
        android.util.Log.d("TaskDialog-ClaimBtn", "🔘 Claim button visibility: isClaimable=$isClaimable, deleteOnClaim=$deleteOnClaim, rewarded=${calendarLink.rewarded}, status=${calendarLink.status}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isBatchEdit) "${allLinks.size} Tasks bearbeiten" else "Aufgabe bearbeiten")
                if (isClaimable && !isBatchEdit) {
                    Button(
                        onClick = {
                            android.util.Log.d("QuickClaim", "Quick claim for linkId: ${calendarLink.id}")
                            tasksViewModel.claimXp(calendarLink.id) {
                                android.util.Log.d("QuickClaim", "Claim successful")
                                onDismiss()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Claim XP", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        text = {
            // Auto-Save: Save changes to ALL selected tasks in batch mode
            LaunchedEffect(
                taskTitle, taskDescription, selectedPercentage, taskCategory,
                startDateTime, endDateTime, addToCalendar, deleteOnClaim,
                deleteOnExpiry, isRecurring, recurringConfig, selectedParentTask,
                autoCompleteParent, shouldReactivate, isDataLoaded
            ) {
                android.util.Log.d("TaskDialog-AutoSave", "🔄 Auto-Save TRIGGERED: isDataLoaded=$isDataLoaded")
                android.util.Log.d("TaskDialog-AutoSave", "🔄 Current values: startDateTime=$startDateTime, endDateTime=$endDateTime")

                // Don't auto-save until initial data is loaded (prevents race condition)
                if (!isDataLoaded) {
                    android.util.Log.d("TaskDialog-AutoSave", "⏸️ Auto-Save BLOCKED: isDataLoaded=false")
                    return@LaunchedEffect
                }
                android.util.Log.d("TaskDialog-AutoSave", "▶️ Auto-Save READY: isDataLoaded=true, taskDescription='$taskDescription'")

                kotlinx.coroutines.delay(500) // Debounce

                if (isBatchEdit) {
                    // Batch Edit Mode: Apply ONLY changed fields to all selected tasks

                    // Detect which fields have changed from initial/common values
                    val titleChanged = taskTitle != (commonTitle ?: "")
                    val descriptionChanged = taskDescription != (commonDescription ?: "")
                    val percentageChanged = selectedPercentage != (commonPercentage ?: 60)
                    val categoryChanged = taskCategory?.id != commonCategoryId
                    val startDateTimeChanged = startDateTime != (commonStartDateTime ?: calendarLink.startsAt)
                    val endDateTimeChanged = endDateTime != (commonEndDateTime ?: calendarLink.endsAt)

                    // Calendar options change detection
                    val initialAddToCalendar = allLinks.any { it.calendarEventId != 0L || it.deleteOnClaim || it.deleteOnExpiry }
                    val addToCalendarChanged = addToCalendar != initialAddToCalendar
                    val deleteOnClaimChanged = deleteOnClaim != (commonDeleteOnClaim ?: false)
                    val deleteOnExpiryChanged = deleteOnExpiry != (commonDeleteOnExpiry ?: false)

                    val parentTaskChanged = selectedParentTask?.id != commonParentTaskId
                    val autoCompleteParentChanged = autoCompleteParent != (commonAutoCompleteParent ?: false)

                    // shouldReactivate: User must explicitly enable it (initial is false)
                    val shouldReactivateChanged = shouldReactivate == true

                    // Recurring task change detection
                    val isRecurringChanged = isRecurring != (commonIsRecurring ?: false)
                    val recurringConfigChanged = recurringConfig != commonRecurringConfig

                    allLinks.forEach { link ->
                        link.taskId?.let { taskId ->
                            val linkTask = availableTasks.find { it.id == taskId }

                            // Use changed values OR preserve original values
                            val linkAddToCalendar = link.calendarEventId != 0L || link.deleteOnClaim || link.deleteOnExpiry

                            tasksViewModel.updateCalendarTask(
                                linkId = link.id,
                                taskId = taskId,
                                title = if (titleChanged) taskTitle else link.title,
                                description = if (descriptionChanged) taskDescription else (linkTask?.description ?: ""),
                                xpPercentage = if (percentageChanged) selectedPercentage else link.xpPercentage,
                                startDateTime = if (startDateTimeChanged) startDateTime else link.startsAt,
                                endDateTime = if (endDateTimeChanged) endDateTime else link.endsAt,
                                categoryId = if (categoryChanged) taskCategory?.id else link.categoryId,
                                shouldReactivate = if (shouldReactivateChanged) shouldReactivate else false,
                                addToCalendar = if (addToCalendarChanged) addToCalendar else linkAddToCalendar,
                                deleteOnClaim = if (deleteOnClaimChanged) deleteOnClaim else link.deleteOnClaim,
                                deleteOnExpiry = if (deleteOnExpiryChanged) deleteOnExpiry else link.deleteOnExpiry,
                                isRecurring = if (isRecurringChanged) isRecurring else link.isRecurring,
                                recurringConfig = if (recurringConfigChanged) recurringConfig else null,
                                parentTaskId = if (parentTaskChanged) selectedParentTask?.id else linkTask?.parentTaskId,
                                autoCompleteParent = if (autoCompleteParentChanged) autoCompleteParent else (linkTask?.autoCompleteParent ?: false)
                            )
                        }
                    }
                } else if (taskTitle.isNotBlank()) {
                    // Single Edit Mode
                    android.util.Log.d("TaskDialog-AutoSave", "🔍 Saving description: '$taskDescription' (length=${taskDescription.length})")
                    android.util.Log.d("DescriptionFlow-UI", "📤 SENDING to ViewModel: taskId=${calendarLink.taskId}, linkId=${calendarLink.id}, description='$taskDescription'")
                    tasksViewModel.updateCalendarTask(
                        linkId = calendarLink.id,
                        taskId = calendarLink.taskId,
                        title = taskTitle,
                        description = taskDescription,
                        xpPercentage = selectedPercentage,
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        categoryId = taskCategory?.id,
                        shouldReactivate = shouldReactivate && (calendarLink.rewarded || calendarLink.status == "CLAIMED"),
                        addToCalendar = addToCalendar,
                        deleteOnClaim = deleteOnClaim,
                        deleteOnExpiry = deleteOnExpiry,
                        isRecurring = isRecurring,
                        recurringConfig = if (isRecurring) recurringConfig else null,
                        parentTaskId = selectedParentTask?.id,
                        autoCompleteParent = autoCompleteParent
                    )
                }
            }

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
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (textTemplates.isEmpty()) "Keine Templates verfügbar" else "Textbaustein auswählen")
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
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
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (textTemplates.isEmpty()) "Keine Templates verfügbar" else "Textbaustein auswählen")
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = taskDescription,
                            onValueChange = {
                                android.util.Log.d("TaskDialog-Description", "✏️ User editing description: '$it' (length=${it.length})")
                                taskDescription = it
                            },
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Übergeordneter Task (optional):", style = MaterialTheme.typography.labelMedium)
                                // + Button to create new sub-task with current task as parent
                                FilledTonalIconButton(
                                    onClick = {
                                        android.util.Log.d("SubTask", "Opening create sub-task dialog with parent: ${calendarLink.taskId}")
                                        showCreateSubTaskDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Sub-Task erstellen",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
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
                                            val parentLink = tasksViewModel.uiState.value.links.find { it.taskId == selectedParentTask?.id }
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
                                            tasksViewModel.updateUISettings(
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
                                    val parentLink = tasksViewModel.uiState.value.links.find { it.taskId == parentTask.id }
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
                                    val subtaskLink = tasksViewModel.uiState.value.links.find { it.taskId == subtask.id }
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

                    // Date and Time Selection - START with inline +/- controls
                    item {
                        com.example.questflow.presentation.components.CompactDateTimeSection(
                            label = "Start",
                            dateTime = startDateTime,
                            onDateTimeChange = { startDateTime = it },
                            dayIncrement = uiSettings.startDayIncrement,
                            minuteIncrement = uiSettings.startMinuteIncrement,
                            onDayIncrementChange = { newValue ->
                                tasksViewModel.updateUISettings(uiSettings.copy(startDayIncrement = newValue))
                            },
                            onMinuteIncrementChange = { newValue ->
                                tasksViewModel.updateUISettings(uiSettings.copy(startMinuteIncrement = newValue))
                            },
                            events = monthViewEvents,
                            occupancyCalculator = occupancyCalculator,
                            categoryColor = taskCategory?.let { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it.color)) },
                            tasks = monthViewTasks,
                            currentTaskId = currentTask?.id,
                            currentCategoryId = taskCategory?.id
                        )
                    }

                    // Duration Row - between start and end
                    item {
                        com.example.questflow.presentation.components.DurationRow(
                            startDateTime = startDateTime,
                            endDateTime = endDateTime,
                            onEndDateTimeChange = { endDateTime = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Date and Time Selection - END with inline +/- controls
                    item {
                        com.example.questflow.presentation.components.CompactDateTimeSection(
                            label = "Ende",
                            dateTime = endDateTime,
                            onDateTimeChange = { endDateTime = it },
                            dayIncrement = uiSettings.endDayIncrement,
                            minuteIncrement = uiSettings.endMinuteIncrement,
                            onDayIncrementChange = { newValue ->
                                tasksViewModel.updateUISettings(uiSettings.copy(endDayIncrement = newValue))
                            },
                            onMinuteIncrementChange = { newValue ->
                                tasksViewModel.updateUISettings(uiSettings.copy(endMinuteIncrement = newValue))
                            },
                            events = monthViewEvents,
                            occupancyCalculator = occupancyCalculator,
                            categoryColor = taskCategory?.let { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it.color)) },
                            tasks = monthViewTasks,
                            currentTaskId = currentTask?.id,
                            currentCategoryId = taskCategory?.id
                        )
                    }

                    // Conflict warning and smart scheduling
                    if (scheduleConflicts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
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
                                                    minDurationMinutes = durationMinutes,
                                                    excludeEventId = calendarLink.calendarEventId
                                                )

                                                // Get suggestions
                                                timeSlotSuggestions = viewModel.suggestTimeSlots(
                                                    requiredDurationMinutes = durationMinutes,
                                                    startSearchFrom = startDateTime,
                                                    maxSuggestions = 5,
                                                    excludeEventId = calendarLink.calendarEventId
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
                                            android.util.Log.d("TasksScreen", "=== AKTIONEN BUTTON CLICKED ===")
                                            android.util.Log.d("TasksScreen", "selectedContactIds: $selectedContactIds")
                                            android.util.Log.d("TasksScreen", "taskId: ${calendarLink.taskId}")
                                            android.util.Log.d("TasksScreen", "Setting showActionDialog = true")
                                            showActionDialog = true
                                            android.util.Log.d("TasksScreen", "showActionDialog is now: $showActionDialog")
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

                            // Show selected contacts with tags - COLLAPSIBLE
                            if (selectedContactIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Collapsible Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tasksViewModel.updateUISettings(
                                                uiSettings.copy(taskContactListExpanded = !uiSettings.taskContactListExpanded)
                                            )
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Verknüpfte Kontakte (${selectedContactIds.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        if (uiSettings.taskContactListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (uiSettings.taskContactListExpanded) "Zuklappen" else "Aufklappen",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Contact List - only show when expanded
                                if (uiSettings.taskContactListExpanded) {
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
                                                    // Photo from Media Library or fallback to photoUri
                                                    val photoMedia = contact.photoMediaId?.let { mediaId ->
                                                        allMedia.find { media -> media.id == mediaId }
                                                    }

                                                    if (photoMedia != null) {
                                                        coil.compose.AsyncImage(
                                                            model = java.io.File(photoMedia.filePath),
                                                            contentDescription = "Kontaktfoto",
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    } else if (contact.photoUri != null) {
                                                        // Fallback für alte photoUri
                                                        coil.compose.AsyncImage(
                                                            model = android.net.Uri.parse(contact.photoUri),
                                                            contentDescription = "Kontaktfoto",
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Emoji icon if present
                                                            contact.iconEmoji?.let { emoji ->
                                                                if (emoji.isNotBlank()) {
                                                                    Text(
                                                                        text = emoji,
                                                                        style = MaterialTheme.typography.titleMedium
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                contact.displayName,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
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

                    // Task History Section
                    if (calendarLink.taskId != null && taskHistory.isNotEmpty()) {
                        item {
                            TaskHistorySection(
                                taskHistory = taskHistory,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Delete Task Button
                    if (calendarLink.taskId != null) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                        }

                        item {
                            var showDeleteDialog by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Löschen",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Task löschen")
                            }

                            // Confirmation Dialog
                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("Task löschen?") },
                                    text = {
                                        Text("Möchtest du \"$taskTitle\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                // Delete task via ViewModel
                                                calendarLink.taskId?.let { taskId ->
                                                    android.util.Log.d("TaskDelete", "Deleting task: $taskId")
                                                    tasksViewModel.deleteTask(taskId) {
                                                        android.util.Log.d("TaskDelete", "Task deleted successfully")
                                                    }
                                                }
                                                showDeleteDialog = false
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
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text("Abbrechen")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Changed from "Speichern" to "Schließen" since auto-save is active
            TextButton(onClick = {
                // Update contact links and task-tags if task has an ID
                calendarLink.taskId?.let { taskId ->
                    viewModel.saveTaskContactLinks(taskId, selectedContactIds)
                    android.util.Log.d("TaskTags", "FINAL SAVE on dialog close - task $taskId: $taskContactTagsMap")
                    tasksViewModel.saveTaskContactTags(taskId, taskContactTagsMap)
                    android.util.Log.d("TaskTags", "FINAL SAVE completed")
                }
                onDismiss()
            }) {
                Text("Schließen")
            }
        },
        dismissButton = {
            // Changed from "Abbrechen" to "Zurücksetzen" to restore initial state
            TextButton(
                onClick = {
                    taskTitle = initialSnapshot.title
                    taskDescription = initialSnapshot.description
                    selectedPercentage = initialSnapshot.xpPercentage
                    taskCategory = categories.find { it.id == initialSnapshot.categoryId }
                    startDateTime = initialSnapshot.startDateTime
                    endDateTime = initialSnapshot.endDateTime
                    addToCalendar = initialSnapshot.addToCalendar
                    deleteOnClaim = initialSnapshot.deleteOnClaim
                    deleteOnExpiry = initialSnapshot.deleteOnExpiry
                    isRecurring = initialSnapshot.isRecurring
                    recurringConfig = initialRecurringConfig  // Use initialRecurringConfig directly instead of from snapshot
                    selectedParentTask = availableTasks.find { it.id == initialSnapshot.parentTaskId }
                    autoCompleteParent = initialSnapshot.autoCompleteParent
                    shouldReactivate = initialSnapshot.shouldReactivate
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Zurücksetzen")
            }
        }
    )

    // Template Selection Dialogs with IMMEDIATE RESOLUTION
    if (showTaskTitleTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTaskTitleTemplateDialog = false },
            title = { Text("Textbaustein für Titel wählen") },
            text = {
                LazyColumn {
                    items(textTemplates.size) { index ->
                        val template = textTemplates[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                coroutineScope.launch {
                                    val templateText = template.subject ?: template.content
                                    // Resolve placeholders IMMEDIATELY
                                    val resolved = if (templateText.contains("{") && calendarLink.taskId != null && selectedContactIds.isNotEmpty()) {
                                        viewModel.placeholderResolver.resolve(templateText, calendarLink.taskId!!, selectedContactIds.first())
                                    } else {
                                        templateText
                                    }
                                    taskTitle = resolved
                                }
                                showTaskTitleTemplateDialog = false
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
                                coroutineScope.launch {
                                    val templateText = template.content
                                    // Resolve placeholders IMMEDIATELY
                                    val resolved = if (templateText.contains("{") && calendarLink.taskId != null && selectedContactIds.isNotEmpty()) {
                                        viewModel.placeholderResolver.resolve(templateText, calendarLink.taskId!!, selectedContactIds.first())
                                    } else {
                                        templateText
                                    }
                                    taskDescription = resolved
                                }
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

    // Placeholder Dialogs with IMMEDIATE RESOLUTION
    if (showTaskTitlePlaceholderDialog) {
        com.example.questflow.presentation.components.PlaceholderSelectorDialog(
            onDismiss = { showTaskTitlePlaceholderDialog = false },
            onPlaceholderSelected = { placeholder ->
                coroutineScope.launch {
                    // Resolve placeholder IMMEDIATELY
                    val resolved = if (calendarLink.taskId != null && selectedContactIds.isNotEmpty()) {
                        viewModel.placeholderResolver.resolve(placeholder, calendarLink.taskId!!, selectedContactIds.first())
                    } else {
                        placeholder
                    }
                    taskTitle = taskTitle + resolved
                }
                showTaskTitlePlaceholderDialog = false
            }
        )
    }

    if (showTaskDescriptionPlaceholderDialog) {
        com.example.questflow.presentation.components.PlaceholderSelectorDialog(
            onDismiss = { showTaskDescriptionPlaceholderDialog = false },
            onPlaceholderSelected = { placeholder ->
                coroutineScope.launch {
                    // Resolve placeholder IMMEDIATELY
                    val resolved = if (calendarLink.taskId != null && selectedContactIds.isNotEmpty()) {
                        viewModel.placeholderResolver.resolve(placeholder, calendarLink.taskId!!, selectedContactIds.first())
                    } else {
                        placeholder
                    }
                    taskDescription = taskDescription + resolved
                }
                showTaskDescriptionPlaceholderDialog = false
            }
        )
    }

    // Show recurring configuration dialog
    if (showRecurringDialog) {
        com.example.questflow.presentation.components.RecurringConfigDialog(
            initialConfig = recurringConfig,
            minuteIncrement = uiSettings.recurringTimeMinuteIncrement,
            onMinuteIncrementChange = { newValue ->
                tasksViewModel.updateUISettings(uiSettings.copy(recurringTimeMinuteIncrement = newValue))
            },
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
            allMedia = allMedia,  // Pass media library data
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

    // Time Slot Suggestion Dialog
    if (showTimeSlotDialog) {
        com.example.questflow.presentation.components.TimeSlotSuggestionDialog(
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

    // Action Dialog
    android.util.Log.d("TasksScreen", "ACTION DIALOG CHECK: showActionDialog=$showActionDialog, taskId=${calendarLink.taskId}")
    if (showActionDialog && calendarLink.taskId != null) {
        android.util.Log.d("TasksScreen", "=== RENDERING TaskContactActionsDialog ===")
        val taskLinkedContacts = availableContacts.filter { it.id in selectedContactIds }
        android.util.Log.d("TasksScreen", "taskLinkedContacts count: ${taskLinkedContacts.size}")
        val coroutineScope = rememberCoroutineScope()

        com.example.questflow.presentation.components.TaskContactActionsDialog(
            taskId = calendarLink.taskId ?: 0L,
            taskLinkedContacts = taskLinkedContacts,
            contactTags = contactTagsMap,
            availableTags = usedContactTags,
            taskContactTags = taskContactTagsMap,
            allMedia = allMedia,
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
                        tasksViewModel.saveTaskContactTags(taskId, taskContactTagsMap)
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

    // Create Sub-Task Dialog - with current task pre-selected as parent
    if (showCreateSubTaskDialog && calendarLink.taskId != null && currentTask != null) {
        // Sync category with Today ViewModel
        LaunchedEffect(viewModel.selectedCategory.collectAsState().value) {
            viewModel.syncSelectedCategory(viewModel.selectedCategory.value)
        }

        AddTaskDialog(
            viewModel = viewModel,
            onDismiss = {
                showCreateSubTaskDialog = false
                // Refresh calendar links after creating sub-task
                tasksViewModel.loadCalendarLinks()
            },
            isCalendarMode = true,
            preSelectedParentId = calendarLink.taskId,  // Pre-select current task as parent
            inheritFromTask = currentTask,  // Inherit category from parent
            inheritFromCalendarLink = calendarLink,  // Inherit time from parent
            occupancyCalculator = occupancyCalculator
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterDialog(
    filterSettings: TaskFilterSettings,
    onDismiss: () -> Unit,
    onApply: (TaskFilterSettings) -> Unit
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

                    com.example.questflow.presentation.components.CompactDateTimeSection(
                        label = "Custom Range Start",
                        dateTime = customStartDateTime,
                        onDateTimeChange = { customStartDateTime = it },
                        dayIncrement = 1,
                        minuteIncrement = 15,
                        onDayIncrementChange = {},
                        onMinuteIncrementChange = {}
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    com.example.questflow.presentation.components.CompactDateTimeSection(
                        label = "Custom Range End",
                        dateTime = customEndDateTime,
                        onDateTimeChange = { customEndDateTime = it },
                        dayIncrement = 1,
                        minuteIncrement = 15,
                        onDayIncrementChange = {},
                        onMinuteIncrementChange = {}
                    )
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

/**
 * Convert Task recurring fields to RecurringConfig object
 */
private fun taskToRecurringConfig(task: com.example.questflow.domain.model.Task): com.example.questflow.presentation.components.RecurringConfig {
    // Parse recurringType
    val mode = when (task.recurringType) {
        "DAILY" -> com.example.questflow.presentation.components.RecurringMode.DAILY
        "WEEKLY" -> com.example.questflow.presentation.components.RecurringMode.WEEKLY
        "MONTHLY" -> com.example.questflow.presentation.components.RecurringMode.MONTHLY
        "CUSTOM" -> com.example.questflow.presentation.components.RecurringMode.CUSTOM
        else -> com.example.questflow.presentation.components.RecurringMode.DAILY
    }

    // Parse triggerMode
    val triggerMode = when (task.triggerMode) {
        "AFTER_COMPLETION" -> com.example.questflow.presentation.components.TriggerMode.AFTER_COMPLETION
        "AFTER_EXPIRY" -> com.example.questflow.presentation.components.TriggerMode.AFTER_EXPIRY
        else -> com.example.questflow.presentation.components.TriggerMode.FIXED_INTERVAL
    }

    // Parse weeklyDays (comma-separated string like "MONDAY,FRIDAY")
    val weeklyDays = task.recurringDays?.split(",")?.mapNotNull { dayStr ->
        try {
            java.time.DayOfWeek.valueOf(dayStr.trim())
        } catch (e: Exception) {
            null
        }
    }?.toSet() ?: emptySet()

    // Parse specificTime (HH:mm format string)
    val specificTime = task.specificTime?.let { timeStr ->
        try {
            android.util.Log.d("TasksScreen", "Parsing specificTime: '$timeStr'")
            java.time.LocalTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            android.util.Log.e("TasksScreen", "Failed to parse specificTime: '$timeStr'", e)
            null
        }
    }

    // Deserialisierung: recurringInterval ist IMMER in MINUTEN gespeichert!
    // DAILY: dailyInterval * 24 * 60 → zurück: / (24 * 60)
    // WEEKLY: 7 * 24 * 60 (fixiert)
    // MONTHLY: monthlyDay * 24 * 60 → zurück: / (24 * 60)
    // CUSTOM: customHours * 60 + customMinutes (direkt)
    val intervalMinutes = task.recurringInterval ?: 60

    return com.example.questflow.presentation.components.RecurringConfig(
        mode = mode,
        dailyInterval = when (mode) {
            com.example.questflow.presentation.components.RecurringMode.DAILY ->
                intervalMinutes / (24 * 60)  // Minuten → Tage
            else -> 1
        },
        weeklyDays = weeklyDays,
        monthlyDay = when (mode) {
            com.example.questflow.presentation.components.RecurringMode.MONTHLY ->
                intervalMinutes / (24 * 60)  // Minuten → Tag des Monats
            else -> 1
        },
        customMinutes = when (mode) {
            com.example.questflow.presentation.components.RecurringMode.CUSTOM ->
                intervalMinutes % 60  // Restminuten
            else -> 60
        },
        customHours = when (mode) {
            com.example.questflow.presentation.components.RecurringMode.CUSTOM ->
                intervalMinutes / 60  // Gesamtminuten → Stunden
            else -> 0
        },
        specificTime = specificTime,
        triggerMode = triggerMode
    )
}