package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Dialog for creating/editing selection box time range
 */
@Composable
fun SelectionBoxDialog(
    initialStart: LocalDateTime? = null,
    initialEnd: LocalDateTime? = null,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime, LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var startDate by remember { mutableStateOf(initialStart?.toLocalDate() ?: LocalDate.now()) }
    var startTime by remember { mutableStateOf(initialStart?.toLocalTime() ?: LocalTime.of(8, 0)) }
    var endDate by remember { mutableStateOf(initialEnd?.toLocalDate() ?: LocalDate.now()) }
    var endTime by remember { mutableStateOf(initialEnd?.toLocalTime() ?: LocalTime.of(17, 0)) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Zeitbereich festlegen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Wähle Start- und Endzeit für den Auswahlbereich",
                    style = MaterialTheme.typography.bodyMedium
                )

                Divider()

                // Start DateTime
                Text(
                    text = "Start",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${startDate.dayOfMonth}.${startDate.monthValue}.${startDate.year}")
                    }

                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(String.format("%02d:%02d", startTime.hour, startTime.minute))
                    }
                }

                Divider()

                // End DateTime
                Text(
                    text = "Ende",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${endDate.dayOfMonth}.${endDate.monthValue}.${endDate.year}")
                    }

                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(String.format("%02d:%02d", endTime.hour, endTime.minute))
                    }
                }

                // Duration info
                val startDateTime = LocalDateTime.of(startDate, startTime)
                val endDateTime = LocalDateTime.of(endDate, endTime)
                val durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startDateTime, endDateTime)

                if (durationMinutes > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Dauer: ${durationMinutes}min (${durationMinutes / 60}h ${durationMinutes % 60}min)",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ Endzeit muss nach Startzeit liegen",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            val startDateTime = LocalDateTime.of(startDate, startTime)
            val endDateTime = LocalDateTime.of(endDate, endTime)
            val isValid = startDateTime.isBefore(endDateTime)

            Button(
                onClick = { onConfirm(startDateTime, endDateTime) },
                enabled = isValid
            ) {
                Text("Bestätigen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        modifier = modifier
    )

    // Date/Time Pickers
    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = {
                startDate = it
                showStartDatePicker = false
            }
        )
    }

    if (showStartTimePicker) {
        SimpleTimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startTime = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = {
                endDate = it
                showEndDatePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        SimpleTimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = it
                showEndTimePicker = false
            }
        )
    }
}

/**
 * Simple date picker dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    onConfirm(date)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Simple time picker dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
