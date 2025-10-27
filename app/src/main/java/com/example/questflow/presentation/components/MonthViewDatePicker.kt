package com.example.questflow.presentation.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import kotlin.math.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.domain.model.SimpleCalendarColorConfig
import com.example.questflow.domain.model.SimpleCalendarColorRepository
import com.example.questflow.domain.usecase.DayOccupancyCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * Calendar interaction mode
 * Determines how clicks on calendar cells behave
 */
enum class CalendarMode {
    /** Click on cell directly sets start date (no radial menu) */
    DIRECT_START,
    /** Click on cell directly sets end date (no radial menu) */
    DIRECT_END,
    /** Click shows button, drag opens radial menu with all options */
    RADIAL_MENU
}

/**
 * Calendar view mode
 * Determines how the calendar is displayed
 */
enum class CalendarViewMode {
    /** Standard horizontal month view with horizontal occupancy bars */
    HORIZONTAL_MONTH,
    /** Vertically stretched month view with vertical 24h occupancy bars */
    VERTICAL_MONTH,
    /** Single week view with 7 days and vertical occupancy bars */
    WEEK_VIEW
}

/**
 * Google Calendar-style month view with visual occupancy indicators
 *
 * Features:
 * - 5-6 week rows showing entire month
 * - Month navigation with arrow buttons
 * - Visual occupancy bars: Green (free) / Red (occupied)
 * - Direct visual overview of schedule
 * - 3 interaction modes: DIRECT_START, DIRECT_END, RADIAL_MENU
 */
