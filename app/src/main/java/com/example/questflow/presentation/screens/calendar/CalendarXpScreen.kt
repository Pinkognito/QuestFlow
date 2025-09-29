package com.example.questflow.presentation.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.questflow.presentation.screens.today.TodayViewModel
import com.example.questflow.presentation.screens.today.AddTaskDialog
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
    todayViewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()
    val showFilterDialog by viewModel.showFilterDialog.collectAsState()
    val filterSettings by viewModel.filterSettings.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedEditLink by remember { mutableStateOf<CalendarEventLinkEntity?>(null) }

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
                    title = "Calendar XP",
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
        if (uiState.links.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No calendar events yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.links) { link ->
                    val isExpired = link.endsAt < java.time.LocalDateTime.now()
                    val isClaimed = link.rewarded || link.status == "CLAIMED"

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        link.title,
                                        fontWeight = FontWeight.Medium,
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
    selectedEditLink?.let { link ->
        EditCalendarTaskDialog(
            calendarLink = link,
            viewModel = todayViewModel,
            calendarViewModel = viewModel,
            onDismiss = {
                selectedEditLink = null
                viewModel.loadCalendarLinks()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCalendarTaskDialog(
    calendarLink: CalendarEventLinkEntity,
    viewModel: TodayViewModel,
    calendarViewModel: CalendarXpViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var taskTitle by remember { mutableStateOf(calendarLink.title) }
    var taskDescription by remember { mutableStateOf("") }
    var selectedPercentage by remember { mutableStateOf(calendarLink.xpPercentage) }
    val categories by viewModel.categories.collectAsState()
    var taskCategory by remember(categories) {
        mutableStateOf(categories.find { it.id == calendarLink.categoryId })
    }
    var shouldReactivate by remember { mutableStateOf(false) }

    // Calendar integration options
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsState()
    var addToCalendar by remember { mutableStateOf(calendarLink.calendarEventId != 0L) }
    var deleteOnClaim by remember { mutableStateOf(calendarLink.deleteOnClaim) }
    var deleteOnExpiry by remember { mutableStateOf(calendarLink.deleteOnExpiry) }

    // Recurring task options
    var isRecurring by remember { mutableStateOf(calendarLink.isRecurring) }
    var recurringConfig by remember {
        mutableStateOf(com.example.questflow.presentation.components.RecurringConfig())
    }
    var showRecurringDialog by remember { mutableStateOf(false) }

    // Date and time state from existing calendar link
    var selectedYear by remember { mutableStateOf(calendarLink.startsAt.year) }
    var selectedMonth by remember { mutableStateOf(calendarLink.startsAt.monthValue) }
    var selectedDay by remember { mutableStateOf(calendarLink.startsAt.dayOfMonth) }
    var selectedHour by remember { mutableStateOf(calendarLink.startsAt.hour) }
    var selectedMinute by remember { mutableStateOf(calendarLink.startsAt.minute) }

    val dateTimeText = "$selectedDay.$selectedMonth.$selectedYear $selectedHour:${String.format("%02d", selectedMinute)}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aufgabe bearbeiten") },
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
                        var categoryExpanded by remember { mutableStateOf(false) }

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
                            categories.forEach { category ->
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
                                    }
                                )
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
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Date and Time Selection
                    item {
                        Text("Termin:", style = MaterialTheme.typography.labelMedium)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                dateTimeText,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            selectedYear = year
                                            selectedMonth = month + 1
                                            selectedDay = dayOfMonth
                                        },
                                        selectedYear,
                                        selectedMonth - 1,
                                        selectedDay
                                    ).show()
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
                        val dateTime = java.time.LocalDateTime.of(
                            selectedYear, selectedMonth, selectedDay,
                            selectedHour, selectedMinute
                        )

                        // Check if we need to reactivate the task (only for claimed tasks)
                        val isReactivating = shouldReactivate &&
                            (calendarLink.rewarded || calendarLink.status == "CLAIMED")

                        if (calendarLink.taskId != null) {
                            // Update task if it exists
                            viewModel.updateCalendarTask(
                                linkId = calendarLink.id,
                                taskId = calendarLink.taskId,
                                title = taskTitle,
                                description = taskDescription,
                                xpPercentage = selectedPercentage,
                                dateTime = dateTime,
                                categoryId = taskCategory?.id,
                                calendarEventId = calendarLink.calendarEventId,
                                shouldReactivate = isReactivating,
                                deleteOnClaim = deleteOnClaim,
                                deleteOnExpiry = deleteOnExpiry,
                                isRecurring = isRecurring,
                                recurringConfig = if (isRecurring) recurringConfig else null
                            )
                        } else {
                            // Just update the calendar link directly
                            calendarViewModel.updateCalendarLinkWithReactivation(
                                linkId = calendarLink.id,
                                title = taskTitle,
                                xpPercentage = selectedPercentage,
                                startsAt = dateTime,
                                endsAt = dateTime.plusHours(1),
                                categoryId = taskCategory?.id,
                                shouldReactivate = isReactivating,
                                deleteOnClaim = deleteOnClaim,
                                deleteOnExpiry = deleteOnExpiry,
                                isRecurring = isRecurring
                            )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterDialog(
    filterSettings: CalendarFilterSettings,
    onDismiss: () -> Unit,
    onApply: (CalendarFilterSettings) -> Unit
) {
    var showCompleted by remember { mutableStateOf(filterSettings.showCompleted) }
    var showOpen by remember { mutableStateOf(filterSettings.showOpen) }
    var showExpired by remember { mutableStateOf(filterSettings.showExpired) }
    var filterByCategory by remember { mutableStateOf(filterSettings.filterByCategory) }
    var dateFilterType by remember { mutableStateOf(filterSettings.dateFilterType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Calendar Events") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                            dateFilterType = dateFilterType
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