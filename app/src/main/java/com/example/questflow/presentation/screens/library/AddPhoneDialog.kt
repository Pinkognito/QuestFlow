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
fun AddPhoneDialog(
    onDismiss: () -> Unit,
    onSave: (MetadataPhoneEntity) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PhoneType.MOBILE) }
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Neue Telefonnummer",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Telefonnummer *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        PhoneType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            PhoneType.MOBILE -> "Mobil"
                                            PhoneType.HOME -> "Privat"
                                            PhoneType.WORK -> "Arbeit"
                                            PhoneType.FAX -> "Fax"
                                            PhoneType.OTHER -> "Sonstiges"
                                        }
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

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
                                val phone = MetadataPhoneEntity(
                                    phoneNumber = phoneNumber.trim(),
                                    phoneType = selectedType
                                )
                                onSave(phone)
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
