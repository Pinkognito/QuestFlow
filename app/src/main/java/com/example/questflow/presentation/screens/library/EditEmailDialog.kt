package com.example.questflow.presentation.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.database.entity.EmailType
import com.example.questflow.data.database.entity.MetadataEmailEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmailDialog(
    email: MetadataEmailEntity? = null,
    contactId: Long,
    onDismiss: () -> Unit,
    onSave: (MetadataEmailEntity) -> Unit
) {
    var emailAddress by remember { mutableStateOf(email?.emailAddress ?: "") }
    var selectedType by remember { mutableStateOf(email?.emailType ?: EmailType.PERSONAL) }
    var label by remember { mutableStateOf(email?.label ?: "") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (email == null) "E-Mail hinzufügen" else "E-Mail bearbeiten",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }

                OutlinedTextField(
                    value = emailAddress,
                    onValueChange = { emailAddress = it },
                    label = { Text("E-Mail-Adresse *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            EmailType.PERSONAL -> "Privat"
                            EmailType.WORK -> "Arbeit"
                            EmailType.OTHER -> "Sonstiges"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Typ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Privat") },
                            onClick = {
                                selectedType = EmailType.PERSONAL
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Arbeit") },
                            onClick = {
                                selectedType = EmailType.WORK
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sonstiges") },
                            onClick = {
                                selectedType = EmailType.OTHER
                                expanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = {
                            if (emailAddress.isNotBlank()) {
                                val updatedEmail = MetadataEmailEntity(
                                    id = email?.id ?: 0,
                                    contactId = contactId,
                                    emailAddress = emailAddress.trim(),
                                    emailType = selectedType,
                                    label = label.trim().ifBlank { null }
                                )
                                onSave(updatedEmail)
                                onDismiss()
                            }
                        },
                        enabled = emailAddress.isNotBlank()
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}
