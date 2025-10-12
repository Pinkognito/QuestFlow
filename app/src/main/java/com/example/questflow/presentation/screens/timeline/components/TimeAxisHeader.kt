package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.util.TimelineCalculator

/**
 * Time axis header showing hour labels.
 * Displays horizontal timeline with hour markers.
 */
@Composable
fun TimeAxisHeader(
    hourStart: Int,
    hourEnd: Int,
    pixelsPerMinute: Float,
    modifier: Modifier = Modifier
) {
    val timelineWidth = TimelineCalculator.calculateTimelineWidth(hourStart, hourEnd, pixelsPerMinute)

    Column(modifier = modifier) {
        // Hour labels
        Box(
            modifier = Modifier
                .width(timelineWidth.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Draw hour markers
            for (hour in hourStart until hourEnd) {
                val hourPixel = (hour - hourStart) * 60 * pixelsPerMinute

                Column(
                    modifier = Modifier
                        .offset(x = hourPixel.dp)
                        .width(120.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = String.format("%02d:00", hour),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    // Vertical line at hour
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(8.dp)
                            .offset(x = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Half-hour marker
                if (pixelsPerMinute >= 1.5f) { // Only show if zoomed in enough
                    val halfHourPixel = hourPixel + (30 * pixelsPerMinute)
                    Divider(
                        modifier = Modifier
                            .offset(x = halfHourPixel.dp, y = 28.dp)
                            .width(1.dp)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Bottom divider
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}

/**
 * Background grid for timeline.
 * Shows vertical lines at hour and half-hour marks.
 */
@Composable
fun TimelineBackgroundGrid(
    hourStart: Int,
    hourEnd: Int,
    pixelsPerMinute: Float,
    modifier: Modifier = Modifier
) {
    val timelineWidth = TimelineCalculator.calculateTimelineWidth(hourStart, hourEnd, pixelsPerMinute)

    Box(
        modifier = modifier
            .width(timelineWidth.dp)
            .fillMaxHeight()
    ) {
        // Hour lines
        for (hour in hourStart until hourEnd) {
            val hourPixel = (hour - hourStart) * 60 * pixelsPerMinute

            Divider(
                modifier = Modifier
                    .offset(x = hourPixel.dp)
                    .width(1.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Half-hour line
            if (pixelsPerMinute >= 1.5f) {
                val halfHourPixel = hourPixel + (30 * pixelsPerMinute)
                Divider(
                    modifier = Modifier
                        .offset(x = halfHourPixel.dp)
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
            }

            // Quarter-hour lines (only if very zoomed in)
            if (pixelsPerMinute >= 3f) {
                for (quarter in listOf(15, 45)) {
                    val quarterPixel = hourPixel + (quarter * pixelsPerMinute)
                    Divider(
                        modifier = Modifier
                            .offset(x = quarterPixel.dp)
                            .width(1.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    )
                }
            }
        }
    }
}
