package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState

/**
 * Main timeline grid component.
 * Contains time axis header and scrollable list of day rows.
 */
@Composable
fun TimelineGrid(
    uiState: TimelineUiState,
    modifier: Modifier = Modifier,
    onTaskClick: (com.example.questflow.domain.model.TimelineTask) -> Unit,
    onTaskLongPress: (com.example.questflow.domain.model.TimelineTask) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Time axis header (fixed at top)
        Row(modifier = Modifier.fillMaxWidth()) {
            // Empty space for day labels
            Spacer(modifier = Modifier.width(80.dp))

            // Time axis
            TimeAxisHeader(
                hourStart = uiState.hourRangeStart,
                hourEnd = uiState.hourRangeEnd,
                pixelsPerMinute = uiState.pixelsPerMinute
            )
        }

        // Scrollable day rows
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                items = uiState.days,
                key = { day -> day.date.toString() }
            ) { day ->
                Column {
                    DayTimelineRow(
                        day = day,
                        uiState = uiState,
                        onTaskClick = onTaskClick,
                        onTaskLongPress = onTaskLongPress
                    )

                    // Divider between days
                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}
