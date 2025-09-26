package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

data class RecurringConfig(
    val mode: RecurringMode = RecurringMode.DAILY,
    val dailyInterval: Int = 1,  // Every X days
    val weeklyDays: Set<DayOfWeek> = setOf(),  // Which weekdays
    val monthlyDay: Int = 1,  // Day of month
    val customMinutes: Int = 60,  // Custom interval in minutes
    val customHours: Int = 0,  // Custom interval in hours
    val specificTime: LocalTime? = null,  // Specific time for daily/weekly/monthly
    val triggerMode: TriggerMode = TriggerMode.FIXED_INTERVAL  // When to trigger
)

enum class RecurringMode {
    DAILY,      // Every X days at specific time
    WEEKLY,     // Specific weekdays at specific time
    MONTHLY,    // Specific day of month at specific time
    CUSTOM      // Every X hours/minutes from completion
}

enum class TriggerMode {
    FIXED_INTERVAL,     // Always repeat at fixed times (e.g., every Monday 14:00)
    AFTER_COMPLETION,   // Repeat X time after completion
    AFTER_EXPIRY       // Repeat X time after expiry (if not completed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringConfigDialog(
    initialConfig: RecurringConfig = RecurringConfig(),
    onDismiss: () -> Unit,
    onConfirm: (RecurringConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Wiederholung konfigurieren",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recurring Mode Selection
                item {
                    Text(
                        "Wiederholungsmodus",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurringModeCard(
                            title = "Täglich",
                            description = "Jeden Tag oder alle X Tage",
                            icon = Icons.Default.DateRange,
                            selected = config.mode == RecurringMode.DAILY,
                            onClick = { config = config.copy(mode = RecurringMode.DAILY) }
                        )

                        RecurringModeCard(
                            title = "Wöchentlich",
                            description = "An bestimmten Wochentagen",
                            icon = Icons.Default.DateRange,
                            selected = config.mode == RecurringMode.WEEKLY,
                            onClick = { config = config.copy(mode = RecurringMode.WEEKLY) }
                        )

                        RecurringModeCard(
                            title = "Monatlich",
                            description = "Am gleichen Tag jeden Monat",
                            icon = Icons.Default.DateRange,
                            selected = config.mode == RecurringMode.MONTHLY,
                            onClick = { config = config.copy(mode = RecurringMode.MONTHLY) }
                        )

                        RecurringModeCard(
                            title = "Benutzerdefiniert",
                            description = "Nach Stunden/Minuten Intervall",
                            icon = Icons.Default.Refresh,
                            selected = config.mode == RecurringMode.CUSTOM,
                            onClick = { config = config.copy(mode = RecurringMode.CUSTOM) }
                        )
                    }
                }

                // Mode-specific configuration
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    when (config.mode) {
                        RecurringMode.DAILY -> {
                            DailyConfig(
                                interval = config.dailyInterval,
                                time = config.specificTime,
                                onIntervalChange = { config = config.copy(dailyInterval = it) },
                                onTimeChange = { config = config.copy(specificTime = it) }
                            )
                        }

                        RecurringMode.WEEKLY -> {
                            WeeklyConfig(
                                selectedDays = config.weeklyDays,
                                time = config.specificTime,
                                onDaysChange = { config = config.copy(weeklyDays = it) },
                                onTimeChange = { config = config.copy(specificTime = it) }
                            )
                        }

                        RecurringMode.MONTHLY -> {
                            MonthlyConfig(
                                dayOfMonth = config.monthlyDay,
                                time = config.specificTime,
                                onDayChange = { config = config.copy(monthlyDay = it) },
                                onTimeChange = { config = config.copy(specificTime = it) }
                            )
                        }

                        RecurringMode.CUSTOM -> {
                            CustomConfig(
                                hours = config.customHours,
                                minutes = config.customMinutes,
                                onHoursChange = { config = config.copy(customHours = it) },
                                onMinutesChange = { config = config.copy(customMinutes = it) }
                            )
                        }
                    }
                }

