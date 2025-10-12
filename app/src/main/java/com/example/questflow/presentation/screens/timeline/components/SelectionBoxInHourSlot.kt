package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.questflow.presentation.screens.timeline.model.SelectionBox
import java.time.LocalDate

/**
 * Renders the selection box overlay within a single hour slot.
 * Similar to how tasks are rendered - only the portion that overlaps with this hour.
 */
@Composable
fun RenderSelectionBoxInHourSlot(
    selectionBox: SelectionBox,
    dayDate: LocalDate,
    hour: Int,
    pixelsPerMinute: Float,
    hourHeightDp: Dp,
    modifier: Modifier = Modifier
) {
    val boxStartDate = selectionBox.startTime.toLocalDate()
    val boxEndDate = selectionBox.endTime.toLocalDate()

    // Only render if this day is within the selection box range
    if (dayDate.isBefore(boxStartDate) || dayDate.isAfter(boxEndDate)) {
        return
    }

    // Calculate which portion of the day should be marked
    val boxStartMinutes: Int
    val boxEndMinutes: Int

    when {
        // Case 1: Single-day selection (start and end on same day)
        boxStartDate == boxEndDate && dayDate == boxStartDate -> {
            boxStartMinutes = selectionBox.startTime.toLocalTime().hour * 60 + selectionBox.startTime.toLocalTime().minute
            boxEndMinutes = selectionBox.endTime.toLocalTime().hour * 60 + selectionBox.endTime.toLocalTime().minute
        }

        // Case 2: Multi-day selection - this is the START day
        dayDate == boxStartDate && dayDate != boxEndDate -> {
            // From start time until end of day (23:59)
            boxStartMinutes = selectionBox.startTime.toLocalTime().hour * 60 + selectionBox.startTime.toLocalTime().minute
            boxEndMinutes = 23 * 60 + 59
        }

        // Case 3: Multi-day selection - this is the END day
        dayDate == boxEndDate && dayDate != boxStartDate -> {
            // From start of day (00:00) until end time
            boxStartMinutes = 0
            boxEndMinutes = selectionBox.endTime.toLocalTime().hour * 60 + selectionBox.endTime.toLocalTime().minute
        }

        // Case 4: Multi-day selection - this is a MIDDLE day
        dayDate.isAfter(boxStartDate) && dayDate.isBefore(boxEndDate) -> {
            // Entire day marked (00:00 - 23:59)
            boxStartMinutes = 0
            boxEndMinutes = 23 * 60 + 59
        }

        else -> {
            // Should not happen, but just in case
            return
        }
    }

    // This hour's time range in minutes
    val hourStart = hour * 60
    val hourEnd = (hour + 1) * 60

    // Check if selection box overlaps with this hour
    if (boxEndMinutes <= hourStart || boxStartMinutes >= hourEnd) {
        return // No overlap
    }

    // Calculate visible portion in this hour
    val visibleStartInHour = if (boxStartMinutes >= hourStart) {
        // Box starts within this hour
        boxStartMinutes - hourStart
    } else {
        // Box started before this hour
        0
    }

    val visibleEndInHour = if (boxEndMinutes <= hourEnd) {
        // Box ends within this hour
        boxEndMinutes - hourStart
    } else {
        // Box continues after this hour
        60
    }

    // Calculate pixel positions within this hour slot
    val yOffsetDp = visibleStartInHour * pixelsPerMinute
    val heightDp = (visibleEndInHour - visibleStartInHour) * pixelsPerMinute

    val primaryColor = MaterialTheme.colorScheme.primary
    val overlayColor = primaryColor.copy(alpha = 0.15f)
    val borderColor = primaryColor.copy(alpha = 0.6f)

    // Render overlay
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .offset(y = yOffsetDp.dp)
            .fillMaxWidth()
            .height(heightDp.dp.coerceAtLeast(1.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw semi-transparent background
            drawRect(
                color = overlayColor,
                size = size
            )

            // Draw dashed border
            val dashPathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(10f, 10f),
                phase = 0f
            )

            // Only draw top border if box starts in this hour
            if (boxStartMinutes >= hourStart && boxStartMinutes < hourEnd) {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(width, 0f),
                    strokeWidth = 3f,
                    pathEffect = dashPathEffect
                )
            }

            // Only draw bottom border if box ends in this hour
            if (boxEndMinutes > hourStart && boxEndMinutes <= hourEnd) {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 3f,
                    pathEffect = dashPathEffect
                )
            }

            // Always draw left border
            drawLine(
                color = borderColor,
                start = Offset(0f, 0f),
                end = Offset(0f, height),
                strokeWidth = 2f,
                pathEffect = dashPathEffect
            )

            // Always draw right border
            drawLine(
                color = borderColor,
                start = Offset(width, 0f),
                end = Offset(width, height),
                strokeWidth = 2f,
                pathEffect = dashPathEffect
            )
        }
    }
}
