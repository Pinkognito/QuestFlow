package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.Task
import com.example.questflow.domain.model.DisplayElementType
import com.example.questflow.domain.model.TaskSearchResult
import com.example.questflow.domain.model.getDisplayText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Renders a single display element based on its type
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDisplayElement(
    type: DisplayElementType,
    task: Task,
    matchedFilters: List<com.example.questflow.domain.model.SearchMatchInfo> = emptyList(),
    availableTasks: List<Task> = emptyList(),
    categoriesMap: Map<Long, com.example.questflow.data.database.entity.CategoryEntity> = emptyMap(),
    isExpired: Boolean = false,
    isClaimed: Boolean = false,
    isParentTask: Boolean = false,
    isSubtask: Boolean = false,
    parentTask: Task? = null,
    maxMatchBadges: Int = 3,
    maxContacts: Int = 3,
    showContactAvatars: Boolean = true,
    descriptionMaxLines: Int = 2,
    searchQuery: String = "",
    dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
    onClaimClick: (() -> Unit)? = null
) {
    when (type) {
        DisplayElementType.TITLE -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isParentTask) {
                    Text("📁", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isParentTask) FontWeight.Bold else FontWeight.Medium,
                    color = if (isExpired && !isClaimed)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        DisplayElementType.DESCRIPTION -> {
            if (task.description.isNotEmpty()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = descriptionMaxLines,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        DisplayElementType.PARENT_PATH -> {
            if (isSubtask && parentTask != null) {
                Text(
                    text = "${parentTask.title} /",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        DisplayElementType.DUE_DATE -> {
            task.dueDate?.let { dueDate ->
                Text(
                    text = dueDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        DisplayElementType.CREATED_DATE -> {
            Text(
                text = "Erstellt: ${task.createdAt.format(dateFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }

        DisplayElementType.COMPLETED_DATE -> {
            task.completedAt?.let { completedAt ->
                Text(
                    text = "Abgeschlossen: ${completedAt.format(dateFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }

        DisplayElementType.DIFFICULTY -> {
            val difficultyText = when (task.xpPercentage) {
                20 -> "Trivial"
                40 -> "Einfach"
                60 -> "Mittel"
                80 -> "Schwer"
                100 -> "Episch"
                else -> "Mittel"
            }
            Text(
                text = difficultyText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isExpired && !isClaimed)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        DisplayElementType.CATEGORY -> {
            task.categoryId?.let { categoryId ->
                val category = categoriesMap[categoryId]
                if (category != null) {
                    Text(
                        text = "${category.emoji} ${category.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        DisplayElementType.PRIORITY -> {
            Text(
                text = task.priority.name,
                style = MaterialTheme.typography.labelSmall,
                color = when (task.priority) {
                    com.example.questflow.domain.model.Priority.URGENT -> MaterialTheme.colorScheme.error
                    com.example.questflow.domain.model.Priority.HIGH -> MaterialTheme.colorScheme.tertiary
                    com.example.questflow.domain.model.Priority.MEDIUM -> MaterialTheme.colorScheme.primary
                    com.example.questflow.domain.model.Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Medium
            )
        }

        DisplayElementType.XP_REWARD -> {
            Text(
                text = "${task.xpReward} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        DisplayElementType.EXPIRED_BADGE -> {
            if (isExpired && !isClaimed) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(
                        "Abgelaufen",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        DisplayElementType.COMPLETED_BADGE -> {
            if (isClaimed) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        "Erledigt",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        DisplayElementType.SUBTASK_COUNT -> {
            if (isParentTask) {
                val subtaskCount = availableTasks.count { it.parentTaskId == task.id }
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        "$subtaskCount",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        DisplayElementType.RECURRING_ICON -> {
            if (task.isRecurring) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Wiederkehrend",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DisplayElementType.LINKED_CONTACTS -> {
            // TODO: Implement when contacts are loaded
            // For now, just a placeholder
        }

        DisplayElementType.MATCH_BADGES -> {
            if (searchQuery.isNotEmpty() && matchedFilters.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    matchedFilters.take(maxMatchBadges).forEach { matchInfo ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = {
                                Text(
                                    text = matchInfo.getDisplayText(),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = null
                        )
                    }
                    if (matchedFilters.size > maxMatchBadges) {
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = {
                                Text(
                                    text = "+${matchedFilters.size - maxMatchBadges} mehr",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        DisplayElementType.CLAIM_BUTTON -> {
            onClaimClick?.let { onClick ->
                Button(
                    onClick = onClick,
                    colors = if (isExpired) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Text(
                        if (isExpired) "Claim" else "Claim",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
