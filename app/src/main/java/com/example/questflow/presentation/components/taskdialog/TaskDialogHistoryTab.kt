package com.example.questflow.presentation.components.taskdialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.entity.TaskHistoryEntity
import com.example.questflow.domain.model.Task

/**
 * TAB 5: Verlauf
 * - Task History only
 */
@Composable
fun TaskDialogHistoryTab(
    taskHistory: List<TaskHistoryEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Task History Section
        if (taskHistory.isNotEmpty()) {
            item {
                com.example.questflow.presentation.components.TaskHistorySection(
                    taskHistory = taskHistory,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            item {
                Text(
                    "Noch keine Verlaufseinträge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
