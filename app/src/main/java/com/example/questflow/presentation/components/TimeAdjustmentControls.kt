package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

/**
 * Time Adjustment Controls Component
 * Provides +/- buttons with configurable increment for quick time adjustments
 * - User can set increment value (persisted)
 * - Separate controls for date (days) and time (minutes)
 * - Option to calculate end time from start + duration
 */

@Composable
fun TimeAdjustmentControls(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    label: String = "Zeit",
    modifier: Modifier = Modifier,
    savedMinuteIncrement: Int = 15,  // Persisted user preference
    savedDayIncrement: Int = 1,      // Persisted user preference
    onMinuteIncrementChange: ((Int) -> Unit)? = null,
    onDayIncrementChange: ((Int) -> Unit)? = null
) {
    var minuteIncrement by remember { mutableStateOf(savedMinuteIncrement) }
    var dayIncrement by remember { mutableStateOf(savedDayIncrement) }
    var showMinuteDialog by remember { mutableStateOf(false) }
    var showDayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        // Date/Time Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date section
            OutlinedCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Datum",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Time section
            OutlinedCard(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Uhrzeit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Time Adjustment Controls (Minutes)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Minuten",
                        style = MaterialTheme.typography.labelMedium
                    )
                    TextButton(
                        onClick = { showMinuteDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("±$minuteIncrement min", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            onDateTimeChange(dateTime.minusMinutes(minuteIncrement.toLong()))
                        }
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }

                    Text(
                        "${dateTime.hour}:${String.format("%02d", dateTime.minute)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    FilledTonalIconButton(
                        onClick = {
                            onDateTimeChange(dateTime.plusMinutes(minuteIncrement.toLong()))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plus $minuteIncrement Minuten")
                    }
                }
            }
        }

        // Date Adjustment Controls (Days)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tage",
                        style = MaterialTheme.typography.labelMedium
                    )
                    TextButton(
                        onClick = { showDayDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("±$dayIncrement Tag(e)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            onDateTimeChange(dateTime.minusDays(dayIncrement.toLong()))
                        }
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }

                    Text(
                        dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FilledTonalIconButton(
                        onClick = {
                            onDateTimeChange(dateTime.plusDays(dayIncrement.toLong()))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plus $dayIncrement Tag(e)")
                    }
                }
            }
        }
    }

    // Minute Increment Dialog
    if (showMinuteDialog) {
        IncrementDialog(
            title = "Minuten-Inkrement",
            label = "Minuten",
            currentValue = minuteIncrement,
            onDismiss = { showMinuteDialog = false },
            onConfirm = { newValue ->
                minuteIncrement = newValue
                onMinuteIncrementChange?.invoke(newValue)
                showMinuteDialog = false
            },
            suggestions = listOf(1, 5, 10, 15, 30, 60, 120)
        )
    }

    // Day Increment Dialog
    if (showDayDialog) {
        IncrementDialog(
            title = "Tage-Inkrement",
            label = "Tage",
            currentValue = dayIncrement,
            onDismiss = { showDayDialog = false },
            onConfirm = { newValue ->
                dayIncrement = newValue
                onDayIncrementChange?.invoke(newValue)
                showDayDialog = false
            },
            suggestions = listOf(1, 2, 3, 7, 14, 30)
        )
    }
}

/**
 * Dialog for setting time increment values
 */
@Composable
private fun IncrementDialog(
    title: String,
    label: String,
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    suggestions: List<Int>
) {
    var valueText by remember { mutableStateOf(currentValue.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Aktuell: $currentValue $label",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = valueText,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            valueText = it
                            errorMessage = null
                        }
                    },
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Quick suggestions
                Text(
                    "Schnellauswahl:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { value ->
                                FilterChip(
                                    selected = valueText == value.toString(),
                                    onClick = { valueText = value.toString() },
                                    label = { Text(value.toString()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if row is incomplete
                            repeat(4 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = valueText.toIntOrNull()
                    if (value != null && value > 0) {
                        onConfirm(value)
                    } else {
                        errorMessage = "Ungültiger Wert (muss > 0 sein)"
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Duration Calculator Component
 * Allows setting end time based on start time + duration
 */
@Composable
fun DurationCalculator(
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    onEndDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var durationMinutes by remember { mutableStateOf(15) }
    var showDurationDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Dauer-Berechnung",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ende = Start + $durationMinutes min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { showDurationDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Ändern", style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(
                onClick = {
                    onEndDateTimeChange(startDateTime.plusMinutes(durationMinutes.toLong()))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ende auf Start + $durationMinutes min setzen")
            }

            // Show calculated result
            val calculatedEnd = startDateTime.plusMinutes(durationMinutes.toLong())
            Text(
                "Ergebnis: ${calculatedEnd.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    // Duration Dialog
    if (showDurationDialog) {
        IncrementDialog(
            title = "Dauer festlegen",
            label = "Minuten",
            currentValue = durationMinutes,
            onDismiss = { showDurationDialog = false },
            onConfirm = { newValue ->
                durationMinutes = newValue
                showDurationDialog = false
            },
            suggestions = listOf(15, 30, 60, 90, 120, 180, 240)
        )
    }
}
