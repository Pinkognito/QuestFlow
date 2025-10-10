package com.example.questflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Color Picker Dialog mit vordefinierter Palette und Custom-Hex-Eingabe
 *
 * @param currentColor Aktuell ausgewählte Farbe (Hex String z.B. "#FF5722")
 * @param onColorSelected Callback mit ausgewählter Farbe (Hex String)
 * @param onDismiss Dialog schließen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    currentColor: String? = null,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColorHex by remember { mutableStateOf(currentColor ?: "#FF5722") }
    var customHexInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Farbe auswählen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Aktuelle Auswahl Vorschau
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(parseColor(selectedColorHex))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        selectedColorHex.uppercase(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Divider()

                // Vordefinierte Farben
                Text("Farb-Palette", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(predefinedColors) { colorHex ->
                        ColorItem(
                            colorHex = colorHex,
                            isSelected = colorHex.equals(selectedColorHex, ignoreCase = true),
                            onClick = { selectedColorHex = colorHex }
                        )
                    }
                }

                Divider()

                // Custom Hex Input
                Text("Eigene Farbe (Hex)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = { customHexInput = it },
                        label = { Text("Hex-Code") },
                        placeholder = { Text("z.B. #FF5722") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val normalized = normalizeHexColor(customHexInput)
                            if (normalized != null) {
                                selectedColorHex = normalized
                                customHexInput = ""
                            }
                        },
                        enabled = normalizeHexColor(customHexInput) != null
                    ) {
                        Text("OK")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onColorSelected(selectedColorHex)
                onDismiss()
            }) {
                Text("Auswählen")
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
private fun ColorItem(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(parseColor(colorHex))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Ausgewählt",
                tint = getContrastColor(parseColor(colorHex)),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Parst Hex-Farbe zu Compose Color
 */
private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16).toInt()
        Color(colorInt or 0xFF000000.toInt())
    } catch (e: Exception) {
        Color.Gray
    }
}

/**
 * Normalisiert Hex-Eingabe (z.B. "FF5722" → "#FF5722")
 */
private fun normalizeHexColor(input: String): String? {
    val clean = input.trim().removePrefix("#").uppercase()
    return when (clean.length) {
        6 -> if (clean.all { it in '0'..'9' || it in 'A'..'F' }) "#$clean" else null
        3 -> {
            // Kurze Form: FFF → FFFFFF
            if (clean.all { it in '0'..'9' || it in 'A'..'F' }) {
                "#${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            } else null
        }
        else -> null
    }
}

/**
 * Berechnet Kontrast-Farbe für Text (Schwarz oder Weiß)
 */
private fun getContrastColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}

/**
 * Vordefinierte Material Design Farben
 */
private val predefinedColors = listOf(
    // Reds
    "#F44336", "#E91E63", "#C2185B",
    // Purples
    "#9C27B0", "#673AB7", "#3F51B5",
    // Blues
    "#2196F3", "#03A9F4", "#00BCD4",
    // Cyans & Teals
    "#009688", "#4CAF50", "#8BC34A",
    // Greens & Limes
    "#CDDC39", "#FFEB3B", "#FFC107",
    // Ambers & Oranges
    "#FF9800", "#FF5722", "#795548",
    // Browns & Greys
    "#9E9E9E", "#607D8B", "#000000",
    // Whites
    "#FFFFFF", "#ECEFF1", "#CFD8DC"
)
