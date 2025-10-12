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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.util.TimelineCalculator

/**
 * Vertical time axis column (left side of timeline).
 * Shows all 24 hours from 00:00 to 23:59 with hour labels and grid lines.
 *
 * This component is FIXED on the left and does NOT scroll horizontally,
 * but scrolls vertically synchronized with all day columns.
 */
@Composable
fun TimeAxisColumn(
    pixelsPerMinute: Float,
    modifier: Modifier = Modifier
) {
    val totalHeight = TimelineCalculator.calculateTimelineHeight(pixelsPerMinute).dp
    val hourHeight = (60 * pixelsPerMinute).dp

    Box(
        modifier = modifier
            .width(60.dp)
            .height(totalHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Grid lines (Canvas for performance)
        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        val hourHeightPx = 60 * pixelsPerMinute

        Canvas(modifier = Modifier.fillMaxSize()) {
            for (hour in 0..23) {
                val y = hour * hourHeightPx
                val strokeWidth = if (hour % 6 == 0) 4f else 2f
                val alpha = if (hour % 6 == 0) 0.3f else 0.2f
                val color = if (hour % 6 == 0) primaryColor else outlineColor

                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )

                // 15-minute markers
                if (hourHeightPx > 40f) {
                    val quarterHeight = hourHeightPx / 4f
                    for (quarter in 1..3) {
                        val quarterY = y + (quarterHeight * quarter)
                        drawLine(
                            color = outlineColor.copy(alpha = 0.1f),
                            start = Offset(0f, quarterY),
                            end = Offset(size.width, quarterY),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }

        // Hour labels (must be separate composables for text rendering)
        Column(modifier = Modifier.fillMaxSize()) {
            for (hour in 0..23) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight)
                ) {
                    Text(
                        text = String.format("%02d:00", hour),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (hour % 6 == 0) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (hour % 6 == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Single hour row for time axis (used in LazyColumn).
 */
@Composable
fun TimeAxisHourRow(
    hour: Int,
    hourHeightDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(hourHeightDp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Grid line at top
        val strokeWidth = if (hour % 6 == 0) 4f else 2f
        val alpha = if (hour % 6 == 0) 0.3f else 0.2f
        val color = if (hour % 6 == 0) primaryColor else outlineColor

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = strokeWidth
            )

            // 15-minute markers
            if (size.height > 120f) { // hourHeightPx > 40
                val quarterHeight = size.height / 4f
                for (quarter in 1..3) {
                    val quarterY = quarterHeight * quarter
                    drawLine(
                        color = outlineColor.copy(alpha = 0.1f),
                        start = Offset(0f, quarterY),
                        end = Offset(size.width, quarterY),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // Hour label
        Text(
            text = String.format("%02d:00", hour),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = if (hour % 6 == 0) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (hour % 6 == 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 2.dp)
        )
    }
}

/**
 * Preview/Info: Current time indicator (optional enhancement)
 */
@Composable
fun TimeAxisCurrentTimeIndicator(
    currentMinuteOfDay: Int,
    pixelsPerMinute: Float,
    modifier: Modifier = Modifier
) {
    val yOffset = (currentMinuteOfDay * pixelsPerMinute).dp

    Box(
        modifier = modifier
            .offset(y = yOffset)
            .height(2.dp)
            .fillMaxWidth()
            .background(Color.Red)
    )
}
