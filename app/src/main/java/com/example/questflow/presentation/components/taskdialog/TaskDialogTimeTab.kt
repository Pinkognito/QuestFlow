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
    isRecurring: Boolean,
    onIsRecurringChange: (Boolean) -> Unit,
    recurringConfig: RecurringConfig,
    onRecurringConfigClick: () -> Unit,
    monthViewEvents: List<CalendarEventLinkEntity>,
    occupancyCalculator: DayOccupancyCalculator?,
    categoryColor: androidx.compose.ui.graphics.Color?,
    monthViewTasks: List<TaskEntity>,
    currentTaskId: Long?,
    currentCategoryId: Long?,
    calendarExpanded: Boolean,
    onCalendarExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // MonthViewDatePicker - Collapsible Calendar (Default: Expanded)
        item {
            // Collapsible header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCalendarExpandedChange(!calendarExpanded)
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
                com.example.questflow.presentation.components.MonthViewDatePicker(
                    selectedDate = startDateTime.toLocalDate(),
                    onDateSelected = { selectedDate ->
                        onStartDateTimeChange(LocalDateTime.of(selectedDate, startDateTime.toLocalTime()))
                    },
                    events = monthViewEvents,
                    occupancyCalculator = occupancyCalculator,
                    tasks = monthViewTasks,
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Date and Time Selection - START with CompactDateTimeSection
        item {
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
                currentTaskId = currentTaskId,
                currentCategoryId = currentCategoryId,
                alternativeTime = endDateTime,
                onAlternativeTimeChange = onEndDateTimeChange
            )
        }

        // Duration Row - between start and end
        item {
            com.example.questflow.presentation.components.DurationRow(
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                onEndDateTimeChange = onEndDateTimeChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Date and Time Selection - END with CompactDateTimeSection
        item {
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
                currentTaskId = currentTaskId,
                currentCategoryId = currentCategoryId,
                alternativeTime = startDateTime,
                onAlternativeTimeChange = onStartDateTimeChange
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
                            onClick = onFindFreeTimesClick,
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

        // Recurring task option
        item {
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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
        }

        // Recurring configuration button
        if (isRecurring) {
            item {
                OutlinedButton(
                    onClick = onRecurringConfigClick,
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
}
