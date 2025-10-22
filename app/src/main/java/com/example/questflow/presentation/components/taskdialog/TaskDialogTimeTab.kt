package com.example.questflow.presentation.components.taskdialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.domain.usecase.DayOccupancyCalculator
import com.example.questflow.presentation.components.RecurringConfig
import java.time.LocalDateTime

/**
 * TAB 2: Zeit
 * - MonthViewDatePicker (collapsible, default: expanded)
 * - Start DateTime (mit CompactDateTimeSection)
 * - Duration Row
 * - End DateTime (mit CompactDateTimeSection)
 * - Conflict Warning
 * - Recurring Options
 *
 * WICHTIG: DatePicker wird nur geladen wenn dieser Tab aktiv ist!
 */
@Composable
fun TaskDialogTimeTab(
    startDateTime: LocalDateTime,
    onStartDateTimeChange: (LocalDateTime) -> Unit,
    endDateTime: LocalDateTime,
    onEndDateTimeChange: (LocalDateTime) -> Unit,
    startDayIncrement: Int,
    onStartDayIncrementChange: (Int) -> Unit,
    startMinuteIncrement: Int,
    onStartMinuteIncrementChange: (Int) -> Unit,
    endDayIncrement: Int,
    onEndDayIncrementChange: (Int) -> Unit,
    endMinuteIncrement: Int,
    onEndMinuteIncrementChange: (Int) -> Unit,
    scheduleConflicts: List<CalendarEventLinkEntity>,
    onFindFreeTimesClick: () -> Unit,
    onShowConflictDetails: (() -> Unit)? = null,
    isRecurring: Boolean,
    onIsRecurringChange: (Boolean) -> Unit,
    recurringConfig: RecurringConfig,
    onRecurringConfigClick: () -> Unit,
    monthViewEvents: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator?,
    categoryColor: androidx.compose.ui.graphics.Color?,
    monthViewTasks: List<TaskEntity>,
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long?,
    currentCategoryId: Long?,
    calendarExpanded: Boolean,
    onCalendarExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Time distance lock state - CENTRAL state for all lock-related UI
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeAdjustmentPrefs = remember { com.example.questflow.domain.preferences.TimeAdjustmentPreferences(context) }
    var isDistanceLocked by remember {
        mutableStateOf(timeAdjustmentPrefs.getAdjustmentMode() != com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT)
    }

    // Callback to update lock state everywhere
    val onLockChange: (Boolean) -> Unit = { newLockState ->
        isDistanceLocked = newLockState
    }

    // Collapsible section states (saved in SharedPreferences)
    val prefs = remember { context.getSharedPreferences("task_dialog_collapse_state", android.content.Context.MODE_PRIVATE) }
    var startSectionExpanded by remember { mutableStateOf(prefs.getBoolean("start_section_expanded", true)) }
    var endSectionExpanded by remember { mutableStateOf(prefs.getBoolean("end_section_expanded", true)) }
    var recurringSectionExpanded by remember { mutableStateOf(prefs.getBoolean("recurring_section_expanded", true)) }

    // Load calendar color configuration for overlap color
    // Uses SimpleCalendarColorRepository with key "simple_cal_overlap"
    val calendarColorPrefs = remember { context.getSharedPreferences("calendar_colors", android.content.Context.MODE_PRIVATE) }
    val overlapColorHex = remember {
        // Correct key from SimpleCalendarColorRepository
        val color = calendarColorPrefs.getString("simple_cal_overlap", "#000000") ?: "#000000"
        android.util.Log.d("TaskDialogTimeTab", "🎨 Overlap color from prefs (simple_cal_overlap): $color")
        color
    }
    val overlapColor = remember(overlapColorHex) {
        try {
            val parsedColor = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(overlapColorHex))
            android.util.Log.d("TaskDialogTimeTab", "✅ Parsed overlap color: $parsedColor")
            parsedColor
        } catch (e: Exception) {
            android.util.Log.e("TaskDialogTimeTab", "❌ Failed to parse color: $overlapColorHex", e)
            androidx.compose.ui.graphics.Color.Black
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // MonthViewDatePicker - Collapsible Calendar (Default: Expanded)
        item {
            // Collapsible header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newState = !calendarExpanded
                        onCalendarExpandedChange(newState)
                        // Save state to SharedPreferences
                        prefs.edit().putBoolean("calendar_expanded", newState).apply()
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kalender",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    if (calendarExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (calendarExpanded) "Zuklappen" else "Aufklappen",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Show MonthViewDatePicker when expanded
        if (calendarExpanded && occupancyCalculator != null) {
            item {
                // IMPORTANT: selectedDate must be independent to avoid recomposition loops!
                // Use remember (not rememberSaveable) to keep it simple
                val calendarSelectedDate by remember {
                    mutableStateOf(startDateTime.toLocalDate())
                }

                com.example.questflow.presentation.components.MonthViewDatePicker(
                    selectedDate = calendarSelectedDate,
                    events = monthViewEvents,
                    occupancyCalculator = occupancyCalculator,
                    mode = com.example.questflow.presentation.components.CalendarMode.RADIAL_MENU,
                    onStartDateSelected = { selectedDate ->
                        android.util.Log.d("TaskDialogTimeTab", "✅ onStartDateSelected: $selectedDate → calling onStartDateTimeChange")
                        onStartDateTimeChange(LocalDateTime.of(selectedDate, startDateTime.toLocalTime()))
                        android.util.Log.d("TaskDialogTimeTab", "   → onStartDateTimeChange DONE")
                    },
                    onEndDateSelected = { selectedDate ->
                        android.util.Log.d("TaskDialogTimeTab", "✅ onEndDateSelected: $selectedDate → calling onEndDateTimeChange")
                        onEndDateTimeChange(LocalDateTime.of(selectedDate, endDateTime.toLocalTime()))
                        android.util.Log.d("TaskDialogTimeTab", "   → onEndDateTimeChange DONE")
                    },
                    tasks = monthViewTasks,
                    timeBlocks = timeBlocks,
                    currentTaskId = currentTaskId,
                    currentCategoryId = currentCategoryId,
                    startTime = startDateTime.toLocalTime(),
                    endTime = endDateTime.toLocalTime(),
                    onStartTimeChange = { newTime ->
                        onStartDateTimeChange(LocalDateTime.of(startDateTime.toLocalDate(), newTime))
                    },
                    onEndTimeChange = { newTime ->
                        onEndDateTimeChange(LocalDateTime.of(endDateTime.toLocalDate(), newTime))
                    },
                    isDistanceLocked = isDistanceLocked,
                    onDistanceLockToggle = {
                        // Toggle lock state using central callback
                        val newLockState = !isDistanceLocked
                        onLockChange(newLockState)
                        // Update preferences
                        if (newLockState) {
                            // Calculate current duration and set FIXED_DURATION mode
                            val durationMinutes = java.time.Duration.between(startDateTime, endDateTime).toMinutes().toInt()
                            timeAdjustmentPrefs.setFixedDurationMinutes(durationMinutes)
                            timeAdjustmentPrefs.setAdjustmentMode(com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION)
                        } else {
                            // Set INDEPENDENT mode
                            timeAdjustmentPrefs.setAdjustmentMode(com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Conflict warning - COMPACT (one line with count + button)
        // Uses overlap color from calendar color settings
        if (scheduleConflicts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Warning icon + count (CLICKABLE if callback provided)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (onShowConflictDetails != null) {
                                    Modifier.clickable { onShowConflictDetails() }
                                } else {
                                    Modifier
                                }
                            ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = overlapColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (scheduleConflicts.size == 1) {
                                "1 Konflikt"
                            } else {
                                "${scheduleConflicts.size} Konflikte"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = overlapColor,
                            fontWeight = FontWeight.Medium
                        )
                        if (onShowConflictDetails != null) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Details anzeigen",
                                tint = overlapColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Find free times button
                    OutlinedButton(
                        onClick = onFindFreeTimesClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = overlapColor
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Freie Zeiten", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Date and Time Selection - START with CompactDateTimeSection (Collapsible)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Collapsible header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            startSectionExpanded = !startSectionExpanded
                            prefs.edit().putBoolean("start_section_expanded", startSectionExpanded).apply()
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Start",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (startSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (startSectionExpanded) "Zuklappen" else "Aufklappen",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Content (only shown when expanded)
                if (startSectionExpanded) {
                    com.example.questflow.presentation.components.CompactDateTimeSection(
                        label = "Start",
                        dateTime = startDateTime,
                        onDateTimeChange = onStartDateTimeChange,
                        dayIncrement = startDayIncrement,
                        minuteIncrement = startMinuteIncrement,
                        onDayIncrementChange = onStartDayIncrementChange,
                        onMinuteIncrementChange = onStartMinuteIncrementChange,
                        events = monthViewEvents,
                        occupancyCalculator = occupancyCalculator,
                        categoryColor = categoryColor,
                        tasks = monthViewTasks,
                        timeBlocks = timeBlocks,
                        currentTaskId = currentTaskId,
                        currentCategoryId = currentCategoryId,
                        alternativeTime = endDateTime,
                        onAlternativeTimeChange = onEndDateTimeChange,
                        showLabel = false  // Hide duplicate label (header already shows "Start")
                    )
                }
            }
        }

        // Duration Row - ALL in one row: [Current] [Radial] [Custom] [Lock]
        item {
            com.example.questflow.presentation.components.DurationRow(
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                onEndDateTimeChange = onEndDateTimeChange,
                isLocked = isDistanceLocked,
                onLockChange = onLockChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Date and Time Selection - END with CompactDateTimeSection (Collapsible)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Collapsible header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            endSectionExpanded = !endSectionExpanded
                            prefs.edit().putBoolean("end_section_expanded", endSectionExpanded).apply()
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ende",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (endSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (endSectionExpanded) "Zuklappen" else "Aufklappen",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Content (only shown when expanded)
                if (endSectionExpanded) {
                    com.example.questflow.presentation.components.CompactDateTimeSection(
                        label = "Ende",
                        dateTime = endDateTime,
                        onDateTimeChange = onEndDateTimeChange,
                        dayIncrement = endDayIncrement,
                        minuteIncrement = endMinuteIncrement,
                        onDayIncrementChange = onEndDayIncrementChange,
                        onMinuteIncrementChange = onEndMinuteIncrementChange,
                        events = monthViewEvents,
                        occupancyCalculator = occupancyCalculator,
                        categoryColor = categoryColor,
                        tasks = monthViewTasks,
                        timeBlocks = timeBlocks,
                        currentTaskId = currentTaskId,
                        currentCategoryId = currentCategoryId,
                        alternativeTime = startDateTime,
                        onAlternativeTimeChange = onStartDateTimeChange,
                        showLabel = false  // Hide duplicate label (header already shows "Ende")
                    )
                }
            }
        }

        // Recurring task option (Collapsible)
        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                // Collapsible header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            recurringSectionExpanded = !recurringSectionExpanded
                            prefs.edit().putBoolean("recurring_section_expanded", recurringSectionExpanded).apply()
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Wiederkehrende Aufgabe",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (recurringSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (recurringSectionExpanded) "Zuklappen" else "Aufklappen",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Content (only shown when expanded)
                if (recurringSectionExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRecurring,
                            onCheckedChange = onIsRecurringChange
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
                        OutlinedButton(
                            onClick = onRecurringConfigClick,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
        }
    }
}
