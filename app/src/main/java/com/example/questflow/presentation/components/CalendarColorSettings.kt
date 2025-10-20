package com.example.questflow.presentation.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.SimpleCalendarColorConfig
import com.example.questflow.domain.model.SimpleCalendarColorRepository

/**
 * Calendar Color Settings Component
 * Allows customization of the 5 calendar colors
 */
@Composable
fun CalendarColorSettings(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("calendar_colors", Context.MODE_PRIVATE)
    }
    val repository = remember { SimpleCalendarColorRepository(prefs) }

    var colorConfig by remember { mutableStateOf(repository.loadConfig()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var editingColor by remember { mutableStateOf<ColorType?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    text = "Kalender-Farben",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = {
                        colorConfig = SimpleCalendarColorConfig.default()
                        repository.saveConfig(colorConfig)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Zurücksetzen"
                    )
                }
            }

            HorizontalDivider()

            // Own Task Color
            ColorSetting(
                label = "Eigener Task",
                description = "Der aktuell geöffnete Task",
                colorHex = colorConfig.ownTaskColor,
                onClick = {
                    editingColor = ColorType.OWN_TASK
                    showColorPicker = true
                }
            )

            // Same Category Color
            ColorSetting(
                label = "Gleiche Kategorie",
                description = "Tasks aus der gleichen Kategorie",
                colorHex = colorConfig.sameCategoryColor,
                onClick = {
                    editingColor = ColorType.SAME_CATEGORY
                    showColorPicker = true
                }
            )

            // Other Task Color
            ColorSetting(
                label = "Andere Tasks",
                description = "Eigene Tasks aus anderen Kategorien",
                colorHex = colorConfig.otherTaskColor,
                onClick = {
                    editingColor = ColorType.OTHER_TASK
                    showColorPicker = true
                }
            )

            // External Event Color
            ColorSetting(
                label = "Google Calendar",
                description = "Externe Google Calendar Events",
                colorHex = colorConfig.externalEventColor,
                onClick = {
                    editingColor = ColorType.EXTERNAL_EVENT
                    showColorPicker = true
                }
            )

            // Overlap Color
            ColorSetting(
                label = "Überlappung",
                description = "Konflikte zwischen Tasks/Events",
                colorHex = colorConfig.overlapColor,
                onClick = {
                    editingColor = ColorType.OVERLAP
                    showColorPicker = true
                }
            )
        }
    }

    // Color Picker Dialog
    if (showColorPicker && editingColor != null) {
        val currentColor = when (editingColor) {
            ColorType.OWN_TASK -> colorConfig.ownTaskColor
            ColorType.SAME_CATEGORY -> colorConfig.sameCategoryColor
            ColorType.OTHER_TASK -> colorConfig.otherTaskColor
            ColorType.EXTERNAL_EVENT -> colorConfig.externalEventColor
            ColorType.OVERLAP -> colorConfig.overlapColor
            null -> "#FFFFFF"
        }

        ColorPickerDialog(
            currentColor = currentColor,
            onColorSelected = { newColor ->
                colorConfig = when (editingColor) {
                    ColorType.OWN_TASK -> colorConfig.copy(ownTaskColor = newColor)
                    ColorType.SAME_CATEGORY -> colorConfig.copy(sameCategoryColor = newColor)
                    ColorType.OTHER_TASK -> colorConfig.copy(otherTaskColor = newColor)
                    ColorType.EXTERNAL_EVENT -> colorConfig.copy(externalEventColor = newColor)
                    ColorType.OVERLAP -> colorConfig.copy(overlapColor = newColor)
                    null -> colorConfig
                }
                repository.saveConfig(colorConfig)
            },
            onDismiss = {
                showColorPicker = false
                editingColor = null
            }
        )
    }
}

@Composable
private fun ColorSetting(
    label: String,
    description: String,
    colorHex: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = colorHex.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(colorHex))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

private fun parseColorHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

private enum class ColorType {
    OWN_TASK,
    SAME_CATEGORY,
    OTHER_TASK,
    EXTERNAL_EVENT,
    OVERLAP
}
