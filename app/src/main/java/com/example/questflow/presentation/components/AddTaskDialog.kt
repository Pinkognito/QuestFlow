package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.questflow.presentation.viewmodels.TodayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    viewModel: TodayViewModel,
    onDismiss: () -> Unit,
    isCalendarMode: Boolean = false  // Add parameter to distinguish context
) {
    val context = LocalContext.current
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
    var taskCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }

    // Date and time state - START
    val currentDateTime = remember { java.time.LocalDateTime.now() }
    var selectedYear by remember { mutableStateOf(currentDateTime.year) }
    var selectedMonth by remember { mutableStateOf(currentDateTime.monthValue) }
    var selectedDay by remember { mutableStateOf(currentDateTime.dayOfMonth) }
    var selectedHour by remember { mutableStateOf(currentDateTime.hour) }
    var selectedMinute by remember { mutableStateOf(currentDateTime.minute) }

    // Date and time state - END (default: 1 hour after start)
    val defaultEndDateTime = currentDateTime.plusHours(1)
    var endYear by remember { mutableStateOf(defaultEndDateTime.year) }
    var endMonth by remember { mutableStateOf(defaultEndDateTime.monthValue) }
    var endDay by remember { mutableStateOf(defaultEndDateTime.dayOfMonth) }
    var endHour by remember { mutableStateOf(defaultEndDateTime.hour) }
    var endMinute by remember { mutableStateOf(defaultEndDateTime.minute) }

    val dateTimeText = "$selectedDay.$selectedMonth.$selectedYear $selectedHour:${String.format("%02d", selectedMinute)}"
    val endDateTimeText = "$endDay.$endMonth.$endYear $endHour:${String.format("%02d", endMinute)}"

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

                    // Date and Time Selection - START
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

                    // Date and Time Selection - END
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
                    if (taskTitle.isNotBlank()) {
                        val startDateTime = java.time.LocalDateTime.of(
                            selectedYear, selectedMonth, selectedDay,
                            selectedHour, selectedMinute
                        )
                        val endDateTime = java.time.LocalDateTime.of(
                            endYear, endMonth, endDay,
                            endHour, endMinute
                        )
                        viewModel.createTaskWithCalendar(
                            title = taskTitle,
                            description = taskDescription,
                            xpPercentage = selectedPercentage,
                            startDateTime = startDateTime,
                            endDateTime = endDateTime,
                            addToCalendar = addToCalendar,
                            categoryId = taskCategory?.id,
                            deleteOnClaim = deleteOnClaim,
                            deleteOnExpiry = deleteOnExpiry,
                            isRecurring = isRecurring,
                            recurringConfig = if (isRecurring) recurringConfig else null
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
}

// Helper function to get button text for recurring configuration - now uses the shared function
private fun getRecurringButtonText(config: RecurringConfig): String {
    return getRecurringSummary(config)
}
