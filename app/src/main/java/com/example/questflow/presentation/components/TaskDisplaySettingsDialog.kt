package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.TaskDisplaySettingsEntity

/**
 * Dialog for customizing which fields are displayed in the task list
 * Provides user control over task card layout and visible information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDisplaySettingsDialog(
    currentSettings: TaskDisplaySettingsEntity,
    onDismiss: () -> Unit,
    onSettingsChange: (TaskDisplaySettingsEntity) -> Unit,
    onResetToDefaults: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Anzeige-Einstellungen")
                IconButton(onClick = {
                    onResetToDefaults()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Zurücksetzen")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Änderungen werden automatisch gespeichert",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Basic Information Section
                SectionHeader("Grundinformationen")

                DisplaySettingSwitch(
                    label = "Beschreibung anzeigen",
                    checked = currentSettings.showTaskDescription,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showTaskDescription = it))
                    }
                )

                if (currentSettings.showTaskDescription) {
                    DisplaySettingSwitch(
                        label = "Beschreibungs-Vorschau",
                        checked = currentSettings.showDescriptionPreview,
                        onCheckedChange = {
                            onSettingsChange(currentSettings.copy(showDescriptionPreview = it))
                        },
                        indent = 1
                    )
                }

                DisplaySettingSwitch(
                    label = "Übergeordnete Task anzeigen",
                    checked = currentSettings.showParentTaskPath,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showParentTaskPath = it))
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Date & Time Section
                SectionHeader("Datum & Zeit")

                DisplaySettingSwitch(
                    label = "Fälligkeitsdatum",
                    checked = currentSettings.showDueDate,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showDueDate = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "Erstellungsdatum",
                    checked = currentSettings.showCreatedDate,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showCreatedDate = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "Abschlussdatum",
                    checked = currentSettings.showCompletedDate,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showCompletedDate = it))
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Metadata Section
                SectionHeader("Metadaten")

                DisplaySettingSwitch(
                    label = "Kategorie",
                    checked = currentSettings.showCategory,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showCategory = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "Priorität",
                    checked = currentSettings.showPriority,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showPriority = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "Schwierigkeitsgrad",
                    checked = currentSettings.showDifficulty,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showDifficulty = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "XP Belohnung",
                    checked = currentSettings.showXpReward,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showXpReward = it))
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Status Indicators Section
                SectionHeader("Status-Anzeigen")

                DisplaySettingSwitch(
                    label = "Abgelaufen Badge",
                    checked = currentSettings.showExpiredBadge,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showExpiredBadge = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "Erledigt Badge",
                    checked = currentSettings.showCompletedBadge,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showCompletedBadge = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "Subtask-Anzahl",
                    checked = currentSettings.showSubtaskCount,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showSubtaskCount = it))
                    }
                )

                DisplaySettingSwitch(
                    label = "Wiederholend Icon",
                    checked = currentSettings.showRecurringIcon,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showRecurringIcon = it))
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Contacts Section
                SectionHeader("Kontakte")

                DisplaySettingSwitch(
                    label = "Verknüpfte Kontakte",
                    checked = currentSettings.showLinkedContacts,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showLinkedContacts = it))
                    }
                )

                if (currentSettings.showLinkedContacts) {
                    DisplaySettingSwitch(
                        label = "Kontakt-Avatare",
                        checked = currentSettings.showContactAvatars,
                        onCheckedChange = {
                            onSettingsChange(currentSettings.copy(showContactAvatars = it))
                        },
                        indent = 1
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Search Match Badges Section
                SectionHeader("Such-Filter Matches")

                DisplaySettingSwitch(
                    label = "Match Badges anzeigen",
                    checked = currentSettings.showMatchBadges,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(showMatchBadges = it))
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Layout Options Section
                SectionHeader("Layout-Optionen")

                DisplaySettingSwitch(
                    label = "Kompakt-Modus",
                    checked = currentSettings.compactMode,
                    onCheckedChange = {
                        onSettingsChange(currentSettings.copy(compactMode = it))
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
        dismissButton = null
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DisplaySettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indent: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 16).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
