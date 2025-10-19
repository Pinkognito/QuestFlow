package com.example.questflow.presentation.components.metadata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.database.entity.*
import com.example.questflow.domain.model.TaskMetadataItem

/**
 * Dialog for adding new metadata to a task
 */
@Composable
fun AddMetadataDialog(
    onDismiss: () -> Unit,
    onAdd: (TaskMetadataItem) -> Unit
) {
    var selectedType by remember { mutableStateOf<MetadataType?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Metadaten hinzufügen",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedType == null) {
                    // Type selection grid
                    MetadataTypeSelectionGrid(
                        onTypeSelected = { selectedType = it }
                    )
                } else {
                    // Type-specific form
                    when (selectedType) {
                        MetadataType.PHONE -> PhoneMetadataForm(
                            onCancel = { selectedType = null },
                            onSave = { phone ->
                                onAdd(
                                    TaskMetadataItem.Phone(
                                        metadataId = 0,
                                        taskId = 0,
                                        displayOrder = 0,
                                        phone = phone
                                    )
                                )
                                onDismiss()
                            }
                        )
                        MetadataType.NOTE -> NoteMetadataForm(
                            onCancel = { selectedType = null },
                            onSave = { note ->
                                onAdd(
                                    TaskMetadataItem.Note(
                                        metadataId = 0,
                                        taskId = 0,
                                        displayOrder = 0,
                                        note = note
                                    )
                                )
                                onDismiss()
                            }
                        )
                        MetadataType.LOCATION -> LocationMetadataForm(
                            onCancel = { selectedType = null },
                            onSave = { location ->
                                onAdd(
                                    TaskMetadataItem.Location(
                                        metadataId = 0,
                                        taskId = 0,
                                        displayOrder = 0,
                                        location = location
                                    )
                                )
                                onDismiss()
                            }
                        )
                        else -> {
                            Text("Dieser Typ wird noch nicht unterstützt")
                            TextButton(onClick = { selectedType = null }) {
                                Text("Zurück")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataTypeSelectionGrid(
    onTypeSelected: (MetadataType) -> Unit
) {
    val supportedTypes = listOf(
        MetadataType.PHONE,
        MetadataType.NOTE,
        MetadataType.LOCATION
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 300.dp)
    ) {
        items(supportedTypes) { type ->
            MetadataTypeCard(
                type = type,
                onClick = { onTypeSelected(type) }
            )
        }
    }
}

@Composable
private fun MetadataTypeCard(
    type: MetadataType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Extension properties for UI
private val MetadataType.icon: ImageVector
    get() = when (this) {
        MetadataType.LOCATION -> Icons.Default.LocationOn
        MetadataType.CONTACT -> Icons.Default.Person
        MetadataType.PHONE -> Icons.Default.Phone
        MetadataType.ADDRESS -> Icons.Default.Home
        MetadataType.EMAIL -> Icons.Default.Email
        MetadataType.URL -> Icons.Default.Star
        MetadataType.NOTE -> Icons.Default.Info
        MetadataType.FILE_ATTACHMENT -> Icons.Default.Settings
    }

private val MetadataType.displayName: String
    get() = when (this) {
        MetadataType.LOCATION -> "Standort"
        MetadataType.CONTACT -> "Kontakt"
        MetadataType.PHONE -> "Telefon"
        MetadataType.ADDRESS -> "Adresse"
        MetadataType.EMAIL -> "E-Mail"
        MetadataType.URL -> "Link"
        MetadataType.NOTE -> "Notiz"
        MetadataType.FILE_ATTACHMENT -> "Datei"
    }

// Simple forms for the main types
@Composable
private fun PhoneMetadataForm(
    onCancel: () -> Unit,
    onSave: (MetadataPhoneEntity) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Telefonnummer") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onCancel) {
                Text("Abbrechen")
            }
            Button(
                onClick = {
                    if (phoneNumber.isNotBlank()) {
                        onSave(
                            MetadataPhoneEntity(
                                phoneNumber = phoneNumber,
                                phoneType = PhoneType.MOBILE
                            )
                        )
                    }
                },
                enabled = phoneNumber.isNotBlank()
            ) {
                Text("Speichern")
            }
        }
    }
}

@Composable
private fun NoteMetadataForm(
    onCancel: () -> Unit,
    onSave: (MetadataNoteEntity) -> Unit
) {
    var noteContent by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = noteContent,
            onValueChange = { noteContent = it },
            label = { Text("Notiz") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onCancel) {
                Text("Abbrechen")
            }
            Button(
                onClick = {
                    if (noteContent.isNotBlank()) {
                        onSave(
                            MetadataNoteEntity(
                                content = noteContent,
                                format = NoteFormat.PLAIN_TEXT
                            )
                        )
                    }
                },
                enabled = noteContent.isNotBlank()
            ) {
                Text("Speichern")
            }
        }
    }
}

@Composable
private fun LocationMetadataForm(
    onCancel: () -> Unit,
    onSave: (MetadataLocationEntity) -> Unit
) {
    var placeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = placeName,
            onValueChange = { placeName = it },
            label = { Text("Ortsname") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Adresse (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onCancel) {
                Text("Abbrechen")
            }
            Button(
                onClick = {
                    if (placeName.isNotBlank()) {
                        onSave(
                            MetadataLocationEntity(
                                placeName = placeName,
                                latitude = 0.0, // Simplified - would need location picker
                                longitude = 0.0,
                                formattedAddress = address.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = placeName.isNotBlank()
            ) {
                Text("Speichern")
            }
        }
    }
}
