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
 * @param taskLinkedContacts Nur Task-verknüpfte Kontakte
 * @param contactTags Map: ContactId -> List<MetadataTagEntity> (globale Contact-Tags)
 * @param availableTags Verfügbare CONTACT-Type Tags für Filter
 * @param taskContactTags Map: ContactId -> List<String> (task-spezifische Tags)
 * @param onDismiss Dialog schließen
 * @param onSaveTaskTags Task-spezifische Tags speichern: Map<ContactId, List<String>>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskContactActionsDialog(
    taskLinkedContacts: List<MetadataContactEntity>,
    contactTags: Map<Long, List<MetadataTagEntity>>,
    availableTags: List<MetadataTagEntity>,
    taskContactTags: Map<Long, List<String>>,
    onDismiss: () -> Unit,
    onSaveTaskTags: (Map<Long, List<String>>) -> Unit
) {
    val context = LocalContext.current

    // State für Tag-Filter
    var selectedFilterTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var useTagFilter by remember { mutableStateOf(false) }
    var tagSearchQuery by remember { mutableStateOf("") }

    // State für Manual Override (gleiche Logik wie TaskContactSelectionDialog)
    var manualOverrides by remember {
        mutableStateOf<Map<Long, Pair<Boolean, Boolean>>>(emptyMap())
    }

    // State für Action Selection
    var showActionPicker by remember { mutableStateOf(false) }
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

            val isSelected = if (override?.first == true) {
                override.second
            } else if (useTagFilter && selectedFilterTags.isNotEmpty()) {
                // Prüfe sowohl globale Contact-Tags als auch task-spezifische Tags
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
                            onClick = { showActionPicker = true },
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

                        Card(
                            onClick = {
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

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
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
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
        dismissButton = null
    )

    // Action Picker Dialog
    if (showActionPicker) {
        AlertDialog(
            onDismissRequest = { showActionPicker = false },
            title = { Text("Aktion wählen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedContacts.forEach { contact ->
                                contact.primaryPhone?.let { phone ->
                                    callContact(context, phone)
                                }
                            }
                            showActionPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Anrufen")
                    }

                    Button(
                        onClick = {
                            selectedContacts.forEach { contact ->
                                contact.primaryPhone?.let { phone ->
                                    sendWhatsApp(context, phone)
                                }
                            }
                            showActionPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("WhatsApp")
                    }

                    Button(
                        onClick = {
                            selectedContacts.forEach { contact ->
                                contact.primaryPhone?.let { phone ->
                                    sendSMS(context, phone)
                                }
                            }
                            showActionPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SMS")
                    }

                    Button(
                        onClick = {
                            selectedContacts.forEach { contact ->
                                contact.primaryEmail?.let { emailAddr ->
                                    sendEmail(context, emailAddr)
                                }
                            }
                            showActionPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Email")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionPicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Task-Tag Dialog
    if (showTaskTagDialog) {
        TaskContactTagAssignmentDialog(
            selectedContacts = selectedContacts,
            currentTaskTags = taskContactTags.filterKeys { it in finalSelection },
            onDismiss = { showTaskTagDialog = false },
            onConfirm = { updatedTags ->
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

                                // Tags für diesen Kontakt
                                val tags = contactTagMap[contact.id] ?: emptyList()
                                if (tags.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(tags) { tag ->
                                            InputChip(
                                                selected = false,
                                                onClick = {
                                                    // Entferne Tag
                                                    contactTagMap[contact.id] = tags - tag
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
            Button(onClick = { onConfirm(contactTagMap) }) {
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

// Helper functions für Aktionen
private fun callContact(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    context.startActivity(intent)
}

private fun sendWhatsApp(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://wa.me/$phone")
        context.startActivity(intent)
    } catch (e: Exception) {
        // WhatsApp nicht installiert
    }
}

private fun sendSMS(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone"))
    context.startActivity(intent)
}

private fun sendEmail(context: Context, emailAddr: String) {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:$emailAddr")
    context.startActivity(intent)
}
