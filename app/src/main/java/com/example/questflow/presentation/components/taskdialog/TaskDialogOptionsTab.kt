package com.example.questflow.presentation.components.taskdialog

import androidx.compose.foundation.BorderStroke
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

/**
 * TAB 4: Optionen
 * - Kalender Integration
 * - Delete on Claim
 * - Delete on Expiry
 * - Reaktivierung (für claimed tasks)
 * - Task löschen
 */
@Composable
fun TaskDialogOptionsTab(
    hasCalendarPermission: Boolean,
    addToCalendar: Boolean,
    onAddToCalendarChange: (Boolean) -> Unit,
    deleteOnClaim: Boolean,
    onDeleteOnClaimChange: (Boolean) -> Unit,
    deleteOnExpiry: Boolean,
    onDeleteOnExpiryChange: (Boolean) -> Unit,
    isClaimedTask: Boolean,
    shouldReactivate: Boolean,
    onShouldReactivateChange: (Boolean) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Calendar Integration Options
        item {
            if (hasCalendarPermission) {
                Text(
                    "Kalender-Optionen",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = addToCalendar,
                        onCheckedChange = onAddToCalendarChange
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            "Google Kalender Integration",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Kalenderintegration verwenden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (addToCalendar) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteOnClaim,
                            onCheckedChange = onDeleteOnClaimChange
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                "Nach XP-Claim löschen",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Kalendereintrag wird nach XP-Erhalt entfernt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteOnExpiry,
                            onCheckedChange = onDeleteOnExpiryChange
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                "Nach Ablauf löschen",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Kalendereintrag wird automatisch entfernt wenn abgelaufen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "⚠️ Kalenderberechtigung erforderlich für Kalenderintegration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Reactivation option only for claimed tasks
        if (isClaimedTask) {
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reaktivieren",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "XP wurden bereits erhalten",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Aktiviere um XP erneut zu claimen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = shouldReactivate,
                            onCheckedChange = onShouldReactivateChange
                        )
                    }
                }
            }
        }

        // Task Options Section - Delete Task Button
        item {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
        }

        item {
            Text(
                "Task-Optionen",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            var showDeleteDialog by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Löschen",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Task löschen")
            }

            // Confirmation Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Task löschen?") },
                    text = {
                        Text("Möchtest du diesen Task wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDeleteTask()
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Löschen")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Abbrechen")
                        }
                    }
                )
            }
        }
    }
}
