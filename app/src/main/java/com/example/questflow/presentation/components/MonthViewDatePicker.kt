package com.example.questflow.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.domain.usecase.DayOccupancyCalculator
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * Google Calendar-style month view with visual occupancy indicators
 *
 * Features:
 * - 5-6 week rows showing entire month
 * - Month navigation with arrow buttons
 * - Visual occupancy bars: Green (free) / Red (occupied)
 * - Direct visual overview of schedule
 */
@Composable
fun MonthViewDatePicker(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    modifier: Modifier = Modifier,
    tasks: List<TaskEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
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

        // Calendar - FIXED size, always fully visible at bottom
        Column(modifier = Modifier.fillMaxWidth()) {
            // Month navigation header
            MonthNavigationHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Table structure (header + grid as one unit)
            Column(modifier = Modifier.fillMaxWidth()) {
                // Weekday headers
                WeekdayHeaders()

                // Calendar grid (connected to header)
                MonthCalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    events = events,
                    occupancyCalculator = occupancyCalculator,
                    tasks = tasks,
                    currentTaskId = currentTaskId,
                    currentCategoryId = currentCategoryId
                )
            }
        }
    }
}

@Composable
private fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Vorheriger Monat")
        }

        Text(
            text = currentMonth.format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nächster Monat")
        }
    }
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
    onDateSelected: (LocalDate) -> Unit,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    tasks: List<TaskEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null
) {
    // Build list of dates for the month view (includes leading/trailing days)
    val calendarDates = buildCalendarDates(currentMonth)
    val rows = (calendarDates.size / 7) // Number of rows

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp) // ~6 weeks * 40dp per row (kompakter)
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
                    onDateSelected = onDateSelected,
                    events = events,
                    occupancyCalculator = occupancyCalculator,
                    tasks = tasks,
                    currentTaskId = currentTaskId,
                    currentCategoryId = currentCategoryId
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
    onDateSelected: (LocalDate) -> Unit,
    events: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator,
    tasks: List<TaskEntity> = emptyList(),
    currentTaskId: Long? = null,
    currentCategoryId: Long? = null
) {
    val isToday = date == LocalDate.now()

    // Calculate occupancy segments for this day (includes events AND tasks)
    val segments = remember(date, events, tasks, currentTaskId, currentCategoryId) {
        occupancyCalculator.calculateDayOccupancy(events, date, tasks, currentTaskId, currentCategoryId)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .clickable { onDateSelected(date) }
            .padding(2.dp)
    ) {
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

@Composable
private fun OccupancyBar(
    segments: List<DayOccupancyCalculator.TimeSegment>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        segments.forEach { segment ->
            // Clamp weight to positive value (min 0.01f to ensure visibility)
            val weight = segment.weightInDay.coerceAtLeast(0.01f)

            val color = when {
                !segment.isOccupied -> Color(0xFF66BB6A) // Grün: Frei
                segment.hasOverlap -> Color(0xFF000000) // Schwarz: Überlappung
                segment.isCurrentTask -> Color(0xFFFFFFFF) // Weiß: Aktueller Task
                segment.isSameCategory -> Color(0xFFFFEB3B) // Gelb: Gleiche Kategorie
                segment.isOwnEvent -> Color(0xFF42A5F5) // Blau: Anderer eigener Task
                else -> Color(0xFFEF5350) // Rot: Externes Google Calendar Event
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

    // Calculate occupancy for debug info
    val segments = occupancyCalculator.calculateDayOccupancy(events, date, tasks, currentTaskId, currentCategoryId)
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
                    ColorLegendItem("Blau", Color(0xFF42A5F5), "Eigene Tasks")
                    ColorLegendItem("Rot", Color(0xFFEF5350), "Google Kalender")
                    ColorLegendItem("Lila", Color(0xFF9C27B0), "Überlappung")
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
