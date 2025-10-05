package com.example.questflow.presentation.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.database.entity.AddressType
import com.example.questflow.data.database.entity.MetadataAddressEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAddressDialog(
    address: MetadataAddressEntity? = null,
    contactId: Long,
    onDismiss: () -> Unit,
    onSave: (MetadataAddressEntity) -> Unit
) {
    var street by remember { mutableStateOf(address?.street ?: "") }
    var houseNumber by remember { mutableStateOf(address?.houseNumber ?: "") }
    var addressLine2 by remember { mutableStateOf(address?.addressLine2 ?: "") }
    var city by remember { mutableStateOf(address?.city ?: "") }
    var postalCode by remember { mutableStateOf(address?.postalCode ?: "") }
    var state by remember { mutableStateOf(address?.state ?: "") }
    var country by remember { mutableStateOf(address?.country ?: "Deutschland") }
    var selectedType by remember { mutableStateOf(address?.addressType ?: AddressType.HOME) }
    var label by remember { mutableStateOf(address?.label ?: "") }
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (address == null) "Adresse hinzufügen" else "Adresse bearbeiten",
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
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("Straße *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = houseNumber,
                        onValueChange = { houseNumber = it },
                        label = { Text("Nr.") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = addressLine2,
                    onValueChange = { addressLine2 = it },
                    label = { Text("Adresszusatz") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text("PLZ *") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Stadt *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("Bundesland") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Land *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            AddressType.HOME -> "Privat"
                            AddressType.WORK -> "Arbeit"
                            AddressType.BILLING -> "Rechnung"
                            AddressType.SHIPPING -> "Versand"
                            AddressType.OTHER -> "Sonstiges"
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
                                selectedType = AddressType.HOME
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Arbeit") },
                            onClick = {
                                selectedType = AddressType.WORK
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rechnung") },
                            onClick = {
                                selectedType = AddressType.BILLING
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Versand") },
                            onClick = {
                                selectedType = AddressType.SHIPPING
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sonstiges") },
                            onClick = {
                                selectedType = AddressType.OTHER
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
                            if (street.isNotBlank() && city.isNotBlank() && postalCode.isNotBlank() && country.isNotBlank()) {
                                val updatedAddress = MetadataAddressEntity(
                                    id = address?.id ?: 0,
                                    contactId = contactId,
                                    street = street.trim(),
                                    houseNumber = houseNumber.trim().ifBlank { null },
                                    addressLine2 = addressLine2.trim().ifBlank { null },
                                    city = city.trim(),
                                    postalCode = postalCode.trim(),
                                    state = state.trim().ifBlank { null },
                                    country = country.trim(),
                                    addressType = selectedType,
                                    label = label.trim().ifBlank { null }
                                )
                                onSave(updatedAddress)
                                onDismiss()
                            }
                        },
                        enabled = street.isNotBlank() && city.isNotBlank() && postalCode.isNotBlank() && country.isNotBlank()
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}
