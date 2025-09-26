package com.example.questflow.presentation.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (link.taskId != null && link.status != "EXPIRED") {
                                    selectedEditLink = link
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = when (link.status) {
                                "EXPIRED" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                "CLAIMED" -> MaterialTheme.colorScheme.surfaceVariant
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
                                        color = if (link.status == "EXPIRED")
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    if (link.status == "EXPIRED") {
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
                                        color = if (link.status == "EXPIRED")
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            trailingContent = {
                                when (link.status) {
                                    "EXPIRED" -> {
                                        Text(
                                            "Abgelaufen",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    "CLAIMED" -> {
                                        Text(
                                            "Erhalten",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    else -> {
                                        Button(
                                            onClick = {
                                                viewModel.claimXp(link.id) {
                                                    // Refresh stats after claiming
                                                    appViewModel.refreshStats()
                                                }
                                            },
                                            enabled = !link.rewarded
                                        ) {
                                            Text("Claim XP")
                                        }
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

    // Edit Task Dialog - temporarily disabled until we create a unified solution
    // TODO: Create unified edit dialog that can handle both regular tasks and calendar links
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