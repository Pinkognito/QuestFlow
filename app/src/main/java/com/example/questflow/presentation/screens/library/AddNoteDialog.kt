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
import com.example.questflow.data.database.entity.MetadataNoteEntity
import com.example.questflow.data.database.entity.NoteFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (MetadataNoteEntity) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(NoteFormat.PLAIN_TEXT) }
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
                        text = "Neue Notiz",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Notiz *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedFormat) {
                            NoteFormat.PLAIN_TEXT -> "Text"
                            NoteFormat.MARKDOWN -> "Markdown"
                            NoteFormat.HTML -> "HTML"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        NoteFormat.values().forEach { format ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (format) {
                                            NoteFormat.PLAIN_TEXT -> "Text"
                                            NoteFormat.MARKDOWN -> "Markdown"
                                            NoteFormat.HTML -> "HTML"
                                        }
                                    )
                                },
                                onClick = {
                                    selectedFormat = format
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
                            if (content.isNotBlank()) {
                                val note = MetadataNoteEntity(
                                    content = content.trim(),
                                    format = selectedFormat
                                )
                                onSave(note)
                                onDismiss()
                            }
                        },
                        enabled = content.isNotBlank()
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}
