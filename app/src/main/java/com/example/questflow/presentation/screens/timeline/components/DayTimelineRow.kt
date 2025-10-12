package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState

/**
 * Timeline row for a single day.
 * Shows day label and horizontal scrollable timeline with task bars.
 */
@Composable
fun DayTimelineRow(
    day: DayTimeline,
    uiState: TimelineUiState,
    modifier: Modifier = Modifier,
    onTaskClick: (com.example.questflow.domain.model.TimelineTask) -> Unit,
    onTaskLongPress: (com.example.questflow.domain.model.TimelineTask) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day label (fixed width)
        DayLabel(
            day = day,
            modifier = Modifier.width(80.dp)
        )

        // Scrollable timeline
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            // Background grid
            TimelineBackgroundGrid(
                hourStart = uiState.hourRangeStart,
                hourEnd = uiState.hourRangeEnd,
                pixelsPerMinute = uiState.pixelsPerMinute
            )

            // Task bars
            Box(modifier = Modifier.fillMaxSize()) {
                day.tasks.forEach { task ->
                    val isDraggingThisTask = uiState.dragState?.task?.id == task.id

                    // Show preview if dragging this task
                    if (isDraggingThisTask && uiState.dragState != null) {
                        TaskBalkenPreview(
                            task = task,
                            dayStart = uiState.hourRangeStart,
                            pixelsPerMinute = uiState.pixelsPerMinute,
                            previewStartTime = uiState.dragState.previewStartTime
                        )
                    }

                    // Show actual task bar
                    TaskBalken(
                        task = task,
                        dayStart = uiState.hourRangeStart,
                        pixelsPerMinute = uiState.pixelsPerMinute,
                        isDragging = isDraggingThisTask,
                        onLongPress = { onTaskLongPress(task) },
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

/**
 * Day label showing date and conflict indicator.
 */
@Composable
fun DayLabel(
    day: DayTimeline,
    modifier: Modifier = Modifier
) {
    val conflictCounts = day.countConflicts()

    Card(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (day.isToday)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Day label
            Text(
                text = day.getDisplayLabel(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isToday)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                lineHeight = 12.sp
            )

            // Conflict indicators
            if (conflictCounts.hasConflicts) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (conflictCounts.overlaps > 0) {
                        ConflictBadge(
                            count = conflictCounts.overlaps,
                            color = androidx.compose.ui.graphics.Color(0xFFFF5252) // Red
                        )
                    }
                    if (conflictCounts.warnings > 0) {
                        ConflictBadge(
                            count = conflictCounts.warnings,
                            color = androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small badge showing conflict count.
 */
@Composable
fun ConflictBadge(
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(16.dp)
            .background(color, shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 8.sp,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
