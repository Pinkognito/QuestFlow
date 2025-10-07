package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.MetadataTagEntity
import com.example.questflow.data.database.entity.TagType

/**
 * Dialog zur Auswahl von Tags
 *
 * Features:
 * - Typ-basierte Filterung
 * - Multi-Select
 * - Hierarchie-Anzeige (TODO: Später)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSelectionDialog(
    availableTags: List<MetadataTagEntity>,
    selectedTagIds: Set<Long>,
    filterType: TagType? = null,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(selectedTagIds) }

    // Filter tags by type
    val filteredTags = remember(availableTags, filterType) {
        if (filterType == null) {
            availableTags
        } else {
            availableTags.filter { it.type == filterType || it.type == TagType.GENERAL }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags auswählen") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                items(filteredTags) { tag ->
                    val isSelected = tag.id in selectedIds

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (isSelected) {
                                    selectedIds - tag.id
                                } else {
                                    selectedIds + tag.id
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Color indicator
                        tag.color?.let { colorHex ->
                            Box(
                                modifier = Modifier.size(16.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color(android.graphics.Color.parseColor(colorHex)),
                                    shape = MaterialTheme.shapes.small
                                ) {}
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = tag.type.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Ausgewählt",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIds) }) {
                Text("OK (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
