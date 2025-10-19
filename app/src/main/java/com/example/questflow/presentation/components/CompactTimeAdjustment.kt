package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Compact Date/Time Section - Combines date and time rows with inline controls
 * Structure:
 * - Label (e.g., "Start" or "Ende")
 * - Date row: date button + inline +/- controls
 * - Time row: time button + inline +/- controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDateTimeSection(
    label: String,
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    dayIncrement: Int,
    minuteIncrement: Int,
    onDayIncrementChange: (Int) -> Unit,
    onMinuteIncrementChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        // Date row with +/- controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Button
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            }

            // Date +/- controls
            CompactDateAdjustment(
                dateTime = dateTime,
                onDateTimeChange = onDateTimeChange,
                increment = dayIncrement,
                onIncrementChange = onDayIncrementChange
            )
        }

        // Time row with +/- controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Button
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(dateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
            }

            // Time +/- controls
            CompactTimeAdjustment(
                dateTime = dateTime,
                onDateTimeChange = onDateTimeChange,
                increment = minuteIncrement,
                onIncrementChange = onMinuteIncrementChange
            )
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate().toEpochDay() * 24 * 60 * 60 * 1000
        )

        // Watch for date selection and auto-close
        LaunchedEffect(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let { millis ->
                val selectedDate = java.time.LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                if (selectedDate != dateTime.toLocalDate()) {
                    onDateTimeChange(LocalDateTime.of(selectedDate, dateTime.toLocalTime()))
                    showDatePicker = false
                }
            }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            },
            dismissButton = {}
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        var selectedHour by remember { mutableStateOf(dateTime.hour) }
        var selectedMinute by remember { mutableStateOf(dateTime.minute) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Uhrzeit wählen") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalWheelTimePicker(
                        initialHour = dateTime.hour,
                        initialMinute = dateTime.minute,
                        onTimeChange = { hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = java.time.LocalTime.of(selectedHour, selectedMinute)
                        onDateTimeChange(LocalDateTime.of(dateTime.toLocalDate(), newTime))
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Compact Time Adjustment Controls - Inline +/- buttons next to date/time fields
 */
@Composable
fun CompactDateAdjustment(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    increment: Int,
    onIncrementChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus button
        FilledTonalIconButton(
            onClick = { onDateTimeChange(dateTime.minusDays(increment.toLong())) },
            modifier = Modifier.size(32.dp)
        ) {
            Text("-", style = MaterialTheme.typography.titleSmall)
        }

        // Plus button
        FilledTonalIconButton(
            onClick = { onDateTimeChange(dateTime.plusDays(increment.toLong())) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
        }

        // Increment config button
        var showDialog by remember { mutableStateOf(false) }
        FilledTonalIconButton(
            onClick = { showDialog = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Config", modifier = Modifier.size(14.dp))
        }

        // Show current increment
        Text(
            "±$increment d",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showDialog) {
            IncrementConfigDialog(
                title = "Tage-Inkrement",
                currentValue = increment,
                suggestions = listOf(1, 2, 3, 7, 14, 30),
                onDismiss = { showDialog = false },
                onConfirm = { newValue ->
                    onIncrementChange(newValue)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun CompactTimeAdjustment(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    increment: Int,
    onIncrementChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus button
        FilledTonalIconButton(
            onClick = { onDateTimeChange(dateTime.minusMinutes(increment.toLong())) },
            modifier = Modifier.size(32.dp)
        ) {
            Text("-", style = MaterialTheme.typography.titleSmall)
        }

        // Plus button
        FilledTonalIconButton(
            onClick = { onDateTimeChange(dateTime.plusMinutes(increment.toLong())) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
        }

        // Increment config button
        var showDialog by remember { mutableStateOf(false) }
        FilledTonalIconButton(
            onClick = { showDialog = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Config", modifier = Modifier.size(14.dp))
        }

        // Show current increment
        Text(
            "±$increment min",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showDialog) {
            IncrementConfigDialog(
                title = "Minuten-Inkrement",
                currentValue = increment,
                suggestions = listOf(1, 5, 10, 15, 30, 60, 120),
                onDismiss = { showDialog = false },
                onConfirm = { newValue ->
                    onIncrementChange(newValue)
                    showDialog = false
                }
            )
        }
    }
}

/**
 * Duration Row - Set end time relative to start time
 */
@Composable
fun DurationRow(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onEndDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var durationMinutes by remember { mutableStateOf(60) }
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Dauer: Ende = Start + $durationMinutes min",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            val calculatedEnd = startDateTime.plusMinutes(durationMinutes.toLong())
            Text(
                "Ergebnis: ${calculatedEnd.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM. HH:mm"))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Config button
            FilledTonalIconButton(
                onClick = { showDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Dauer ändern", modifier = Modifier.size(16.dp))
            }

            // Apply button
            Button(
                onClick = {
                    onEndDateTimeChange(startDateTime.plusMinutes(durationMinutes.toLong()))
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Anwenden", style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    if (showDialog) {
        IncrementConfigDialog(
            title = "Dauer festlegen",
            currentValue = durationMinutes,
            suggestions = listOf(15, 30, 60, 90, 120, 180, 240, 480),
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                durationMinutes = newValue
                showDialog = false
            }
        )
    }
}

/**
 * Compact Increment Configuration Dialog
 */
@Composable
private fun IncrementConfigDialog(
    title: String,
    currentValue: Int,
    suggestions: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var valueText by remember { mutableStateOf("") }  // Start empty
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus and open keyboard
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show current value as reference with button to reuse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Aktueller Wert:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            currentValue.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Button to reuse current value and close dialog
                    FilledTonalButton(
                        onClick = {
                            onConfirm(currentValue)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Übernehmen", style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedTextField(
                    value = valueText,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            valueText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Neuer Wert") },
                    placeholder = { Text("Wert eingeben") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val value = valueText.toIntOrNull()
                            if (value != null && value > 0) {
                                onConfirm(value)
                            } else {
                                errorMessage = "Ungültiger Wert (muss > 0 sein)"
                            }
                        }
                    ),
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Quick suggestions
                Text(
                    "Schnellauswahl:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Grid of suggestions
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    suggestions.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { value ->
                                FilterChip(
                                    selected = valueText == value.toString(),
                                    onClick = { valueText = value.toString() },
                                    label = { Text(value.toString()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space
                            repeat(4 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = valueText.toIntOrNull()
                    if (value != null && value > 0) {
                        onConfirm(value)
                    } else {
                        errorMessage = "Ungültiger Wert (muss > 0 sein)"
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Compact Time-Only Section - Only time row with inline +/- controls
 * For recurring task configuration (no date needed)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTimeOnlySection(
    label: String,
    time: LocalTime?,
    onTimeChange: (LocalTime?) -> Unit,
    minuteIncrement: Int = 15,
    onMinuteIncrementChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val currentTime = time ?: LocalTime.of(14, 0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        // Time row with +/- controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Button
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"
                )
            }

            // Time +/- controls (same as CompactTimeAdjustment but for LocalTime)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus button
                FilledTonalIconButton(
                    onClick = { 
                        onTimeChange(currentTime.minusMinutes(minuteIncrement.toLong()))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("-", style = MaterialTheme.typography.titleSmall)
                }

                // Plus button
                FilledTonalIconButton(
                    onClick = { 
                        onTimeChange(currentTime.plusMinutes(minuteIncrement.toLong()))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                }

                // Increment config button
                var showIncrementDialog by remember { mutableStateOf(false) }
                FilledTonalIconButton(
                    onClick = { showIncrementDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Config", modifier = Modifier.size(14.dp))
                }

                // Show current increment
                Text(
                    "±$minuteIncrement min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showIncrementDialog) {
                    IncrementConfigDialog(
                        title = "Minuten-Inkrement",
                        currentValue = minuteIncrement,
                        suggestions = listOf(1, 5, 10, 15, 30, 60, 120),
                        onDismiss = { showIncrementDialog = false },
                        onConfirm = { newValue ->
                            onMinuteIncrementChange(newValue)
                            showIncrementDialog = false
                        }
                    )
                }
            }
        }
    }

    // Time Picker Dialog (same as in CompactDateTimeSection)
    if (showTimePicker) {
        var selectedHour by remember { mutableStateOf(currentTime.hour) }
        var selectedMinute by remember { mutableStateOf(currentTime.minute) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Uhrzeit wählen") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalWheelTimePicker(
                        initialHour = currentTime.hour,
                        initialMinute = currentTime.minute,
                        onTimeChange = { hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(selectedHour, selectedMinute))
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
