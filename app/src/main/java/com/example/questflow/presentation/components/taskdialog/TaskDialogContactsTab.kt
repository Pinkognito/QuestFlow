package com.example.questflow.presentation.components.taskdialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.MediaLibraryEntity

/**
 * TAB 3: Personen
 * - Kontakte Auswahl
 * - Verknüpfte Kontakte Liste (collapsible)
 * - Kontakt-Tags
 * - Aktionen Button
 */
@Composable
fun TaskDialogContactsTab(
    selectedContactIds: Set<Long>,
    availableContacts: List<MetadataContactEntity>,
    onSelectContactsClick: () -> Unit,
    onActionsClick: () -> Unit,
    taskContactListExpanded: Boolean,
    onTaskContactListExpandedChange: (Boolean) -> Unit,
    taskContactTagsMap: Map<Long, List<String>>,
    allMedia: List<MediaLibraryEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Contact Selection Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Kontakte & Aktionen",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (selectedContactIds.isEmpty()) "Keine verknüpft"
                        else "${selectedContactIds.size} verknüpft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onSelectContactsClick,
                        enabled = availableContacts.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Task Contacts",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onActionsClick,
                        enabled = selectedContactIds.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Aktionen",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Show selected contacts with tags - COLLAPSIBLE
        if (selectedContactIds.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Collapsible Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onTaskContactListExpandedChange(!taskContactListExpanded)
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Verknüpfte Kontakte (${selectedContactIds.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (taskContactListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (taskContactListExpanded) "Zuklappen" else "Aufklappen",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Contact List - only show when expanded
                if (taskContactListExpanded) {
                    val selectedContacts = availableContacts.filter { it.id in selectedContactIds }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            selectedContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Photo from Media Library or fallback to photoUri
                                    val photoMedia = contact.photoMediaId?.let { mediaId ->
                                        allMedia.find { media -> media.id == mediaId }
                                    }

                                    if (photoMedia != null) {
                                        coil.compose.AsyncImage(
                                            model = java.io.File(photoMedia.filePath),
                                            contentDescription = "Kontaktfoto",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (contact.photoUri != null) {
                                        // Fallback für alte photoUri
                                        coil.compose.AsyncImage(
                                            model = android.net.Uri.parse(contact.photoUri),
                                            contentDescription = "Kontaktfoto",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Emoji icon if present
                                            contact.iconEmoji?.let { emoji ->
                                                if (emoji.isNotBlank()) {
                                                    Text(
                                                        text = emoji,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                            }
                                            Text(
                                                contact.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        // Show task-tags for this contact
                                        val contactTags = taskContactTagsMap[contact.id] ?: emptyList()
                                        if (contactTags.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                contactTags.take(3).forEach { tag ->
                                                    AssistChip(
                                                        onClick = { },
                                                        label = {
                                                            Text(
                                                                tag,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        },
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                }
                                                if (contactTags.size > 3) {
                                                    Text(
                                                        "+${contactTags.size - 3}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
