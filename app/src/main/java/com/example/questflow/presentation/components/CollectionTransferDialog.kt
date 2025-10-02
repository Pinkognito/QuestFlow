package com.example.questflow.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.CategoryEntity

/**
 * Dialog to transfer selected media to collection
 *
 * @param mediaCount Number of selected media items
 * @param categories Available categories
 * @param onDismiss Called when dialog is dismissed
 * @param onConfirm Called with selectedCategoryId (null = global) and metadata
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionTransferDialog(
    mediaCount: Int,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long?, name: String, description: String, rarity: String) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var selectedRarity by remember { mutableStateOf("COMMON") }

    val rarities = listOf(
        "COMMON" to "Gewöhnlich",
        "UNCOMMON" to "Ungewöhnlich",
        "RARE" to "Selten",
        "EPIC" to "Episch",
        "LEGENDARY" to "Legendär"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        title = { Text("$mediaCount ${if (mediaCount == 1) "Datei" else "Dateien"} zur Collection hinzufügen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Selection
                Text(
                    text = "Kategorie wählen:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Global option
                    CategoryOption(
                        emoji = "🌍",
                        name = "Global",
                        isSelected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null }
                    )

                    // Category options
                    categories.forEach { category ->
                        CategoryOption(
                            emoji = category.emoji,
                            name = category.name,
                            isSelected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id }
                        )
                    }
                }

                Divider()

                // Item Name
                Text(
                    text = "Name des Collection-Items:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("z.B. \"Mein Bild\"") },
                    singleLine = true
                )

                // Item Description
                Text(
                    text = "Beschreibung (optional):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = itemDescription,
                    onValueChange = { itemDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("z.B. \"Ein besonderes Foto\"") },
                    minLines = 2,
                    maxLines = 3
                )

                // Rarity Selection
                Text(
                    text = "Seltenheit:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rarities.forEach { (rarityKey, rarityLabel) ->
                        RarityOption(
                            rarityKey = rarityKey,
                            rarityLabel = rarityLabel,
                            isSelected = selectedRarity == rarityKey,
                            onClick = { selectedRarity = rarityKey }
                        )
                    }
                }

                // Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Die $mediaCount ${if (mediaCount == 1) "Datei" else "Dateien"} ${if (mediaCount == 1) "wird" else "werden"} als Collection-${if (mediaCount == 1) "Item" else "Items"} hinzugefügt.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCategoryId, itemName.trim(), itemDescription.trim(), selectedRarity)
                },
                enabled = itemName.isNotBlank()
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun CategoryOption(
    emoji: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Text(name, style = MaterialTheme.typography.bodyMedium)
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Ausgewählt",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RarityOption(
    rarityKey: String,
    rarityLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rarityColor = when (rarityKey) {
        "COMMON" -> MaterialTheme.colorScheme.surfaceVariant
        "UNCOMMON" -> MaterialTheme.colorScheme.secondaryContainer
        "RARE" -> MaterialTheme.colorScheme.tertiaryContainer
        "EPIC" -> MaterialTheme.colorScheme.primaryContainer
        "LEGENDARY" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = rarityColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(rarityLabel, style = MaterialTheme.typography.bodyMedium)
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Ausgewählt",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
