package com.example.questflow.presentation.components.metadata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.data.database.entity.*
import com.example.questflow.domain.model.TaskMetadataItem
import com.example.questflow.presentation.viewmodels.MetadataLibraryViewModel

/**
 * Dialog for linking existing metadata objects to a task
 */
@Composable
fun LinkMetadataDialog(
    taskId: Long,
    onDismiss: () -> Unit,
    onMetadataLinked: (TaskMetadataItem) -> Unit,
    viewModel: MetadataLibraryViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(
        "Standorte" to Icons.Default.LocationOn,
        "Telefone" to Icons.Default.Phone,
        "Notizen" to Icons.Default.Info
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Metadaten verknüpfen",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Divider()

                // Tab Row
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                            icon = { Icon(icon, contentDescription = title) }
                        )
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> LocationList(taskId, viewModel, onMetadataLinked)
                        1 -> PhoneList(taskId, viewModel, onMetadataLinked)
                        2 -> NoteList(taskId, viewModel, onMetadataLinked)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationList(
    taskId: Long,
    viewModel: MetadataLibraryViewModel,
    onMetadataLinked: (TaskMetadataItem) -> Unit
) {
    val locations by viewModel.locations.collectAsState()

    if (locations.isEmpty()) {
        EmptyState("Keine Standorte vorhanden")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(locations) { location ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMetadataLinked(
                                TaskMetadataItem.Location(
                                    metadataId = 0,
                                    taskId = taskId,
                                    displayOrder = 0,
                                    location = location
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = location.placeName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            location.formattedAddress?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hinzufügen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneList(
    taskId: Long,
    viewModel: MetadataLibraryViewModel,
    onMetadataLinked: (TaskMetadataItem) -> Unit
) {
    val phones by viewModel.phoneNumbers.collectAsState()

    if (phones.isEmpty()) {
        EmptyState("Keine Telefonnummern vorhanden")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(phones) { phone ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMetadataLinked(
                                TaskMetadataItem.Phone(
                                    metadataId = 0,
                                    taskId = taskId,
                                    displayOrder = 0,
                                    phone = phone
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = phone.phoneNumber,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = phone.phoneType.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hinzufügen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteList(
    taskId: Long,
    viewModel: MetadataLibraryViewModel,
    onMetadataLinked: (TaskMetadataItem) -> Unit
) {
    val notes by viewModel.notes.collectAsState()

    if (notes.isEmpty()) {
        EmptyState("Keine Notizen vorhanden")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMetadataLinked(
                                TaskMetadataItem.Note(
                                    metadataId = 0,
                                    taskId = taskId,
                                    displayOrder = 0,
                                    note = note
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.content.take(50) + if (note.content.length > 50) "..." else "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = note.format.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hinzufügen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
