package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.TimelineViewModel
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.domain.model.TimelineTask
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * UNIFIED Timeline Grid - Single-Layer Gesture Architecture
 *
 * KEY ARCHITECTURE:
 * - Single LazyColumn with rows containing [Time | Day1 | Day2 | Day3]
 * - Time scrolls WITH the days - unified table
 * - Sticky header (stays visible during scroll)
 * - UNIFIED GESTURE HANDLER: All gestures handled in HourSlots directly
 * - No separate overlay - LazyColumn is always responsive
 * - Drag-to-select with detectDragGesturesAfterLongPress (nahtlos!)
 * - Smart scroll: only when near edges during drag selection
 * - Only visible rows rendered (~5-10 out of 24)
 */
@Composable
fun TimelineGrid(
    uiState: TimelineUiState,
    longPressDelayMs: Long = 250L,
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
    val timeColumnWidthPx = with(density) { 60.dp.toPx() }

    // Measure screen width to calculate content area width
    var screenWidthPx by remember { mutableStateOf(0f) }

    // Update ViewModel with layout metrics (once at composition)
    // NOTE: Do NOT include pixelsPerMinute as a key - it would create a feedback loop!
    LaunchedEffect(headerHeightPx, timeColumnWidthPx, screenWidthPx, density.density) {
        if (screenWidthPx > 0f) {
            val contentWidthPx = screenWidthPx - timeColumnWidthPx
            android.util.Log.d("TimelineGrid", "📐 Updating layout metrics: headerHeight=${headerHeightPx}px, timeColWidth=${timeColumnWidthPx}px, screenWidth=${screenWidthPx}px, contentWidth=${contentWidthPx}px, density=${density.density}")
            viewModel.updateLayoutMetrics(
                headerHeightPx = headerHeightPx,
                timeColumnWidthPx = timeColumnWidthPx,
                contentWidthPx = contentWidthPx,
                density = density.density
            )
        }
    }

    // Update ViewModel with scroll position (on every scroll)
    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        val hourHeightPx = with(density) { hourHeightDp.toPx() }
        val scrollOffsetPx = scrollState.firstVisibleItemIndex * hourHeightPx +
            scrollState.firstVisibleItemScrollOffset.toFloat()

        // Only log significant changes (reduce spam)
        if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0) {
            android.util.Log.d("TimelineGrid", "📜 Scroll at top: offset=0px")
        }

        viewModel.updateScrollPosition(scrollOffsetPx)
    }

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

    // NEW: Gesture detection for horizontal swipe and edge auto-scroll during vertical scrolling
    var isTouching by remember { mutableStateOf(false) }
    var currentTouchX by remember { mutableStateOf(0f) }
    var isVerticalScrolling by remember { mutableStateOf(false) }
    var screenWidth by remember { mutableStateOf(0f) }
    var dayColumnWidth by remember { mutableStateOf(0f) }
    var horizontalSwipeTriggered by remember { mutableStateOf(false) } // Prevent multiple triggers
    var totalDragX by remember { mutableStateOf(0f) } // Accumulate horizontal movement

    // Edge auto-scroll during vertical scrolling (NOT drag-selection)
    LaunchedEffect(isTouching, currentTouchX, isVerticalScrolling) {
        if (isTouching && isVerticalScrolling && !isDragging && dayColumnWidth > 0f) {
            // Detect edge position (20% of day column width)
            val edgeBorderWidth = dayColumnWidth * 0.2f
            val timeColumnWidth = with(density) { 60.dp.toPx() }
            val contentStartX = timeColumnWidth
            val contentEndX = screenWidth
            val contentWidth = contentEndX - timeColumnWidth

            val relativeX = currentTouchX - timeColumnWidth
            val isAtLeftEdge = relativeX < edgeBorderWidth
            val isAtRightEdge = relativeX > (contentWidth - edgeBorderWidth)

            if (isAtLeftEdge || isAtRightEdge) {
                android.util.Log.d("TimelineGrid", "🔄 EDGE AUTO-SCROLL (vertical): touchX=$currentTouchX, leftEdge=$isAtLeftEdge, rightEdge=$isAtRightEdge")

                // Slow delay for edge scrolling (800ms vs 300ms for drag-selection)
                kotlinx.coroutines.delay(800)

                if (isAtLeftEdge) {
                    android.util.Log.d("TimelineGrid", "🔄 Shifting day window LEFT (vertical scroll)")
                    onDayWindowShift(-1)
                } else if (isAtRightEdge) {
                    android.util.Log.d("TimelineGrid", "🔄 Shifting day window RIGHT (vertical scroll)")
                    onDayWindowShift(1)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                val width = layoutCoordinates.size.width.toFloat()
                if (width != screenWidth && width > 0f) {
                    screenWidth = width
                    // Calculate day column width (3 columns in content area)
                    val timeColWidth = with(density) { 60.dp.toPx() }
                    val contentWidth = width - timeColWidth
                    dayColumnWidth = contentWidth / 3f
                    android.util.Log.d("TimelineGrid", "📏 Layout measured: screenWidth=$screenWidth, dayColumnWidth=$dayColumnWidth")
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isTouching = true
                        currentTouchX = offset.x
                        isVerticalScrolling = false
                        horizontalSwipeTriggered = false
                        totalDragX = 0f
                        android.util.Log.d("TimelineGrid", "👆 Gesture start: x=${offset.x}, y=${offset.y}")
                    },
                    onDrag = { change, dragAmount ->
                        currentTouchX = change.position.x
                        totalDragX += dragAmount.x

                        // Detect direction early
                        val absX = kotlin.math.abs(dragAmount.x)
                        val absY = kotlin.math.abs(dragAmount.y)

                        if (absY > absX * 2f) {
                            // Vertical scrolling detected
                            if (!isVerticalScrolling) {
                                isVerticalScrolling = true
                                android.util.Log.d("TimelineGrid", "⬆️ Vertical scroll detected")
                            }
                        } else if (absX > absY * 2f && !isVerticalScrolling && !horizontalSwipeTriggered) {
                            // Horizontal swipe detected (only if not already vertical and not already triggered)
                            if (kotlin.math.abs(totalDragX) > 100f) {
                                horizontalSwipeTriggered = true
                                val direction = if (totalDragX > 0) -1 else 1 // Right swipe = past, left = future
                                android.util.Log.d("TimelineGrid", "↔️ Horizontal swipe detected ONCE: totalDragX=$totalDragX, direction=$direction")
                                onDayWindowShift(direction)
                            }
                        }
                    },
                    onDragEnd = {
                        android.util.Log.d("TimelineGrid", "👆 Gesture end")
                        isTouching = false
                        isVerticalScrolling = false
                        horizontalSwipeTriggered = false
                        totalDragX = 0f
                    },
                    onDragCancel = {
                        android.util.Log.d("TimelineGrid", "❌ Gesture cancelled")
                        isTouching = false
                        isVerticalScrolling = false
                        horizontalSwipeTriggered = false
                        totalDragX = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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

        // UNIFIED GESTURE ARCHITECTURE: LazyColumn is ALWAYS responsive
        // Gestures handled directly in HourSlots with detectDragGesturesAfterLongPress
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

                    // 3 day columns - UNIFIED GESTURE HANDLER
                    visibleDays.forEach { day ->
                        HourSlotWithTasks(
                            hour = hour,
                            hourHeightDp = hourHeightDp,
                            dayTimeline = day,
                            dpPerMinute = dpPerMinute,
                            pixelsPerMinute = pixelsPerMinute,
                            scrollState = scrollState,
                            longPressDelayMs = longPressDelayMs,
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
        }
    }
}

/**
 * Single hour slot for one day, containing background grid, selection box overlay, and tasks.
 * UNIFIED GESTURE HANDLER: detectDragGesturesAfterLongPress for nahtlosen Übergang
 */
@Composable
private fun HourSlotWithTasks(
    hour: Int,
    hourHeightDp: androidx.compose.ui.unit.Dp,
    dayTimeline: DayTimeline,
    dpPerMinute: Float,  // DP per minute for task rendering
    pixelsPerMinute: Float,  // Pixels per minute for calculations
    scrollState: LazyListState,
    longPressDelayMs: Long = 250L,
    selectedTaskIds: Set<Long> = emptySet(),
    selectionBox: com.example.questflow.presentation.screens.timeline.model.SelectionBox? = null,
    dragSelectionState: com.example.questflow.presentation.screens.timeline.model.DragSelectionState? = null,
    onTaskClick: (TimelineTask) -> Unit,
    onTaskLongPress: (TimelineTask) -> Unit,
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var lastDragPosition by remember { mutableStateOf<Offset?>(null) }

    // NEW: Track absolute position of this cell on screen
    var cellAbsolutePosition by remember { mutableStateOf(Offset.Zero) }

    // CRITICAL: positionInRoot() returns position relative to ROOT (entire screen)
    // We need to subtract TopAppBar height only (Table header is part of scrolling content)
    // TopAppBar (Scaffold topBar): 64dp = ~192px at density 3.0
    val topAppBarHeightPx = with(density) { 64.dp.toPx() }  // Material3 TopAppBar standard height

    /**
     * Helper: Calculate DateTime from touch position within this HourSlot
     */
    fun calculateDateTime(offset: Offset, slotWidth: Float, slotHeight: Float): LocalDateTime {
        // Y-position within hour (0-59 minutes)
        val minute = (offset.y / slotHeight * 60).toInt().coerceIn(0, 59)

        // Calculate absolute scroll position to handle multi-day drag
        val hourHeightPx = slotHeight
        val scrollOffsetPx = scrollState.firstVisibleItemIndex * hourHeightPx +
                scrollState.firstVisibleItemScrollOffset.toFloat()
        val absoluteY = offset.y + (hour * hourHeightPx) + scrollOffsetPx

        // Calculate total minutes from midnight
        val totalMinutesRaw = (absoluteY / (hourHeightPx / 60)).toInt()

        // Handle day overflow for multi-day drag
        val dayOffset = when {
            totalMinutesRaw < 0 -> (totalMinutesRaw / 1440) - 1
            totalMinutesRaw >= 1440 -> totalMinutesRaw / 1440
            else -> 0
        }

        val minutesInDay = if (totalMinutesRaw < 0) {
            1440 + (totalMinutesRaw % 1440)
        } else {
            totalMinutesRaw % 1440
        }

        val finalHour = (minutesInDay / 60).coerceIn(0, 23)
        val finalMinute = (minutesInDay % 60).coerceIn(0, 59)
        val targetDate = dayTimeline.date.plusDays(dayOffset.toLong())

        return LocalDateTime.of(targetDate, LocalTime.of(finalHour, finalMinute))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(hourHeightDp)
            .onGloballyPositioned { layoutCoordinates ->
                // Track absolute screen position of this cell
                val newPosition = layoutCoordinates.positionInRoot()
                if (cellAbsolutePosition != newPosition) {
                    cellAbsolutePosition = newPosition
                    android.util.Log.d("HourSlot", "📍 Cell position updated: day=${dayTimeline.date}, hour=$hour, position=$newPosition")
                }
            }
            .pointerInput(Unit) {
                // CUSTOM 500ms Long-Press → Drag mit ABSOLUTEN Koordinaten
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position

                    android.util.Log.d("HourSlot", "👇 Touch down at: $downPosition")

                    // Wait for configurable long press
                    val longPressDetected = withTimeoutOrNull(longPressDelayMs) {
                        // Wait for pointer to go up or move significantly
                        var currentEvent = down
                        while (true) {
                            val event = awaitPointerEvent()
                            currentEvent = event.changes.first()

                            // Check if moved too much (> 20px = not a long press)
                            val delta = (currentEvent.position - downPosition).getDistance()
                            if (delta > 20f) {
                                android.util.Log.d("HourSlot", "❌ Moved too much ($delta px), not a long press")
                                return@withTimeoutOrNull false
                            }

                            // Check if released (tap, not long press)
                            if (!currentEvent.pressed) {
                                android.util.Log.d("HourSlot", "❌ Released too early, not a long press")
                                return@withTimeoutOrNull false
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        true
                    }

                    // If timeout occurred, it's a long press!
                    if (longPressDetected == null) {
                        android.util.Log.d("HourSlot", "✅ ${longPressDelayMs}ms long press detected!")

                        // Calculate absolute position
                        val relativeOffset = downPosition
                        val absoluteScreenPos = relativeOffset + cellAbsolutePosition
                        val correctedY = absoluteScreenPos.y - topAppBarHeightPx

                        android.util.Log.d("HourSlot", "🔥 DRAG START: day=${dayTimeline.date}, hour=$hour")
                        android.util.Log.d("HourSlot", "  ├─ Relative offset: $relativeOffset")
                        android.util.Log.d("HourSlot", "  ├─ Cell absolute pos: $cellAbsolutePosition")
                        android.util.Log.d("HourSlot", "  ├─ Absolute screen pos: $absoluteScreenPos")
                        android.util.Log.d("HourSlot", "  └─ Corrected Y: $correctedY")

                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastDragPosition = absoluteScreenPos

                        viewModel.onDragSelectionStartAbsolute(
                            absoluteX = absoluteScreenPos.x,
                            absoluteY = correctedY
                        )

                        // Now handle drag
                        drag(down.id) { change ->
                            val absoluteScreenPos = change.position + cellAbsolutePosition
                            val correctedY = absoluteScreenPos.y - topAppBarHeightPx

                            lastDragPosition = absoluteScreenPos

                            viewModel.onDragSelectionUpdateAbsolute(
                                absoluteX = absoluteScreenPos.x,
                                absoluteY = correctedY
                            )

                            if (System.currentTimeMillis() % 5 == 0L) {
                                android.util.Log.d("HourSlot", "📍 DRAG UPDATE: absolute=$absoluteScreenPos, correctedY=$correctedY")
                            }
                        }

                        // Drag ended
                        val releasePos = lastDragPosition
                        android.util.Log.d("HourSlot", "✅ DRAG END at: $releasePos")

                        viewModel.onDragSelectionEnd()
                        if (releasePos != null) {
                            viewModel.showContextMenu(releasePos.x, releasePos.y)
                        }
                        viewModel.clearGestureDebug()
                    } else {
                        android.util.Log.d("HourSlot", "⏭️ Not a long press, ignoring")
                    }
                }
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
                    longPressDelayMs = longPressDelayMs,
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
    longPressDelayMs: Long = 250L,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    TaskBalken(
        task = task,
        pixelsPerMinute = 2f,
        longPressDelayMs = longPressDelayMs,
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

    // Format: "15.10(Mi)"
    val headerText = "$dayOfMonth.$month($dayOfWeek)"

    Box(
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
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = headerText,
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
    }
}
