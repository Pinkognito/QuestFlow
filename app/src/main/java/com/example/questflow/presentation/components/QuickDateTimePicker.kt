package com.example.questflow.presentation.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Quick Date/Time Picker inspired by Google Calendar
 * Combines date + time selection with direct text input
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDateTimePicker(
    label: String,
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var dateText by remember(dateTime) {
        mutableStateOf(dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
    }
    var timeText by remember(dateTime) {
        mutableStateOf(dateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date Field
            OutlinedTextField(
                value = dateText,
                onValueChange = { input ->
                    dateText = input
                    // Try to parse date
                    try {
                        val parts = input.split(".")
                        if (parts.size == 3) {
                            val day = parts[0].toIntOrNull()
                            val month = parts[1].toIntOrNull()
                            val year = parts[2].toIntOrNull()
                            if (day != null && month != null && year != null) {
                                val newDate = LocalDate.of(year, month, day)
                                onDateTimeChange(LocalDateTime.of(newDate, dateTime.toLocalTime()))
                            }
                        }
                    } catch (e: Exception) {
                        // Invalid input - ignore
                    }
                },
                label = { Text("Datum") },
                placeholder = { Text("TT.MM.JJJJ") },
                leadingIcon = {
                    IconButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                                dateText = newDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                onDateTimeChange(LocalDateTime.of(newDate, dateTime.toLocalTime()))
                            },
                            dateTime.year,
                            dateTime.monthValue - 1,
                            dateTime.dayOfMonth
                        ).show()
                    }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Datum wählen")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Time Field
            OutlinedTextField(
                value = timeText,
                onValueChange = { input ->
                    timeText = input
                    // Try to parse time
                    try {
                        val parts = input.split(":")
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull()
                            val minute = parts[1].toIntOrNull()
                            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                                val newTime = LocalTime.of(hour, minute)
                                onDateTimeChange(LocalDateTime.of(dateTime.toLocalDate(), newTime))
                            }
                        }
                    } catch (e: Exception) {
                        // Invalid input - ignore
                    }
                },
                label = { Text("Uhrzeit") },
                placeholder = { Text("HH:MM") },
                leadingIcon = {
                    IconButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val newTime = LocalTime.of(hourOfDay, minute)
                                timeText = newTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                                onDateTimeChange(LocalDateTime.of(dateTime.toLocalDate(), newTime))
                            },
                            dateTime.hour,
                            dateTime.minute,
                            true
                        ).show()
                    }) {
                        Icon(Icons.Default.Star, contentDescription = "Uhrzeit wählen")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = {
                    val now = LocalDateTime.now().plusHours(1).withMinute(0)
                    dateText = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"))
                    onDateTimeChange(now)
                },
                label = { Text("In 1h", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = false,
                onClick = {
                    val tomorrow = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
                    dateText = tomorrow.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    timeText = tomorrow.format(DateTimeFormatter.ofPattern("HH:mm"))
                    onDateTimeChange(tomorrow)
                },
                label = { Text("Morgen 9:00", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = false,
                onClick = {
                    val nextWeek = LocalDateTime.now().plusWeeks(1).withHour(9).withMinute(0)
                    dateText = nextWeek.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    timeText = nextWeek.format(DateTimeFormatter.ofPattern("HH:mm"))
                    onDateTimeChange(nextWeek)
                },
                label = { Text("Nächste Woche", style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
