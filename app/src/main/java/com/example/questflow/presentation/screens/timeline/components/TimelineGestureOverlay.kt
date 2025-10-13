package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyListState
import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.presentation.screens.timeline.TimelineViewModel
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

/**
 * Transparent gesture overlay for the Timeline.
 * Handles all touch events globally to enable:
 * - Multi-day drag-to-select
 * - Coordinate conversion (screen → DateTime)
 * - Smart event consumption (pass through swipes, consume long-press + drag)
 */
@Composable
fun TimelineGestureOverlay(
    visibleDays: List<DayTimeline>,
    scrollState: LazyListState,
    pixelsPerMinute: Float,
    headerHeightPx: Float,
    timeColumnWidthDp: Dp,
    onTaskClick: (TimelineTask) -> Unit,
    onTaskLongPress: (TimelineTask) -> Unit,
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val timeColumnWidthPx = with(density) { timeColumnWidthDp.toPx() }
    val hourHeightPx = 60 * pixelsPerMinute

    /**
     * Convert screen position to DateTime.
     * Returns null if position is outside valid areas.
     *
     * CRITICAL: Handles multi-day drag by detecting day overflow based on Y-position.
     * When dragging vertically beyond 23:59, automatically switches to next/previous day.
     */
    fun screenPosToDateTime(x: Float, y: Float): LocalDateTime? {
        // 1. Which day column? (X-axis - determines BASE day)
        val xRelativeToContent = x - timeColumnWidthPx
        if (xRelativeToContent < 0) return null // Touch in time column

        // Estimate day column width (3 days equally split the remaining width)
        // Note: This is approximate but works for touch detection
        val estimatedDayColumnWidth = xRelativeToContent / visibleDays.size
        val dayIndex = (xRelativeToContent / (estimatedDayColumnWidth.coerceAtLeast(1f))).toInt().coerceIn(0, visibleDays.size - 1)

        val baseDayTimeline = visibleDays[dayIndex]
        val baseDate = baseDayTimeline.date

        // 2. Which time? (Y-axis + scroll offset)
        // IMPORTANT: Touch coordinates are RELATIVE to the Box (which starts AFTER the header)
        // No need to subtract header - coordinates are already in content space!
        val yRelativeToContent = y
        if (yRelativeToContent < 0) return null // Sanity check

        val scrollOffsetPx = scrollState.firstVisibleItemIndex * hourHeightPx +
                scrollState.firstVisibleItemScrollOffset.toFloat()
        val absoluteY = yRelativeToContent + scrollOffsetPx

        // CRITICAL: Calculate minutes WITHOUT clamping to allow day overflow!
        val totalMinutesRaw = (absoluteY / pixelsPerMinute).toInt()

        // Calculate day offset (how many days to add/subtract based on vertical position)
        val dayOffset = when {
            totalMinutesRaw < 0 -> {
                // Negative = dragged above 00:00 = previous day(s)
                (totalMinutesRaw / 1440) - 1  // -1 to -1439 → -1 day, -1440 to -2879 → -2 days
            }
            totalMinutesRaw >= 1440 -> {
                // Beyond 23:59 = next day(s)
                totalMinutesRaw / 1440  // 1440-2879 → +1 day, 2880-4319 → +2 days
            }
            else -> 0  // Within same day (0-1439)
        }

        // Calculate time within the target day (0-1439 minutes)
        val minutesInDay = if (totalMinutesRaw < 0) {
            // For negative, wrap around: -30min = 23:30 previous day
            1440 + (totalMinutesRaw % 1440)
        } else {
            totalMinutesRaw % 1440
        }

        val hour = (minutesInDay / 60).coerceIn(0, 23)
        val minute = (minutesInDay % 60).coerceIn(0, 59)

        // Apply day offset to base date
        val targetDate = baseDate.plusDays(dayOffset.toLong())

        android.util.Log.d("TimelineGesture", "Coords: touchX=$x, touchY=$y, scrollOffset=$scrollOffsetPx, absoluteY=$absoluteY, totalMinutesRaw=$totalMinutesRaw, dayOffset=$dayOffset, time=${hour}:${String.format("%02d", minute)}, baseDay=${baseDate}, targetDay=${targetDate}")

        return LocalDateTime.of(targetDate, LocalTime.of(hour, minute))
    }

    /**
     * Find task at screen position.
     */
    fun getTaskAt(x: Float, y: Float): TimelineTask? {
        val dateTime = screenPosToDateTime(x, y) ?: return null

        // Find day
        val dayIndex = ((x - timeColumnWidthPx) * visibleDays.size / (x - timeColumnWidthPx + 1)).toInt()
        if (dayIndex !in visibleDays.indices) return null
        val day = visibleDays[dayIndex]

        val clickMinutes = dateTime.toLocalTime().hour * 60 + dateTime.toLocalTime().minute

        return day.tasks.find { task ->
            if (task.startTime.toLocalDate() != dateTime.toLocalDate()) return@find false
            val taskStartMinutes = task.startTime.toLocalTime().hour * 60 + task.startTime.toLocalTime().minute
            val taskEndMinutes = task.endTime.toLocalTime().hour * 60 + task.endTime.toLocalTime().minute
            clickMinutes in taskStartMinutes until taskEndMinutes
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(visibleDays, scrollState, pixelsPerMinute) {
                // Get total screen width in this scope
                val totalScreenWidth = size.width

                /**
                 * Convert screen position to DateTime (inner version with access to size).
                 */
                fun screenPosToDateTimeInner(x: Float, y: Float): LocalDateTime? {
                    // 1. Which day column? (X-axis - determines BASE day)
                    val xRelativeToContent = x - timeColumnWidthPx
                    if (xRelativeToContent < 0) return null // Touch in time column

                    // Calculate actual day column width
                    val totalContentWidth = totalScreenWidth - timeColumnWidthPx
                    val dayColumnWidth = totalContentWidth / visibleDays.size
                    val dayIndex = (xRelativeToContent / dayColumnWidth).toInt().coerceIn(0, visibleDays.size - 1)

                    val baseDayTimeline = visibleDays[dayIndex]
                    val baseDate = baseDayTimeline.date

                    // 2. Which time? (Y-axis + scroll offset)
                    // IMPORTANT: Touch coordinates are RELATIVE to the Box (which starts AFTER the header)
                    // No need to subtract header - coordinates are already in content space!
                    val yRelativeToContent = y
                    if (yRelativeToContent < 0) return null // Sanity check

                    val scrollOffsetPx = scrollState.firstVisibleItemIndex * hourHeightPx +
                            scrollState.firstVisibleItemScrollOffset.toFloat()
                    val absoluteY = yRelativeToContent + scrollOffsetPx

                    // CRITICAL: Calculate minutes WITHOUT clamping to allow day overflow!
                    val totalMinutesRaw = (absoluteY / pixelsPerMinute).toInt()

                    android.util.Log.d("TimelineGesture", "🧮 FIXED Calculation: touchY=$y (no header subtraction!), yRel=$yRelativeToContent, scrollIdx=${scrollState.firstVisibleItemIndex}, scrollOff=${scrollState.firstVisibleItemScrollOffset}, hourHeightPx=$hourHeightPx, scrollOffsetPx=$scrollOffsetPx, absoluteY=$absoluteY, pixelsPerMinute=$pixelsPerMinute, totalMinutesRaw=$totalMinutesRaw")

                    // Calculate day offset (how many days to add/subtract based on vertical position)
                    val dayOffset = when {
                        totalMinutesRaw < 0 -> {
                            // Negative = dragged above 00:00 = previous day(s)
                            (totalMinutesRaw / 1440) - 1  // -1 to -1439 → -1 day, -1440 to -2879 → -2 days
                        }
                        totalMinutesRaw >= 1440 -> {
                            // Beyond 23:59 = next day(s)
                            totalMinutesRaw / 1440  // 1440-2879 → +1 day, 2880-4319 → +2 days
                        }
                        else -> 0  // Within same day (0-1439)
                    }

                    // Calculate time within the target day (0-1439 minutes)
                    val minutesInDay = if (totalMinutesRaw < 0) {
                        // For negative, wrap around: -30min = 23:30 previous day
                        1440 + (totalMinutesRaw % 1440)
                    } else {
                        totalMinutesRaw % 1440
                    }

                    val hour = (minutesInDay / 60).coerceIn(0, 23)
                    val minute = (minutesInDay % 60).coerceIn(0, 59)

                    // Apply day offset to base date
                    val targetDate = baseDate.plusDays(dayOffset.toLong())

                    android.util.Log.d("TimelineGesture", "Coords: touchX=$x, touchY=$y, xRel=$xRelativeToContent, dayColWidth=$dayColumnWidth, dayIdx=$dayIndex, scrollOffset=$scrollOffsetPx, absoluteY=$absoluteY, totalMinutesRaw=$totalMinutesRaw, dayOffset=$dayOffset, time=${hour}:${String.format("%02d", minute)}, baseDay=${baseDate}, targetDay=${targetDate}")

                    return LocalDateTime.of(targetDate, LocalTime.of(hour, minute))
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val downTime = System.currentTimeMillis()
                    val downPos = down.position

                    android.util.Log.d("TimelineGesture", "=== GESTURE START === Touch Down at ${downPos.x}, ${downPos.y}")
                    viewModel.updateGestureDebug("DOWN", 0, downPos.x, downPos.y, "Touch Down")

                    // Early movement detection (150ms) for swipe
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
                            val task = getTaskAt(downPos.x, downPos.y)
                            val releaseTime = System.currentTimeMillis() - downTime

                            android.util.Log.d("TimelineGesture", "QUICK TAP! Released after ${releaseTime}ms")

                            if (task != null) {
                                viewModel.updateGestureDebug("TAP_TASK", releaseTime, 0f, 0f,
                                    "Quick-Tap nach ${releaseTime}ms: ${task.title}")
                                onTaskClick(task)
                            } else {
                                val dateTime = screenPosToDateTimeInner(downPos.x, downPos.y)
                                if (dateTime != null) {
                                    viewModel.updateGestureDebug("TAP_EMPTY", releaseTime, 0f, 0f,
                                        "Quick-Tap nach ${releaseTime}ms @ ${dateTime.toLocalTime()} → TODO: Create Task")
                                }
                            }
                        }

                        // Early movement detected = SWIPE → pass through
                        earlyCheck.movement != null -> {
                            val (initX, initY) = earlyCheck.movement
                            android.util.Log.d("TimelineGesture", "SWIPE detected → passing through to LazyColumn")

                            viewModel.updateGestureDebug("SWIPING",
                                System.currentTimeMillis() - downTime,
                                initX, initY,
                                "Swipe erkannt → Pass-through")

                            // Don't consume - let LazyColumn handle scroll
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
                                // Quick tap (released before 1000ms)
                                "released" -> {
                                    val task = getTaskAt(downPos.x, downPos.y)
                                    val releaseTime = System.currentTimeMillis() - downTime

                                    android.util.Log.d("TimelineGesture", "TAP confirmed! Released after ${releaseTime}ms, task=$task")

                                    if (task != null) {
                                        viewModel.updateGestureDebug("TAP_TASK", releaseTime, 0f, 0f,
                                            "Task-Tap nach ${releaseTime}ms: ${task.title}")
                                        onTaskClick(task)
                                    } else {
                                        val dateTime = screenPosToDateTimeInner(downPos.x, downPos.y)
                                        if (dateTime != null) {
                                            viewModel.updateGestureDebug("TAP_EMPTY", releaseTime, 0f, 0f,
                                                "Leer-Tap nach ${releaseTime}ms @ ${dateTime.toLocalTime()} → TODO: Create Task")
                                        }
                                    }
                                }

                                // Long-press (1000ms timeout)
                                else -> {
                                    android.util.Log.d("TimelineGesture", "LONG_PRESS! Timeout reached (1000ms)")
                                    val startDateTime = screenPosToDateTimeInner(downPos.x, downPos.y)

                                    if (startDateTime == null) {
                                        android.util.Log.d("TimelineGesture", "Invalid start position - ignoring")
                                        return@awaitEachGesture
                                    }

                                    val task = getTaskAt(downPos.x, downPos.y)

                                    viewModel.updateGestureDebug("LONG_PRESS", 1000, 0f, 0f,
                                        "Long-Press erkannt (1 Sekunde)")

                                    // Check if movement after long-press
                                    val postLongPressMovement = withTimeoutOrNull(100L) {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.first()

                                            val dx = abs(change.positionChange().x)
                                            val dy = abs(change.positionChange().y)

                                            if (dx > 5f || dy > 5f) {
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
                                        // Long-press + release without movement
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
                                        }

                                        // Long-press + drag (MULTI-DAY SUPPORT!)
                                        postLongPressMovement == true -> {
                                            viewModel.updateGestureDebug("DRAG_START", 0, 0f, 0f,
                                                "Drag Markierung starten (Multi-Day)")

                                            viewModel.onDragSelectionStart(startDateTime)

                                            // NOW consume events for drag
                                            down.consume()

                                            // Track drag across days
                                            drag(down.id) { change ->
                                                val currentDateTime = screenPosToDateTimeInner(change.position.x, change.position.y)

                                                if (currentDateTime != null) {
                                                    viewModel.onDragSelectionUpdate(currentDateTime)

                                                    viewModel.updateGestureDebug("DRAGGING",
                                                        System.currentTimeMillis() - downTime,
                                                        change.position.x,
                                                        change.position.y,
                                                        "Ziehen: ${currentDateTime.toLocalDate()} ${currentDateTime.toLocalTime()}")
                                                }

                                                change.consume()
                                            }

                                            // Release after drag
                                            viewModel.onDragSelectionEnd()
                                            viewModel.updateGestureDebug("DRAG_END", 0, 0f, 0f,
                                                "Multi-Day Markierung gesetzt")
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    )
}
