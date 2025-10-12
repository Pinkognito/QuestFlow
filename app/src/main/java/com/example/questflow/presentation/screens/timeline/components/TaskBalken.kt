package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.presentation.screens.timeline.util.TimelineCalculator

/**
 * Draggable task bar component for timeline view.
 * Displays task with color-coded conflict state.
 */
@Composable
fun TaskBalken(
    task: TimelineTask,
    modifier: Modifier = Modifier,
    dayStart: Int,
    pixelsPerMinute: Float,
    isDragging: Boolean = false,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    // Calculate position and size
    val startPixel = TimelineCalculator.dateTimeToPixel(task.startTime, dayStart, pixelsPerMinute)
    val width = TimelineCalculator.calculateTaskWidth(task.startTime, task.endTime, pixelsPerMinute)

    // Get color based on conflict state
    val backgroundColor = remember(task.conflictState, task.categoryColor) {
        try {
            Color(android.graphics.Color.parseColor(task.getDisplayColor()))
        } catch (e: Exception) {
            Color(0xFF4CAF50) // Default green
        }
    }

    val contentColor = Color.White

    Box(
        modifier = modifier
            .offset(x = startPixel.dp, y = 0.dp)
            .width(maxOf(width.dp, 60.dp)) // Minimum width
            .height(48.dp)
            .shadow(
                elevation = if (isDragging) 8.dp else 2.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = backgroundColor.copy(alpha = if (isDragging) 0.9f else 1f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(task.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category emoji
            if (task.categoryEmoji != null) {
                Text(
                    text = task.categoryEmoji,
                    fontSize = 16.sp,
                    color = contentColor
                )
            }

            // Task title
            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )

                // Duration (if space allows)
                if (width > 100) {
                    val duration = task.durationMinutes()
                    val durationText = when {
                        duration < 60 -> "${duration}m"
                        duration % 60 == 0L -> "${duration / 60}h"
                        else -> "${duration / 60}h ${duration % 60}m"
                    }

                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Completion indicator
        if (task.isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Text(
                text = "✓",
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                color = contentColor,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Preview task balken during drag operation.
 * Shows semi-transparent preview at new position.
 */
@Composable
fun TaskBalkenPreview(
    task: TimelineTask,
    modifier: Modifier = Modifier,
    dayStart: Int,
    pixelsPerMinute: Float,
    previewStartTime: java.time.LocalDateTime
) {
    val startPixel = TimelineCalculator.dateTimeToPixel(previewStartTime, dayStart, pixelsPerMinute)
    val width = TimelineCalculator.calculateTaskWidth(task.startTime, task.endTime, pixelsPerMinute)

    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(task.getDisplayColor()))
    } catch (e: Exception) {
        Color(0xFF4CAF50)
    }

    Box(
        modifier = modifier
            .offset(x = startPixel.dp, y = 0.dp)
            .width(maxOf(width.dp, 60.dp))
            .height(48.dp)
            .background(
                color = backgroundColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp
        )
    }
}
