package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.ActionHistoryEntity
import java.time.format.DateTimeFormatter

/**
 * Displays action history for a task
 * Shows all communications with contacts (WhatsApp, SMS, Calls, etc.)
 */
@Composable
fun ActionHistoryCard(
    history: List<ActionHistoryEntity>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, "Historie")
                    Text(
                        "Aktions-Historie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Weniger" else "${history.size} Aktion${if (history.size != 1) "en" else ""}")
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        "Noch keine Aktionen durchgeführt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        history.take(10).forEach { action ->
                            ActionHistoryItem(action)
                        }
                        if (history.size > 10) {
                            Text(
                                "Und ${history.size - 10} weitere...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionHistoryItem(action: ActionHistoryEntity) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (action.success)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        getActionIcon(action.actionType),
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (action.success)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Text(
                        getActionDisplayName(action.actionType),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    action.executedAt.format(DateTimeFormatter.ofPattern("dd.MM. HH:mm")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                action.targetContactNames,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (expanded) {
                action.templateUsed?.let {
                    Text(
                        "Template: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                action.message?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (!action.success) {
                    action.errorMessage?.let {
                        Text(
                            "Fehler: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getActionIcon(actionType: String) = when (actionType) {
    "WHATSAPP" -> Icons.Default.Send
    "SMS" -> Icons.Default.Send
    "EMAIL" -> Icons.Default.Email
    "CALL" -> Icons.Default.Call
    "MEETING" -> Icons.Default.DateRange
    else -> Icons.Default.Info
}

private fun getActionDisplayName(actionType: String) = when (actionType) {
    "WHATSAPP" -> "WhatsApp"
    "SMS" -> "SMS"
    "EMAIL" -> "Email"
    "CALL" -> "Anruf"
    "MEETING" -> "Meeting"
    else -> actionType
}