                // Trigger Mode Selection
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Auslöse-Modus",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TriggerModeOption(
                            title = "Festes Intervall",
                            description = "Wiederholt sich immer zur gleichen Zeit",
                            selected = config.triggerMode == TriggerMode.FIXED_INTERVAL,
                            onClick = { config = config.copy(triggerMode = TriggerMode.FIXED_INTERVAL) }
                        )

                        if (config.mode == RecurringMode.CUSTOM) {
                            TriggerModeOption(
                                title = "Nach Abschluss",
                                description = "Startet neu nach Aufgaben-Abschluss",
                                selected = config.triggerMode == TriggerMode.AFTER_COMPLETION,
                                onClick = { config = config.copy(triggerMode = TriggerMode.AFTER_COMPLETION) }
                            )

                            TriggerModeOption(
                                title = "Nach Ablauf",
                                description = "Startet neu wenn abgelaufen (nicht abgeschlossen)",
                                selected = config.triggerMode == TriggerMode.AFTER_EXPIRY,
                                onClick = { config = config.copy(triggerMode = TriggerMode.AFTER_EXPIRY) }
                            )
                        }
                    }
                }

                // Summary
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "Zusammenfassung",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                getRecurringSummary(config),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(config) }
            ) {
                Text("Speichern")
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
private fun RecurringModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyConfig(
    interval: Int,
    time: LocalTime?,
    onIntervalChange: (Int) -> Unit,
    onTimeChange: (LocalTime?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Intervall", style = MaterialTheme.typography.labelMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Alle")

            OutlinedTextField(
                value = interval.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { value ->
                        if (value > 0) onIntervalChange(value)
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true
            )

            Text("Tag(e)")
        }

        // Time picker
        TimePickerField(
            time = time,
            onTimeChange = onTimeChange,
            label = "Uhrzeit (optional)"
        )
    }
}

@Composable
private fun WeeklyConfig(
    selectedDays: Set<DayOfWeek>,
    time: LocalTime?,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    onTimeChange: (LocalTime?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Wochentage", style = MaterialTheme.typography.labelMedium)

        // Weekday selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                DayOfWeek.MONDAY to "Mo",
                DayOfWeek.TUESDAY to "Di",
                DayOfWeek.WEDNESDAY to "Mi",
                DayOfWeek.THURSDAY to "Do"
            ).forEach { (day, label) ->
                WeekdayChip(
                    label = label,
                    selected = day in selectedDays,
                    onClick = {
                        onDaysChange(
                            if (day in selectedDays)
                                selectedDays - day
                            else
                                selectedDays + day
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                DayOfWeek.FRIDAY to "Fr",
                DayOfWeek.SATURDAY to "Sa",
                DayOfWeek.SUNDAY to "So"
            ).forEach { (day, label) ->
                WeekdayChip(
                    label = label,
                    selected = day in selectedDays,
                    onClick = {
                        onDaysChange(
                            if (day in selectedDays)
                                selectedDays - day
                            else
                                selectedDays + day
                        )
                    }
                )
            }
            // Add invisible spacer for alignment
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Time picker
        TimePickerField(
            time = time,
            onTimeChange = onTimeChange,
            label = "Uhrzeit (optional)"
        )
    }
}

@Composable
private fun WeekdayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = Modifier
            .width(52.dp)
            .height(40.dp)
    )
}

@Composable
private fun MonthlyConfig(
    dayOfMonth: Int,
    time: LocalTime?,
    onDayChange: (Int) -> Unit,
    onTimeChange: (LocalTime?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tag im Monat", style = MaterialTheme.typography.labelMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Am")

            OutlinedTextField(
                value = dayOfMonth.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { value ->
                        if (value in 1..31) onDayChange(value)
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true
            )

            Text("des Monats")
        }

        // Time picker
        TimePickerField(
            time = time,
            onTimeChange = onTimeChange,
            label = "Uhrzeit (optional)"
        )
    }
}

