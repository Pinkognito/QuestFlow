package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.*
import java.time.format.DateTimeFormatter

/**
 * Task Card with configurable 2-column layout
 * Left column: 2/3 width (main content)
 * Right column: 1/3 width (metadata/actions)
 */
@Composable
fun TaskCardV2(
    task: Task,
    layoutConfig: List<TaskDisplayElementConfig>,
    matchedFilters: List<SearchMatchInfo> = emptyList(),
    availableTasks: List<Task> = emptyList(),
    categoriesMap: Map<Long, com.example.questflow.data.database.entity.CategoryEntity> = emptyMap(),
    isExpired: Boolean = false,
    isClaimed: Boolean = false,
    isSelected: Boolean = false,
    searchQuery: String = "",
    dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
    onClaimClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Calculate task states
    val isParentTask = availableTasks.any { it.parentTaskId == task.id }
    val isSubtask = task.parentTaskId != null
    val parentTask = if (isSubtask) {
        availableTasks.find { it.id == task.parentTaskId }
    } else null

    // Get elements for each column
    val leftElements = TaskDisplayLayoutHelper.getElementsForColumn(layoutConfig, DisplayColumn.LEFT)
    val rightElements = TaskDisplayLayoutHelper.getElementsForColumn(layoutConfig, DisplayColumn.RIGHT)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isClaimed -> MaterialTheme.colorScheme.surfaceVariant
                isExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column (2/3 width - main content)
            Column(
                modifier = Modifier.weight(2f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                leftElements.forEach { elementConfig ->
                    TaskDisplayElement(
                        type = elementConfig.type,
                        task = task,
                        matchedFilters = matchedFilters,
                        availableTasks = availableTasks,
                        categoriesMap = categoriesMap,
                        isExpired = isExpired,
                        isClaimed = isClaimed,
                        isParentTask = isParentTask,
                        isSubtask = isSubtask,
                        parentTask = parentTask,
                        searchQuery = searchQuery,
                        dateFormatter = dateFormatter,
                        onClaimClick = if (elementConfig.type == DisplayElementType.CLAIM_BUTTON) onClaimClick else null
                    )
                }
            }

            // Right Column (1/3 width - metadata/actions)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                rightElements.forEach { elementConfig ->
                    TaskDisplayElement(
                        type = elementConfig.type,
                        task = task,
                        matchedFilters = matchedFilters,
                        availableTasks = availableTasks,
                        categoriesMap = categoriesMap,
                        isExpired = isExpired,
                        isClaimed = isClaimed,
                        isParentTask = isParentTask,
                        isSubtask = isSubtask,
                        parentTask = parentTask,
                        searchQuery = searchQuery,
                        dateFormatter = dateFormatter,
                        onClaimClick = if (elementConfig.type == DisplayElementType.CLAIM_BUTTON) onClaimClick else null
                    )
                }
            }
        }
    }
}
