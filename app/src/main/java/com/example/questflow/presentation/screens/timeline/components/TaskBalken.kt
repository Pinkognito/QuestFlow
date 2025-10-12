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

/**
 * Task bar component for vertical timeline view.
 * Displays task with color-coded conflict state.
 *
 * UPDATED: Position and size are now controlled by parent (DayTimelineColumn).
 * This component only handles the visual appearance and gestures.
 */
@Composable
fun TaskBalken(
    task: TimelineTask,
    pixelsPerMinute: Float,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false
) {
    // Get color based on conflict state
    val backgroundColor = remember(task.conflictState, task.categoryColor) {
        try {
            Color(android.graphics.Color.parseColor(task.getDisplayColor()))
        } catch (e: Exception) {
            Color(0xFF4CAF50) // Default green
        }
    }

    val contentColor = Color.White
    val durationMinutes = task.durationMinutes()

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDragging) 8.dp else if (task.isExternal) 1.dp else 2.dp,
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                color = if (task.isExternal) {
                    backgroundColor.copy(alpha = 0.6f) // More transparent for external events
                } else {
                    backgroundColor.copy(alpha = if (isDragging) 0.9f else 1f)
                },
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(task.id) {
                if (!task.isExternal) { // External events are not clickable
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongPress() }
                    )
                }
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.TopStart
    ) {
        // Task content
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Title row with emoji
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category emoji
                if (task.categoryEmoji != null) {
                    Text(
                        text = task.categoryEmoji,
                        fontSize = 14.sp,
                        color = contentColor
                    )
                }

                // Task title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Time range (if height allows)
            if (durationMinutes > 30) {
                val startTime = task.startTime.toLocalTime()
                val endTime = task.endTime.toLocalTime()
                Text(
                    text = String.format("%02d:%02d - %02d:%02d",
                        startTime.hour, startTime.minute,
                        endTime.hour, endTime.minute
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 9.sp
                )
            }

            // Duration (if height allows)
            if (durationMinutes > 60) {
                val durationText = when {
                    durationMinutes < 60 -> "${durationMinutes}min"
                    durationMinutes % 60 == 0L -> "${durationMinutes / 60}h"
                    else -> "${durationMinutes / 60}h ${durationMinutes % 60}min"
                }

                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }

        // Completion indicator
        if (task.isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    )
            )

            Text(
                text = "✓",
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                color = contentColor,
                fontSize = 12.sp
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
    modifier: Modifier = Modifier
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(task.getDisplayColor()))
    } catch (e: Exception) {
        Color(0xFF4CAF50)
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(6.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.categoryEmoji != null) {
                Text(
                    text = task.categoryEmoji,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp
            )
        }
    }
}
