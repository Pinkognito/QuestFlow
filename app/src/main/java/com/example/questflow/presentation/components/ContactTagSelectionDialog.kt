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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.repository.TagSuggestion
import com.example.questflow.domain.contact.ContactSelectionManager
import kotlinx.coroutines.launch

/**
 * Dialog for selecting contacts and tags with intelligent user-priority logic
 *
 * Features:
 * - Tag-based contact selection
 * - Manual contact override
 * - Auto-suggest tags based on usage frequency
 * - Visual indication of tag-vs-manual selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTagSelectionDialog(
    availableContacts: List<MetadataContactEntity>,
    initialSelectedContacts: Set<Long>,
    initialTags: Map<String, List<Long>>, // tag -> list of contact IDs
    onDismiss: () -> Unit,
    onConfirm: (selectedContacts: Set<Long>, contactTagMap: Map<Long, List<String>>) -> Unit,
    getTagSuggestions: suspend (String) -> List<TagSuggestion>,
    contactSelectionManager: ContactSelectionManager
) {
    var selectedContacts by remember { mutableStateOf(initialSelectedContacts) }
    var activeTags by remember { mutableStateOf(initialTags.keys.toSet()) }
    var manuallySelected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var manuallyDeselected by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var tagInput by remember { mutableStateOf("") }
    var tagSuggestions by remember { mutableStateOf<List<TagSuggestion>>(emptyList()) }
    var showTagSuggestions by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Load tag suggestions when input changes
    LaunchedEffect(tagInput) {
        if (tagInput.isNotBlank()) {
            tagSuggestions = getTagSuggestions(tagInput)
            showTagSuggestions = true
        } else {
            showTagSuggestions = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Kontakte & Tags auswählen",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tag Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tag hinzufügen") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (tagInput.isNotBlank() && tagInput !in activeTags) {
                                val result = contactSelectionManager.applyTagChange(
                                    currentSelected = selectedContacts,
                                    activeTags = activeTags,
                                    tagToToggle = tagInput,
                                    isAdding = true,
                                    tagContactMap = initialTags + (tagInput to emptyList()),
                                    manuallySelected = manuallySelected,
                                    manuallyDeselected = manuallyDeselected
                                )
                                selectedContacts = result.selectedContacts
                                activeTags = result.activeTags
                                tagInput = ""
                                showTagSuggestions = false
                            }
                        }
                    ) {
                        Text("+")
                    }
                }

                // Tag Suggestions
                if (showTagSuggestions && tagSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            tagSuggestions.take(5).forEach { suggestion ->
                                TextButton(
                                    onClick = {
                                        tagInput = suggestion.tag
                                        showTagSuggestions = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(suggestion.tag)
                                        Text(
                                            "${suggestion.usageCount}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Tags
                if (activeTags.isNotEmpty()) {
                    Text(
                        "Aktive Tags:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeTags.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    val result = contactSelectionManager.applyTagChange(
                                        currentSelected = selectedContacts,
                                        activeTags = activeTags,
                                        tagToToggle = tag,
                                        isAdding = false,
                                        tagContactMap = initialTags,
                                        manuallySelected = manuallySelected,
                                        manuallyDeselected = manuallyDeselected
                                    )
                                    selectedContacts = result.selectedContacts
                                    activeTags = result.activeTags
                                },
                                label = { Text(tag) },
                                leadingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Summary
                Text(
                    "${selectedContacts.size} von ${availableContacts.size} Kontakten ausgewählt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Contacts List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableContacts, key = { it.id }) { contact ->
                        val isSelected = contact.id in selectedContacts
                        val isManual = contact.id in manuallySelected
                        val isFromTag = isSelected && !isManual

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val result = contactSelectionManager.applyManualChange(
                                    currentSelected = selectedContacts,
                                    contactId = contact.id,
                                    isAdding = !isSelected,
                                    manuallySelected = manuallySelected,
                                    manuallyDeselected = manuallyDeselected,
                                    activeTags = activeTags,
                                    tagContactMap = initialTags
                                )
                                selectedContacts = result.selectedContacts
                                manuallySelected = result.manuallySelected
                                manuallyDeselected = result.manuallyDeselected
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isManual -> MaterialTheme.colorScheme.primaryContainer
                                    isFromTag -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(contact.displayName, fontWeight = FontWeight.Medium)
                                    if (isManual) {
                                        Text(
                                            "Manuell ausgewählt",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isFromTag) {
                                        Text(
                                            "Aus Tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Button(
                        onClick = {
                            // Build contact -> tags map
                            val contactTagMap = mutableMapOf<Long, List<String>>()
                            for (contactId in selectedContacts) {
                                val tags = activeTags.filter { tag ->
                                    initialTags[tag]?.contains(contactId) == true
                                }
                                contactTagMap[contactId] = tags
                            }
                            onConfirm(selectedContacts, contactTagMap)
                        }
                    ) {
                        Text("Bestätigen")
                    }
                }
            }
        }
    }
}
