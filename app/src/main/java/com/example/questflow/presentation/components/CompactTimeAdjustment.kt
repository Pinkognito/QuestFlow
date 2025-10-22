package com.example.questflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.domain.usecase.DayOccupancyCalculator
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
    modifier: Modifier = Modifier,
    events: List<CalendarEventLinkEntity> = emptyList(),
    occupancyCalculator: DayOccupancyCalculator? = null,
    categoryColor: androidx.compose.ui.graphics.Color? = null,
    tasks: List<TaskEntity> = emptyList(),
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null,
    // Alternative time to show in calendar (e.g., end time when this is start)
    alternativeTime: LocalDateTime? = null,
    onAlternativeTimeChange: ((LocalDateTime) -> Unit)? = null,
    // Show label (default: true, set to false to hide duplicate labels)
    showLabel: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showLabel) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

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

    // Date Picker Dialog with Month View Occupancy Visualization
    if (showDatePicker && occupancyCalculator != null) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Datum wählen") },
            text = {
                MonthViewDatePicker(
                    selectedDate = dateTime.toLocalDate(),
                    events = events,
                    occupancyCalculator = occupancyCalculator,
                    // DIRECT MODE: Click directly sets the date (Start or End based on label)
                    mode = if (label == "Start") CalendarMode.DIRECT_START else CalendarMode.DIRECT_END,
                    onStartDateSelected = if (label == "Start") {
                        { selectedDate -> onDateTimeChange(LocalDateTime.of(selectedDate, dateTime.toLocalTime())) }
                    } else null,
                    onEndDateSelected = if (label == "Ende") {
                        { selectedDate -> onDateTimeChange(LocalDateTime.of(selectedDate, dateTime.toLocalTime())) }
                    } else null,
                    tasks = tasks,
                    timeBlocks = timeBlocks,
                    currentTaskId = currentTaskId,
                    currentCategoryId = currentCategoryId,
                    // ALWAYS show Start | Ende (regardless of which picker this is)
                    startTime = if (label == "Start") dateTime.toLocalTime() else alternativeTime?.toLocalTime(),
                    endTime = if (label == "Start") alternativeTime?.toLocalTime() else dateTime.toLocalTime(),
                    onStartTimeChange = if (label == "Start") {
                        { newTime -> onDateTimeChange(LocalDateTime.of(dateTime.toLocalDate(), newTime)) }
                    } else {
                        { newTime -> onAlternativeTimeChange?.invoke(LocalDateTime.of(alternativeTime!!.toLocalDate(), newTime)) }
                    },
                    onEndTimeChange = if (label == "Start") {
                        { newTime -> onAlternativeTimeChange?.invoke(LocalDateTime.of(alternativeTime!!.toLocalDate(), newTime)) }
                    } else {
                        { newTime -> onDateTimeChange(LocalDateTime.of(dateTime.toLocalDate(), newTime)) }
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    } else if (showDatePicker) {
        // Fallback to standard Material3 DatePicker
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
 * All elements in ONE row: [Current] [Radial] [Custom] [Lock]
 */
@Composable
fun DurationRow(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onEndDateTimeChange: (LocalDateTime) -> Unit,
    isLocked: Boolean,
    onLockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate current duration between start and end
    val currentDurationMinutes = remember(startDateTime, endDateTime) {
        java.time.Duration.between(startDateTime, endDateTime).toMinutes().toInt()
    }

    var customDurationMinutes by remember { mutableStateOf(currentDurationMinutes) }
    var showInputDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val timeAdjustmentPrefs = remember { com.example.questflow.domain.preferences.TimeAdjustmentPreferences(context) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current duration display (read-only)
        OutlinedButton(
            onClick = { /* read-only */ },
            enabled = false,
            modifier = Modifier.width(70.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                "$currentDurationMinutes",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Radial menu button (compact, no extra space)
        DurationRadialMenuButton(
            startDateTime = startDateTime,
            durationMinutes = customDurationMinutes,
            onEndDateTimeChange = onEndDateTimeChange,
            onDurationChange = { customDurationMinutes = it },
            isLocked = isLocked,
            onLockChange = { newLockState ->
                onLockChange(newLockState)
                if (newLockState) {
                    timeAdjustmentPrefs.setFixedDurationMinutes(customDurationMinutes)
                    timeAdjustmentPrefs.setAdjustmentMode(com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION)
                } else {
                    timeAdjustmentPrefs.setAdjustmentMode(com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT)
                }
            },
            modifier = Modifier.size(36.dp)
        )

        // Desired duration input field (clickable, editable)
        OutlinedButton(
            onClick = { showInputDialog = true },
            modifier = Modifier.width(70.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                "$customDurationMinutes",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Lock/Unlock duration button
        FilledTonalIconButton(
            onClick = {
                val newLockState = !isLocked
                onLockChange(newLockState)
                if (newLockState) {
                    timeAdjustmentPrefs.setFixedDurationMinutes(customDurationMinutes)
                    timeAdjustmentPrefs.setAdjustmentMode(com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION)
                } else {
                    timeAdjustmentPrefs.setAdjustmentMode(com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT)
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Close,
                contentDescription = if (isLocked) "Gesperrt" else "Entsperrt",
                modifier = Modifier.size(18.dp)
            )
        }
    }

    // Input dialog for custom duration
    if (showInputDialog) {
        IncrementConfigDialog(
            title = "Dauer festlegen",
            currentValue = currentDurationMinutes,
            suggestions = listOf(15, 30, 60, 90, 120, 180, 240, 480),
            onDismiss = { showInputDialog = false },
            onConfirm = { newValue ->
                customDurationMinutes = newValue
                showInputDialog = false
            }
        )
    }
}

/**
 * Radial Menu Button for Duration Actions - COMPACT version for inline use
 */
@Composable
fun DurationRadialMenuButton(
    startDateTime: LocalDateTime,
    durationMinutes: Int,
    onEndDateTimeChange: (LocalDateTime) -> Unit,
    onDurationChange: (Int) -> Unit,
    isLocked: Boolean,
    onLockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isMenuActive by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Dynamic actions based on lock state
    val actions = listOf(
        RadialMenuAction(id = "apply", label = "Anwenden", angle = 0f),      // Rechts (0°)
        RadialMenuAction(
            id = "lock",
            label = if (isLocked) "Entsperren" else "Sperren",
            angle = 90f
        ),       // Unten (90°)
        RadialMenuAction(id = "quick_30", label = "30min", angle = 180f),    // Links (180°)
        RadialMenuAction(id = "quick_60", label = "60min", angle = 270f)     // Oben (270°)
    )

    // COMPACT: No wrapper box, just the button itself
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Center button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isMenuActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isMenuActive = true
                            selectedAction = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount

                            // Calculate angle
                            val angle = (Math.toDegrees(kotlin.math.atan2(dragOffset.y.toDouble(), dragOffset.x.toDouble())) + 360) % 360

                            // Find closest action
                            selectedAction = actions.minByOrNull { action ->
                                val diff = kotlin.math.abs(angle - action.angle)
                                kotlin.math.min(diff, 360 - diff)
                            }?.id
                        },
                        onDragEnd = {
                            when (selectedAction) {
                                "apply" -> {
                                    onEndDateTimeChange(startDateTime.plusMinutes(durationMinutes.toLong()))
                                    android.widget.Toast.makeText(context, "Ende angepasst ($durationMinutes min)", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                "lock" -> {
                                    // Toggle lock state
                                    val newLockState = !isLocked
                                    onLockChange(newLockState)
                                    android.widget.Toast.makeText(
                                        context,
                                        if (newLockState) "Dauer gesperrt ($durationMinutes min)" else "Dauer entsperrt",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                "quick_30" -> {
                                    onDurationChange(30)
                                    android.widget.Toast.makeText(context, "30 Minuten", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                "quick_60" -> {
                                    onDurationChange(60)
                                    android.widget.Toast.makeText(context, "60 Minuten", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            isMenuActive = false
                            dragOffset = Offset.Zero
                            selectedAction = null
                        },
                        onDragCancel = {
                            isMenuActive = false
                            dragOffset = Offset.Zero
                            selectedAction = null
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Dauer-Optionen",
                tint = if (isMenuActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Radial menu options
        if (isMenuActive) {
            actions.forEach { action ->
                val angleRad = Math.toRadians(action.angle.toDouble())
                val radius = 50.dp  // Smaller radius to fit within container
                val offsetX = (radius.value * kotlin.math.cos(angleRad)).dp
                val offsetY = (radius.value * kotlin.math.sin(angleRad)).dp
                val isSelected = selectedAction == action.id

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(if (isSelected) 44.dp else 40.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
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
