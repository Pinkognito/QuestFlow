package com.example.questflow.presentation.screens.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.questflow.presentation.components.XpLevelBadge
import com.example.questflow.presentation.components.XpBurstAnimation
import com.example.questflow.presentation.components.CategoryDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    navController: NavController,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToComplete by remember { mutableStateOf<com.example.questflow.domain.model.Task?>(null) }
    val currentLevel by remember { derivedStateOf {
        selectedCategory?.currentLevel ?: uiState.level
    } }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CategoryDropdown(
                            selectedCategory = selectedCategory,
                            categories = categories,
                            onCategorySelected = viewModel::selectCategory,
                            onManageCategoriesClick = {
                                navController.navigate("categories")
                            },
                            modifier = Modifier.weight(0.4f)
                        )
                        XpLevelBadge(
                            level = selectedCategory?.currentLevel ?: uiState.level,
                            xp = selectedCategory?.totalXp?.toLong() ?: uiState.totalXp,
                            modifier = Modifier.weight(0.6f),
                            isCategory = selectedCategory != null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = selectedCategory?.let { category ->
                        try {
                            Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f)
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    } ?: MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tasks List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No tasks yet! Add your first quest.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tasks) { task ->
                        TaskItem(
                            task = task,
                            currentLevel = currentLevel,
                            onToggleComplete = {
                                if (!task.isCompleted) {
                                    taskToComplete = task
                                } else {
                                    viewModel.toggleTaskCompletion(task.id, true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

        // XP Animation Overlay
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
                    unlockedMemes = animationData.unlockedMemes,
                    onAnimationEnd = {
                        viewModel.clearXpAnimation()
                    }
                )
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            viewModel = viewModel,
            onDismiss = { showAddTaskDialog = false }
        )
    }

    // Task Completion Confirmation Dialog
    taskToComplete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToComplete = null },
            title = { Text("Task abschließen") },
            text = {
                Column {
                    Text("Hast du diese Aufgabe wirklich erledigt?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Du erhältst ${viewModel.getXpForPercentage(task.xpPercentage)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleTaskCompletion(task.id, false)
                        taskToComplete = null
                    }
                ) {
                    Text("Ja, erledigt!")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToComplete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: com.example.questflow.domain.model.Task,
    currentLevel: Int,
    onToggleComplete: () -> Unit
) {
    // Calculate XP based on current level and task's percentage
    val calculatedXp = remember(currentLevel, task.xpPercentage) {
        val xpRequiredForNext = (currentLevel + 1) * (currentLevel + 1) * 100 - currentLevel * currentLevel * 100
        val xpReward = (xpRequiredForNext * task.xpPercentage / 100.0).toInt()
        ((xpReward + 2) / 5) * 5 // Round to nearest 5
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AssistChip(
                onClick = { },
                label = {
                    val difficultyText = when (task.xpPercentage) {
                        20 -> "Trivial"
                        40 -> "Einfach"
                        60 -> "Mittel"
                        80 -> "Schwer"
                        100 -> "Episch"
                        else -> "Mittel"
                    }
                    Text(difficultyText)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    viewModel: TodayViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedPercentage by remember { mutableStateOf(60) } // Default to 60%
    var addToCalendar by remember { mutableStateOf(true) } // Default to true
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var taskCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }

    // Date and time state
    val currentDateTime = remember { java.time.LocalDateTime.now() }
    var selectedYear by remember { mutableStateOf(currentDateTime.year) }
    var selectedMonth by remember { mutableStateOf(currentDateTime.monthValue) }
    var selectedDay by remember { mutableStateOf(currentDateTime.dayOfMonth) }
    var selectedHour by remember { mutableStateOf(14) }
    var selectedMinute by remember { mutableStateOf(0) }

    val dateTimeText = "$selectedDay.$selectedMonth.$selectedYear $selectedHour:${String.format("%02d", selectedMinute)}"

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
                    if (taskTitle.isNotBlank()) {
                        val dateTime = java.time.LocalDateTime.of(
                            selectedYear, selectedMonth, selectedDay,
                            selectedHour, selectedMinute
                        )
                        viewModel.createTaskWithCalendar(
                            title = taskTitle,
                            description = taskDescription,
                            xpPercentage = selectedPercentage,
                            dateTime = dateTime,
                            addToCalendar = addToCalendar,
                            categoryId = taskCategory?.id
                        )
                        onDismiss()
                    }
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
}