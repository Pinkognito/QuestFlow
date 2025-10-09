package com.example.questflow.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.MetadataTagEntity

/**
 * Dialog für Aktionen auf Task-Kontakte
 *
 * Ermöglicht:
 * 1. Aktionen ausführen (WhatsApp, Call, SMS, Email)
 * 2. Task-spezifische Tags vergeben
 *
 * @param taskId Task ID für ActionDialog
 * @param taskLinkedContacts Nur Task-verknüpfte Kontakte
 * @param contactTags Map: ContactId -> List<MetadataTagEntity> (globale Contact-Tags)
 * @param availableTags Verfügbare CONTACT-Type Tags für Filter
 * @param taskContactTags Map: ContactId -> List<String> (task-spezifische Tags)
 * @param textTemplates Verfügbare Text-Templates für Aktionen
 * @param actionExecutor Executor für Aktionen
 * @param placeholderResolver Resolver für Platzhalter in Templates
 * @param onDismiss Dialog schließen
 * @param onSaveTaskTags Task-spezifische Tags speichern: Map<ContactId, List<String>>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskContactActionsDialog(
    taskId: Long = 0L,
    taskLinkedContacts: List<MetadataContactEntity>,
    contactTags: Map<Long, List<MetadataTagEntity>>,
    availableTags: List<MetadataTagEntity>,
    taskContactTags: Map<Long, List<String>>,
    textTemplates: List<com.example.questflow.data.database.entity.TextTemplateEntity> = emptyList(),
    actionExecutor: com.example.questflow.domain.action.ActionExecutor? = null,
    placeholderResolver: com.example.questflow.domain.placeholder.PlaceholderResolver? = null,
    multiContactActionManager: com.example.questflow.domain.action.MultiContactActionManager? = null,
    onDismiss: () -> Unit,
    onSaveTaskTags: (Map<Long, List<String>>) -> Unit
) {
    val context = LocalContext.current

    // State für Tag-Filter
    var selectedFilterTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var useTagFilter by remember { mutableStateOf(false) }
    var tagSearchQuery by remember { mutableStateOf("") }

    // State für Manual Override (gleiche Logik wie TaskContactSelectionDialog)
    // Map<ContactId, Triple<hasOverride: Boolean, isSelected: Boolean, isLocked: Boolean>>
    var manualOverrides by remember {
        mutableStateOf<Map<Long, Triple<Boolean, Boolean, Boolean>>>(emptyMap())
    }

    // State für Action Selection
    var showActionDialog by remember { mutableStateOf(false) }
    var showTaskTagDialog by remember { mutableStateOf(false) }

    // Sammle alle verfügbaren Tags (globale CONTACT-Tags + task-spezifische Tags)
    val allAvailableTags = remember(availableTags, taskContactTags) {
        val globalTagNames = availableTags.map { it.name }.toSet()
        val taskTagNames = taskContactTags.values.flatten().toSet()
        (globalTagNames + taskTagNames).sorted()
    }

    // Berechne finale Auswahl
    val finalSelection = remember(taskLinkedContacts, selectedFilterTags, useTagFilter, manualOverrides, contactTags, taskContactTags) {
        taskLinkedContacts.mapNotNull { contact ->
            val override = manualOverrides[contact.id]

            val isSelected = if (override?.first == true && override.third == true) {
                // Locked: use manual selection
                override.second
            } else if (useTagFilter && selectedFilterTags.isNotEmpty()) {
                // Tag filter active (only if not locked)
                val globalTagNames = contactTags[contact.id]?.map { it.name }?.toSet() ?: emptySet()
                val taskTagNames = taskContactTags[contact.id]?.toSet() ?: emptySet()
                val allContactTags = globalTagNames + taskTagNames

                selectedFilterTags.any { it in allContactTags }
            } else {
                false
            }

            if (isSelected) contact.id else null
        }.toSet()
    }

    // Finde ausgewählte Kontakte
    val selectedContacts = remember(finalSelection) {
        taskLinkedContacts.filter { it.id in finalSelection }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kontakt-Aktionen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                // Tag Chips (globale + task-spezifische)
                if (useTagFilter && allAvailableTags.isNotEmpty()) {
                    // Filtere Tags basierend auf Suchquery
                    val filteredTags = remember(allAvailableTags, tagSearchQuery) {
                        if (tagSearchQuery.isBlank()) {
                            allAvailableTags
                        } else {
                            allAvailableTags.filter { tagName ->
                                tagName.contains(tagSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredTags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTags) { tagName ->
                                FilterChip(
                                    selected = tagName in selectedFilterTags,
                                    onClick = {
                                        selectedFilterTags = if (tagName in selectedFilterTags) {
                                            selectedFilterTags - tagName
                                        } else {
                                            selectedFilterTags + tagName
                                        }
                                    },
                                    label = { Text(tagName) }
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
                } else if (useTagFilter && allAvailableTags.isEmpty()) {
                    Text(
                        "Keine Tags verfügbar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider()

                // Select All / Deselect All Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Select all contacts with lock
                            manualOverrides = taskLinkedContacts.associate { contact ->
                                contact.id to Triple(true, true, true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Alle auswählen")
                    }

                    OutlinedButton(
                        onClick = {
                            // Deselect all contacts with lock
                            manualOverrides = taskLinkedContacts.associate { contact ->
                                contact.id to Triple(true, false, true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Alle abwählen")
                    }
                }

                // Info
                Text(
                    "${finalSelection.size} von ${taskLinkedContacts.size} Kontakten ausgewählt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action Buttons
                if (finalSelection.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                android.util.Log.d("ActionDialog", "Opening ActionDialog with ${selectedContacts.size} contacts")
                                showActionDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Aktionen")
                        }

                        Button(
                            onClick = { showTaskTagDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Task-Tags")
                        }
                    }
                }

                Divider()

                // Kontakt-Liste
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(taskLinkedContacts) { contact ->
                        val isSelected = contact.id in finalSelection
                        val taskTags = taskContactTags[contact.id] ?: emptyList()
                        val override = manualOverrides[contact.id]
                        val isLocked = override?.third ?: false

                        Card(
                            onClick = {
                                // Toggle selection and lock it
                                manualOverrides = manualOverrides + (contact.id to Triple(true, !isSelected, true))
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    // Globale Contact-Tags
                                    val globalTags = contactTags[contact.id] ?: emptyList()
                                    if (globalTags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(globalTags) { tag ->
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

                                    // Task-spezifische Tags
                                    if (taskTags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Task-Tags:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(taskTags) { tag ->
                                                AssistChip(
                                                    onClick = { },
                                                    label = {
                                                        Text(
                                                            tag,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                                    ),
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Lock Icon
                                    IconButton(
                                        onClick = {
                                            // Toggle lock state
                                            val currentOverride = manualOverrides[contact.id]
                                            if (currentOverride != null) {
                                                manualOverrides = manualOverrides + (contact.id to Triple(
                                                    true,
                                                    currentOverride.second,
                                                    !currentOverride.third
                                                ))
                                            } else {
                                                // First time: lock with current selection state
                                                manualOverrides = manualOverrides + (contact.id to Triple(true, isSelected, true))
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Check,
                                            contentDescription = if (isLocked) "Gesperrt" else "Entsperrt",
                                            tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            // Toggle selection and lock it
                                            manualOverrides = manualOverrides + (contact.id to Triple(true, it, true))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
        dismissButton = null
    )

    // Action Dialog (WhatsApp, SMS, Email mit Text-Eingabe)
    if (showActionDialog && selectedContacts.isNotEmpty() && actionExecutor != null && placeholderResolver != null && multiContactActionManager != null) {
        android.util.Log.d("ActionDialog", "Rendering ActionDialog for ${selectedContacts.size} contacts")
        ActionDialog(
            taskId = taskId,
            selectedContacts = selectedContacts,
            availableTemplates = textTemplates,
            onDismiss = {
                android.util.Log.d("ActionDialog", "ActionDialog dismissed")
                showActionDialog = false
            },
            onActionExecuted = {
                android.util.Log.d("ActionDialog", "Action executed successfully")
                showActionDialog = false
            },
            actionExecutor = actionExecutor,
            placeholderResolver = placeholderResolver,
            multiContactActionManager = multiContactActionManager
        )
    } else if (showActionDialog && selectedContacts.isNotEmpty() && (actionExecutor == null || placeholderResolver == null)) {
        android.util.Log.e("ActionDialog", "ERROR: ActionDialog cannot render - missing actionExecutor or placeholderResolver")
        android.util.Log.e("ActionDialog", "  actionExecutor = $actionExecutor")
        android.util.Log.e("ActionDialog", "  placeholderResolver = $placeholderResolver")
        // Fallback: Show a simple error message
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("Fehler") },
            text = { Text("Aktion-System nicht verfügbar. Bitte von Kalender-Ansicht aus öffnen.") },
            confirmButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Task-Tag Dialog
    if (showTaskTagDialog) {
        TaskContactTagAssignmentDialog(
            selectedContacts = selectedContacts,
            currentTaskTags = taskContactTags.filterKeys { it in finalSelection },
            onDismiss = {
                android.util.Log.d("TaskTagDialog", "Dialog dismissed without saving")
                showTaskTagDialog = false
            },
            onConfirm = { updatedTags ->
                android.util.Log.d("TaskTagDialog", "=== SAVING TASK TAGS ===")
                android.util.Log.d("TaskTagDialog", "Updated tags to save: $updatedTags")
                android.util.Log.d("TaskTagDialog", "Number of contacts with tags: ${updatedTags.size}")
                updatedTags.forEach { (contactId, tags) ->
                    android.util.Log.d("TaskTagDialog", "  Contact $contactId: $tags")
                }
                onSaveTaskTags(updatedTags)
                showTaskTagDialog = false
            }
        )
    }
}

/**
 * Dialog zum Vergeben von Task-spezifischen Tags für ausgewählte Kontakte
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskContactTagAssignmentDialog(
    selectedContacts: List<MetadataContactEntity>,
    currentTaskTags: Map<Long, List<String>>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, List<String>>) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var contactTagMap by remember {
        mutableStateOf(currentTaskTags.toMutableMap())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task-Tags vergeben") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tag Input
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Neues Tag") },
                    placeholder = { Text("z.B. Teilnehmer, Entscheidungsträger") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (tagInput.isNotBlank()) {
                                    // Füge Tag zu allen ausgewählten Kontakten hinzu
                                    selectedContacts.forEach { contact ->
                                        val existingTags = contactTagMap[contact.id] ?: emptyList()
                                        if (tagInput !in existingTags) {
                                            contactTagMap[contact.id] = existingTags + tagInput
                                        }
                                    }
                                    tagInput = ""
                                }
                            },
                            enabled = tagInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tag hinzufügen")
                        }
                    }
                )

                Divider()

                // Kontakt-Liste mit Tags
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedContacts) { contact ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                // Tags für diesen Kontakt - DIREKT aus State lesen statt cachen!
                                val tags = contactTagMap[contact.id] ?: emptyList()
                                android.util.Log.d("TaskTagDialog", "Rendering tags for ${contact.displayName}: $tags")
                                if (tags.isNotEmpty()) {
                                    // FlowRow statt LazyRow, um Touch-Konflikte zu vermeiden
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        tags.forEach { tag ->
                                            InputChip(
                                                selected = false,
                                                onClick = {
                                                    android.util.Log.d("TaskTagDialog", "=== REMOVE TAG CLICKED ===")
                                                    android.util.Log.d("TaskTagDialog", "Contact: ${contact.displayName} (ID: ${contact.id})")
                                                    android.util.Log.d("TaskTagDialog", "Tag to remove: '$tag'")

                                                    // WICHTIG: Aktuelle Tags aus Map holen, nicht cached 'tags' verwenden!
                                                    val currentTags = contactTagMap[contact.id] ?: emptyList()
                                                    android.util.Log.d("TaskTagDialog", "Current tags in map: $currentTags")

                                                    val updatedTags = currentTags - tag
                                                    android.util.Log.d("TaskTagDialog", "Updated tags after removal: $updatedTags")

                                                    contactTagMap = contactTagMap.toMutableMap().apply {
                                                        if (updatedTags.isEmpty()) {
                                                            remove(contact.id)
                                                            android.util.Log.d("TaskTagDialog", "All tags removed, removed contact from map")
                                                        } else {
                                                            put(contact.id, updatedTags)
                                                            android.util.Log.d("TaskTagDialog", "Updated map with new tags")
                                                        }
                                                    }
                                                    android.util.Log.d("TaskTagDialog", "Final map state: ${contactTagMap[contact.id]}")
                                                },
                                                label = { Text(tag) },
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Entfernen",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        "Keine Task-Tags",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                android.util.Log.d("TaskTagDialog", "=== CONFIRM BUTTON CLICKED ===")
                android.util.Log.d("TaskTagDialog", "Current contactTagMap before save: $contactTagMap")
                android.util.Log.d("TaskTagDialog", "Selected contacts: ${selectedContacts.map { it.id }}")

                // WICHTIG: Auch Kontakte ohne Tags müssen als leere Liste gespeichert werden!
                val finalMap = selectedContacts.associate { contact ->
                    contact.id to (contactTagMap[contact.id] ?: emptyList())
                }
                android.util.Log.d("TaskTagDialog", "Final map with all contacts: $finalMap")

                onConfirm(finalMap)
            }) {
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
