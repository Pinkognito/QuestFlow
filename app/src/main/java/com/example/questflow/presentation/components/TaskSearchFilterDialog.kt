package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.questflow.data.database.entity.TaskSearchFilterSettingsEntity

/**
 * Dialog for configuring task search filters
 * Shows hierarchical checkboxes for selecting which metadata to include in search
 * Auto-saves all changes immediately
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSearchFilterDialog(
    currentSettings: TaskSearchFilterSettingsEntity,
    onDismiss: () -> Unit,
    onSettingsChange: (TaskSearchFilterSettingsEntity) -> Unit,
    onResetToDefaults: () -> Unit
) {
    // Use currentSettings directly - no local state needed for auto-save
    val settings = currentSettings

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                TopAppBar(
                    title = {
                        Column {
                            Text("Such-Filter Einstellungen")
                            Text(
                                "Änderungen werden automatisch gespeichert",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Help text
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        "Wähle aus, welche Felder in die Suche einbezogen werden sollen. " +
                        "Tasks werden gefunden, wenn der Suchtext in einem der aktivierten Felder vorkommt.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Filter Settings List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Task Direct Fields Section
                    item {
                        CollapsibleSectionHeader(
                            title = "Task Felder",
                            isExpanded = settings.taskFieldsSectionExpanded,
                            onToggle = { onSettingsChange(settings.copy(taskFieldsSectionExpanded = !settings.taskFieldsSectionExpanded)) }
                        )
                    }

                    if (settings.taskFieldsSectionExpanded) {
                        item {
                            FilterCheckboxItem(
                                label = "Titel",
                                checked = settings.taskTitle,
                                onCheckedChange = { onSettingsChange(settings.copy(taskTitle = it)) },
                                indent = 0
                            )
                        }

                        item {
                            FilterCheckboxItem(
                                label = "Beschreibung",
                                checked = settings.taskDescription,
                                onCheckedChange = { onSettingsChange(settings.copy(taskDescription = it)) },
                                indent = 0
                            )
                        }

                        item {
                            FilterCheckboxItem(
                                label = "Tags",
                                checked = settings.taskTags,
                                onCheckedChange = { onSettingsChange(settings.copy(taskTags = it)) },
                                indent = 0
                            )
                        }

                        item {
                            FilterCheckboxItem(
                                label = "Kategorie Name",
                                checked = settings.categoryName,
                                onCheckedChange = { onSettingsChange(settings.copy(categoryName = it)) },
                                indent = 0
                            )
                        }
                    }

                    // Contacts Section
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        CollapsibleSectionHeader(
                            title = "Verknüpfte Kontakte",
                            isExpanded = settings.contactsSectionExpanded,
                            onToggle = { onSettingsChange(settings.copy(contactsSectionExpanded = !settings.contactsSectionExpanded)) }
                        )
                    }

                    if (settings.contactsSectionExpanded) {
                        item {
                            FilterCheckboxItem(
                                label = "Hat Kontakte (Aktiviert Kontakt-Suche)",
                                checked = settings.contactEnabled,
                                onCheckedChange = { onSettingsChange(settings.copy(contactEnabled = it)) },
                                indent = 0,
                                isParent = true
                            )
                        }

                        if (settings.contactEnabled) {
                            item {
                                FilterCheckboxItem(
                                    label = "Anzeigename",
                                    checked = settings.contactDisplayName,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactDisplayName = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Vorname",
                                    checked = settings.contactGivenName,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactGivenName = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Nachname",
                                    checked = settings.contactFamilyName,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactFamilyName = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Telefonnummer",
                                    checked = settings.contactPrimaryPhone,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactPrimaryPhone = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "E-Mail",
                                    checked = settings.contactPrimaryEmail,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactPrimaryEmail = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Organisation",
                                    checked = settings.contactOrganization,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactOrganization = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Job-Titel",
                                    checked = settings.contactJobTitle,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactJobTitle = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Notiz",
                                    checked = settings.contactNote,
                                    onCheckedChange = { onSettingsChange(settings.copy(contactNote = it)) },
                                    indent = 1
                                )
                            }
                        }
                    }

                    // Parent Task Section
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        CollapsibleSectionHeader(
                            title = "Übergeordnete Aufgabe",
                            isExpanded = settings.parentTaskSectionExpanded,
                            onToggle = { onSettingsChange(settings.copy(parentTaskSectionExpanded = !settings.parentTaskSectionExpanded)) }
                        )
                    }

                    if (settings.parentTaskSectionExpanded) {
                        item {
                            FilterCheckboxItem(
                                label = "Hat Parent Task (Aktiviert Parent-Suche)",
                                checked = settings.parentTaskEnabled,
                                onCheckedChange = { onSettingsChange(settings.copy(parentTaskEnabled = it)) },
                                indent = 0,
                                isParent = true
                            )
                        }

                        if (settings.parentTaskEnabled) {
                            item {
                                FilterCheckboxItem(
                                    label = "Titel",
                                    checked = settings.parentTaskTitle,
                                    onCheckedChange = { onSettingsChange(settings.copy(parentTaskTitle = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Beschreibung",
                                    checked = settings.parentTaskDescription,
                                    onCheckedChange = { onSettingsChange(settings.copy(parentTaskDescription = it)) },
                                    indent = 1
                                )
                            }
                        }
                    }

                    // XP Percentage (Difficulty) Section
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        CollapsibleSectionHeader(
                            title = "Schwierigkeitsgrad (XP %)",
                            isExpanded = settings.xpPercentageSectionExpanded,
                            onToggle = { onSettingsChange(settings.copy(xpPercentageSectionExpanded = !settings.xpPercentageSectionExpanded)) }
                        )
                    }

                    if (settings.xpPercentageSectionExpanded) {
                        item {
                            FilterCheckboxItem(
                                label = "Filter nach Schwierigkeitsgrad aktivieren",
                                checked = settings.xpPercentageEnabled,
                                onCheckedChange = { onSettingsChange(settings.copy(xpPercentageEnabled = it)) },
                                indent = 0,
                                isParent = true
                            )
                        }

                        if (settings.xpPercentageEnabled) {
                            item {
                                FilterCheckboxItem(
                                    label = "Trivial (20%)",
                                    checked = settings.xpPercentage20,
                                    onCheckedChange = { onSettingsChange(settings.copy(xpPercentage20 = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Einfach (40%)",
                                    checked = settings.xpPercentage40,
                                    onCheckedChange = { onSettingsChange(settings.copy(xpPercentage40 = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Mittel (60%)",
                                    checked = settings.xpPercentage60,
                                    onCheckedChange = { onSettingsChange(settings.copy(xpPercentage60 = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Schwer (80%)",
                                    checked = settings.xpPercentage80,
                                    onCheckedChange = { onSettingsChange(settings.copy(xpPercentage80 = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Episch (100%)",
                                    checked = settings.xpPercentage100,
                                    onCheckedChange = { onSettingsChange(settings.copy(xpPercentage100 = it)) },
                                    indent = 1
                                )
                            }
                        }
                    }

                    // Time-based Filters Section
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        CollapsibleSectionHeader(
                            title = "Zeit-basierte Filter",
                            isExpanded = settings.timeFilterSectionExpanded,
                            onToggle = { onSettingsChange(settings.copy(timeFilterSectionExpanded = !settings.timeFilterSectionExpanded)) }
                        )
                    }

                    if (settings.timeFilterSectionExpanded) {
                        item {
                            FilterCheckboxItem(
                                label = "Zeitfilter aktivieren",
                                checked = settings.timeFilterEnabled,
                                onCheckedChange = { onSettingsChange(settings.copy(timeFilterEnabled = it)) },
                                indent = 0,
                                isParent = true
                            )
                        }

                        if (settings.timeFilterEnabled) {
                            item {
                                FilterCheckboxItem(
                                    label = "Startzeit durchsuchen",
                                    checked = settings.filterByStartTime,
                                    onCheckedChange = { onSettingsChange(settings.copy(filterByStartTime = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Endzeit durchsuchen",
                                    checked = settings.filterByEndTime,
                                    onCheckedChange = { onSettingsChange(settings.copy(filterByEndTime = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Fälligkeitsdatum durchsuchen",
                                    checked = settings.filterByDueDate,
                                    onCheckedChange = { onSettingsChange(settings.copy(filterByDueDate = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Erstellungsdatum durchsuchen",
                                    checked = settings.filterByCreatedDate,
                                    onCheckedChange = { onSettingsChange(settings.copy(filterByCreatedDate = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Abschlussdatum durchsuchen",
                                    checked = settings.filterByCompletedDate,
                                    onCheckedChange = { onSettingsChange(settings.copy(filterByCompletedDate = it)) },
                                    indent = 1
                                )
                            }
                        }
                    }

                    // Date Range Filters Section
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        CollapsibleSectionHeader(
                            title = "Zeitbereich-Filter",
                            isExpanded = settings.dateRangeSectionExpanded,
                            onToggle = { onSettingsChange(settings.copy(dateRangeSectionExpanded = !settings.dateRangeSectionExpanded)) }
                        )
                    }

                    if (settings.dateRangeSectionExpanded) {
                        item {
                            FilterCheckboxItem(
                                label = "Zeitbereich-Filter aktivieren",
                                checked = settings.dateRangeEnabled,
                                onCheckedChange = { onSettingsChange(settings.copy(dateRangeEnabled = it)) },
                                indent = 0,
                                isParent = true
                            )
                        }

                        if (settings.dateRangeEnabled) {
                            item {
                                FilterCheckboxItem(
                                    label = "Vergangene Tasks einbeziehen",
                                    checked = settings.includePastTasks,
                                    onCheckedChange = { onSettingsChange(settings.copy(includePastTasks = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Zukünftige Tasks einbeziehen",
                                    checked = settings.includeFutureTasks,
                                    onCheckedChange = { onSettingsChange(settings.copy(includeFutureTasks = it)) },
                                    indent = 1
                                )
                            }

                            item {
                                FilterCheckboxItem(
                                    label = "Überfällige Tasks einbeziehen",
                                    checked = settings.includeOverdueTasks,
                                    onCheckedChange = { onSettingsChange(settings.copy(includeOverdueTasks = it)) },
                                    indent = 1
                                )
                            }
                        }
                    }

                    // Spacer at the end
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FilterCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indent: Int = 0,
    isParent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 24).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = if (isParent) {
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            modifier = Modifier.weight(1f)
        )
    }
}
