package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.MetadataTagEntity
import com.example.questflow.data.database.entity.TagType

/**
 * Reusable component for managing contact tags
 *
 * Features:
 * - Display assigned tags as chips
 * - Add new tags (select from existing or create new)
 * - Remove tags
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTagManagementSection(
    contactId: Long,
    assignedTags: List<MetadataTagEntity>,
    onAddTagsClick: () -> Unit,
    onRemoveTag: (MetadataTagEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onAddTagsClick) {
                Icon(Icons.Default.Add, contentDescription = "Tags hinzufügen")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (assignedTags.isEmpty()) {
            Text(
                text = "Keine Tags zugewiesen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(assignedTags, key = { it.id }) { tag ->
                    InputChip(
                        selected = true,
                        onClick = { },
                        label = { Text(tag.name) },
                        leadingIcon = {
                            tag.color?.let { colorHex ->
                                Box(
                                    modifier = Modifier.size(12.dp),
                                    content = {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(android.graphics.Color.parseColor(colorHex)),
                                            shape = MaterialTheme.shapes.small
                                        ) {}
                                    }
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { onRemoveTag(tag) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Tag entfernen",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dialog for selecting and creating tags for a contact
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTagSelectionDialog(
    availableTags: List<MetadataTagEntity>, // All CONTACT type tags
    currentlyAssignedTagIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
    onCreateNewTag: (String) -> Unit
) {
    var selectedTagIds by remember { mutableStateOf(currentlyAssignedTagIds) }
    var showCreateDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags auswählen") },
        text = {
            Column {
                // Create New Tag Button
                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Neues Tag erstellen")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (availableTags.isEmpty()) {
                    Text(
                        text = "Keine Tags verfügbar. Erstellen Sie ein neues Tag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Vorhandene Tags:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableTags.forEach { tag ->
                            val isSelected = tag.id in selectedTagIds
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTagIds = if (isSelected) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                },
                                label = { Text(tag.name) },
                                leadingIcon = {
                                    tag.color?.let { colorHex ->
                                        Box(
                                            modifier = Modifier.size(12.dp),
                                            content = {
                                                Surface(
                                                    modifier = Modifier.fillMaxSize(),
                                                    color = Color(android.graphics.Color.parseColor(colorHex)),
                                                    shape = MaterialTheme.shapes.small
                                                ) {}
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(selectedTagIds)
                onDismiss()
            }) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    // Create Tag Dialog
    if (showCreateDialog) {
        CreateContactTagDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { tagName ->
                onCreateNewTag(tagName)
                showCreateDialog = false
            }
        )
    }
}

/**
 * Simple dialog to create a new CONTACT tag
 */
@Composable
fun CreateContactTagDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Tag erstellen") },
        text = {
            Column {
                Text(
                    text = "Das Tag wird als Typ CONTACT erstellt und kann für alle Kontakte verwendet werden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag-Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(tagName) },
                enabled = tagName.isNotBlank()
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