@Composable
fun MonthViewDatePicker(
    selectedDate: LocalDate?,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    modifier: Modifier = Modifier,
    mode: CalendarMode = CalendarMode.RADIAL_MENU,
    // Date selection callbacks (separate for start/end)
    onStartDateSelected: ((LocalDate) -> Unit)? = null,
    onEndDateSelected: ((LocalDate) -> Unit)? = null,
    // Deprecated callback (kept for backward compatibility)
    onDateSelected: ((LocalDate) -> Unit)? = null,
    tasks: List<TaskEntity> = emptyList(),
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null,
    // Time input support - ALWAYS shows "Start | Ende"
    startTime: LocalTime? = null,
    endTime: LocalTime? = null,
    onStartTimeChange: ((LocalTime) -> Unit)? = null,
    onEndTimeChange: ((LocalTime) -> Unit)? = null,
    // Time distance settings (NEW)
    isDistanceLocked: Boolean = false,
    onDistanceLockToggle: (() -> Unit)? = null,
    // PERFORMANCE OPTIMIZATION: Month change callback
    onMonthChange: ((YearMonth) -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModePrefs = remember {
        context.getSharedPreferences("calendar_view_mode", Context.MODE_PRIVATE)
    }

    // Load saved view mode (default: HORIZONTAL_MONTH)
    var currentViewMode by remember {
        mutableStateOf(
            CalendarViewMode.values()[
                viewModePrefs.getInt("view_mode", CalendarViewMode.HORIZONTAL_MONTH.ordinal)
            ]
        )
    }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // PERFORMANCE OPTIMIZATION: Notify ViewModel when month changes
    LaunchedEffect(currentMonth) {
        onMonthChange?.invoke(currentMonth)
        android.util.Log.d("MonthViewDatePicker", "📅 Month changed to: $currentMonth (callback invoked)")
    }

    // Week View State - separate from month navigation
    // Initialize with week containing selectedDate or today
    val initialWeekStart = (selectedDate ?: LocalDate.now())
        .minusDays((selectedDate ?: LocalDate.now()).dayOfWeek.value.toLong() - 1) // Monday
    var currentWeekStart by remember { mutableStateOf(initialWeekStart) }

    var showColorSettings by remember { mutableStateOf(false) }
    var showTimeSettings by remember { mutableStateOf(false) }  // NEW: Time settings dialog
    var refreshKey by remember { mutableStateOf(0) } // Trigger recomposition when settings change

    // Time input state
    var showStartTimeInput by remember { mutableStateOf(false) }
    var showEndTimeInput by remember { mutableStateOf(false) }

    // Radial Button Menu state - Track which date has active button
    var activeButtonDate by remember { mutableStateOf<LocalDate?>(null) }

    // View Mode Menu State
    var showViewModeMenu by remember { mutableStateOf(false) }
    var viewModeButtonPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredViewMode by remember { mutableStateOf<CalendarViewMode?>(null) }
    var containerPosition by remember { mutableStateOf(Offset.Zero) } // NEW: Track container position

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onGloballyPositioned { coordinates ->
                    // Track Column position (not Box) because Column contains scrollable content
                    containerPosition = coordinates.positionInWindow()
                }
        ) {
        // Event/Task list for selected date (Info box - flexible size, scrollable)
        if (selectedDate != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes remaining space after calendar
                    .verticalScroll(rememberScrollState())
            ) {
                DayDetailsList(
                    date = selectedDate,
                    events = events,
                    tasks = tasks,
                    occupancyCalculator = occupancyCalculator,
                    currentTaskId = currentTaskId,
                    currentCategoryId = currentCategoryId
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Time input row - REMOVED (2025-10-22)
        // Moved to settings dialog to save space
        // TODO: Create time settings dialog and move CompactTimeInputRow there
        /*
        if (selectedDate != null && startTime != null && endTime != null && onStartTimeChange != null && onEndTimeChange != null) {
            CompactTimeInputRow(
                selectedDate = selectedDate,
                startTime = startTime,
                endTime = endTime,
                onStartTimeClick = { showStartTimeInput = true },
                onEndTimeClick = { showEndTimeInput = true },
                onEndTimeChange = onEndTimeChange
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        */

        // Calendar - FIXED size, always fully visible at bottom
        Column(modifier = Modifier.fillMaxWidth()) {
            // Month navigation header
            MonthNavigationHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                currentWeekStart = currentWeekStart,
                onPreviousWeek = { currentWeekStart = currentWeekStart.minusWeeks(1) },
                onNextWeek = { currentWeekStart = currentWeekStart.plusWeeks(1) },
                onSettingsClick = { showColorSettings = true },
                isDistanceLocked = isDistanceLocked,
                onDistanceLockToggle = onDistanceLockToggle,
                onTimeDistanceSettingsClick = { showTimeSettings = true },
                currentViewMode = currentViewMode,
                onViewModeChange = { newMode ->
                    currentViewMode = newMode
                    // Save to SharedPreferences
                    viewModePrefs.edit().putInt("view_mode", newMode.ordinal).apply()
                },
                showViewModeMenu = showViewModeMenu,
                onShowViewModeMenu = { showViewModeMenu = it },
                buttonPosition = viewModeButtonPosition,
                onButtonPositionChange = { buttonWindowPos ->
                    // Calculate relative position within container (like DayCell radial menu)
                    viewModeButtonPosition = Offset(
                        x = buttonWindowPos.x - containerPosition.x,
                        y = buttonWindowPos.y - containerPosition.y
                    )
                },
                hoveredViewMode = hoveredViewMode,
                onHoveredViewModeChange = { hoveredViewMode = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Table structure (header + grid as one unit)
            Column(modifier = Modifier.fillMaxWidth()) {
                // Render different views based on currentViewMode
                when (currentViewMode) {
                    CalendarViewMode.HORIZONTAL_MONTH -> {
                        // ANSICHT 1: Original horizontal month view
                        WeekdayHeaders()
                        MonthCalendarGrid(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            mode = mode,
                            onStartDateSelected = onStartDateSelected ?: onDateSelected,
                            onEndDateSelected = onEndDateSelected,
                            events = events,
                            occupancyCalculator = occupancyCalculator,
                            tasks = tasks,
                            timeBlocks = timeBlocks,
                            currentTaskId = currentTaskId,
                            currentCategoryId = currentCategoryId,
                            activeButtonDate = activeButtonDate,
                            onButtonRequest = { date ->
                                activeButtonDate = date
                            },
                            onSetAsStart = { date ->
                                android.util.Log.d("MonthViewDatePicker", "🔥 onSetAsStart: date=$date, callback=${onStartDateSelected != null || onDateSelected != null}")
                                (onStartDateSelected ?: onDateSelected)?.invoke(date)
                                activeButtonDate = null
                            },
                            onSetAsEnd = { date ->
                                android.util.Log.d("MonthViewDatePicker", "🔥 onSetAsEnd: date=$date, callback=${onEndDateSelected != null}")
                                onEndDateSelected?.invoke(date)
                                activeButtonDate = null
                            },
                            onChangeStartTime = {
                                showStartTimeInput = true
                                activeButtonDate = null
                            },
                            onChangeEndTime = {
                                showEndTimeInput = true
                                activeButtonDate = null
                            }
                        )
                    }

                    CalendarViewMode.VERTICAL_MONTH -> {
                        // ANSICHT 2: Vertical month view with tall cells
                        WeekdayHeaders()
                        VerticalMonthCalendarGrid(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            mode = mode,
                            onStartDateSelected = onStartDateSelected ?: onDateSelected,
                            onEndDateSelected = onEndDateSelected,
                            events = events,
                            occupancyCalculator = occupancyCalculator,
                            tasks = tasks,
                            timeBlocks = timeBlocks,
                            currentTaskId = currentTaskId,
                            currentCategoryId = currentCategoryId,
                            activeButtonDate = activeButtonDate,
                            onButtonRequest = { date -> activeButtonDate = date },
                            onSetAsStart = { date ->
                                (onStartDateSelected ?: onDateSelected)?.invoke(date)
                                activeButtonDate = null
                            },
                            onSetAsEnd = { date ->
                                onEndDateSelected?.invoke(date)
                                activeButtonDate = null
                            },
                            onChangeStartTime = {
                                showStartTimeInput = true
                                activeButtonDate = null
                            },
                            onChangeEndTime = {
                                showEndTimeInput = true
                                activeButtonDate = null
                            }
                        )
                    }

                    CalendarViewMode.WEEK_VIEW -> {
                        // ANSICHT 3: Week view (single row of 7 days)
                        WeekViewCalendar(
                            weekStart = currentWeekStart,
                            selectedDate = selectedDate,
                            mode = mode,
                            onStartDateSelected = onStartDateSelected ?: onDateSelected,
                            onEndDateSelected = onEndDateSelected,
                            events = events,
                            occupancyCalculator = occupancyCalculator,
                            tasks = tasks,
                            timeBlocks = timeBlocks,
                            currentTaskId = currentTaskId,
                            currentCategoryId = currentCategoryId,
                            activeButtonDate = activeButtonDate,
                            onButtonRequest = { date -> activeButtonDate = date },
                            onSetAsStart = { date ->
                                (onStartDateSelected ?: onDateSelected)?.invoke(date)
                                activeButtonDate = null
                            },
                            onSetAsEnd = { date ->
                                onEndDateSelected?.invoke(date)
                                activeButtonDate = null
                            },
                            onChangeStartTime = {
                                showStartTimeInput = true
                                activeButtonDate = null
                            },
                            onChangeEndTime = {
                                showEndTimeInput = true
                                activeButtonDate = null
                            }
                        )
                    }
                }
            }
        }
        }

        // Radial Menu Overlay - ONLY VISUAL, no pointer input!
        if (showViewModeMenu) {
            CalendarViewModeRadialMenu(
                currentViewMode = currentViewMode,
                hoveredViewMode = hoveredViewMode,
                buttonPosition = viewModeButtonPosition
            )
        }
    }

    // Color Settings Dialog
    if (showColorSettings) {
        CalendarColorSettingsDialog(
            onDismiss = {
                showColorSettings = false
                refreshKey++ // Trigger recomposition to reload colors
            }
        )
    }

    // Use refreshKey in a LaunchedEffect to force recomposition
    LaunchedEffect(refreshKey) {
        // This will trigger recomposition whenever refreshKey changes
    }

    // Time Input Dialogs
    if (showStartTimeInput && startTime != null && onStartTimeChange != null) {
        HHMMInputDialog(
            currentHour = startTime.hour,
            currentMinute = startTime.minute,
            onDismiss = { showStartTimeInput = false },
            onConfirm = { hour, minute ->
                onStartTimeChange(LocalTime.of(hour, minute))
                showStartTimeInput = false
            }
        )
    }

    if (showEndTimeInput && endTime != null && onEndTimeChange != null) {
        HHMMInputDialog(
            currentHour = endTime.hour,
            currentMinute = endTime.minute,
            onDismiss = { showEndTimeInput = false },
            onConfirm = { hour, minute ->
                onEndTimeChange(LocalTime.of(hour, minute))
                showEndTimeInput = false
            }
        )
    }

    // Time Settings Dialog
    if (showTimeSettings && selectedDate != null && startTime != null && endTime != null && onStartTimeChange != null && onEndTimeChange != null) {
        TimeDistanceSettingsDialog(
            selectedDate = selectedDate,
            startTime = startTime,
            endTime = endTime,
            onStartTimeClick = { showStartTimeInput = true },
            onEndTimeClick = { showEndTimeInput = true },
            onEndTimeChange = onEndTimeChange,
            onDismiss = { showTimeSettings = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarColorSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("calendar_colors", Context.MODE_PRIVATE)
    }
    val repository = remember { SimpleCalendarColorRepository(prefs) }
    var colorConfig by remember { mutableStateOf(repository.loadConfig()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var editingColor by remember { mutableStateOf<CalendarColorType?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kalender-Farben") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Own Task Color
                ColorSettingRowWithToggle(
                    label = "Eigener Task",
                    description = "Der aktuell geöffnete Task",
                    colorHex = colorConfig.ownTaskColor,
                    enabled = colorConfig.ownTaskEnabled,
                    onEnabledChange = {
                        colorConfig = colorConfig.copy(ownTaskEnabled = it)
                        repository.saveConfig(colorConfig)
                    },
                    onColorClick = {
                        editingColor = CalendarColorType.OWN_TASK
                        showColorPicker = true
                    }
                )

                // Same Category Color
                ColorSettingRowWithToggle(
                    label = "Gleiche Kategorie",
                    description = "Tasks aus der gleichen Kategorie",
                    colorHex = colorConfig.sameCategoryColor,
                    enabled = colorConfig.sameCategoryEnabled,
                    onEnabledChange = {
                        colorConfig = colorConfig.copy(sameCategoryEnabled = it)
                        repository.saveConfig(colorConfig)
                    },
                    onColorClick = {
                        editingColor = CalendarColorType.SAME_CATEGORY
                        showColorPicker = true
                    }
                )

                // Other Task Color
                ColorSettingRowWithToggle(
                    label = "Andere Tasks",
                    description = "Eigene Tasks aus anderen Kategorien",
                    colorHex = colorConfig.otherTaskColor,
                    enabled = colorConfig.otherTaskEnabled,
                    onEnabledChange = {
                        colorConfig = colorConfig.copy(otherTaskEnabled = it)
                        repository.saveConfig(colorConfig)
                    },
                    onColorClick = {
                        editingColor = CalendarColorType.OTHER_TASK
                        showColorPicker = true
                    }
                )

                // External Event Color
                ColorSettingRowWithToggle(
                    label = "Google Calendar",
                    description = "Externe Google Calendar Events",
                    colorHex = colorConfig.externalEventColor,
                    enabled = colorConfig.externalEventEnabled,
                    onEnabledChange = {
                        colorConfig = colorConfig.copy(externalEventEnabled = it)
                        repository.saveConfig(colorConfig)
                    },
                    onColorClick = {
                        editingColor = CalendarColorType.EXTERNAL_EVENT
                        showColorPicker = true
                    }
                )

                // TimeBlock Color
                ColorSettingRowWithToggle(
                    label = "Zeitblockierungen",
                    description = "Blockierte Zeitfenster",
                    colorHex = colorConfig.timeBlockColor,
                    enabled = colorConfig.timeBlockEnabled,
                    onEnabledChange = {
                        colorConfig = colorConfig.copy(timeBlockEnabled = it)
                        repository.saveConfig(colorConfig)
                    },
                    onColorClick = {
                        editingColor = CalendarColorType.TIME_BLOCK
                        showColorPicker = true
                    }
                )

                // Overlap Color
                ColorSettingRowWithToggle(
                    label = "Überschneidung",
                    description = "Konflikte zwischen Tasks/Events",
                    colorHex = colorConfig.overlapColor,
                    enabled = colorConfig.overlapEnabled,
                    onEnabledChange = {
                        colorConfig = colorConfig.copy(overlapEnabled = it)
                        repository.saveConfig(colorConfig)
                    },
                    onColorClick = {
                        editingColor = CalendarColorType.OVERLAP
                        showColorPicker = true
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fertig")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                colorConfig = SimpleCalendarColorConfig.default()
                repository.saveConfig(colorConfig)
            }) {
                Text("Zurücksetzen")
            }
        }
    )

    // Color Picker Dialog
    if (showColorPicker && editingColor != null) {
        val currentColor = when (editingColor) {
            CalendarColorType.OWN_TASK -> colorConfig.ownTaskColor
            CalendarColorType.SAME_CATEGORY -> colorConfig.sameCategoryColor
            CalendarColorType.OTHER_TASK -> colorConfig.otherTaskColor
            CalendarColorType.EXTERNAL_EVENT -> colorConfig.externalEventColor
            CalendarColorType.TIME_BLOCK -> colorConfig.timeBlockColor
            CalendarColorType.OVERLAP -> colorConfig.overlapColor
            null -> "#FFFFFF"
        }

        ColorPickerDialog(
            currentColor = currentColor,
            onColorSelected = { newColor ->
                colorConfig = when (editingColor) {
                    CalendarColorType.OWN_TASK -> colorConfig.copy(ownTaskColor = newColor)
                    CalendarColorType.SAME_CATEGORY -> colorConfig.copy(sameCategoryColor = newColor)
                    CalendarColorType.OTHER_TASK -> colorConfig.copy(otherTaskColor = newColor)
                    CalendarColorType.EXTERNAL_EVENT -> colorConfig.copy(externalEventColor = newColor)
                    CalendarColorType.TIME_BLOCK -> colorConfig.copy(timeBlockColor = newColor)
                    CalendarColorType.OVERLAP -> colorConfig.copy(overlapColor = newColor)
                    null -> colorConfig
                }
                repository.saveConfig(colorConfig)
                showColorPicker = false
                editingColor = null
            },
            onDismiss = {
                showColorPicker = false
                editingColor = null
            }
        )
    }
}

@Composable
private fun ColorSettingRowWithToggle(
    label: String,
    description: String,
    colorHex: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onColorClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = enabled, onClick = onColorClick)
            ) {
                Text(
                    text = colorHex.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (enabled) parseColorHex(colorHex) else Color.Gray.copy(alpha = 0.3f))
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ColorSettingRow(
    label: String,
    description: String,
    colorHex: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = colorHex.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(colorHex))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

private enum class CalendarColorType {
    OWN_TASK,
    SAME_CATEGORY,
    OTHER_TASK,
    EXTERNAL_EVENT,
    TIME_BLOCK,
    OVERLAP
}

@Composable
private fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    currentWeekStart: LocalDate = LocalDate.now(),
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onSettingsClick: () -> Unit,
    isDistanceLocked: Boolean = false,
    onDistanceLockToggle: (() -> Unit)? = null,
    onTimeDistanceSettingsClick: (() -> Unit)? = null,
    currentViewMode: CalendarViewMode = CalendarViewMode.HORIZONTAL_MONTH,
    onViewModeChange: (CalendarViewMode) -> Unit = {},
    showViewModeMenu: Boolean = false,
    onShowViewModeMenu: (Boolean) -> Unit = {},
    buttonPosition: Offset = Offset.Zero,
    onButtonPositionChange: (Offset) -> Unit = {},
    hoveredViewMode: CalendarViewMode? = null,
    onHoveredViewModeChange: (CalendarViewMode?) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Navigation buttons (← →) - different behavior for Week vs Month view
        IconButton(
            onClick = if (currentViewMode == CalendarViewMode.WEEK_VIEW) onPreviousWeek else onPreviousMonth
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = if (currentViewMode == CalendarViewMode.WEEK_VIEW) "Vorherige Woche" else "Vorheriger Monat"
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display depends on view mode
            Text(
                text = when (currentViewMode) {
                    CalendarViewMode.WEEK_VIEW -> {
                        // Show "KW XX" (Kalenderwoche)
                        val weekOfYear = currentWeekStart.get(java.time.temporal.WeekFields.of(Locale.GERMAN).weekOfWeekBasedYear())
                        "KW $weekOfYear"
                    }
                    else -> {
                        // Show "MMM yy" for month views
                        currentMonth.format(DateTimeFormatter.ofPattern("MMM yy", Locale.GERMAN))
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            // View Mode Button - Press & Drag (like TaskDialogRadialTabSwitcher)
            // CRITICAL: ALL drag logic here, overlay is ONLY visual
            var dragOffset by remember { mutableStateOf(Offset.Zero) }

            // LIVE VIEW MODE SWITCHING - Change view immediately when hovering over option
            LaunchedEffect(hoveredViewMode) {
                hoveredViewMode?.let { mode ->
                    if (mode != currentViewMode) {
                        android.util.Log.d("ViewModeRadial", "🔄 Live view switch: $currentViewMode → $mode")
                        onViewModeChange(mode)
                    }
                }
            }

            val density = LocalDensity.current
            val buttonSizePx = with(density) { 40.dp.toPx() }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .onGloballyPositioned { coordinates ->
                        // Get top-left corner position and add half button size to center it
                        val topLeft = coordinates.positionInWindow()
                        val centered = Offset(
                            x = topLeft.x + (buttonSizePx / 2f),
                            y = topLeft.y + (buttonSizePx / 2f)
                        )
                        onButtonPositionChange(centered)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                android.util.Log.d("ViewModeRadial", "🎮 Drag started")
                                onShowViewModeMenu(true)
                                dragOffset = Offset.Zero
                                onHoveredViewModeChange(null)
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                dragOffset += drag
                                val distance = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)

                                if (distance > 20f) {
                                    val angle = atan2(dragOffset.y, dragOffset.x) * 180 / PI.toFloat()
                                    val normalizedAngle = ((angle + 360) % 360)

                                    // 3 options pointing DOWN: DOWN=90, DOWN-LEFT=150, DOWN-RIGHT=30
                                    val modes = listOf(
                                        CalendarViewMode.HORIZONTAL_MONTH to 90f,   // DOWN
                                        CalendarViewMode.VERTICAL_MONTH to 150f,    // DOWN-LEFT
                                        CalendarViewMode.WEEK_VIEW to 30f           // DOWN-RIGHT
                                    )

                                    val closest = modes.minByOrNull { (_, modeAngle) ->
                                        val diff = abs(normalizedAngle - modeAngle)
                                        min(diff, 360 - diff)
                                    }

                                    android.util.Log.d("ViewModeRadial", "📍 Drag: angle=$normalizedAngle°, closest=${closest?.first}")
                                    onHoveredViewModeChange(closest?.first)
                                } else {
                                    onHoveredViewModeChange(null)
                                }
                            },
                            onDragEnd = {
                                android.util.Log.d("ViewModeRadial", "✅ DragEnd: View already changed via LaunchedEffect")
                                // View mode was already changed live via LaunchedEffect, just close menu
                                onShowViewModeMenu(false)
                                dragOffset = Offset.Zero
                                onHoveredViewModeChange(null)
                            },
                            onDragCancel = {
                                onShowViewModeMenu(false)
                                dragOffset = Offset.Zero
                                onHoveredViewModeChange(null)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Ansicht wechseln (Halten & Ziehen)",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Farbeinstellungen",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Lock button for distance locking (only show if callback provided)
            if (onDistanceLockToggle != null) {
                IconButton(onClick = onDistanceLockToggle) {
                    Icon(
                        imageVector = if (isDistanceLocked) Icons.Default.Lock else Icons.Default.Close,
                        contentDescription = if (isDistanceLocked) "Distanz gesperrt" else "Distanz freigegeben",
                        tint = if (isDistanceLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Time distance settings button (only show if callback provided)
            if (onTimeDistanceSettingsClick != null) {
                IconButton(onClick = onTimeDistanceSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Zeitdistanz-Einstellungen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        IconButton(
            onClick = if (currentViewMode == CalendarViewMode.WEEK_VIEW) onNextWeek else onNextMonth
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (currentViewMode == CalendarViewMode.WEEK_VIEW) "Nächste Woche" else "Nächster Monat"
            )
        }
    }
}

/**
 * Radial Menu for Calendar View Mode Selection
 * VISUAL ONLY - No pointer input! (drag handled by button)
 */
@Composable
private fun CalendarViewModeRadialMenu(
    currentViewMode: CalendarViewMode,
    hoveredViewMode: CalendarViewMode?,
    buttonPosition: Offset
) {
    val density = LocalDensity.current

    // 3 options pointing DOWN (more user-friendly, calendar is below)
    // DOWN=90°, DOWN-LEFT=150°, DOWN-RIGHT=30°
    val viewModeActions = listOf(
        ViewModeAction(mode = CalendarViewMode.HORIZONTAL_MONTH, label = "H", angle = 90f),   // DOWN
        ViewModeAction(mode = CalendarViewMode.VERTICAL_MONTH, label = "V", angle = 150f),    // DOWN-LEFT
        ViewModeAction(mode = CalendarViewMode.WEEK_VIEW, label = "W", angle = 30f)           // DOWN-RIGHT
    )

    val menuScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        ),
        label = "menu_scale"
    )

    // Calculate menu offset (center menu on button)
    val menuSizePx = with(density) { 180.dp.toPx() }
    val halfMenuSize = menuSizePx / 2f

    // Fullscreen overlay for dimming (NO POINTER INPUT!)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .zIndex(2000f)
    ) {
        // Menu positioned at button (NO POINTER INPUT!)
        // Position is already relative to container (calculated in MonthViewDatePicker)
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        (buttonPosition.x - halfMenuSize).toInt(),
                        (buttonPosition.y - halfMenuSize).toInt()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Center button (visual only)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Ziehen",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Radial menu options (visual only)
            viewModeActions.forEach { action ->
                val angleRad = Math.toRadians(action.angle.toDouble())
                val radius = 60.dp * menuScale
                val offsetX = (radius.value * cos(angleRad)).dp
                val offsetY = (radius.value * sin(angleRad)).dp
                val isSelected = hoveredViewMode == action.mode
                val isCurrent = currentViewMode == action.mode

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(if (isSelected) 50.dp else 44.dp)
                        .scale(menuScale)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        tonalElevation = if (isSelected) 8.dp else 4.dp,
                        shadowElevation = if (isSelected) 12.dp else 6.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ViewModeAction(
    val mode: CalendarViewMode,
    val label: String,
    val angle: Float
)

private fun getViewModeByAngle(angle: Float, actions: List<ViewModeAction>): CalendarViewMode? {
    val normalizedAngle = ((angle + 360) % 360)
    return actions.minByOrNull { action ->
        val diff = abs(normalizedAngle - action.angle)
        min(diff, 360 - diff)
    }?.mode
}

@Composable
private fun WeekdayHeaders() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp
                )
            )
            .padding(vertical = 8.dp)
    ) {
        val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    mode: CalendarMode,
    onStartDateSelected: ((LocalDate) -> Unit)?,
    onEndDateSelected: ((LocalDate) -> Unit)?,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    tasks: List<TaskEntity> = emptyList(),
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null,
    activeButtonDate: LocalDate? = null,
    onButtonRequest: (LocalDate) -> Unit = {},
    onSetAsStart: (LocalDate) -> Unit = {},
    onSetAsEnd: (LocalDate) -> Unit = {},
    onChangeStartTime: () -> Unit = {},
    onChangeEndTime: () -> Unit = {}
) {
    // Build list of dates for the month view (includes leading/trailing days)
    val calendarDates = buildCalendarDates(currentMonth)
    val rows = (calendarDates.size / 7) // Number of rows

    // Track button position for overlay rendering
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var gridPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp) // ~6 weeks * 40dp per row (kompakter)
            .onGloballyPositioned { coordinates ->
                gridPosition = coordinates.positionInWindow()
            }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            )
            .padding(1.dp) // Padding inside border for grid
    ) {
        // Grid cells
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top,
            userScrollEnabled = false
        ) {
            items(calendarDates) { date ->
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isCurrentMonth = date.month == currentMonth.month,
                    mode = mode,
                    onStartDateSelected = onStartDateSelected,
                    onEndDateSelected = onEndDateSelected,
                    events = events,
                    occupancyCalculator = occupancyCalculator,
                    tasks = tasks,
                    timeBlocks = timeBlocks,
                    currentTaskId = currentTaskId,
                    currentCategoryId = currentCategoryId,
                    showButton = activeButtonDate == date,
                    onButtonRequest = { cellWindowPosition, cellSize ->
                        // Calculate position relative to grid container, centered on cell
                        buttonPosition = Offset(
                            x = cellWindowPosition.x - gridPosition.x + (cellSize.width / 2f),
                            y = cellWindowPosition.y - gridPosition.y + (cellSize.height / 2f)
                        )
                        onButtonRequest(date)
                    },
                    onSetAsStart = { onSetAsStart(date) },
                    onSetAsEnd = { onSetAsEnd(date) },
                    onChangeStartTime = onChangeStartTime,
                    onChangeEndTime = onChangeEndTime
                )
            }
        }

        // Grid lines overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)
            val cellWidth = size.width / 7f
            val cellHeight = size.height / rows

            // Draw vertical lines (7 columns = 6 lines)
            for (i in 1 until 7) {
                val x = cellWidth * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }

            // Draw horizontal lines
            for (i in 1 until rows) {
                val y = cellHeight * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
        }

        // Radial menu overlay - rendered ABOVE all cells with zIndex
        if (activeButtonDate != null) {
            val density = LocalDensity.current
            val menuSizePx = with(density) { 200.dp.toPx() }
            val halfMenuSize = menuSizePx / 2f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1000f)
            ) {
                InlineDateRadialMenu(
                    onSetAsStart = {
                        android.util.Log.d("MonthViewDatePicker", "📅 Radial: Set as Start - date=$activeButtonDate")
                        onSetAsStart(activeButtonDate)
                    },
                    onSetAsEnd = {
                        android.util.Log.d("MonthViewDatePicker", "📅 Radial: Set as End - date=$activeButtonDate")
                        onSetAsEnd(activeButtonDate)
                    },
                    onChangeStartTime = {
                        android.util.Log.d("MonthViewDatePicker", "🕐 Radial: Change Start Time")
                        onChangeStartTime()
                    },
                    onChangeEndTime = {
                        android.util.Log.d("MonthViewDatePicker", "🕐 Radial: Change End Time")
                        onChangeEndTime()
                    },
                    modifier = Modifier.offset {
                        androidx.compose.ui.unit.IntOffset(
                            (buttonPosition.x - halfMenuSize).toInt(),
                            (buttonPosition.y - halfMenuSize).toInt()
                        )
                    }
                )
            }
        }
    }
}

/**
 * Build complete calendar grid including leading/trailing days
 * Returns 35-42 dates (5-6 weeks) starting from Monday
 */
private fun buildCalendarDates(yearMonth: YearMonth): List<LocalDate> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()

    // Find Monday of the week containing first day
    val startDate = firstDayOfMonth.minusDays(
        (firstDayOfMonth.dayOfWeek.value - 1).toLong()
    )

    // Find Sunday of the week containing last day
    val endDate = lastDayOfMonth.plusDays(
        (7 - lastDayOfMonth.dayOfWeek.value).toLong()
    )

    // Generate all dates in range
    val dates = mutableListOf<LocalDate>()
    var current = startDate
    while (!current.isAfter(endDate)) {
        dates.add(current)
        current = current.plusDays(1)
    }

    return dates
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    mode: CalendarMode,
    onStartDateSelected: ((LocalDate) -> Unit)?,
    onEndDateSelected: ((LocalDate) -> Unit)?,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    tasks: List<TaskEntity> = emptyList(),
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null,
    showButton: Boolean = false,
    onButtonRequest: (Offset, androidx.compose.ui.geometry.Size) -> Unit = { _, _ -> },
    onSetAsStart: () -> Unit = {},
    onSetAsEnd: () -> Unit = {},
    onChangeStartTime: () -> Unit = {},
    onChangeEndTime: () -> Unit = {}
) {
    val isToday = date == LocalDate.now()

    // Calculate occupancy segments for this day (includes events, tasks, and TimeBlocks)
    val segments = remember(date, events, tasks, timeBlocks, currentTaskId, currentCategoryId) {
        occupancyCalculator.calculateDayOccupancy(events, date, tasks, timeBlocks, currentTaskId, currentCategoryId)
    }

    var cellPosition by remember { mutableStateOf(Offset.Zero) }
    var cellSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coordinates ->
                cellPosition = coordinates.positionInWindow()
                cellSize = androidx.compose.ui.geometry.Size(
                    width = coordinates.size.width.toFloat(),
                    height = coordinates.size.height.toFloat()
                )
            }
            .clickable(
                enabled = !showButton,
                onClick = {
                    when (mode) {
                        CalendarMode.DIRECT_START -> {
                            // Direct click sets start date
                            onStartDateSelected?.invoke(date)
                        }
                        CalendarMode.DIRECT_END -> {
                            // Direct click sets end date
                            onEndDateSelected?.invoke(date)
                        }
                        CalendarMode.RADIAL_MENU -> {
                            // Click requests button/radial menu
                            if (!showButton) {
                                onButtonRequest(cellPosition, cellSize)
                            }
                        }
                    }
                }
            )
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .padding(2.dp)
    ) {
        // Show indicator dot if this cell is active (menu rendered at grid level)
        if (showButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day number
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 12.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Spacer(modifier = Modifier.weight(1f))

            // Occupancy bar visualization (HORIZONTAL)
            if (segments.isNotEmpty()) {
                OccupancyBar(
                    segments = segments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }
    }
}

/**
 * ANSICHT 2: Vertical Month Calendar Grid with tall cells
 */
@Composable
private fun VerticalMonthCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    mode: CalendarMode,
    onStartDateSelected: ((LocalDate) -> Unit)?,
    onEndDateSelected: ((LocalDate) -> Unit)?,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    tasks: List<TaskEntity> = emptyList(),
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null,
    activeButtonDate: LocalDate? = null,
    onButtonRequest: (LocalDate) -> Unit = {},
    onSetAsStart: (LocalDate) -> Unit = {},
    onSetAsEnd: (LocalDate) -> Unit = {},
    onChangeStartTime: () -> Unit = {},
    onChangeEndTime: () -> Unit = {}
) {
    val calendarDates = buildCalendarDates(currentMonth)
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var gridPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(600.dp) // TALL for vertical bars
            .onGloballyPositioned { coordinates ->
                gridPosition = coordinates.positionInWindow()
            }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(1.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top,
            userScrollEnabled = true // LazyGrid handles scroll internally
        ) {
            items(
                count = calendarDates.size,
                key = { index -> calendarDates[index].toEpochDay() }
            ) { index ->
                val date = calendarDates[index]
                VerticalDayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isCurrentMonth = date.month == currentMonth.month,
                    mode = mode,
                    onStartDateSelected = onStartDateSelected,
                    onEndDateSelected = onEndDateSelected,
                    events = events,
                    occupancyCalculator = occupancyCalculator,
                    tasks = tasks,
                    timeBlocks = timeBlocks,
                    currentTaskId = currentTaskId,
                    currentCategoryId = currentCategoryId,
                    showButton = activeButtonDate == date,
                    onButtonRequest = { cellWindowPosition, cellSize ->
                        buttonPosition = Offset(
                            x = cellWindowPosition.x - gridPosition.x + (cellSize.width / 2f),
                            y = cellWindowPosition.y - gridPosition.y + (cellSize.height / 2f)
                        )
                        onButtonRequest(date)
                    },
                    onSetAsStart = { onSetAsStart(date) },
                    onSetAsEnd = { onSetAsEnd(date) },
                    onChangeStartTime = onChangeStartTime,
                    onChangeEndTime = onChangeEndTime,
                    modifier = Modifier.height(100.dp) // EXPLICIT HEIGHT für vertikale Balken
                )
            }
        }

        // Radial menu overlay
        if (activeButtonDate != null) {
            val density = LocalDensity.current
            val menuSizePx = with(density) { 200.dp.toPx() }
            val halfMenuSize = menuSizePx / 2f

            Box(modifier = Modifier.fillMaxSize().zIndex(1000f)) {
                InlineDateRadialMenu(
                    onSetAsStart = { onSetAsStart(activeButtonDate) },
                    onSetAsEnd = { onSetAsEnd(activeButtonDate) },
                    onChangeStartTime = onChangeStartTime,
                    onChangeEndTime = onChangeEndTime,
                    modifier = Modifier.offset {
                        androidx.compose.ui.unit.IntOffset(
                            (buttonPosition.x - halfMenuSize).toInt(),
                            (buttonPosition.y - halfMenuSize).toInt()
                        )
                    }
                )
            }
        }
    }
}