@Composable
private fun CustomConfig(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Intervall", style = MaterialTheme.typography.labelMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = if (hours == 0) "" else hours.toString(),
                onValueChange = { text ->
                    when {
                        text.isEmpty() -> onHoursChange(0)
                        text.all { it.isDigit() } -> {
                            text.toIntOrNull()?.let { value ->
                                if (value >= 0 && value < 1000) onHoursChange(value)
                            }
                        }
                    }
                },
                modifier = Modifier.width(80.dp),
                label = { Text("Std") },
                singleLine = true,
                placeholder = { Text("0") }
            )

            Text(":")

            OutlinedTextField(
                value = if (minutes == 0) "" else minutes.toString(),
                onValueChange = { text ->
                    when {
                        text.isEmpty() -> onMinutesChange(0)
                        text.all { it.isDigit() } -> {
                            text.toIntOrNull()?.let { value ->
                                if (value >= 0 && value < 60) onMinutesChange(value)
                            }
                        }
                    }
                },
                modifier = Modifier.width(80.dp),
                label = { Text("Min") },
                singleLine = true,
                placeholder = { Text("0") }
            )
        }

        Text(
            "Gesamt: ${hours * 60 + minutes} Minuten",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimePickerField(
    time: LocalTime?,
    onTimeChange: (LocalTime?) -> Unit,
    label: String
) {
    var showTimePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = time?.let {
            String.format("%02d:%02d", it.hour, it.minute)
        } ?: "",
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showTimePicker = true },
        trailingIcon = {
            IconButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Zeit wählen")
            }
        }
    )

    // Note: In production, you'd use a proper time picker dialog
    // For simplicity, we'll use a basic input here
    if (showTimePicker) {
        var hourText by remember { mutableStateOf(time?.hour?.toString() ?: "14") }
        var minuteText by remember { mutableStateOf(time?.minute?.toString() ?: "0") }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Zeit wählen") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it },
                        label = { Text("Std") },
                        modifier = Modifier.width(80.dp)
                    )
                    Text(":")
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it },
                        label = { Text("Min") },
                        modifier = Modifier.width(80.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = hourText.toIntOrNull() ?: 0
                        val minute = minuteText.toIntOrNull() ?: 0
                        if (hour in 0..23 && minute in 0..59) {
                            onTimeChange(LocalTime.of(hour, minute))
                        }
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

@Composable
private fun TriggerModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getRecurringSummary(config: RecurringConfig): String {
    val modeText = when (config.mode) {
        RecurringMode.DAILY -> {
            if (config.dailyInterval == 1) {
                "Täglich${config.specificTime?.let { " um ${it.hour}:${String.format("%02d", it.minute)} Uhr" } ?: ""}"
            } else {
                "Alle ${config.dailyInterval} Tage${config.specificTime?.let { " um ${it.hour}:${String.format("%02d", it.minute)} Uhr" } ?: ""}"
            }
        }
        RecurringMode.WEEKLY -> {
            val days = config.weeklyDays.sorted().joinToString(", ") {
                it.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
            }
            "Wöchentlich: $days${config.specificTime?.let { " um ${it.hour}:${String.format("%02d", it.minute)} Uhr" } ?: ""}"
        }
        RecurringMode.MONTHLY -> {
            "Monatlich am ${config.monthlyDay}.${config.specificTime?.let { " um ${it.hour}:${String.format("%02d", it.minute)} Uhr" } ?: ""}"
        }
        RecurringMode.CUSTOM -> {
            val totalMinutes = config.customHours * 60 + config.customMinutes
            if (totalMinutes < 60) {
                "Alle $totalMinutes min"
            } else if (config.customMinutes == 0) {
                "Alle ${config.customHours}h"
            } else {
                "Alle ${config.customHours}h ${config.customMinutes}min"
            }
        }
    }

    val triggerText = when (config.triggerMode) {
        TriggerMode.FIXED_INTERVAL -> " (Fest)"
        TriggerMode.AFTER_COMPLETION -> " (Nach Abschluss)"
        TriggerMode.AFTER_EXPIRY -> " (Nach Ablauf)"
    }

    // Always show trigger mode, not just for custom
    return modeText + triggerText
}