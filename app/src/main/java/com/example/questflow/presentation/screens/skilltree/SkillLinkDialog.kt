package com.example.questflow.presentation.screens.skilltree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.repository.SkillNodeWithStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillLinkDialog(
    currentSkill: SkillNodeWithStatus,
    availableSkills: List<SkillNodeWithStatus>,
    existingParents: List<String>,
    onDismiss: () -> Unit,
    onLinkSkill: (parentId: String, minInvestment: Int) -> Unit
) {
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var minInvestment by remember { mutableStateOf("1") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Skill verknüpfen",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Für: ${currentSkill.node.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Voraussetzungs-Skill wählen:",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(8.dp))

                // Available parents list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        availableSkills.filter {
                            it.node.id != currentSkill.node.id &&
                            !existingParents.contains(it.node.id)
                        }
                    ) { skill ->
                        Card(
                            onClick = {
                                selectedParentId = if (selectedParentId == skill.node.id) null else skill.node.id
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedParentId == skill.node.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (selectedParentId == skill.node.id)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skill.node.title,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Max: ${skill.node.maxInvestment} Punkte",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (selectedParentId == skill.node.id) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Ausgewählt",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Min Investment configuration
                if (selectedParentId != null) {
                    Spacer(Modifier.height(16.dp))

                    val selectedSkill = availableSkills.find { it.node.id == selectedParentId }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "Benötigte Investment-Stufe",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = minInvestment,
                                onValueChange = {
                                    val max = selectedSkill?.node?.maxInvestment ?: 10
                                    val value = it.toIntOrNull()
                                    if (value != null && value in 1..max) {
                                        minInvestment = it
                                    } else if (it.isEmpty()) {
                                        minInvestment = ""
                                    }
                                },
                                label = { Text("Min. Punkte") },
                                suffix = { Text("/ ${selectedSkill?.node?.maxInvestment ?: 0}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Der Spieler muss mindestens diese Anzahl Punkte in '${selectedSkill?.node?.title}' investiert haben, bevor er '${currentSkill.node.title}' freischalten kann.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = {
                            selectedParentId?.let { parentId ->
                                val minInv = minInvestment.toIntOrNull() ?: 1
                                onLinkSkill(parentId, minInv)
                                onDismiss()
                            }
                        },
                        enabled = selectedParentId != null && minInvestment.toIntOrNull() != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Verknüpfen")
                    }
                }
            }
        }
    }
}