/**
 * ANSICHT 3: Week View Calendar (single row of 7 days)
 */
@Composable
private fun WeekViewCalendar(
    weekStart: LocalDate,
    selectedDate: LocalDate?,
    mode: CalendarMode,
    onStartDateSelected: ((LocalDate) -> Unit)?,
    onEndDateSelected: ((LocalDate) -> Unit)?,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    tasks: List<TaskEntity> = emptyList(),
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null,
    activeButtonDate: LocalDate? = null,
    onButtonRequest: (LocalDate) -> Unit = {},
    onSetAsStart: (LocalDate) -> Unit = {},
    onSetAsEnd: (LocalDate) -> Unit = {},
    onChangeStartTime: () -> Unit = {},
    onChangeEndTime: () -> Unit = {}
) {
    // Use provided weekStart (from parent state management)
    val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }

    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var gridPosition by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Weekday headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
                .padding(vertical = 8.dp)
        ) {
            val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp) // KOMPAKT wie horizontal month view
                .onGloballyPositioned { coordinates ->
                    gridPosition = coordinates.positionInWindow()
                }
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(1.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                weekDates.forEach { date ->
                    VerticalDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isCurrentMonth = true, // All days in week view are equally important
                        mode = mode,
                        onStartDateSelected = onStartDateSelected,
                        onEndDateSelected = onEndDateSelected,
                        events = events,
                        occupancyCalculator = occupancyCalculator,
                        tasks = tasks,
                        timeBlocks = timeBlocks,
                        currentTaskId = currentTaskId,
                        currentCategoryId = currentCategoryId,
                        showButton = activeButtonDate == date,
                        onButtonRequest = { cellWindowPosition, cellSize ->
                            buttonPosition = Offset(
                                x = cellWindowPosition.x - gridPosition.x + (cellSize.width / 2f),
                                y = cellWindowPosition.y - gridPosition.y + (cellSize.height / 2f)
                            )
                            onButtonRequest(date)
                        },
                        onSetAsStart = { onSetAsStart(date) },
                        onSetAsEnd = { onSetAsEnd(date) },
                        onChangeStartTime = onChangeStartTime,
                        onChangeEndTime = onChangeEndTime,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Radial menu overlay
            if (activeButtonDate != null) {
                val density = LocalDensity.current
                val menuSizePx = with(density) { 200.dp.toPx() }
                val halfMenuSize = menuSizePx / 2f

                Box(modifier = Modifier.fillMaxSize().zIndex(1000f)) {
                    InlineDateRadialMenu(
                        onSetAsStart = { onSetAsStart(activeButtonDate) },
                        onSetAsEnd = { onSetAsEnd(activeButtonDate) },
                        onChangeStartTime = onChangeStartTime,
                        onChangeEndTime = onChangeEndTime,
                        modifier = Modifier.offset {
                            androidx.compose.ui.unit.IntOffset(
                                (buttonPosition.x - halfMenuSize).toInt(),
                                (buttonPosition.y - halfMenuSize).toInt()
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Vertical Day Cell - TALL cell with vertical 24h occupancy bar
 */
@Composable
private fun VerticalDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    mode: CalendarMode,
    onStartDateSelected: ((LocalDate) -> Unit)?,
    onEndDateSelected: ((LocalDate) -> Unit)?,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    tasks: List<TaskEntity> = emptyList(),
    timeBlocks: List<com.example.questflow.data.database.entity.TimeBlockEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null,
    showButton: Boolean = false,
    onButtonRequest: (Offset, androidx.compose.ui.geometry.Size) -> Unit = { _, _ -> },
    onSetAsStart: () -> Unit = {},
    onSetAsEnd: () -> Unit = {},
    onChangeStartTime: () -> Unit = {},
    onChangeEndTime: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isToday = date == LocalDate.now()
    val segments = remember(date, events, tasks, timeBlocks, currentTaskId, currentCategoryId) {
        occupancyCalculator.calculateDayOccupancy(events, date, tasks, timeBlocks, currentTaskId, currentCategoryId)
    }

    var cellPosition by remember { mutableStateOf(Offset.Zero) }
    var cellSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                cellPosition = coordinates.positionInWindow()
                cellSize = androidx.compose.ui.geometry.Size(
                    width = coordinates.size.width.toFloat(),
                    height = coordinates.size.height.toFloat()
                )
            }
            .clickable(
                enabled = !showButton,
                onClick = {
                    when (mode) {
                        CalendarMode.DIRECT_START -> onStartDateSelected?.invoke(date)
                        CalendarMode.DIRECT_END -> onEndDateSelected?.invoke(date)
                        CalendarMode.RADIAL_MENU -> {
                            if (!showButton) {
                                onButtonRequest(cellPosition, cellSize)
                            }
                        }
                    }
                }
            )
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .padding(4.dp)
    ) {
        // NEW LAYOUT: Bar takes FULL WIDTH, day number overlaid on top
        Box(modifier = Modifier.fillMaxSize()) {
            // Vertical occupancy bar (FULL WIDTH for maximum visibility)
            if (segments.isNotEmpty()) {
                VerticalOccupancyBar(
                    segments = segments,
                    modifier = Modifier.fillMaxSize() // Full cell width!
                )
            }

            // Day number overlaid on bar (white with black shadow for visibility)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // White text with strong black shadow for contrast on any background color
                Text(
                    text = String.format("%02d", date.dayOfMonth),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                            blurRadius = 3f
                        )
                    )
                )

                // Button indicator (if active)
                if (showButton) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 20.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/**
 * Inline radial menu for date selection - renders directly in cell
 * Press & hold button → Drag to option → Release to select
 * NO OVERLAY - Calendar remains fully functional!
 */
@Composable
private fun InlineDateRadialMenu(
    onSetAsStart: () -> Unit,
    onSetAsEnd: () -> Unit,
    onChangeStartTime: () -> Unit,
    onChangeEndTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isMenuActive by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedAction by remember { mutableStateOf<String?>(null) }

    // FIXED ANGLES (2025-10-22): User requirements
    // - Links (180°): Als Start
    // - Rechts (0°): Als Ende
    // - Oben (270°): Startzeit
    // - Unten (90°): Endzeit
    val actions = listOf(
        RadialMenuAction(id = "end", label = "Ende", angle = 0f),          // Rechts
        RadialMenuAction(id = "end_time", label = "Bis", angle = 90f),     // Unten
        RadialMenuAction(id = "start", label = "Start", angle = 180f),     // Links
        RadialMenuAction(id = "start_time", label = "Von", angle = 270f)   // Oben
    )

    // Animation für Menu-Scale
    val menuScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isMenuActive) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        ),
        label = "menu_scale"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center button with drag detection
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isMenuActive = true
                            dragOffset = Offset.Zero
                            selectedAction = null
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            dragOffset += drag
                            val distance = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)
                            if (distance > 20f) {
                                val angle = atan2(dragOffset.y, dragOffset.x) * 180 / PI.toFloat()
                                selectedAction = getActionByAngle(angle, actions)
                            } else {
                                selectedAction = null
                            }
                        },
                        onDragEnd = {
                            android.util.Log.d("InlineDateRadialMenu", "🎯 onDragEnd: selectedAction=$selectedAction")
                            val actionLabel = actions.find { it.id == selectedAction }?.label
                            when (selectedAction) {
                                "start" -> {
                                    android.util.Log.d("InlineDateRadialMenu", "   → Calling onSetAsStart()")
                                    onSetAsStart()
                                    android.widget.Toast.makeText(context, "✓ Als Startdatum gesetzt", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                "end" -> {
                                    android.util.Log.d("InlineDateRadialMenu", "   → Calling onSetAsEnd()")
                                    onSetAsEnd()
                                    android.widget.Toast.makeText(context, "✓ Als Enddatum gesetzt", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                "start_time" -> {
                                    android.util.Log.d("InlineDateRadialMenu", "   → Calling onChangeStartTime()")
                                    onChangeStartTime()
                                    android.widget.Toast.makeText(context, "✓ Startzeit ändern", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                "end_time" -> {
                                    android.util.Log.d("InlineDateRadialMenu", "   → Calling onChangeEndTime()")
                                    onChangeEndTime()
                                    android.widget.Toast.makeText(context, "✓ Endzeit ändern", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                null -> {
                                    android.util.Log.d("InlineDateRadialMenu", "   → No action selected")
                                    android.widget.Toast.makeText(context, "Keine Option gewählt", android.widget.Toast.LENGTH_SHORT).show()
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
                imageVector = Icons.Default.DateRange,
                contentDescription = "Datum-Menü",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }

        // Radial menu buttons with animation
        if (isMenuActive) {
            actions.forEach { action ->
                val angleRad = Math.toRadians(action.angle.toDouble())
                val radius = 60.dp * menuScale  // Größerer Radius, animiert
                val offsetX = (radius.value * cos(angleRad)).dp
                val offsetY = (radius.value * sin(angleRad)).dp
                val isSelected = selectedAction == action.id

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(if (isSelected) 44.dp else 40.dp)
                        .scale(menuScale)
                ) {
                    androidx.compose.material3.Surface(
                        shape = CircleShape,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = if (isSelected) 8.dp else 4.dp,
                        shadowElevation = if (isSelected) 12.dp else 6.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = action.label.take(1),  // Erster Buchstabe
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

data class RadialMenuAction(
    val id: String,
    val label: String,
    val angle: Float
)

private fun getActionByAngle(angle: Float, actions: List<RadialMenuAction>): String? {
    val normalizedAngle = ((angle + 360) % 360)
    return actions.minByOrNull { action ->
        val diff = abs(normalizedAngle - action.angle)
        min(diff, 360 - diff)
    }?.id
}

@Composable
private fun OccupancyBar(
    segments: List<DayOccupancyCalculator.TimeSegment>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("calendar_colors", Context.MODE_PRIVATE)
    }
    val repository = remember { SimpleCalendarColorRepository(prefs) }
    // Make colorConfig reactive by loading it every time this composable recomposes
    val colorConfig = repository.loadConfig()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        segments.forEach { segment ->
            // NEW LOGIC: Use separate flags for each task type
            val actualType = when {
                !segment.isOccupied -> SegmentType.FREE

                // Count how many task types are BOTH present AND enabled
                else -> {
                    var enabledTypesInSegment = 0

                    if (segment.hasCurrentTask && colorConfig.ownTaskEnabled) enabledTypesInSegment++
                    if (segment.hasSameCategory && colorConfig.sameCategoryEnabled) enabledTypesInSegment++
                    if (segment.hasOtherOwnTasks && colorConfig.otherTaskEnabled) enabledTypesInSegment++
                    if (segment.hasExternalEvents && colorConfig.externalEventEnabled) enabledTypesInSegment++
                    if (segment.hasTimeBlocks && colorConfig.timeBlockEnabled) enabledTypesInSegment++

                    android.util.Log.d("MonthViewDebug", "📊 Segment [${segment.startHour}-${segment.endHour}]: cur=${segment.hasCurrentTask} sam=${segment.hasSameCategory} oth=${segment.hasOtherOwnTasks} ext=${segment.hasExternalEvents} tim=${segment.hasTimeBlocks} | enabled=$enabledTypesInSegment")

                    when {
                        // 2+ types present AND enabled AND overlap display enabled -> BLACK
                        enabledTypesInSegment >= 2 && colorConfig.overlapEnabled -> {
                            android.util.Log.d("MonthViewDebug", "✅ OVERLAP shown (black)")
                            SegmentType.OVERLAP
                        }

                        // 2+ types but overlap disabled OR only 1 type -> Show highest priority
                        else -> {
                            when {
                                segment.hasCurrentTask && colorConfig.ownTaskEnabled -> SegmentType.OWN_TASK
                                segment.hasSameCategory && colorConfig.sameCategoryEnabled -> SegmentType.SAME_CATEGORY
                                segment.hasOtherOwnTasks && colorConfig.otherTaskEnabled -> SegmentType.OTHER_TASK
                                segment.hasExternalEvents && colorConfig.externalEventEnabled -> SegmentType.EXTERNAL_EVENT
                                segment.hasTimeBlocks && colorConfig.timeBlockEnabled -> SegmentType.TIME_BLOCK
                                else -> SegmentType.FREE
                            }
                        }
                    }
                }
            }

            // Clamp weight to positive value (min 0.01f to ensure visibility)
            val weight = segment.weightInDay.coerceAtLeast(0.01f)

            val color = when (actualType) {
                SegmentType.FREE -> Color(0xFF66BB6A) // Green
                SegmentType.OVERLAP -> parseColorHex(colorConfig.overlapColor)
                SegmentType.OWN_TASK -> parseColorHex(colorConfig.ownTaskColor)
                SegmentType.SAME_CATEGORY -> parseColorHex(colorConfig.sameCategoryColor)
                SegmentType.OTHER_TASK -> parseColorHex(colorConfig.otherTaskColor)
                SegmentType.EXTERNAL_EVENT -> parseColorHex(colorConfig.externalEventColor)
                SegmentType.TIME_BLOCK -> parseColorHex(colorConfig.timeBlockColor)
            }

            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

/**
 * Vertical occupancy bar - 24h vertical visualization
 * Used in VERTICAL_MONTH and WEEK_VIEW modes
 */
@Composable
private fun VerticalOccupancyBar(
    segments: List<DayOccupancyCalculator.TimeSegment>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("calendar_colors", Context.MODE_PRIVATE)
    }
    val repository = remember { SimpleCalendarColorRepository(prefs) }
    val colorConfig = repository.loadConfig()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        segments.forEach { segment ->
            // Same logic as horizontal bar
            val actualType = when {
                !segment.isOccupied -> SegmentType.FREE
                else -> {
                    var enabledTypesInSegment = 0

                    if (segment.hasCurrentTask && colorConfig.ownTaskEnabled) enabledTypesInSegment++
                    if (segment.hasSameCategory && colorConfig.sameCategoryEnabled) enabledTypesInSegment++
                    if (segment.hasOtherOwnTasks && colorConfig.otherTaskEnabled) enabledTypesInSegment++
                    if (segment.hasExternalEvents && colorConfig.externalEventEnabled) enabledTypesInSegment++
                    if (segment.hasTimeBlocks && colorConfig.timeBlockEnabled) enabledTypesInSegment++

                    when {
                        enabledTypesInSegment >= 2 && colorConfig.overlapEnabled -> SegmentType.OVERLAP
                        else -> {
                            when {
                                segment.hasCurrentTask && colorConfig.ownTaskEnabled -> SegmentType.OWN_TASK
                                segment.hasSameCategory && colorConfig.sameCategoryEnabled -> SegmentType.SAME_CATEGORY
                                segment.hasOtherOwnTasks && colorConfig.otherTaskEnabled -> SegmentType.OTHER_TASK
                                segment.hasExternalEvents && colorConfig.externalEventEnabled -> SegmentType.EXTERNAL_EVENT
                                segment.hasTimeBlocks && colorConfig.timeBlockEnabled -> SegmentType.TIME_BLOCK
                                else -> SegmentType.FREE
                            }
                        }
                    }
                }
            }

            val weight = segment.weightInDay.coerceAtLeast(0.01f)

            val color = when (actualType) {
                SegmentType.FREE -> Color(0xFF66BB6A)
                SegmentType.OVERLAP -> parseColorHex(colorConfig.overlapColor)
                SegmentType.OWN_TASK -> parseColorHex(colorConfig.ownTaskColor)
                SegmentType.SAME_CATEGORY -> parseColorHex(colorConfig.sameCategoryColor)
                SegmentType.OTHER_TASK -> parseColorHex(colorConfig.otherTaskColor)
                SegmentType.EXTERNAL_EVENT -> parseColorHex(colorConfig.externalEventColor)
                SegmentType.TIME_BLOCK -> parseColorHex(colorConfig.timeBlockColor)
            }

            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .background(color)
            )
        }
    }
}

private enum class SegmentType {
    FREE,
    OVERLAP,
    OWN_TASK,
    SAME_CATEGORY,
    OTHER_TASK,
    EXTERNAL_EVENT,
    TIME_BLOCK
}

private fun parseColorHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

/**
 * Detail list showing all events and tasks for a specific day
 * with debug information about occupancy calculation and color legend
 */
@Composable
private fun DayDetailsList(
    date: LocalDate,
    events: List<CalendarEventLinkEntity>,
    tasks: List<TaskEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("calendar_colors", Context.MODE_PRIVATE)
    }
    val repository = remember { SimpleCalendarColorRepository(prefs) }
    val colorConfig = remember { repository.loadConfig() }

    val dayStart = date.atStartOfDay()
    val dayEnd = date.plusDays(1).atStartOfDay()

    // Filter events for this day
    val dayEvents = events.filter { event ->
        event.startsAt.toLocalDate() == date ||
        (event.startsAt.isBefore(dayEnd) && event.endsAt.isAfter(dayStart))
    }

    // Filter tasks for this day
    val dayTasks = tasks.filter { task ->
        task.dueDate?.toLocalDate() == date && !task.isCompleted
    }

    // Calculate occupancy for debug info (TimeBlocks not included in this detail view)
    val segments = occupancyCalculator.calculateDayOccupancy(events, date, tasks, emptyList(), currentTaskId, currentCategoryId)
    val occupiedSegments = segments.filter { it.isOccupied }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp) // Maximale Höhe für Scroll
        ) {
            // Header (fixed, nicht scrollbar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Details: ${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Farblegende
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorLegendItem("Weiß", parseColorHex(colorConfig.ownTaskColor), "Eigener Task")
                    ColorLegendItem("Gelb", parseColorHex(colorConfig.sameCategoryColor), "Gleiche Kategorie")
                    ColorLegendItem("Blau", parseColorHex(colorConfig.otherTaskColor), "Andere Tasks")
                    ColorLegendItem("Rot", parseColorHex(colorConfig.externalEventColor), "Google Kalender")
                    ColorLegendItem("Schwarz", parseColorHex(colorConfig.overlapColor), "Überlappung")
                }
            }

            HorizontalDivider()

            // Scrollbare Liste
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    // Occupancy Segments (ALLE, inkl. freie Zeiten)
                    Text(
                        text = "Zeitsegmente (${segments.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    segments.forEach { segment ->
                        val startHour = (segment.startHour).toInt()
                        val startMin = ((segment.startHour - startHour) * 60).toInt()
                        val endHour = (segment.endHour).toInt()
                        val endMin = ((segment.endHour - endHour) * 60).toInt()

                        val icon = when {
                            !segment.isOccupied -> "🟢"
                            segment.hasOverlap -> "⚫"
                            segment.isCurrentTask -> "⚪"
                            segment.isSameCategory -> "🟡"
                            segment.isOwnEvent -> "🔵"
                            else -> "🔴"
                        }

                        val label = when {
                            !segment.isOccupied -> "Frei"
                            segment.hasOverlap -> "Überlappung"
                            segment.isCurrentTask -> "Aktueller Task"
                            segment.isSameCategory -> "Gleiche Kategorie"
                            segment.isOwnEvent -> "Eigener Task"
                            else -> "Google Event"
                        }

                        Text(
                            text = "$icon ${String.format("%02d:%02d", startHour, startMin)} - ${String.format("%02d:%02d", endHour, endMin)} ($label)",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    if (segments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Events
                    if (dayEvents.isNotEmpty()) {
                        Text(
                            text = "📅 Google Kalender (${dayEvents.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        dayEvents.forEach { event ->
                            Text(
                                text = "  • ${event.startsAt.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${event.endsAt.format(DateTimeFormatter.ofPattern("HH:mm"))}: ${event.title}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Tasks
                    if (dayTasks.isNotEmpty()) {
                        Text(
                            text = "✓ Eigene Tasks (${dayTasks.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        dayTasks.forEach { task ->
                            val dueTime = task.dueDate?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "??:??"
                            val duration = task.estimatedMinutes ?: 60
                            Text(
                                text = "  • $dueTime (~${duration}min): ${task.title}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                // Empty state
                if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                    Text(
                        text = "Keine Events oder Tasks an diesem Tag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorLegendItem(label: String, color: Color, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
    }
}

/**
 * Compact time input row - ALWAYS shows "S | Ende" with settings icon and duration display
 */
@Composable
private fun CompactTimeInputRow(
    selectedDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onEndTimeChange: (LocalTime) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeAdjustmentPrefs = remember { com.example.questflow.domain.preferences.TimeAdjustmentPreferences(context) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Calculate duration in minutes using LocalDateTime to account for day changes
    val startDateTime = java.time.LocalDateTime.of(selectedDate, startTime)
    val endDateTime = java.time.LocalDateTime.of(selectedDate, endTime)

    // If endTime is earlier than startTime, assume it's the next day
    val adjustedEndDateTime = if (endTime.isBefore(startTime)) {
        endDateTime.plusDays(1)
    } else {
        endDateTime
    }

    val durationMinutes = java.time.Duration.between(startDateTime, adjustedEndDateTime).toMinutes().toInt()

    // Remember initial duration ONCE when component is first created (no keys = never recalculated)
    val initialDuration = remember { durationMinutes }
    android.util.Log.d("CompactTimeInputRow", "Initial duration: $initialDuration min, Current duration: $durationMinutes min (Start: $startDateTime, End: $adjustedEndDateTime)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var showDistanceDialog by remember { mutableStateOf(false) }

        val adjustmentMode = timeAdjustmentPrefs.getAdjustmentMode()
        val isLocked = adjustmentMode != com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start Time (no label)
            TimeInputField(
                time = startTime,
                onClick = onStartTimeClick,
                modifier = Modifier.weight(1f)
            )

            // Lock/Unlock Button with Distance
            Surface(
                modifier = Modifier.clickable { showDistanceDialog = true },
                color = if (isLocked)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (isLocked)
                            androidx.compose.material.icons.Icons.Default.Lock
                        else
                            androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = if (isLocked) "Gesperrt" else "Unabhängig",
                        modifier = Modifier.size(16.dp),
                        tint = if (isLocked)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Text(
                        text = "$durationMinutes Min",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isLocked)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // End Time (no label)
            TimeInputField(
                time = endTime,
                onClick = onEndTimeClick,
                modifier = Modifier.weight(1f)
            )

            // Reset Button
            androidx.compose.material3.IconButton(
                onClick = {
                    // Reset to initial duration
                    android.util.Log.d("CompactTimeInputRow", "RESET clicked: startTime=$startTime, initialDuration=$initialDuration")
                    val newEndTime = startTime.plusMinutes(initialDuration.toLong())
                    android.util.Log.d("CompactTimeInputRow", "RESET: Calculated newEndTime=$newEndTime")
                    onEndTimeChange(newEndTime)
                },
                modifier = Modifier.size(32.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Zurücksetzen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Distance Settings Dialog
        if (showDistanceDialog) {
            QuickDistanceDialog(
                timeAdjustmentPrefs = timeAdjustmentPrefs,
                currentDuration = durationMinutes,
                onDismiss = { showDistanceDialog = false }
            )
        }
    }

    // Time Adjustment Settings Dialog
    if (showSettingsDialog) {
        TimeAdjustmentDialog(
            timeAdjustmentPrefs = timeAdjustmentPrefs,
            currentDuration = durationMinutes,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

/**
 * Single time input field (HH:MM format, clickable)
 */
@Composable
private fun TimeInputField(
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d", time.hour, time.minute),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Quick Distance Dialog - Compact version for fast distance adjustment
 */
@Composable
private fun QuickDistanceDialog(
    timeAdjustmentPrefs: com.example.questflow.domain.preferences.TimeAdjustmentPreferences,
    currentDuration: Int,
    onDismiss: () -> Unit
) {
    var tempDuration by remember { mutableStateOf(currentDuration) }
    val currentMode = timeAdjustmentPrefs.getAdjustmentMode()
    val isLocked = currentMode != com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            androidx.compose.material3.Icon(
                imageVector = if (isLocked) androidx.compose.material.icons.Icons.Default.Lock else androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = null
            )
        },
        title = { androidx.compose.material3.Text("Zeitdistanz") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Toggle Lock/Unlock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isLocked) {
                                timeAdjustmentPrefs.setAdjustmentMode(
                                    com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT
                                )
                            } else {
                                timeAdjustmentPrefs.setAdjustmentMode(
                                    com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE
                                )
                                timeAdjustmentPrefs.setFixedDurationMinutes(tempDuration)
                            }
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.material3.Text(
                        text = if (isLocked) "Distanz gesperrt" else "Unabhängig",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.Switch(
                        checked = isLocked,
                        onCheckedChange = { locked ->
                            if (locked) {
                                timeAdjustmentPrefs.setAdjustmentMode(
                                    com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE
                                )
                                timeAdjustmentPrefs.setFixedDurationMinutes(tempDuration)
                            } else {
                                timeAdjustmentPrefs.setAdjustmentMode(
                                    com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT
                                )
                            }
                        }
                    )
                }

                // Duration Input (shown when locked)
                if (isLocked) {
                    androidx.compose.material3.OutlinedTextField(
                        value = tempDuration.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let { tempDuration = it }
                        },
                        label = { androidx.compose.material3.Text("Distanz (Minuten)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (isLocked) {
                        timeAdjustmentPrefs.setFixedDurationMinutes(tempDuration)
                    }
                    onDismiss()
                }
            ) {
                androidx.compose.material3.Text("OK")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Abbrechen")
            }
        }
    )
}

/**
 * Time Adjustment Settings Dialog - shown when clicking the settings icon in time input row
 */
@Composable
private fun TimeAdjustmentDialog(
    timeAdjustmentPrefs: com.example.questflow.domain.preferences.TimeAdjustmentPreferences,
    currentDuration: Int,
    onDismiss: () -> Unit
) {
    var tempMode by remember { mutableStateOf(timeAdjustmentPrefs.getAdjustmentMode()) }
    var tempDuration by remember { mutableStateOf(timeAdjustmentPrefs.getFixedDurationMinutes()) }

    // Automatically set tempDuration to currentDuration when CURRENT_DISTANCE is selected
    LaunchedEffect(tempMode) {
        if (tempMode == com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE) {
            tempDuration = currentDuration
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                contentDescription = null
            )
        },
        title = { androidx.compose.material3.Text("Zeit-Anpassung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                androidx.compose.material3.Text(
                    text = "Wähle, wie sich Start- und Endzeit verhalten:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Option 1: INDEPENDENT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tempMode = com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = tempMode == com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT,
                        onClick = { tempMode = com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        androidx.compose.material3.Text(
                            text = "Unabhängig",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        androidx.compose.material3.Text(
                            text = "Start und Ende sind vollständig unabhängig",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 2: FIXED_DURATION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tempMode = com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = tempMode == com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION,
                        onClick = { tempMode = com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        androidx.compose.material3.Text(
                            text = "Feste Distanz",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        androidx.compose.material3.Text(
                            text = "Ende = Start + feste Minutenzahl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Option 3: CURRENT_DISTANCE (NEW)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tempMode = com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = tempMode == com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE,
                        onClick = { tempMode = com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        androidx.compose.material3.Text(
                            text = "Aktuelle Distanz",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        androidx.compose.material3.Text(
                            text = "Nutzt die vorhandene Distanz ($currentDuration Min)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Duration TextField (shown for FIXED_DURATION and CURRENT_DISTANCE)
                if (tempMode == com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION ||
                    tempMode == com.example.questflow.domain.preferences.TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE) {

                    androidx.compose.material3.OutlinedTextField(
                        value = tempDuration.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let { tempDuration = it }
                        },
                        label = { androidx.compose.material3.Text("Distanz in Minuten") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    timeAdjustmentPrefs.setAdjustmentMode(tempMode)
                    timeAdjustmentPrefs.setFixedDurationMinutes(tempDuration)
                    onDismiss()
                }
            ) {
                androidx.compose.material3.Text("Speichern")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Abbrechen")
            }
        }
    )
}

/**
 * Time Distance Settings Dialog
 * Shows the CompactTimeInputRow in a dialog for time distance configuration
 */
@Composable
private fun TimeDistanceSettingsDialog(
    selectedDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onEndTimeChange: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zeitdistanz-Einstellungen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hier kannst du die Zeitdistanz zwischen Start- und Endzeit konfigurieren.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Use the existing CompactTimeInputRow
                CompactTimeInputRow(
                    selectedDate = selectedDate,
                    startTime = startTime,
                    endTime = endTime,
                    onStartTimeClick = onStartTimeClick,
                    onEndTimeClick = onEndTimeClick,
                    onEndTimeChange = onEndTimeChange
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fertig")
            }
        },
        dismissButton = {}
    )
}
