package com.example.questflow.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.presentation.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isBackupInProgress by viewModel.isBackupInProgress.collectAsState()
    val isCalendarSyncInProgress by viewModel.isCalendarSyncInProgress.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val calendarSyncProgress by viewModel.calendarSyncProgress.collectAsState()

    var showBackupConfirmation by remember { mutableStateOf(false) }
    var showSyncConfirmation by remember { mutableStateOf(false) }
    var lastBackupPath by remember { mutableStateOf<String?>(null) }

    val workingHours by viewModel.workingHoursSettings.collectAsState()
    var showWorkingHoursDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Datenmanagement
            Text(
                text = "Datenmanagement",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Daten-Backup erstellen",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Kopiert die komplette Datenbank in den Download-Ordner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isBackupInProgress) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = backupProgress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (lastBackupPath != null && !isBackupInProgress) {
                        Text(
                            text = "Letztes Backup: $lastBackupPath",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Button(
                        onClick = { showBackupConfirmation = true },
                        enabled = !isBackupInProgress && !isCalendarSyncInProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup erstellen")
                    }
                }
            }

            // Calendar Sync Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Kalender synchronisieren",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Erstellt fehlende Google Calendar Einträge für Tasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isCalendarSyncInProgress) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = calendarSyncProgress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = { showSyncConfirmation = true },
                        enabled = !isBackupInProgress && !isCalendarSyncInProgress,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kalender reparieren")
                    }
                }
            }

            // Working Hours Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Arbeitszeiten",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = workingHours?.let {
                                    if (it.enabled) {
                                        "${String.format("%02d:%02d", it.startHour, it.startMinute)} - ${String.format("%02d:%02d", it.endHour, it.endMinute)} Uhr"
                                    } else {
                                        "Deaktiviert (24h)"
                                    }
                                } ?: "Laden...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Legt den Zeitraum fest, in dem Terminvorschläge gemacht werden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showWorkingHoursDialog = true },
                        enabled = !isBackupInProgress && !isCalendarSyncInProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Arbeitszeiten anpassen")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Hinweise",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        text = "• Backups werden im Download-Ordner gespeichert\n" +
                                "• Die Backup-Datei ist eine komplette Kopie der SQLite-Datenbank\n" +
                                "• Kalender-Sync erstellt nur fehlende Events, löscht keine bestehenden\n" +
                                "• Beide Funktionen können einige Sekunden dauern",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }

    // Backup Confirmation Dialog
    if (showBackupConfirmation) {
        AlertDialog(
            onDismissRequest = { showBackupConfirmation = false },
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            title = { Text("Backup erstellen?") },
            text = {
                Text("Die komplette Datenbank-Datei wird in den Download-Ordner kopiert. Dies kann einige Sekunden dauern.\n\nDie Backup-Datei enthält alle Daten und kann zur Wiederherstellung verwendet werden.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupConfirmation = false
                        viewModel.createBackup { success, path ->
                            if (success && path != null) {
                                lastBackupPath = path
                            }
                        }
                    }
                ) {
                    Text("Backup starten")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirmation = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Calendar Sync Confirmation Dialog
    if (showSyncConfirmation) {
        AlertDialog(
            onDismissRequest = { showSyncConfirmation = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            title = { Text("Kalender reparieren?") },
            text = {
                Text("Alle Tasks werden geprüft und fehlende Google Calendar Einträge werden erstellt.\n\nBestehende Events werden NICHT gelöscht oder verändert.\n\nDies kann bei vielen Tasks einige Minuten dauern.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSyncConfirmation = false
                        viewModel.syncCalendarEvents { count ->
                            // Completion handled by Toast in ViewModel
                        }
                    }
                ) {
                    Text("Sync starten")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncConfirmation = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Working Hours Configuration Dialog
    if (showWorkingHoursDialog) {
        val currentSettings = workingHours ?: return@SettingsScreen
        var enabled by remember { mutableStateOf(currentSettings.enabled) }
        var startHour by remember { mutableStateOf(currentSettings.startHour) }
        var startMinute by remember { mutableStateOf(currentSettings.startMinute) }
        var endHour by remember { mutableStateOf(currentSettings.endHour) }
        var endMinute by remember { mutableStateOf(currentSettings.endMinute) }

        AlertDialog(
            onDismissRequest = { showWorkingHoursDialog = false },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            title = { Text("Arbeitszeiten konfigurieren") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Legt fest, in welchem Zeitraum Terminvorschläge gemacht werden sollen.")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )
                        Text("Arbeitszeiten aktivieren")
                    }

                    if (enabled) {
                        Text("Start:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startHour.toString(),
                                onValueChange = { startHour = it.toIntOrNull()?.coerceIn(0, 23) ?: startHour },
                                label = { Text("Stunde") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = startMinute.toString(),
                                onValueChange = { startMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: startMinute },
                                label = { Text("Minute") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Ende:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = endHour.toString(),
                                onValueChange = { endHour = it.toIntOrNull()?.coerceIn(0, 23) ?: endHour },
                                label = { Text("Stunde") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endMinute.toString(),
                                onValueChange = { endMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: endMinute },
                                label = { Text("Minute") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Text(
                            "Deaktiviert: Termine werden rund um die Uhr (24h) vorgeschlagen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateWorkingHours(
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            enabled = enabled
                        )
                        showWorkingHoursDialog = false
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkingHoursDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
