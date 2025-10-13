package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.TimelineViewModel
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.domain.model.TimelineTask
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

/**
 * UNIFIED Timeline Grid - TRUE TABLE where EVERYTHING scrolls together.
 *
 * KEY ARCHITECTURE:
 * - Single LazyColumn with rows containing [Time | Day1 | Day2 | Day3]
 * - Time scrolls WITH the days - unified table
 * - Sticky header (stays visible during scroll)
 * - Drag-to-select gesture overlay (spans entire day, not just per-hour)
 * - Transparent overlay for gesture capture (doesn't block task taps)
 * - Smart scroll: only when near edges during drag selection
 * - Only visible rows rendered (~5-10 out of 24)
 */
@Composable
fun TimelineGrid(
    uiState: TimelineUiState,
    onTaskClick: (TimelineTask) -> Unit,
    onTaskLongPress: (TimelineTask) -> Unit,
    onLoadMore: (direction: TimelineViewModel.LoadDirection) -> Unit,
    onDayWindowShift: (direction: Int) -> Unit,
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val pixelsPerMinute = uiState.pixelsPerMinute
    val hourHeightDp = (60 * pixelsPerMinute).dp
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Get visible days using the offset from state
    val visibleDays = uiState.getVisibleDays()

    // Single scroll state for the entire table
    val scrollState = rememberLazyListState()

    // Header height for coordinate calculations
    val headerHeightDp = 48.dp
    val headerHeightPx = with(density) { headerHeightDp.toPx() }

    // Helper for gesture detection coordinate conversion
    val timeColumnWidthPx = with(density) { 60.dp.toPx() }

    Column(modifier = modifier.fillMaxSize()) {
        // STICKY Header (fixed at top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeightDp)
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

        // Scrollable content with gesture detection BEHIND tasks (no overlay layer!)
        LazyColumn(
            state = scrollState,
            userScrollEnabled = uiState.dragSelectionState == null, // Disable scroll during drag
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(visibleDays, scrollState, pixelsPerMinute, timeColumnWidthPx, headerHeightPx) {
                    // Gesture detection directly on LazyColumn modifier
                    // This runs BEHIND the tasks - tasks are clickable!
                    val hourHeightPx = 60 * pixelsPerMinute

                    fun screenPosToDateTime(x: Float, y: Float): LocalDateTime? {
                        val xRelativeToContent = x - timeColumnWidthPx
                        if (xRelativeToContent < 0) return null

                        val dayColumnWidth = xRelativeToContent / visibleDays.size
                        val dayIndex = (xRelativeToContent / dayColumnWidth).toInt()
                        if (dayIndex !in visibleDays.indices) return null

                        val targetDay = visibleDays[dayIndex]

                        val yRelativeToContent = y - headerHeightPx
                        if (yRelativeToContent < 0) return null

                        val scrollOffsetPx = scrollState.firstVisibleItemIndex * hourHeightPx +
                                scrollState.firstVisibleItemScrollOffset.toFloat()
                        val absoluteY = yRelativeToContent + scrollOffsetPx

                        val totalMinutes = (absoluteY / pixelsPerMinute).toInt().coerceIn(0, 1439)
                        val hour = totalMinutes / 60
                        val minute = totalMinutes % 60

                        return LocalDateTime.of(targetDay.date, LocalTime.of(hour, minute))
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val downPos = down.position

                        android.util.Log.d("TimelineGesture", "GESTURE START at ${downPos.x}, ${downPos.y}")

                        data class EarlyResult(val movement: Pair<Float, Float>?, val released: Boolean)

                        val earlyCheck = withTimeoutOrNull(150L) {
                            var totalX = 0f
                            var totalY = 0f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()

                                totalX += change.positionChange().x
                                totalY += change.positionChange().y

                                if (abs(totalX) > 10f || abs(totalY) > 10f) {
                                    android.util.Log.d("TimelineGesture", "SWIPE detected → pass-through")
                                    return@withTimeoutOrNull EarlyResult(Pair(totalX, totalY), false)
                                }

                                if (!change.pressed) {
                                    android.util.Log.d("TimelineGesture", "QUICK TAP → pass-through")
                                    return@withTimeoutOrNull EarlyResult(null, true)
                                }
                            }
                            @Suppress("UNREACHABLE_CODE")
                            EarlyResult(null, false)
                        } ?: EarlyResult(null, false)

                        when {
                            earlyCheck.released || earlyCheck.movement != null -> {
                                // Quick tap or swipe → pass through to tasks/scrolling
                                viewModel.updateGestureDebug("PASS_THROUGH", 0, 0f, 0f, "→ Tasks/Scroll")
                                return@awaitEachGesture
                            }

                            else -> {
                                val longPressResult = withTimeoutOrNull(850L) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()
                                        if (!change.pressed) return@withTimeoutOrNull "released"
                                    }
                                    @Suppress("UNREACHABLE_CODE")
                                    "timeout"
                                }

                                if (longPressResult == "released") {
                                    viewModel.updateGestureDebug("TAP", 0, 0f, 0f, "→ Tasks")
                                    return@awaitEachGesture
                                }

                                // LONG PRESS - only here we handle it!
                                val startDateTime = screenPosToDateTime(downPos.x, downPos.y) ?: return@awaitEachGesture

                                viewModel.updateGestureDebug("LONG_PRESS", 1000, 0f, 0f, "Detected!")

                                val postLongPressMovement = withTimeoutOrNull(100L) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()

                                        if (abs(change.positionChange().x) > 5f || abs(change.positionChange().y) > 5f) {
                                            return@withTimeoutOrNull true
                                        }
                                        if (!change.pressed) return@withTimeoutOrNull false
                                    }
                                    @Suppress("UNREACHABLE_CODE")
                                    false
                                }

                                when {
                                    postLongPressMovement == false -> {
                                        // Long-press without drag → 15min mark
                                        val endDateTime = startDateTime.plusMinutes(15)
                                        viewModel.onDragSelectionStart(startDateTime)
                                        viewModel.onDragSelectionUpdate(endDateTime)
                                        viewModel.onDragSelectionEnd()
                                        viewModel.updateGestureDebug("15MIN_MARK", 0, 0f, 0f, "Set")
                                    }

                                    postLongPressMovement == true -> {
                                        // Long-press + drag → MULTI-DAY SELECTION!
                                        viewModel.onDragSelectionStart(startDateTime)
                                        down.consume()

                                        drag(down.id) { change ->
                                            val currentDateTime = screenPosToDateTime(change.position.x, change.position.y)
                                            if (currentDateTime != null) {
                                                viewModel.onDragSelectionUpdate(currentDateTime)
                                                viewModel.updateGestureDebug("DRAGGING", 0,
                                                    change.position.x, change.position.y,
                                                    "${currentDateTime.toLocalDate()} ${currentDateTime.toLocalTime()}")
                                            }
                                            change.consume()
                                        }

                                        viewModel.onDragSelectionEnd()
                                        viewModel.updateGestureDebug("DRAG_END", 0, 0f, 0f, "Multi-Day set")
                                    }

                                    else -> {
                                        val endDateTime = startDateTime.plusMinutes(15)
                                        viewModel.onDragSelectionStart(startDateTime)
                                        viewModel.onDragSelectionUpdate(endDateTime)
                                        viewModel.onDragSelectionEnd()
                                    }
                                }
                            }
                        }
                    }
                }
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

                    // 3 day columns with tasks
                    visibleDays.forEach { day ->
                        HourSlotWithTasks(
                            hour = hour,
                            hourHeightDp = hourHeightDp,
                            dayTimeline = day,
                            pixelsPerMinute = pixelsPerMinute,
                            selectedTaskIds = uiState.selectedTaskIds,
                            selectionBox = uiState.selectionBox,
                            dragSelectionState = uiState.dragSelectionState,
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

/**
 * Single hour slot for one day, containing background grid, selection box overlay, and tasks.
 * NO gesture detection - all gestures handled by global overlay!
 */
@Composable
private fun HourSlotWithTasks(
    hour: Int,
    hourHeightDp: androidx.compose.ui.unit.Dp,
    dayTimeline: DayTimeline,
    pixelsPerMinute: Float,
    selectedTaskIds: Set<Long> = emptySet(),
    selectionBox: com.example.questflow.presentation.screens.timeline.model.SelectionBox? = null,
    dragSelectionState: com.example.questflow.presentation.screens.timeline.model.DragSelectionState? = null,
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

        // Drag preview (live during drag) - rendered first with more transparency
        if (dragSelectionState != null) {
            val previewBox = dragSelectionState.toSelectionBox()
            RenderSelectionBoxInHourSlot(
                selectionBox = previewBox,
                dayDate = dayTimeline.date,
                hour = hour,
                pixelsPerMinute = pixelsPerMinute,
                isPreview = true
            )
        }

        // Final selection box overlay (only in this hour if it overlaps)
        if (selectionBox != null) {
            RenderSelectionBoxInHourSlot(
                selectionBox = selectionBox,
                dayDate = dayTimeline.date,
                hour = hour,
                pixelsPerMinute = pixelsPerMinute,
                isPreview = false
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

/**
 * Task bar wrapper component - delegates to TaskBalken
 */
@Composable
private fun TaskBar(
    task: TimelineTask,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    TaskBalken(
        task = task,
        pixelsPerMinute = 2f,
        onLongPress = onLongPress,
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    )
}

/**
 * Single hour row for background grid (used in LazyColumn).
 */
@Composable
private fun HourBackgroundGrid(
    hour: Int,
    pixelsPerMinute: Float,
    hourHeightDp: androidx.compose.ui.unit.Dp,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primaryContainer
    val hourHeightPx = 60 * pixelsPerMinute

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(hourHeightDp)
            .background(
                if (isToday) {
                    primaryColor.copy(alpha = 0.05f)
                } else {
                    Color.Transparent
                }
            )
    ) {
        // Top hour line
        val strokeWidth = if (hour % 6 == 0) 2f else 1f
        val alpha = if (hour % 6 == 0) 0.2f else 0.1f

        drawLine(
            color = outlineColor.copy(alpha = alpha),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth
        )

        // 15-minute markers
        if (hourHeightPx > 40f) {
            val quarterHeight = hourHeightPx / 4f
            for (quarter in 1..3) {
                val quarterY = quarterHeight * quarter
                drawLine(
                    color = outlineColor.copy(alpha = 0.05f),
                    start = Offset(0f, quarterY),
                    end = Offset(size.width, quarterY),
                    strokeWidth = 1f
                )
            }
        }
    }
}

/**
 * Day header showing day name and date.
 */
@Composable
private fun DayHeader(
    date: LocalDate,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
    val dayOfMonth = date.dayOfMonth
    val month = date.monthValue

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isToday) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Day of week
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            ),
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )

        // Day and month
        Text(
            text = "$dayOfMonth.$month",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }
}
