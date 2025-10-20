package com.example.questflow.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

/**
 * Modern Date/Time Picker inspired by Google Calendar
 * - DatePicker: Swipeable months with day selection
 * - TimePicker: Scroll wheels for hours and minutes (slot machine style)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDateTimePicker(
    label: String,
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

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
            // Date Button
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            }

            // Time Button
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(dateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
        }
    }

    // Date Picker Dialog - Auto-closes on selection
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate().toEpochDay() * 24 * 60 * 60 * 1000
        )

        // Watch for date selection and auto-close
        LaunchedEffect(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let { millis ->
                // Only process if it's different from initial
                val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                if (selectedDate != dateTime.toLocalDate()) {
                    onDateTimeChange(LocalDateTime.of(selectedDate, dateTime.toLocalTime()))
                    showDatePicker = false
                }
            }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            },
            dismissButton = {}
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Time Picker Dialog with Horizontal Wheel (vertical scroll)
    if (showTimePicker) {
        var selectedHour by remember { mutableStateOf(dateTime.hour) }
        var selectedMinute by remember { mutableStateOf(dateTime.minute) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Uhrzeit wählen") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalWheelTimePicker(
                        initialHour = dateTime.hour,
                        initialMinute = dateTime.minute,
                        onTimeChange = { hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(selectedHour, selectedMinute)
                        onDateTimeChange(LocalDateTime.of(dateTime.toLocalDate(), newTime))
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

