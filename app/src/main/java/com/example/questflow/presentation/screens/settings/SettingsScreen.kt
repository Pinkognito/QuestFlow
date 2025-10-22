package com.example.questflow.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.domain.preferences.TimeAdjustmentPreferences
import com.example.questflow.presentation.components.CalendarColorSettings
import com.example.questflow.presentation.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeAdjustmentPrefs = remember { TimeAdjustmentPreferences(context) }

    val isBackupInProgress by viewModel.isBackupInProgress.collectAsState()
    val isCalendarSyncInProgress by viewModel.isCalendarSyncInProgress.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val calendarSyncProgress by viewModel.calendarSyncProgress.collectAsState()

    var showBackupConfirmation by remember { mutableStateOf(false) }
    var showSyncConfirmation by remember { mutableStateOf(false) }
    var lastBackupPath by remember { mutableStateOf<String?>(null) }

    // Time Adjustment Settings
    var showTimeAdjustmentDialog by remember { mutableStateOf(false) }
    var timeAdjustmentMode by remember { mutableStateOf(timeAdjustmentPrefs.getAdjustmentMode()) }
    var fixedDuration by remember { mutableStateOf(timeAdjustmentPrefs.getFixedDurationMinutes()) }

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

            // Time Adjustment Card
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
                                text = "Automatische Ende-Zeit",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = when (timeAdjustmentMode) {
                                    TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT ->
                                        "Unabhängig (keine automatische Anpassung)"
                                    TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION ->
                                        "Feste Dauer: ${fixedDuration} Minuten"
                                    TimeAdjustmentPreferences.AdjustmentMode.CURRENT_DISTANCE ->
                                        "Aktuelle Distanz (nutzt vorhandene Zeitspanne)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Legt fest, wie sich die Ende-Zeit verhält, wenn du die Start-Zeit änderst",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showTimeAdjustmentDialog = true },
                        enabled = !isBackupInProgress && !isCalendarSyncInProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ende-Zeit-Verhalten anpassen")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section: Darstellung
            Text(
                text = "Darstellung",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // Calendar Color Settings
            CalendarColorSettings()

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

    // Time Adjustment Configuration Dialog
    if (showTimeAdjustmentDialog) {
        var tempMode by remember { mutableStateOf(timeAdjustmentMode) }
        var tempDuration by remember { mutableStateOf(fixedDuration) }

        AlertDialog(
            onDismissRequest = { showTimeAdjustmentDialog = false },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            title = { Text("Ende-Zeit-Verhalten konfigurieren") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Legt fest, ob Start- und Ende-Zeit automatisch zusammen angepasst werden (bidirektional).")

                    // Mode Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Modus:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // INDEPENDENT Option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempMode = TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT }
                        ) {
                            RadioButton(
                                selected = tempMode == TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT,
                                onClick = { tempMode = TimeAdjustmentPreferences.AdjustmentMode.INDEPENDENT }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Unabhängig", fontWeight = FontWeight.Medium)
                                Text(
                                    "Start und Ende sind komplett unabhängig voneinander",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // FIXED_DURATION Option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempMode = TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION }
                        ) {
                            RadioButton(
                                selected = tempMode == TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION,
                                onClick = { tempMode = TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Automatische Anpassung", fontWeight = FontWeight.Medium)
                                Text(
                                    "Feste Dauer - beide Zeiten passen sich gegenseitig an",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Duration Input (only shown when FIXED_DURATION is selected)
                    if (tempMode == TimeAdjustmentPreferences.AdjustmentMode.FIXED_DURATION) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Dauer in Minuten:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = tempDuration.toString(),
                                onValueChange = { newValue ->
                                    tempDuration = newValue.toIntOrNull()?.coerceIn(1, 1440) ?: tempDuration
                                },
                                label = { Text("Minuten") },
                                placeholder = { Text("60") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Beispiel bei 60 Min:\n• Start 09:00 → Ende 10:00\n• Ende 12:00 → Start 11:00",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Save settings
                        timeAdjustmentPrefs.setAdjustmentMode(tempMode)
                        timeAdjustmentPrefs.setFixedDurationMinutes(tempDuration)

                        // Update UI state
                        timeAdjustmentMode = tempMode
                        fixedDuration = tempDuration

                        showTimeAdjustmentDialog = false
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeAdjustmentDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
