package com.example.questflow.presentation.screens.timeblock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.TimeBlockEntity
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBlockDialog(
    timeBlock: TimeBlockEntity?,
    onDismiss: () -> Unit,
    onSave: (TimeBlockEntity, List<Long>) -> Unit
) {
    // Basic fields
    var name by remember { mutableStateOf(timeBlock?.name ?: "") }
    var description by remember { mutableStateOf(timeBlock?.description ?: "") }
    var type by remember { mutableStateOf(timeBlock?.type ?: "") }
    var startTime by remember { mutableStateOf(timeBlock?.startTime ?: "09:00:00") }
    var endTime by remember { mutableStateOf(timeBlock?.endTime ?: "17:00:00") }
    var allDay by remember { mutableStateOf(timeBlock?.allDay ?: false) }

    // Recurrence fields
    var daysOfWeek by remember { mutableStateOf(timeBlock?.daysOfWeek ?: "") }
    var daysOfMonth by remember { mutableStateOf(timeBlock?.daysOfMonth ?: "") }
    var monthsOfYear by remember { mutableStateOf(timeBlock?.monthsOfYear ?: "") }
    var specificDates by remember { mutableStateOf(timeBlock?.specificDates ?: "") }
    var repeatInterval by remember { mutableStateOf(timeBlock?.repeatInterval?.toString() ?: "") }
    var repeatUnit by remember { mutableStateOf(timeBlock?.repeatUnit ?: "") }

    // Date range
    var validFrom by remember { mutableStateOf(timeBlock?.validFrom ?: "") }
    var validUntil by remember { mutableStateOf(timeBlock?.validUntil ?: "") }

    // Status
    var isActive by remember { mutableStateOf(timeBlock?.isActive ?: true) }

    // Tags - TODO: Implement tag selection with proper ViewModel
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (timeBlock == null) "Zeitblockierung erstellen" else "Zeitblockierung bearbeiten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name (Pflichtfeld)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Beschreibung
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Typ
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Typ") },
                    placeholder = { Text("z.B. WORK, VACATION, MEETING") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Divider()
                Text("Zeitfenster", style = MaterialTheme.typography.titleSmall)

                // Ganztägig Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ganztägig", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = allDay,
                        onCheckedChange = { allDay = it }
                    )
                }

                // Zeitfenster (nur wenn nicht ganztägig)
                if (!allDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime.take(5),
                            onValueChange = { startTime = "$it:00" },
                            label = { Text("Von") },
                            placeholder = { Text("09:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime.take(5),
                            onValueChange = { endTime = "$it:00" },
                            label = { Text("Bis") },
                            placeholder = { Text("17:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                Divider()
                Text("Wiederholung", style = MaterialTheme.typography.titleSmall)

                // Wochentage
                OutlinedTextField(
                    value = daysOfWeek,
                    onValueChange = { daysOfWeek = it },
                    label = { Text("Wochentage") },
                    placeholder = { Text("z.B. 1,2,3,4,5 (Mo-Fr)") },
                    supportingText = { Text("1=Mo, 2=Di, 3=Mi, 4=Do, 5=Fr, 6=Sa, 7=So") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Monatstage
                OutlinedTextField(
                    value = daysOfMonth,
                    onValueChange = { daysOfMonth = it },
                    label = { Text("Monatstage") },
                    placeholder = { Text("z.B. 1,15,31") },
                    supportingText = { Text("Komma-getrennte Tage (1-31)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Monate
                OutlinedTextField(
                    value = monthsOfYear,
                    onValueChange = { monthsOfYear = it },
                    label = { Text("Monate") },
                    placeholder = { Text("z.B. 1,6,12") },
                    supportingText = { Text("1=Jan, 2=Feb, ..., 12=Dez") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Spezifische Daten
                OutlinedTextField(
                    value = specificDates,
                    onValueChange = { specificDates = it },
                    label = { Text("Spezifische Daten") },
                    placeholder = { Text("z.B. 2025-12-24,2025-12-31") },
                    supportingText = { Text("Format: YYYY-MM-DD, komma-getrennt") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Wiederholungs-Intervall
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = repeatInterval,
                        onValueChange = { repeatInterval = it },
                        label = { Text("Intervall") },
                        placeholder = { Text("z.B. 2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = repeatUnit,
                        onValueChange = { repeatUnit = it },
                        label = { Text("Einheit") },
                        placeholder = { Text("DAYS, WEEKS, MONTHS") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Divider()
                Text("Gültigkeitszeitraum", style = MaterialTheme.typography.titleSmall)

                // Gültig von/bis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = validFrom,
                        onValueChange = { validFrom = it },
                        label = { Text("Von") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = validUntil,
                        onValueChange = { validUntil = it },
                        label = { Text("Bis") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Divider()
                Text(
                    "Tags (TODO: Wird später implementiert)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                // Aktiv Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aktiv", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) return@TextButton

                    val newTimeBlock = TimeBlockEntity(
                        id = timeBlock?.id ?: 0,
                        name = name,
                        description = description.ifBlank { null },
                        type = type.ifBlank { null },
                        startTime = if (allDay) null else startTime,
                        endTime = if (allDay) null else endTime,
                        allDay = allDay,
                        daysOfWeek = daysOfWeek.ifBlank { null },
                        daysOfMonth = daysOfMonth.ifBlank { null },
                        monthsOfYear = monthsOfYear.ifBlank { null },
                        specificDates = specificDates.ifBlank { null },
                        repeatInterval = repeatInterval.toIntOrNull(),
                        repeatUnit = repeatUnit.ifBlank { null },
                        validFrom = validFrom.ifBlank { null },
                        validUntil = validUntil.ifBlank { null },
                        isActive = isActive,
                        createdAt = timeBlock?.createdAt ?: LocalDateTime.now().toString(),
                        updatedAt = LocalDateTime.now().toString()
                    )

                    onSave(newTimeBlock, selectedTagIds.toList())
                },
                enabled = name.isNotBlank()
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
