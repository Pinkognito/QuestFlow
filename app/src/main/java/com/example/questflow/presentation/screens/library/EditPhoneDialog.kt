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
import com.example.questflow.data.database.entity.MetadataPhoneEntity
import com.example.questflow.data.database.entity.PhoneType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhoneDialog(
    phone: MetadataPhoneEntity? = null,
    contactId: Long,
    onDismiss: () -> Unit,
    onSave: (MetadataPhoneEntity) -> Unit
) {
    var phoneNumber by remember { mutableStateOf(phone?.phoneNumber ?: "") }
    var selectedType by remember { mutableStateOf(phone?.phoneType ?: PhoneType.MOBILE) }
    var label by remember { mutableStateOf(phone?.label ?: "") }
    var countryCode by remember { mutableStateOf(phone?.countryCode ?: "+49") }
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
                        text = if (phone == null) "Telefonnummer hinzufügen" else "Telefonnummer bearbeiten",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it },
                        label = { Text("Land") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Telefonnummer *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            PhoneType.MOBILE -> "Mobil"
                            PhoneType.HOME -> "Privat"
                            PhoneType.WORK -> "Arbeit"
                            PhoneType.FAX -> "Fax"
                            PhoneType.OTHER -> "Sonstiges"
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
                            text = { Text("Mobil") },
                            onClick = {
                                selectedType = PhoneType.MOBILE
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Privat") },
                            onClick = {
                                selectedType = PhoneType.HOME
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Arbeit") },
                            onClick = {
                                selectedType = PhoneType.WORK
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fax") },
                            onClick = {
                                selectedType = PhoneType.FAX
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sonstiges") },
                            onClick = {
                                selectedType = PhoneType.OTHER
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
                            if (phoneNumber.isNotBlank()) {
                                val updatedPhone = MetadataPhoneEntity(
                                    id = phone?.id ?: 0,
                                    contactId = contactId,
                                    phoneNumber = phoneNumber.trim(),
                                    phoneType = selectedType,
                                    label = label.trim().ifBlank { null },
                                    countryCode = countryCode.trim().ifBlank { null }
                                )
                                onSave(updatedPhone)
                                onDismiss()
                            }
                        },
                        enabled = phoneNumber.isNotBlank()
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}
