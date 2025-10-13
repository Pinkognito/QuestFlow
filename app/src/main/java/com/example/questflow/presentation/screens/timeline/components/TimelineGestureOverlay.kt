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
 * Smart transparent gesture overlay for Timeline.
 *
 * Strategy:
 * - Overlay receives ALL events first (on top)
 * - Quick taps/swipes: Don't consume → pass to tasks/scroll
 * - Long-press: Consume and handle → multi-day drag
 *
 * Position tracking: Always tracks coordinates during drag
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
     * Convert screen position to DateTime with full position tracking.
     */
    fun screenPosToDateTime(x: Float, y: Float): LocalDateTime? {
        // X-axis → Which day?
        val xRelativeToContent = x - timeColumnWidthPx
        if (xRelativeToContent < 0) return null

        val dayColumnWidth = xRelativeToContent / visibleDays.size
        val dayIndex = (xRelativeToContent / dayColumnWidth).toInt()
        if (dayIndex !in visibleDays.indices) return null

        val targetDay = visibleDays[dayIndex]

        // Y-axis + scroll → Which time?
        val yRelativeToContent = y - headerHeightPx
        if (yRelativeToContent < 0) return null

        val scrollOffsetPx = scrollState.firstVisibleItemIndex * hourHeightPx +
                scrollState.firstVisibleItemScrollOffset.toFloat()
        val absoluteY = yRelativeToContent + scrollOffsetPx

        val totalMinutes = (absoluteY / pixelsPerMinute).toInt().coerceIn(0, 1439)
        val hour = totalMinutes / 60
        val minute = totalMinutes % 60

        android.util.Log.d("TimelineGesture", "Pos tracking: x=$x, y=$y → Day=${targetDay.date}, Time=$hour:${String.format("%02d", minute)}")

        return LocalDateTime.of(targetDay.date, LocalTime.of(hour, minute))
    }

    /**
     * Find task at position.
     */
    fun getTaskAt(x: Float, y: Float): TimelineTask? {
        val dateTime = screenPosToDateTime(x, y) ?: return null

        val xRelativeToContent = x - timeColumnWidthPx
        val dayColumnWidth = xRelativeToContent / visibleDays.size
        val dayIndex = (xRelativeToContent / dayColumnWidth).toInt()
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
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val downTime = System.currentTimeMillis()
                    val downPos = down.position

                    android.util.Log.d("TimelineGesture", "=== GESTURE START === at ${downPos.x}, ${downPos.y}")
                    viewModel.updateGestureDebug("DOWN", 0, downPos.x, downPos.y, "Touch Down")

                    data class EarlyResult(val movement: Pair<Float, Float>?, val released: Boolean)

                    // Early detection (150ms) for quick tap/swipe
                    val earlyCheck = withTimeoutOrNull(150L) {
                        var totalX = 0f
                        var totalY = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()

                            totalX += change.positionChange().x
                            totalY += change.positionChange().y

                            if (abs(totalX) > 10f || abs(totalY) > 10f) {
                                android.util.Log.d("TimelineGesture", "SWIPE detected (X=$totalX, Y=$totalY)")
                                return@withTimeoutOrNull EarlyResult(Pair(totalX, totalY), false)
                            }

                            if (!change.pressed) {
                                android.util.Log.d("TimelineGesture", "QUICK TAP (released < 150ms)")
                                return@withTimeoutOrNull EarlyResult(null, true)
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        EarlyResult(null, false)
                    } ?: EarlyResult(null, false)

                    when {
                        // Quick tap or swipe → Don't consume, pass through
                        earlyCheck.released || earlyCheck.movement != null -> {
                            val gestureType = if (earlyCheck.released) "QUICK_TAP" else "SWIPE"
                            android.util.Log.d("TimelineGesture", "$gestureType → Pass-through")
                            viewModel.updateGestureDebug(gestureType, System.currentTimeMillis() - downTime,
                                0f, 0f, "Pass-through to tasks/scroll")
                            // Exit without consuming
                            return@awaitEachGesture
                        }

                        // No early action → Wait for long press
                        else -> {
                            android.util.Log.d("TimelineGesture", "Waiting for long-press (850ms)...")
                            viewModel.updateGestureDebug("WAITING", 150, 0f, 0f, "Waiting for 1 sec...")

                            val longPressResult = withTimeoutOrNull(850L) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()
                                    if (!change.pressed) {
                                        android.util.Log.d("TimelineGesture", "Released before 1 sec")
                                        return@withTimeoutOrNull "released"
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE")
                                "timeout"
                            }

                            if (longPressResult == "released") {
                                // Tap (150ms - 1000ms) → Pass through
                                android.util.Log.d("TimelineGesture", "TAP → Pass-through")
                                viewModel.updateGestureDebug("TAP", System.currentTimeMillis() - downTime,
                                    0f, 0f, "Pass-through to tasks")
                                return@awaitEachGesture
                            }

                            // LONG PRESS reached!
                            android.util.Log.d("TimelineGesture", "LONG PRESS detected! (1 second)")
                            val startDateTime = screenPosToDateTime(downPos.x, downPos.y)
                            if (startDateTime == null) {
                                android.util.Log.d("TimelineGesture", "Invalid position, ignoring")
                                return@awaitEachGesture
                            }

                            val task = getTaskAt(downPos.x, downPos.y)
                            viewModel.updateGestureDebug("LONG_PRESS", 1000, 0f, 0f,
                                if (task != null) "On task: ${task.title}" else "On empty space")

                            // Check for movement after long-press
                            val postLongPressMovement = withTimeoutOrNull(100L) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()

                                    val dx = abs(change.positionChange().x)
                                    val dy = abs(change.positionChange().y)

                                    if (dx > 5f || dy > 5f) {
                                        android.util.Log.d("TimelineGesture", "Movement detected after long-press")
                                        return@withTimeoutOrNull true
                                    }

                                    if (!change.pressed) {
                                        android.util.Log.d("TimelineGesture", "Released after long-press")
                                        return@withTimeoutOrNull false
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE")
                                false
                            }

                            when {
                                // Long-press + release (no drag)
                                postLongPressMovement == false -> {
                                    if (task != null) {
                                        android.util.Log.d("TimelineGesture", "Long-press on task → Select")
                                        viewModel.updateGestureDebug("TASK_SELECT", 0, 0f, 0f, "Task: ${task.title}")
                                        onTaskLongPress(task)
                                    } else {
                                        android.util.Log.d("TimelineGesture", "Long-press on empty → 15min mark")
                                        val endDateTime = startDateTime.plusMinutes(15)
                                        viewModel.onDragSelectionStart(startDateTime)
                                        viewModel.onDragSelectionUpdate(endDateTime)
                                        viewModel.onDragSelectionEnd()
                                        viewModel.updateGestureDebug("15MIN_MARK", 0, 0f, 0f, "Marked 15 min")
                                    }
                                }

                                // Long-press + drag → MULTI-DAY DRAG!
                                postLongPressMovement == true -> {
                                    android.util.Log.d("TimelineGesture", "DRAG START → Multi-day selection")
                                    viewModel.updateGestureDebug("DRAG_START", 0, 0f, 0f, "Multi-day drag starting")

                                    viewModel.onDragSelectionStart(startDateTime)
                                    down.consume() // NOW consume for dragging

                                    drag(down.id) { change ->
                                        val currentDateTime = screenPosToDateTime(change.position.x, change.position.y)

                                        if (currentDateTime != null) {
                                            viewModel.onDragSelectionUpdate(currentDateTime)

                                            android.util.Log.d("TimelineGesture", "DRAGGING → ${currentDateTime.toLocalDate()} ${currentDateTime.toLocalTime()}")
                                            viewModel.updateGestureDebug("DRAGGING",
                                                System.currentTimeMillis() - downTime,
                                                change.position.x,
                                                change.position.y,
                                                "${currentDateTime.toLocalDate()} ${currentDateTime.toLocalTime()}")
                                        }

                                        change.consume()
                                    }

                                    viewModel.onDragSelectionEnd()
                                    android.util.Log.d("TimelineGesture", "DRAG END → Selection complete")
                                    viewModel.updateGestureDebug("DRAG_END", 0, 0f, 0f, "Multi-day selection set")
                                }

                                // Timeout without movement
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
    )
}
