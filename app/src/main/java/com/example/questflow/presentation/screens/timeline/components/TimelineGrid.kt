package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import kotlin.math.abs
import kotlin.math.sqrt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.TimelineViewModel
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.domain.model.TimelineTask
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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
    // CRITICAL: uiState.pixelsPerMinute is actually DP per minute, not pixels!
    val dpPerMinute = uiState.pixelsPerMinute  // DP per minute for rendering
    val hourHeightDp = (60 * dpPerMinute).dp
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Calculate actual pixels per minute for coordinate calculations
    val pixelsPerMinute = with(density) { dpPerMinute * density.density }

    android.util.Log.d("TimelineGrid", "🔍 DP/Pixel conversion: dpPerMinute=$dpPerMinute, density=${density.density}, pixelsPerMinute=$pixelsPerMinute, hourHeightDp=$hourHeightDp")

    // Get visible days using the offset from state
    val visibleDays = uiState.getVisibleDays()

    // Single scroll state for the entire table
    val scrollState = rememberLazyListState()

    // Header height for coordinate calculations
    val headerHeightDp = 48.dp
    val headerHeightPx = with(density) { headerHeightDp.toPx() }

    // Auto-scroll effect: Continuously scroll when at edge during drag selection
    val atEdge = uiState.gestureDebugInfo?.atEdge
    val isDragging = uiState.dragSelectionState != null

    LaunchedEffect(atEdge, isDragging) {
        if (isDragging && atEdge != null) {
            while (true) {
                when (atEdge) {
                    com.example.questflow.presentation.screens.timeline.model.EdgePosition.LEFT -> {
                        android.util.Log.d("TimelineGrid", "🔄 AUTO-SCROLL: Shifting day window LEFT")
                        onDayWindowShift(-1) // Shift to past days
                        kotlinx.coroutines.delay(300) // Delay between shifts
                    }
                    com.example.questflow.presentation.screens.timeline.model.EdgePosition.RIGHT -> {
                        android.util.Log.d("TimelineGrid", "🔄 AUTO-SCROLL: Shifting day window RIGHT")
                        onDayWindowShift(1) // Shift to future days
                        kotlinx.coroutines.delay(300)
                    }
                    com.example.questflow.presentation.screens.timeline.model.EdgePosition.TOP -> {
                        android.util.Log.d("TimelineGrid", "🔄 AUTO-SCROLL: Scrolling UP")
                        scrollState.animateScrollBy(-200f) // Scroll up
                        kotlinx.coroutines.delay(50)
                    }
                    com.example.questflow.presentation.screens.timeline.model.EdgePosition.BOTTOM -> {
                        android.util.Log.d("TimelineGrid", "🔄 AUTO-SCROLL: Scrolling DOWN")
                        scrollState.animateScrollBy(200f) // Scroll down
                        kotlinx.coroutines.delay(50)
                    }
                    com.example.questflow.presentation.screens.timeline.model.EdgePosition.NONE -> {
                        // No edge, stop scrolling
                        break
                    }
                }
            }
        }
    }

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

        // Scrollable content with CONDITIONAL overlay
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = scrollState,
                userScrollEnabled = uiState.dragSelectionState == null, // Disable scroll during drag
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Comprehensive gesture tracking with live position updates
                        awaitEachGesture {
                            // === DOWN Phase ===
                            val down = awaitFirstDown()
                            val startPos = down.position
                            val startTime = System.currentTimeMillis()

                            android.util.Log.d("TimelineGrid", "🔽 TOUCH_DOWN at (${startPos.x.toInt()}, ${startPos.y.toInt()})")
                            viewModel.updateGestureDebug("TOUCH_DOWN", 0, startPos.x, startPos.y,
                                "Down at (${startPos.x.toInt()}, ${startPos.y.toInt()})")

                            var totalDistance = 0f
                            var totalDeltaX = 0f
                            var totalDeltaY = 0f
                            var lastPos = startPos
                            var lastMoveTime = startTime
                            var isSwipe = false
                            var swipeDirection = ""
                            var holdStartTime = 0L
                            var lastLoggedGesture = "TOUCH_DOWN"

                            // === MOVE Phase Loop ===
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                if (!change.pressed) {
                                    // === UP Phase ===
                                    val duration = System.currentTimeMillis() - startTime
                                    val finalPos = change.position

                                    android.util.Log.d("TimelineGrid", "🔼 TOUCH_UP at (${finalPos.x.toInt()}, ${finalPos.y.toInt()}) after ${duration}ms, distance=${totalDistance.toInt()}px")
                                    viewModel.updateGestureDebug("TOUCH_UP", duration,
                                        finalPos.x, finalPos.y,
                                        "Up after ${duration}ms, total=${totalDistance.toInt()}px")
                                    break
                                }

                                val currentPos = change.position
                                val delta = currentPos - lastPos
                                val distance = sqrt(delta.x * delta.x + delta.y * delta.y)
                                totalDistance += distance
                                totalDeltaX += delta.x
                                totalDeltaY += delta.y

                                val now = System.currentTimeMillis()
                                val timeDelta = now - lastMoveTime
                                val velocity = if (timeDelta > 0) distance / timeDelta * 1000f else 0f

                                // Holding Detection (stationary for > 500ms)
                                if (totalDistance < 10f && (now - startTime) > 500 && lastLoggedGesture != "HOLDING") {
                                    android.util.Log.d("TimelineGrid", "✋ HOLDING at (${currentPos.x.toInt()}, ${currentPos.y.toInt()}) for ${now - startTime}ms")
                                    viewModel.updateGestureDebug("HOLDING",
                                        now - startTime, currentPos.x, currentPos.y,
                                        "Holding for ${now - startTime}ms")
                                    lastLoggedGesture = "HOLDING"
                                }

                                // Edge Detection (convert DP to pixels for comparison)
                                val edgeBorderPx = with(density) { uiState.edgeBorderWidthDp.dp.toPx() }
                                val screenWidthPx = size.width
                                val screenHeightPx = size.height

                                val isAtLeftEdge = currentPos.x < edgeBorderPx
                                val isAtRightEdge = currentPos.x > (screenWidthPx - edgeBorderPx)
                                val isAtTopEdge = currentPos.y < edgeBorderPx
                                val isAtBottomEdge = currentPos.y > (screenHeightPx - edgeBorderPx)

                                // Log edge detection (only if at any edge)
                                val edgePosition = when {
                                    isAtLeftEdge -> com.example.questflow.presentation.screens.timeline.model.EdgePosition.LEFT
                                    isAtRightEdge -> com.example.questflow.presentation.screens.timeline.model.EdgePosition.RIGHT
                                    isAtTopEdge -> com.example.questflow.presentation.screens.timeline.model.EdgePosition.TOP
                                    isAtBottomEdge -> com.example.questflow.presentation.screens.timeline.model.EdgePosition.BOTTOM
                                    else -> com.example.questflow.presentation.screens.timeline.model.EdgePosition.NONE
                                }

                                if (isAtLeftEdge || isAtRightEdge || isAtTopEdge || isAtBottomEdge) {
                                    val edgeType = when {
                                        isAtLeftEdge -> "EDGE_LEFT"
                                        isAtRightEdge -> "EDGE_RIGHT"
                                        isAtTopEdge -> "EDGE_TOP"
                                        isAtBottomEdge -> "EDGE_BOTTOM"
                                        else -> "EDGE_UNKNOWN"
                                    }

                                    android.util.Log.d("TimelineGrid", "🔶 $edgeType at (${currentPos.x.toInt()}, ${currentPos.y.toInt()}), border=${edgeBorderPx.toInt()}px")
                                    viewModel.updateGestureDebug(edgeType, now - startTime, currentPos.x, currentPos.y,
                                        "$edgeType | X: ${currentPos.x.toInt()}/${screenWidthPx.toInt()}px, Y: ${currentPos.y.toInt()}/${screenHeightPx.toInt()}px",
                                        atEdge = edgePosition)
                                    lastLoggedGesture = edgeType
                                } else {
                                    // Not at edge, clear edge position if last gesture was an edge
                                    if (lastLoggedGesture.startsWith("EDGE_")) {
                                        viewModel.updateGestureDebug(lastLoggedGesture, now - startTime, currentPos.x, currentPos.y,
                                            "Left edge zone",
                                            atEdge = com.example.questflow.presentation.screens.timeline.model.EdgePosition.NONE)
                                    }
                                }

                                // Movement Detection (> 5px)
                                if (distance > 5f) {
                                    val direction = when {
                                        abs(delta.y) > abs(delta.x) -> if (delta.y > 0) "DOWN" else "UP"
                                        else -> if (delta.x > 0) "RIGHT" else "LEFT"
                                    }

                                    // Swipe Detection (velocity > 1000 px/s)
                                    if (velocity > 1000f && !isSwipe) {
                                        isSwipe = true
                                        swipeDirection = direction
                                        holdStartTime = now

                                        // Calculate dominant direction percentage
                                        val totalMovement = abs(totalDeltaX) + abs(totalDeltaY)
                                        val verticalPercent = if (totalMovement > 0) (abs(totalDeltaY) / totalMovement * 100).toInt() else 0
                                        val horizontalPercent = if (totalMovement > 0) (abs(totalDeltaX) / totalMovement * 100).toInt() else 0

                                        android.util.Log.d("TimelineGrid", "⚡ SWIPE_$direction at (${currentPos.x.toInt()}, ${currentPos.y.toInt()}): ${velocity.toInt()} px/s")
                                        viewModel.updateGestureDebug("SWIPE_$direction",
                                            now - startTime, currentPos.x, currentPos.y,
                                            "Swipe $direction: ${velocity.toInt()} px/s | Dominant: ${if (verticalPercent > horizontalPercent) "vertical ${verticalPercent}%" else "horizontal ${horizontalPercent}%"}")
                                        lastLoggedGesture = "SWIPE_$direction"

                                    } else if (isSwipe && (now - holdStartTime) > 100) {
                                        // Continue holding after swipe
                                        android.util.Log.d("TimelineGrid", "🤚 SWIPE_HOLD_$swipeDirection at (${currentPos.x.toInt()}, ${currentPos.y.toInt()}) for ${now - holdStartTime}ms")
                                        viewModel.updateGestureDebug("SWIPE_HOLD_$swipeDirection",
                                            now - holdStartTime, currentPos.x, currentPos.y,
                                            "Holding after swipe for ${now - holdStartTime}ms")
                                        lastLoggedGesture = "SWIPE_HOLD_$swipeDirection"

                                    } else if (velocity < 1000f && !isSwipe) {
                                        // Slow movement = SCROLL
                                        android.util.Log.d("TimelineGrid", "📜 SCROLL_$direction at (${currentPos.x.toInt()}, ${currentPos.y.toInt()}): ${velocity.toInt()} px/s")
                                        viewModel.updateGestureDebug("SCROLL_$direction",
                                            0, currentPos.x, currentPos.y,
                                            "Scroll $direction: ${velocity.toInt()} px/s")
                                        lastLoggedGesture = "SCROLL_$direction"
                                    }
                                }

                                lastPos = currentPos
                                lastMoveTime = now
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

                        // 3 day columns - with LongPress detection on empty areas
                        visibleDays.forEach { day ->
                            HourSlotWithTasks(
                                hour = hour,
                                hourHeightDp = hourHeightDp,
                                dayTimeline = day,
                                dpPerMinute = dpPerMinute,
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

            // CONDITIONAL OVERLAY - ONLY render when drag mode is active
            // LazyColumn handles ALL gestures by default (tap, scroll, swipe)
            // Overlay ONLY activates after long-press on empty area
            if (uiState.dragSelectionState != null) {
                TimelineGestureOverlay(
                    visibleDays = visibleDays,
                    scrollState = scrollState,
                    pixelsPerMinute = pixelsPerMinute,
                    headerHeightPx = headerHeightPx,
                    timeColumnWidthDp = 60.dp,
                    onTaskClick = onTaskClick,
                    onTaskLongPress = onTaskLongPress,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Single hour slot for one day, containing background grid, selection box overlay, and tasks.
 * Gesture detection: LongPress on EMPTY areas activates drag mode
 */
@Composable
private fun HourSlotWithTasks(
    hour: Int,
    hourHeightDp: androidx.compose.ui.unit.Dp,
    dayTimeline: DayTimeline,
    dpPerMinute: Float,  // DP per minute for task rendering
    selectedTaskIds: Set<Long> = emptySet(),
    selectionBox: com.example.questflow.presentation.screens.timeline.model.SelectionBox? = null,
    dragSelectionState: com.example.questflow.presentation.screens.timeline.model.DragSelectionState? = null,
    onTaskClick: (TimelineTask) -> Unit,
    onTaskLongPress: (TimelineTask) -> Unit,
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(hourHeightDp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // Tap detected - show in debug
                        android.util.Log.d("HourSlot", "👆 Tap at offset=$offset in hour=$hour, day=${dayTimeline.date}")
                        viewModel.updateGestureDebug("TAP", 0, offset.x, offset.y, "Tap at hour $hour, day ${dayTimeline.date}")
                    },
                    onLongPress = { offset ->
                        // LongPress on empty area - activate drag mode
                        android.util.Log.d("HourSlot", "🔴 LongPress detected at offset=$offset in hour=$hour, day=${dayTimeline.date}")

                        // Check if we hit a task - if so, don't activate drag mode
                        val hourStart = hour * 60
                        val hourEnd = (hour + 1) * 60
                        val yInHourPx = offset.y
                        val yInHourDp = with(density) { yInHourPx.toDp().value }
                        val minuteInHour = (yInHourDp / dpPerMinute).toInt()
                        val absoluteMinute = hourStart + minuteInHour

                        // Check if any task is at this position
                        val hitTask = dayTimeline.tasks.any { task ->
                            val taskStartMinutes = task.startTime.toLocalTime().hour * 60 + task.startTime.toLocalTime().minute
                            val taskEndMinutes = task.endTime.toLocalTime().hour * 60 + task.endTime.toLocalTime().minute
                            absoluteMinute in taskStartMinutes until taskEndMinutes
                        }

                        if (!hitTask) {
                            // Empty area - activate drag mode!
                            android.util.Log.d("HourSlot", "✅ Empty area - activating drag mode at minute=$absoluteMinute")
                            viewModel.updateGestureDebug("LONGPRESS_EMPTY", 0, offset.x, offset.y, "Activating drag mode")

                            // Calculate LocalDateTime from day + minute
                            val timeHour = absoluteMinute / 60
                            val timeMinute = absoluteMinute % 60
                            val startTime = java.time.LocalDateTime.of(
                                dayTimeline.date,
                                java.time.LocalTime.of(timeHour, timeMinute)
                            )
                            viewModel.onDragSelectionStart(startTime)
                        } else {
                            android.util.Log.d("HourSlot", "❌ Task hit - not activating drag mode")
                            viewModel.updateGestureDebug("LONGPRESS_TASK", 0, offset.x, offset.y, "Task hit, ignoring")
                        }
                    }
                )
            }
    ) {
        // Background grid for this hour slot
        HourBackgroundGrid(
            hour = hour,
            dpPerMinute = dpPerMinute,
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
                dpPerMinute = dpPerMinute,
                isPreview = true
            )
        }

        // Final selection box overlay (only in this hour if it overlaps)
        if (selectionBox != null) {
            RenderSelectionBoxInHourSlot(
                selectionBox = selectionBox,
                dayDate = dayTimeline.date,
                hour = hour,
                dpPerMinute = dpPerMinute,
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
                    (taskStartMinutes - hourStart) * dpPerMinute  // Use DP for rendering!
                } else {
                    0f // Task started before this hour
                }

                val visibleHeight = when {
                    taskStartMinutes < hourStart && taskEndMinutes > hourEnd -> {
                        // Task spans entire hour
                        60 * dpPerMinute  // Use DP for rendering!
                    }
                    taskStartMinutes < hourStart -> {
                        // Task started before, ends during hour
                        (taskEndMinutes - hourStart) * dpPerMinute  // Use DP for rendering!
                    }
                    taskEndMinutes > hourEnd -> {
                        // Task starts during hour, ends after
                        (hourEnd - taskStartMinutes) * dpPerMinute  // Use DP for rendering!
                    }
                    else -> {
                        // Task fully within hour
                        (taskEndMinutes - taskStartMinutes) * dpPerMinute  // Use DP for rendering!
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
    dpPerMinute: Float,  // DP per minute for rendering
    hourHeightDp: androidx.compose.ui.unit.Dp,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primaryContainer
    val hourHeightPx = with(density) { hourHeightDp.toPx() }

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
