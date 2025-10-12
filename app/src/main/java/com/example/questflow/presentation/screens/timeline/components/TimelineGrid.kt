package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.questflow.presentation.screens.timeline.TimelineViewModel
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.domain.model.TimelineTask
import kotlin.math.abs

/**
 * UNIFIED Timeline Grid - TRUE TABLE where EVERYTHING scrolls together.
 *
 * KEY ARCHITECTURE:
 * - Single LazyColumn with rows containing [Time | Day1 | Day2 | Day3]
 * - Time scrolls WITH the days - unified table
 * - Sticky header (stays visible during scroll)
 * - Simple swipe gesture for day shifting
 * - Only visible rows rendered (~5-10 out of 24)
 */
@Composable
fun TimelineGrid(
    uiState: TimelineUiState,
    onTaskClick: (TimelineTask) -> Unit,
    onTaskLongPress: (TimelineTask) -> Unit,
    onLoadMore: (direction: TimelineViewModel.LoadDirection) -> Unit,
    onDayWindowShift: (direction: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pixelsPerMinute = uiState.pixelsPerMinute
    val hourHeightDp = (60 * pixelsPerMinute).dp

    // Get visible days using the offset from state
    val visibleDays = uiState.getVisibleDays()

    // Single scroll state for the entire table
    val scrollState = rememberLazyListState()

    // Swipe detection state
    var accumulatedDrag by remember { mutableStateOf(0f) }
    val swipeThreshold = 100f

    Column(modifier = modifier.fillMaxSize()) {
        // STICKY Header (fixed at top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 8.dp)
        ) {
            // Time column header
            Box(modifier = Modifier.width(60.dp))

            // Day headers
            visibleDays.forEach { day ->
                DayHeader(
                    date = day.date,
                    isToday = day.isToday,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Scrollable content with swipe detection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(accumulatedDrag) > swipeThreshold) {
                                val direction = if (accumulatedDrag > 0) -1 else 1
                                onDayWindowShift(direction)
                            }
                            accumulatedDrag = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Only accumulate if not scrolling vertically
                            if (abs(dragAmount) > 2f) {
                                accumulatedDrag += dragAmount
                            }
                        }
                    )
                }
        ) {
            // Single LazyColumn for entire table (24 hour rows)
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize()
            ) {
                // 24 hour rows
                items(24) { hour ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Time label (scrolls with row)
                        TimeAxisHourRow(
                            hour = hour,
                            hourHeightDp = hourHeightDp,
                            modifier = Modifier.width(60.dp)
                        )

                        // 3 day columns
                        visibleDays.forEach { day ->
                            HourSlotWithTasks(
                                hour = hour,
                                hourHeightDp = hourHeightDp,
                                dayTimeline = day,
                                pixelsPerMinute = pixelsPerMinute,
                                selectedTaskIds = uiState.selectedTaskIds,
                                selectionBox = uiState.selectionBox,
                                onTaskClick = onTaskClick,
                                onTaskLongPress = onTaskLongPress,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single hour slot for one day, containing background grid, selection box overlay, and tasks.
 */
@Composable
private fun HourSlotWithTasks(
    hour: Int,
    hourHeightDp: androidx.compose.ui.unit.Dp,
    dayTimeline: DayTimeline,
    pixelsPerMinute: Float,
    selectedTaskIds: Set<Long> = emptySet(),
    selectionBox: com.example.questflow.presentation.screens.timeline.model.SelectionBox? = null,
    onTaskClick: (TimelineTask) -> Unit,
    onTaskLongPress: (TimelineTask) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(hourHeightDp)
    ) {
        // Background grid for this hour slot
        HourBackgroundGrid(
            hour = hour,
            pixelsPerMinute = pixelsPerMinute,
            hourHeightDp = hourHeightDp,
            isToday = dayTimeline.isToday
        )

        // Selection box overlay (only in this hour if it overlaps)
        if (selectionBox != null) {
            RenderSelectionBoxInHourSlot(
                selectionBox = selectionBox,
                dayDate = dayTimeline.date,
                hour = hour,
                pixelsPerMinute = pixelsPerMinute,
                hourHeightDp = hourHeightDp
            )
        }

        // Tasks that overlap with this hour
        val hourStart = hour * 60 // Minutes since midnight
        val hourEnd = (hour + 1) * 60

        dayTimeline.tasks.forEach { task ->
            val taskStartMinutes = task.startTime.toLocalTime().hour * 60 + task.startTime.toLocalTime().minute
            val taskEndMinutes = task.endTime.toLocalTime().hour * 60 + task.endTime.toLocalTime().minute

            // Check if task overlaps with this hour
            if (taskEndMinutes > hourStart && taskStartMinutes < hourEnd) {
                val yOffsetInHour = if (taskStartMinutes >= hourStart) {
                    (taskStartMinutes - hourStart) * pixelsPerMinute
                } else {
                    0f // Task started before this hour
                }

                val visibleHeight = when {
                    taskStartMinutes < hourStart && taskEndMinutes > hourEnd -> {
                        // Task spans entire hour
                        60 * pixelsPerMinute
                    }
                    taskStartMinutes < hourStart -> {
                        // Task started before, ends during hour
                        (taskEndMinutes - hourStart) * pixelsPerMinute
                    }
                    taskEndMinutes > hourEnd -> {
                        // Task starts during hour, ends after
                        (hourEnd - taskStartMinutes) * pixelsPerMinute
                    }
                    else -> {
                        // Task fully within hour
                        (taskEndMinutes - taskStartMinutes) * pixelsPerMinute
                    }
                }

                TaskBar(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onLongPress = { onTaskLongPress(task) },
                    isSelected = task.id in selectedTaskIds,
                    modifier = Modifier
                        .offset(y = yOffsetInHour.dp)
                        .fillMaxWidth(0.95f)
                        .height(visibleHeight.dp.coerceAtLeast(20.dp))
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}
