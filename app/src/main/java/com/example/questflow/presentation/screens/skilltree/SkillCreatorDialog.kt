package com.example.questflow.presentation.screens.skilltree

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.database.entity.SkillEffectType
import java.io.File

data class SkillEffectTemplate(
    val type: SkillEffectType,
    val displayName: String,
    val description: String,
    val suggestedBaseValue: Float,
    val suggestedScaling: Float,
    val suggestedMaxInvestment: Int,
    val unit: String
)

val SKILL_EFFECT_TEMPLATES = listOf(
    SkillEffectTemplate(
        type = SkillEffectType.XP_MULTIPLIER,
        displayName = "XP Multiplikator (Global)",
        description = "Erhöht alle erhaltenen XP um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 3f,
        suggestedMaxInvestment = 10,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.TASK_XP_BONUS,
        displayName = "Task XP Bonus",
        description = "Erhöht XP von abgeschlossenen Tasks um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 2f,
        suggestedMaxInvestment = 10,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.CALENDAR_XP_BONUS,
        displayName = "Kalender XP Bonus",
        description = "Erhöht XP von Kalender-Events um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 3f,
        suggestedMaxInvestment = 10,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.SKILL_POINT_GAIN,
        displayName = "Skillpunkt Bonus",
        description = "Erhalte +X Skillpunkte pro Level-Up",
        suggestedBaseValue = 0f,
        suggestedScaling = 1f,
        suggestedMaxInvestment = 5,
        unit = " SP"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.RARE_COLLECTION_CHANCE,
        displayName = "Seltene Items Chance",
        description = "Erhöht die Chance auf seltenere Collection-Items um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 5f,
        suggestedMaxInvestment = 8,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.EXTRA_COLLECTION_UNLOCK,
        displayName = "Extra Collection-Item",
        description = "Schaltet ein zusätzliches Collection-Item pro Level-Up frei",
        suggestedBaseValue = 1f,
        suggestedScaling = 0f,
        suggestedMaxInvestment = 1,
        unit = ""
    ),
    SkillEffectTemplate(
        type = SkillEffectType.COLLECTION_SLOT_INCREASE,
        displayName = "Collection-Slots",
        description = "Erhöht die Anzahl der Collection-Slots um X",
        suggestedBaseValue = 0f,
        suggestedScaling = 1f,
        suggestedMaxInvestment = 10,
        unit = " Slots"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.STREAK_PROTECTION,
        displayName = "Streak Schutz",
        description = "Schützt deinen Daily-Streak vor Verlust",
        suggestedBaseValue = 1f,
        suggestedScaling = 0f,
        suggestedMaxInvestment = 1,
        unit = ""
    ),
    SkillEffectTemplate(
        type = SkillEffectType.STREAK_XP_MULTIPLIER,
        displayName = "Streak XP Multiplikator",
        description = "Erhöht XP basierend auf deinem Streak um X% pro Streak-Tag",
        suggestedBaseValue = 0f,
        suggestedScaling = 0.5f,
        suggestedMaxInvestment = 10,
        unit = "%/Tag"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.TRIVIAL_TASK_BONUS,
        displayName = "Trivial Task Bonus",
        description = "Erhöht XP für Trivial-Tasks um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 5f,
        suggestedMaxInvestment = 8,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.EASY_TASK_BONUS,
        displayName = "Easy Task Bonus",
        description = "Erhöht XP für Easy-Tasks um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 4f,
        suggestedMaxInvestment = 8,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.MEDIUM_TASK_BONUS,
        displayName = "Medium Task Bonus",
        description = "Erhöht XP für Medium-Tasks um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 3f,
        suggestedMaxInvestment = 8,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.HARD_TASK_BONUS,
        displayName = "Hard Task Bonus",
        description = "Erhöht XP für Hard-Tasks um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 2f,
        suggestedMaxInvestment = 8,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.EPIC_TASK_BONUS,
        displayName = "Epic Task Bonus",
        description = "Erhöht XP für Epic-Tasks um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 2f,
        suggestedMaxInvestment = 10,
        unit = "%"
    ),
    SkillEffectTemplate(
        type = SkillEffectType.CATEGORY_XP_BOOST,
        displayName = "Kategorie XP Boost",
        description = "Erhöht XP für diese Kategorie um X%",
        suggestedBaseValue = 0f,
        suggestedScaling = 5f,
        suggestedMaxInvestment = 10,
        unit = "%"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillCreatorDialog(
    availableSkills: List<com.example.questflow.data.repository.SkillNodeWithStatus> = emptyList(),
    onDismiss: () -> Unit,
    onCreateSkill: (
        title: String,
        description: String,
        effectType: SkillEffectType,
        baseValue: Float,
        scalingPerPoint: Float,
        maxInvestment: Int,
        colorHex: String,
        parentSkills: List<Pair<String, Int>> // List of (parentId, minInvestment)
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<SkillEffectTemplate?>(null) }
    var showEffectPicker by remember { mutableStateOf(false) }

    var baseValue by remember { mutableStateOf("0") }
    var scalingPerPoint by remember { mutableStateOf("3") }
    var maxInvestment by remember { mutableStateOf("10") }
    var colorHex by remember { mutableStateOf("#FFD700") }

    // Parent skills management
    var showParentPicker by remember { mutableStateOf(false) }
    var selectedParents by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) } // (parentId, minInvestment)

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
                    text = "Neuer Skill",
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
                        placeholder = { Text("z.B. Schneller Denker") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Beschreibung") },
                        placeholder = { Text("Was macht dieser Skill?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(Modifier.height(16.dp))

                    // Effect Picker Button
                    OutlinedButton(
                        onClick = { showEffectPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedTemplate?.displayName ?: "Effekt auswählen"
                        )
                    }

                    // Show effect details if selected
                    selectedTemplate?.let { template ->
                        Spacer(Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodyMedium,
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

                    // Parent Skills Section
                    if (availableSkills.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showParentPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (selectedParents.isEmpty())
                                    "Voraussetzungen hinzufügen (optional)"
                                else
                                    "${selectedParents.size} Voraussetzung(en)"
                            )
                        }

                        // Show selected parents
                        if (selectedParents.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Voraussetzungen:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    selectedParents.forEach { (parentId, minInv) ->
                                        val parentSkill = availableSkills.find { it.node.id == parentId }
                                        parentSkill?.let {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${it.node.title} (min. $minInv Punkte)",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                IconButton(
                                                    onClick = {
                                                        selectedParents = selectedParents.filter { p -> p.first != parentId }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Entfernen",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }

                    // Color & Icon Picker
                    Text(
                        text = "Aussehen",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    ColorPickerField(
                        colorHex = colorHex,
                        onColorChange = { colorHex = it },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                            if (title.isNotBlank() && selectedTemplate != null) {
                                onCreateSkill(
                                    title,
                                    description,
                                    selectedTemplate!!.type,
                                    baseValue.toFloatOrNull() ?: 0f,
                                    scalingPerPoint.toFloatOrNull() ?: 0f,
                                    maxInvestment.toIntOrNull() ?: 1,
                                    colorHex,
                                    selectedParents
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && selectedTemplate != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Erstellen")
                    }
                }
            }
        }
    }

    // Effect Picker Dialog
    if (showEffectPicker) {
        Dialog(onDismissRequest = { showEffectPicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                Column {
                    Text(
                        text = "Skill-Effekt wählen",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )

                    LazyColumn {
                        items(SKILL_EFFECT_TEMPLATES) { template ->
                            Card(
                                onClick = {
                                    selectedTemplate = template
                                    baseValue = template.suggestedBaseValue.toString()
                                    scalingPerPoint = template.suggestedScaling.toString()
                                    maxInvestment = template.suggestedMaxInvestment.toString()
                                    showEffectPicker = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTemplate == template)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = template.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = template.description,
                                        style = MaterialTheme.typography.bodySmall,
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

    // Parent Picker Dialog
    if (showParentPicker) {
        ParentSkillPickerDialog(
            availableSkills = availableSkills,
            selectedParents = selectedParents,
            onDismiss = { showParentPicker = false },
            onConfirm = { newParents ->
                selectedParents = newParents
                showParentPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSkillPickerDialog(
    availableSkills: List<com.example.questflow.data.repository.SkillNodeWithStatus>,
    selectedParents: List<Pair<String, Int>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<String, Int>>) -> Unit
) {
    var tempSelectedParents by remember { mutableStateOf(selectedParents) }
    var editingParent by remember { mutableStateOf<String?>(null) }
    var editingMinInvestment by remember { mutableStateOf("1") }

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
                    text = "Voraussetzungen wählen",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Wähle Skills aus, die investiert sein müssen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Available skills list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableSkills) { skill ->
                        val isSelected = tempSelectedParents.any { it.first == skill.node.id }
                        val selectedPair = tempSelectedParents.find { it.first == skill.node.id }

                        Card(
                            onClick = {
                                if (isSelected) {
                                    // Remove
                                    tempSelectedParents = tempSelectedParents.filter { it.first != skill.node.id }
                                } else {
                                    // Add with default minInvestment = 1
                                    editingParent = skill.node.id
                                    editingMinInvestment = "1"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
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

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Ausgewählt",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Show min investment editor
                                if (isSelected && selectedPair != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Min. Punkte:",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        OutlinedTextField(
                                            value = selectedPair.second.toString(),
                                            onValueChange = { newValue ->
                                                val intValue = newValue.toIntOrNull()
                                                if (intValue != null && intValue in 1..skill.node.maxInvestment) {
                                                    tempSelectedParents = tempSelectedParents.map {
                                                        if (it.first == skill.node.id) {
                                                            it.first to intValue
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                }
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Text(
                                            text = "/ ${skill.node.maxInvestment}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Min Investment Dialog for new parent
                if (editingParent != null) {
                    val skill = availableSkills.find { it.node.id == editingParent }
                    skill?.let {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = "Minimale Investment-Stufe für ${it.node.title}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = editingMinInvestment,
                                    onValueChange = { editingMinInvestment = it },
                                    label = { Text("Min. Punkte") },
                                    suffix = { Text("/ ${it.node.maxInvestment}") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = { editingParent = null },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Abbrechen")
                                    }
                                    Button(
                                        onClick = {
                                            val minInv = editingMinInvestment.toIntOrNull()
                                            if (minInv != null && minInv in 1..it.node.maxInvestment) {
                                                tempSelectedParents = tempSelectedParents + (editingParent!! to minInv)
                                                editingParent = null
                                            }
                                        },
                                        enabled = editingMinInvestment.toIntOrNull()?.let { inv ->
                                            inv in 1..it.node.maxInvestment
                                        } ?: false,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Hinzufügen")
                                    }
                                }
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
                        onClick = { onConfirm(tempSelectedParents) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bestätigen")
                    }
                }
            }
        }
    }
}

// Predefined color palette
private val SKILL_COLORS = listOf(
    "#FFD700", // Gold
    "#FF6B6B", // Red
    "#4ECDC4", // Turquoise
    "#45B7D1", // Blue
    "#96CEB4", // Green
    "#FFEAA7", // Yellow
    "#DFE6E9", // Light Gray
    "#A29BFE", // Purple
    "#FD79A8", // Pink
    "#FDCB6E", // Orange
    "#E17055", // Dark Orange
    "#00B894", // Emerald
    "#0984E3", // Ocean Blue
    "#6C5CE7", // Violet
    "#B2BEC3"  // Gray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerField(
    colorHex: String,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var useCustomColor by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Color preview button
        OutlinedButton(
            onClick = { showColorPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(colorHex)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Edit, contentDescription = null)
        }

        // Color picker dialog
        if (showColorPicker) {
            Dialog(onDismissRequest = { showColorPicker = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Farbe wählen",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.height(16.dp))

                        // Predefined colors grid
                        Text(
                            text = "Vordefinierte Farben",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(SKILL_COLORS) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(color)))
                                        .border(
                                            width = if (colorHex.uppercase() == color.uppercase()) 3.dp else 1.dp,
                                            color = if (colorHex.uppercase() == color.uppercase())
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onColorChange(color)
                                            useCustomColor = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (colorHex.uppercase() == color.uppercase()) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Ausgewählt",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Custom color section
                        Text(
                            text = "Eigene Farbe",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = if (useCustomColor) colorHex else "",
                                onValueChange = {
                                    onColorChange(it)
                                    useCustomColor = true
                                },
                                label = { Text("Hex-Code") },
                                placeholder = { Text("#FFD700") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                try {
                                                    if (useCustomColor && colorHex.isNotEmpty()) {
                                                        Color(android.graphics.Color.parseColor(colorHex))
                                                    } else {
                                                        Color.Gray
                                                    }
                                                } catch (e: Exception) {
                                                    Color.Gray
                                                }
                                            )
                                    )
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showColorPicker = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Abbrechen")
                            }

                            Button(
                                onClick = { showColorPicker = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Fertig")
                            }
                        }
                    }
                }
            }
        }
    }
}
