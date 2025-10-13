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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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

    // Coroutine scope for programmatic scrolling
    val coroutineScope = rememberCoroutineScope()

    // Header height for coordinate calculations
    val headerHeightDp = 48.dp
    val headerHeightPx = with(density) { headerHeightDp.toPx() }

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

        // Scrollable content with gesture overlay
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                userScrollEnabled = uiState.dragSelectionState == null, // Disable scroll during drag
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

                        // 3 day columns with per-cell gesture detection
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
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Gesture handling is done per-cell in HourSlotWithTasks
        }
    }
}

/**
 * Single hour slot for one day, containing background grid, selection box overlay, and tasks.
 * Includes gesture detection for tap, long-press, and drag-to-select.
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
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    // Convert Y offset to DateTime
    // NOTE: offsetY is relative to THIS HOUR CELL, but during drag it can go NEGATIVE (drag up)
    // or very large (drag down) as the finger leaves the cell boundaries
    fun offsetToDateTime(offsetY: Float): LocalDateTime {
        // This hour's offset in the full timeline (in pixels from 00:00)
        val thisHourOffsetPx = hour * 60 * pixelsPerMinute

        // Absolute position in full timeline = this hour's position + touch offset within this hour
        // This works even for negative offsetY (drag above cell) or large offsetY (drag below cell)
        val absoluteY = thisHourOffsetPx + offsetY

        // Convert to time (coerce to valid range 0-1439 minutes = 00:00-23:59)
        val totalMinutes = (absoluteY / pixelsPerMinute).toInt().coerceIn(0, 1439)
        val h = totalMinutes / 60
        val m = totalMinutes % 60

        android.util.Log.d("TimelineGesture", "Coords: touchY=$offsetY, scrollOffset=0.0, absoluteY=$absoluteY, time=${h}:${String.format("%02d", m)}")

        return LocalDateTime.of(dayTimeline.date, LocalTime.of(h, m))
    }

    // Find task at position
    fun getTaskAt(offsetY: Float): TimelineTask? {
        // Same calculation as offsetToDateTime
        val thisHourOffsetPx = hour * 60 * pixelsPerMinute
        val absoluteY = thisHourOffsetPx + offsetY
        val clickMinutes = (absoluteY / pixelsPerMinute).toInt()

        return dayTimeline.tasks.find { task ->
            val taskStartMinutes = task.startTime.toLocalTime().hour * 60 + task.startTime.toLocalTime().minute
            val taskEndMinutes = task.endTime.toLocalTime().hour * 60 + task.endTime.toLocalTime().minute
            clickMinutes in taskStartMinutes until taskEndMinutes
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(hourHeightDp)
            .pointerInput(dayTimeline.date, hour) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downPos = down.position

                    android.util.Log.d("TimelineGesture", "=== GESTURE START === Touch Down at ${downPos.x}, ${downPos.y}")
                    viewModel.updateGestureDebug("DOWN", 0, downPos.x, downPos.y, "Touch Down")

                    // Check if movement happens within 150ms (early movement detection for swipe)
                    android.util.Log.d("TimelineGesture", "Checking for early movement (150ms window)...")

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
                                android.util.Log.d("TimelineGesture", "Early movement detected! X=$totalX Y=$totalY → SWIPE")
                                return@withTimeoutOrNull EarlyResult(Pair(totalX, totalY), false)
                            }

                            if (!change.pressed) {
                                android.util.Log.d("TimelineGesture", "Released during early detection → QUICK TAP!")
                                return@withTimeoutOrNull EarlyResult(null, true)
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        EarlyResult(null, false)
                    } ?: EarlyResult(null, false)

                    android.util.Log.d("TimelineGesture", "Early check result: movement=${earlyCheck.movement}, released=${earlyCheck.released}")

                    when {
                        // Quick tap during early detection (released < 150ms)
                        earlyCheck.released -> {
                            val task = getTaskAt(downPos.y)
                            val releaseTime = System.currentTimeMillis() - downTime

                            android.util.Log.d("TimelineGesture", "QUICK TAP! Released after ${releaseTime}ms")

                            if (task != null) {
                                viewModel.updateGestureDebug("TAP_TASK", releaseTime, 0f, 0f,
                                    "Quick-Tap nach ${releaseTime}ms: ${task.title}")
                                onTaskClick(task)
                            } else {
                                val dateTime = offsetToDateTime(downPos.y)
                                viewModel.updateGestureDebug("TAP_EMPTY", releaseTime, 0f, 0f,
                                    "Quick-Tap nach ${releaseTime}ms @ ${dateTime.toLocalTime()} → TODO: Create Task")
                            }
                        }

                        // CASE 1 & 2: Early movement detected = SWIPE
                        earlyCheck.movement != null -> {
                            val (initX, initY) = earlyCheck.movement
                            var totalX = initX
                            var totalY = initY
                            var lastMoveTime = System.currentTimeMillis()

                            viewModel.updateGestureDebug("SWIPING",
                                System.currentTimeMillis() - downTime,
                                totalX, totalY,
                                "Swipe erkannt")

                            // Track swipe
                            val swipeStartTime = System.currentTimeMillis()
                            var lastX = totalX
                            var lastY = totalY

                            drag(down.id) { change ->
                                totalX += change.positionChange().x
                                totalY += change.positionChange().y

                                // Check if still moving (ignore micro-movements < 5px for hold detection)
                                val movementX = abs(change.positionChange().x)
                                val movementY = abs(change.positionChange().y)

                                if (movementX > 5f || movementY > 5f) {
                                    // Significant movement - update last move time
                                    lastMoveTime = System.currentTimeMillis()
                                    lastX = totalX
                                    lastY = totalY
                                }
                                // Micro-movements (< 5px) don't reset lastMoveTime - allows thumb tremor

                                viewModel.updateGestureDebug("SWIPING",
                                    System.currentTimeMillis() - downTime,
                                    totalX, totalY,
                                    "Swipe: X=${totalX.toInt()} Y=${totalY.toInt()}")

                                // Don't consume - let LazyColumn handle scroll
                            }

                            // After drag ends, check if held still
                            val timeSinceLastMove = System.currentTimeMillis() - lastMoveTime

                            android.util.Log.d("TimelineGesture", "Swipe ended. Time since last significant move: ${timeSinceLastMove}ms")

                            if (timeSinceLastMove > 150L) {
                                // CASE 2: Swipe + Hold = Continuous scroll
                                viewModel.updateGestureDebug("SWIPE_HOLD", 0, lastX, lastY,
                                    "Swipe+Hold (${timeSinceLastMove}ms still) → TODO: Continuous Scroll")
                                // No auto-clear - stays visible until next gesture
                            } else {
                                // CASE 1: Direct swipe = Impulse scroll
                                viewModel.updateGestureDebug("SWIPE_IMPULSE", 0, totalX, totalY,
                                    "Swipe-Impuls: X=${totalX.toInt()} Y=${totalY.toInt()}")
                                // No auto-clear - stays visible until next gesture
                            }
                        }

                        // No early movement - wait for timeout or release
                        else -> {
                            android.util.Log.d("TimelineGesture", "No early movement → WAITING for 850ms (or release)")
                            viewModel.updateGestureDebug("WAITING", 150, 0f, 0f, "Warte auf 1000ms (1 Sek)...")

                            val longPressResult = withTimeoutOrNull(850L) { // 1000ms total - 150ms already waited
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()

                                    if (!change.pressed) {
                                        android.util.Log.d("TimelineGesture", "RELEASED before 1 second! → TAP")
                                        return@withTimeoutOrNull "released"
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE")
                                "timeout"
                            }

                            android.util.Log.d("TimelineGesture", "longPressResult = $longPressResult")

                            when (longPressResult) {
                                // CASE 7 & 8: Quick tap (released before 1000ms)
                                "released" -> {
                                    val task = getTaskAt(downPos.y)
                                    val releaseTime = System.currentTimeMillis() - downTime

                                    android.util.Log.d("TimelineGesture", "TAP confirmed! Released after ${releaseTime}ms, task=$task")

                                    if (task != null) {
                                        // CASE 7: Tap on task
                                        viewModel.updateGestureDebug("TAP_TASK", releaseTime, 0f, 0f,
                                            "Task-Tap nach ${releaseTime}ms: ${task.title}")
                                        onTaskClick(task)
                                        // No auto-clear - stays visible until next gesture
                                    } else {
                                        // CASE 8: Tap on empty space
                                        val dateTime = offsetToDateTime(downPos.y)
                                        viewModel.updateGestureDebug("TAP_EMPTY", releaseTime, 0f, 0f,
                                            "Leer-Tap nach ${releaseTime}ms @ ${dateTime.toLocalTime()} → TODO: Create Task")
                                        // No auto-clear - stays visible until next gesture
                                    }
                                }

                                // CASE 3, 4, 5, 6: Long-press (1000ms timeout)
                                else -> {
                                    android.util.Log.d("TimelineGesture", "LONG_PRESS! Timeout reached (1000ms)")
                                    val task = getTaskAt(downPos.y)
                                    val startDateTime = offsetToDateTime(downPos.y)

                                    viewModel.updateGestureDebug("LONG_PRESS", 1000, 0f, 0f,
                                        "Long-Press erkannt (1 Sekunde)")

                                    // Check if movement after long-press
                                    val postLongPressMovement = withTimeoutOrNull(100L) {
                                        var moved = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.first()

                                            val dx = abs(change.positionChange().x)
                                            val dy = abs(change.positionChange().y)

                                            if (dx > 5f || dy > 5f) {
                                                moved = true
                                                return@withTimeoutOrNull true
                                            }

                                            if (!change.pressed) {
                                                return@withTimeoutOrNull false
                                            }
                                        }
                                        @Suppress("UNREACHABLE_CODE")
                                        false
                                    }

                                    when {
                                        // CASE 3: Long-press + release without movement
                                        postLongPressMovement == false -> {
                                            if (task != null) {
                                                // Mark entire task
                                                viewModel.updateGestureDebug("MARK_TASK", 0, 0f, 0f,
                                                    "Task markieren: ${task.title}")
                                                onTaskLongPress(task)
                                            } else {
                                                // Mark 15min spot
                                                val endDateTime = startDateTime.plusMinutes(15)
                                                viewModel.updateGestureDebug("MARK_15MIN", 0, 0f, 0f,
                                                    "15min markieren")
                                                viewModel.onDragSelectionStart(startDateTime)
                                                viewModel.onDragSelectionUpdate(endDateTime)
                                                viewModel.onDragSelectionEnd()
                                            }
                                            // No auto-clear - stays visible until next gesture
                                        }

                                        // CASE 4, 5, 6: Long-press + drag
                                        postLongPressMovement == true -> {
                                            viewModel.updateGestureDebug("DRAG_START", 0, 0f, 0f,
                                                "Drag Markierung starten")

                                            viewModel.onDragSelectionStart(startDateTime)

                                            // Track drag with edge detection
                                            drag(down.id) { change ->
                                                val currentDateTime = offsetToDateTime(change.position.y)
                                                viewModel.onDragSelectionUpdate(currentDateTime)

                                                viewModel.updateGestureDebug("DRAGGING",
                                                    System.currentTimeMillis() - downTime,
                                                    change.position.x,
                                                    change.position.y,
                                                    "Ziehen: ${currentDateTime.toLocalTime()}")

                                                // CASE 5: Edge detection for auto-scroll
                                                // TODO: Implement edge scroll

                                                change.consume()
                                            }

                                            // CASE 6: Release after drag
                                            viewModel.onDragSelectionEnd()
                                            viewModel.updateGestureDebug("DRAG_END", 0, 0f, 0f,
                                                "Markierung gesetzt")
                                            // No auto-clear - stays visible until next gesture
                                        }

                                        // Timeout - treated as release without movement
                                        else -> {
                                            if (task != null) {
                                                onTaskLongPress(task)
                                            } else {
                                                val endDateTime = startDateTime.plusMinutes(15)
                                                viewModel.onDragSelectionStart(startDateTime)
                                                viewModel.onDragSelectionUpdate(endDateTime)
                                                viewModel.onDragSelectionEnd()
                                            }
                                            // No auto-clear - stays visible until next gesture
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
