package com.example.questflow.presentation.screens.skilltree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.repository.SkillNodeWithStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillEditorDialog(
    skill: SkillNodeWithStatus,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        baseValue: Float,
        scalingPerPoint: Float,
        maxInvestment: Int,
        colorHex: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(skill.node.title) }
    var description by remember { mutableStateOf(skill.node.description) }
    var baseValue by remember { mutableStateOf(skill.node.baseValue.toString()) }
    var scalingPerPoint by remember { mutableStateOf(skill.node.scalingPerPoint.toString()) }
    var maxInvestment by remember { mutableStateOf(skill.node.maxInvestment.toString()) }
    var colorHex by remember { mutableStateOf(skill.node.colorHex) }

    val effectTemplate = SKILL_EFFECT_TEMPLATES.find { it.type == skill.node.effectType }

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
                    text = "Skill bearbeiten",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Skill-Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Beschreibung") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(Modifier.height(16.dp))

                    // Effect type (read-only)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "Effekt-Typ (nicht änderbar)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = effectTemplate?.displayName ?: skill.node.effectType.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            effectTemplate?.let {
                                Text(
                                    text = it.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Values
                    effectTemplate?.let { template ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = "Werte anpassen",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(Modifier.height(12.dp))

                                // Base Value
                                OutlinedTextField(
                                    value = baseValue,
                                    onValueChange = { baseValue = it },
                                    label = { Text("Basis-Wert") },
                                    suffix = { Text(template.unit) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                // Scaling per Point
                                OutlinedTextField(
                                    value = scalingPerPoint,
                                    onValueChange = { scalingPerPoint = it },
                                    label = { Text("Skalierung pro Punkt") },
                                    suffix = { Text(template.unit) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                // Max Investment
                                OutlinedTextField(
                                    value = maxInvestment,
                                    onValueChange = { maxInvestment = it },
                                    label = { Text("Max. Investment") },
                                    suffix = { Text("Punkte") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Show preview calculation
                                val baseVal = baseValue.toFloatOrNull() ?: 0f
                                val scaling = scalingPerPoint.toFloatOrNull() ?: 0f
                                val maxInv = maxInvestment.toIntOrNull() ?: 1
                                val maxValue = baseVal + (scaling * maxInv)

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Bei Max. Investment: ${maxValue}${template.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Color Picker
                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { colorHex = it },
                        label = { Text("Farbe (Hex)") },
                        placeholder = { Text("#FFD700") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Warning if skill is invested
                    if (skill.currentInvestment > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Achtung: Dieser Skill wurde bereits ${skill.currentInvestment}x investiert. Änderungen an den Werten wirken sich sofort auf die Spielmechanik aus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
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
                            if (title.isNotBlank()) {
                                onSave(
                                    title,
                                    description,
                                    baseValue.toFloatOrNull() ?: 0f,
                                    scalingPerPoint.toFloatOrNull() ?: 0f,
                                    maxInvestment.toIntOrNull() ?: 1,
                                    colorHex
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Speichern")
                    }
                }
            }
        }
    }
}
