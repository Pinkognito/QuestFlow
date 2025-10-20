package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.absoluteValue

/**
 * Unified Time Picker Component (P2-002)
 *
 * Wiederverwendbarer Time-Picker mit zwei Modi:
 * - Als Button mit Dialog (für DateTime-Kombinationen)
 * - Als direktes Inline-Widget (für Zeit-only Auswahl)
 *
 * Features:
 * - Horizontal Wheel mit vertikalem Scroll
 * - HHMM Quick-Input Feld
 * - Einzelne Stunden/Minuten Direkteingabe
 * - Infinite Circular Scroll
 * - Fade-Out Effekt für Seiten-Werte
 */

@Composable
fun TimePickerUnified(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    showAsButton: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showAsButton) {
        // Button Mode: Click to open dialog
        Column(modifier = modifier) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(String.format("%02d:%02d", time.hour, time.minute))
            }
        }

        if (showDialog) {
            TimePickerUnifiedDialog(
                initialTime = time,
                onTimeSelected = { newTime ->
                    onTimeChange(newTime)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    } else {
        // Inline Mode: Direct widget
        Column(modifier = modifier) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            HorizontalWheelTimePicker(
                initialHour = time.hour,
                initialMinute = time.minute,
                onTimeChange = { hour, minute ->
                    onTimeChange(LocalTime.of(hour, minute))
                }
            )
        }
    }
}

@Composable
fun TimePickerUnifiedDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uhrzeit wählen") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalWheelTimePicker(
                    initialHour = initialTime.hour,
                    initialMinute = initialTime.minute,
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
                    onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
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
 * Horizontal Wheel Time Picker mit vertikalem Scroll
 * - HHMM Feld oben für schnelle Eingabe der gesamten Zeit
 * - Swipe up/down zum Drehen des horizontalen Wheels
 * - Zentrierter Wert ist hervorgehoben und klickbar für Direkteingabe
 * - Seiten-Werte werden ausgeblendet
 * - Infinite Circular Scroll
 */
@Composable
fun HorizontalWheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentHour by remember { mutableStateOf(initialHour) }
    var currentMinute by remember { mutableStateOf(initialMinute) }
    var showHHMMInput by remember { mutableStateOf(false) }
    var showHourInput by remember { mutableStateOf(false) }
    var showMinuteInput by remember { mutableStateOf(false) }

    // Key to force wheel recomposition when direct input is confirmed
    var wheelKey by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // HHMM Display (clickable for full time input)
        Surface(
            modifier = Modifier
                .clickable { showHHMMInput = true }
                .padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = String.format("%02d:%02d", currentHour, currentMinute),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }

        // Horizontal Wheel: Hours and Minutes side by side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hours Wheel
            TimeWheelColumn(
                key = "$wheelKey-hour",
                value = currentHour,
                range = 0..23,
                onValueChange = {
                    currentHour = it
                    onTimeChange(currentHour, currentMinute)
                },
                onValueClick = { showHourInput = true },
                modifier = Modifier.weight(1f)
            )

            // Colon Separator
            Text(
                text = ":",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minutes Wheel
            TimeWheelColumn(
                key = "$wheelKey-minute",
                value = currentMinute,
                range = 0..59,
                onValueChange = {
                    currentMinute = it
                    onTimeChange(currentHour, currentMinute)
                },
                onValueClick = { showMinuteInput = true },
                modifier = Modifier.weight(1f)
            )
        }

        // Helper Text
        Text(
            text = "Swipe oder klicke zum Ändern",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // HHMM Input Dialog
    if (showHHMMInput) {
        DirectTimeInputDialog(
            title = "Uhrzeit eingeben (HHMM)",
            placeholder = "z.B. 1430 für 14:30",
            initialValue = String.format("%02d%02d", currentHour, currentMinute),
            onConfirm = { input ->
                if (input.length == 4) {
                    val hour = input.substring(0, 2).toIntOrNull()
                    val minute = input.substring(2, 4).toIntOrNull()
                    if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                        currentHour = hour
                        currentMinute = minute
                        onTimeChange(currentHour, currentMinute)
                        wheelKey++
                    }
                }
                showHHMMInput = false
            },
            onDismiss = { showHHMMInput = false },
            maxLength = 4
        )
    }

    // Hour Input Dialog
    if (showHourInput) {
        DirectTimeInputDialog(
            title = "Stunde eingeben (0-23)",
            placeholder = "z.B. 14",
            initialValue = currentHour.toString(),
            onConfirm = { input ->
                val hour = input.toIntOrNull()
                if (hour != null && hour in 0..23) {
                    currentHour = hour
                    onTimeChange(currentHour, currentMinute)
                    wheelKey++
                }
                showHourInput = false
            },
            onDismiss = { showHourInput = false },
            maxLength = 2
        )
    }

    // Minute Input Dialog
    if (showMinuteInput) {
        DirectTimeInputDialog(
            title = "Minute eingeben (0-59)",
            placeholder = "z.B. 30",
            initialValue = currentMinute.toString(),
            onConfirm = { input ->
                val minute = input.toIntOrNull()
                if (minute != null && minute in 0..59) {
                    currentMinute = minute
                    onTimeChange(currentHour, currentMinute)
                    wheelKey++
                }
                showMinuteInput = false
            },
            onDismiss = { showMinuteInput = false },
            maxLength = 2
        )
    }
}

@Composable
private fun TimeWheelColumn(
    key: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = value + 1000 * range.count()
    )
    val coroutineScope = rememberCoroutineScope()

    // Snap to nearest value on scroll end
    LaunchedEffect(key, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex + 1
            val centerValue = centerIndex % range.count()

            if (centerValue != value) {
                onValueChange(centerValue)
            }

            // Snap to center
            coroutineScope.launch {
                listState.animateScrollBy((listState.firstVisibleItemScrollOffset).toFloat() * -1f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            userScrollEnabled = true
        ) {
            items(count = Int.MAX_VALUE) { index ->
                val itemValue = index % range.count()
                val layoutInfo = listState.layoutInfo
                val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                val distanceFromCenter = itemInfo?.let {
                    val itemCenter = it.offset + it.size / 2
                    (itemCenter - viewportCenter).toFloat().absoluteValue
                } ?: Float.MAX_VALUE

                val maxDistance = 200f
                val alpha = 1f - (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                val scale = 0.6f + (0.4f * alpha)
                val isCenter = distanceFromCenter < 50f

                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .clickable(enabled = isCenter) { onValueClick() }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", itemValue),
                        style = if (isCenter) {
                            MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp
                            )
                        } else {
                            MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp)
                        },
                        color = if (isCenter) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectTimeInputDialog(
    title: String,
    placeholder: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    maxLength: Int
) {
    var inputValue by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { if (it.length <= maxLength) inputValue = it },
                placeholder = { Text(placeholder) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onConfirm(inputValue) }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(inputValue) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
