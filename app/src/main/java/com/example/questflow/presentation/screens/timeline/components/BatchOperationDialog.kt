package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.presentation.screens.timeline.TaskSortOption

/**
 * Dialog for batch task insertion with sort/filter options.
 * Default option is CUSTOM_ORDER (manual order from selection list).
 */
@Composable
fun BatchOperationDialog(
    taskCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (TaskSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf(TaskSortOption.CUSTOM_ORDER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tasks einfügen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$taskCount Tasks in Zeitbereich einfügen",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sortierung wählen:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Sort options
                SortOptionItem(
                    option = TaskSortOption.CUSTOM_ORDER,
                    label = "Manuelle Reihenfolge (Standard)",
                    description = "Nutzt die Reihenfolge aus der Auswahlliste",
                    selected = selectedOption == TaskSortOption.CUSTOM_ORDER,
                    onSelect = { selectedOption = TaskSortOption.CUSTOM_ORDER }
                )

                SortOptionItem(
                    option = TaskSortOption.PRIORITY,
                    label = "Nach Priorität",
                    description = "HOCH → NIEDRIG",
                    selected = selectedOption == TaskSortOption.PRIORITY,
                    onSelect = { selectedOption = TaskSortOption.PRIORITY }
                )

                SortOptionItem(
                    option = TaskSortOption.XP_PERCENTAGE,
                    label = "Nach Schwierigkeit (XP%)",
                    description = "Episch → Trivial",
                    selected = selectedOption == TaskSortOption.XP_PERCENTAGE,
                    onSelect = { selectedOption = TaskSortOption.XP_PERCENTAGE }
                )

                SortOptionItem(
                    option = TaskSortOption.DURATION,
                    label = "Nach Dauer (aufsteigend)",
                    description = "Kurz → Lang",
                    selected = selectedOption == TaskSortOption.DURATION,
                    onSelect = { selectedOption = TaskSortOption.DURATION }
                )

                SortOptionItem(
                    option = TaskSortOption.DURATION_DESC,
                    label = "Nach Dauer (absteigend)",
                    description = "Lang → Kurz",
                    selected = selectedOption == TaskSortOption.DURATION_DESC,
                    onSelect = { selectedOption = TaskSortOption.DURATION_DESC }
                )

                SortOptionItem(
                    option = TaskSortOption.ALPHABETICAL,
                    label = "Alphabetisch",
                    description = "A → Z",
                    selected = selectedOption == TaskSortOption.ALPHABETICAL,
                    onSelect = { selectedOption = TaskSortOption.ALPHABETICAL }
                )

                SortOptionItem(
                    option = TaskSortOption.CATEGORY,
                    label = "Nach Kategorie",
                    description = "Gruppiert nach Kategorien",
                    selected = selectedOption == TaskSortOption.CATEGORY,
                    onSelect = { selectedOption = TaskSortOption.CATEGORY }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedOption) }) {
                Text("Einfügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        modifier = modifier
    )
}

/**
 * Single sort option radio button item
 */
@Composable
private fun SortOptionItem(
    option: TaskSortOption,
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            ),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
