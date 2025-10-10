package com.example.questflow.presentation.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.MetadataTagEntity

/**
 * Dialog für Task-Kontakt-Auswahl mit Tag-Filter und Manual Override
 *
 * @param allContacts Alle verfügbaren Kontakte (bei Mode.ALL) oder nur task-linked (bei Mode.TASK_LINKED)
 * @param contactTags Map: ContactId -> List<MetadataTagEntity>
 * @param availableTags Verfügbare CONTACT-Type Tags für Filter
 * @param initialSelectedContactIds Initial ausgewählte Kontakt-IDs
 * @param allMedia List of all media library entities for contact photos
 * @param onDismiss Dialog schließen
 * @param onConfirm Bestätigung mit ausgewählten Kontakt-IDs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskContactSelectionDialog(
    allContacts: List<MetadataContactEntity>,
    contactTags: Map<Long, List<MetadataTagEntity>>,
    availableTags: List<MetadataTagEntity>,
    initialSelectedContactIds: Set<Long>,
    allMedia: List<MediaLibraryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    // State für Tag-Filter
    var selectedFilterTags by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var useTagFilter by remember { mutableStateOf(false) }
    var tagSearchQuery by remember { mutableStateOf("") }

    // State für Manual Override
    // Map: ContactId -> Pair(isManuallyOverridden, manualSelection)
    var manualOverrides by remember {
        mutableStateOf<Map<Long, Pair<Boolean, Boolean>>>(emptyMap())
    }

    // Initial: Alle initial selected contacts sind manuell überschrieben
    LaunchedEffect(initialSelectedContactIds) {
        manualOverrides = initialSelectedContactIds.associateWith { contactId ->
            true to true // isManuallyOverridden=true, manualSelection=true
        }
    }

    // Berechne finale Auswahl basierend auf Tag-Filter und Manual Override
    val finalSelection = remember(allContacts, selectedFilterTags, useTagFilter, manualOverrides) {
        allContacts.mapNotNull { contact ->
            val override = manualOverrides[contact.id]

            val isSelected = if (override?.first == true) {
                // Manuell überschrieben → benutze manuelle Auswahl
                override.second
            } else if (useTagFilter && selectedFilterTags.isNotEmpty()) {
                // Tag-Filter aktiv → prüfe ob Kontakt mindestens einen der Tags hat
                val contactTagIds = contactTags[contact.id]?.map { it.id }?.toSet() ?: emptySet()
                selectedFilterTags.any { it in contactTagIds }
            } else {
                // Kein Filter → initial selection
                contact.id in initialSelectedContactIds
            }

            if (isSelected) contact.id else null
        }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task Contacts") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Debug Info
                Text(
                    "DEBUG: ${availableTags.size} Tags verfügbar, ${allContacts.size} Kontakte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                // Tag Filter Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tag-Filter", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = useTagFilter,
                        onCheckedChange = { useTagFilter = it }
                    )
                }

                // Tag Suchfeld (wenn Filter aktiv)
                if (useTagFilter) {
                    OutlinedTextField(
                        value = tagSearchQuery,
                        onValueChange = { tagSearchQuery = it },
                        label = { Text("Tags suchen") },
                        placeholder = { Text("Tag-Name eingeben...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Tag Chips (wenn Filter aktiv)
                if (useTagFilter && availableTags.isNotEmpty()) {
                    // Filtere Tags basierend auf Suchquery
                    val filteredTags = remember(availableTags, tagSearchQuery) {
                        if (tagSearchQuery.isBlank()) {
                            availableTags
                        } else {
                            availableTags.filter { tag ->
                                tag.name.contains(tagSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredTags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTags) { tag ->
                                FilterChip(
                                    selected = tag.id in selectedFilterTags,
                                    onClick = {
                                        selectedFilterTags = if (tag.id in selectedFilterTags) {
                                            selectedFilterTags - tag.id
                                        } else {
                                            selectedFilterTags + tag.id
                                        }
                                    },
                                    label = { Text(tag.name) }
                                )
                            }
                        }
                    } else {
                        Text(
                            "Keine Tags gefunden für \"$tagSearchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (useTagFilter && availableTags.isEmpty()) {
                    Text(
                        "Keine Tags verfügbar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider()

                // Info über Auswahl
                Text(
                    "${finalSelection.size} von ${allContacts.size} Kontakten ausgewählt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Kontakt-Liste
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allContacts) { contact ->
                        val isSelected = contact.id in finalSelection
                        val isManuallyOverridden = manualOverrides[contact.id]?.first == true

                        Card(
                            onClick = {
                                // Manual Override: Toggle selection
                                manualOverrides = manualOverrides + (contact.id to (true to !isSelected))
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Photo from Media Library or fallback to photoUri
                                    val photoMedia = contact.photoMediaId?.let { mediaId ->
                                        allMedia.find { it.id == mediaId }
                                    }

                                    if (photoMedia != null) {
                                        AsyncImage(
                                            model = java.io.File(photoMedia.filePath),
                                            contentDescription = "Kontaktfoto",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (contact.photoUri != null) {
                                        // Fallback für alte photoUri
                                        AsyncImage(
                                            model = Uri.parse(contact.photoUri),
                                            contentDescription = "Kontaktfoto",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Emoji icon if present
                                            contact.iconEmoji?.let { emoji ->
                                                if (emoji.isNotBlank()) {
                                                    Text(
                                                        text = emoji,
                                                        style = MaterialTheme.typography.titleLarge
                                                    )
                                                }
                                            }
                                            Text(
                                                text = contact.displayName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (isManuallyOverridden) {
                                                // Indikator für manuelle Override
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Manuell ausgewählt",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Zeige Tags des Kontakts
                                        val tags = contactTags[contact.id] ?: emptyList()
                                        if (tags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                items(tags) { tag ->
                                                    AssistChip(
                                                        onClick = { },
                                                        label = {
                                                            Text(
                                                                tag.name,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        },
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Checkbox
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        // Manual Override: Set selection
                                        manualOverrides = manualOverrides + (contact.id to (true to it))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(finalSelection) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
