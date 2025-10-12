package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.presentation.screens.timeline.util.TimelineCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Vertical column representing a single day in the timeline.
 * Shows day header at top and tasks positioned absolutely based on their start time.
 *
 * This component does NOT have its own scroll state - scrolling is controlled
 * by the parent and synchronized across all day columns.
 */
@Composable
fun DayTimelineColumn(
    dayTimeline: DayTimeline,
    pixelsPerMinute: Float,
    onTaskClick: (com.example.questflow.domain.model.TimelineTask) -> Unit,
    onTaskLongPress: (com.example.questflow.domain.model.TimelineTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalHeight = TimelineCalculator.calculateTimelineHeight(pixelsPerMinute).dp
    val columnWidth = 120.dp

    Column(
        modifier = modifier.width(columnWidth)
    ) {
        // Day header (fixed at top)
        DayHeader(
            date = dayTimeline.date,
            isToday = dayTimeline.isToday
        )

        // Timeline content area
        Box(
            modifier = Modifier
                .width(columnWidth)
                .height(totalHeight)
                .background(
                    if (dayTimeline.isToday) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
        ) {
            // Background grid
            TimelineBackgroundGrid(
                pixelsPerMinute = pixelsPerMinute,
                totalHeight = totalHeight
            )

            // Tasks positioned absolutely
            dayTimeline.tasks.forEach { task ->
                val yOffset = TimelineCalculator.timeToPixel(
                    task.startTime.toLocalTime(),
                    pixelsPerMinute
                )
                val height = TimelineCalculator.calculateTaskHeight(
                    task.startTime,
                    task.endTime,
                    pixelsPerMinute
                )

                TaskBar(
                    task = task,
                    modifier = Modifier
                        .offset(y = yOffset.dp)
                        .width(columnWidth - 8.dp) // 4dp padding on each side
                        .height(height.dp.coerceAtLeast(20.dp))
                        .padding(horizontal = 4.dp),
                    onClick = { onTaskClick(task) },
                    onLongPress = { onTaskLongPress(task) }
                )
            }
        }
    }
}

/**
 * Background grid lines for time slots
 * OPTIMIZED: Uses Canvas drawing instead of composables to reduce memory usage
 */
@Composable
private fun TimelineBackgroundGrid(
    pixelsPerMinute: Float,
    totalHeight: androidx.compose.ui.unit.Dp
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val hourHeightPx = 60 * pixelsPerMinute

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw hour lines
        for (hour in 0..23) {
            val y = hour * hourHeightPx
            val strokeWidth = if (hour % 6 == 0) 2f else 1f
            val alpha = if (hour % 6 == 0) 0.2f else 0.1f

            drawLine(
                color = outlineColor.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )

            // Draw 15-minute markers only if hour height is sufficient
            if (hourHeightPx > 40f) {
                val quarterHeight = hourHeightPx / 4f
                for (quarter in 1..3) {
                    val quarterY = y + (quarterHeight * quarter)
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
}

/**
 * Vertical task bar for timeline
 */
@Composable
fun TaskBar(
    task: com.example.questflow.domain.model.TimelineTask,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use existing TaskBalken component (will be updated separately)
    TaskBalken(
        task = task,
        pixelsPerMinute = 2f, // Will be passed from parent
        onLongPress = onLongPress,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Single hour row for background grid (used in LazyColumn).
 */
@Composable
fun HourBackgroundGrid(
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
 * Day header showing day name and date (public for reuse).
 */
@Composable
fun DayHeader(
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
