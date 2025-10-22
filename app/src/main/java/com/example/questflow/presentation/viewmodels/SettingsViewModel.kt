package com.example.questflow.presentation.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.database.QuestFlowDatabase
import com.example.questflow.domain.usecase.UpdateTaskWithCalendarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val database: QuestFlowDatabase,
    private val calendarManager: CalendarManager
) : AndroidViewModel(application) {

    private val _isBackupInProgress = MutableStateFlow(false)
    val isBackupInProgress: StateFlow<Boolean> = _isBackupInProgress.asStateFlow()

    private val _isCalendarSyncInProgress = MutableStateFlow(false)
    val isCalendarSyncInProgress: StateFlow<Boolean> = _isCalendarSyncInProgress.asStateFlow()

    private val _backupProgress = MutableStateFlow("")
    val backupProgress: StateFlow<String> = _backupProgress.asStateFlow()

    private val _calendarSyncProgress = MutableStateFlow("")
    val calendarSyncProgress: StateFlow<String> = _calendarSyncProgress.asStateFlow()

    /**
     * Create complete database backup (SQLite file copy)
     * Copies the entire database file to Downloads folder
     */
    fun createBackup(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isBackupInProgress.value = true
            _backupProgress.value = "Backup-Funktion wird vorbereitet..."

            try {
                val appContext = getApplication<Application>().applicationContext

                // Create timestamp for filename
                val timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                val backupFileName = "QuestFlow_Backup_$timestamp.db"

                // Get Downloads directory
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val backupFile = java.io.File(downloadsDir, backupFileName)

                _backupProgress.value = "Erstelle Datenbank-Kopie..."

                // Copy database file
                withContext(Dispatchers.IO) {
                    // Close database connections temporarily
                    database.close()

                    // Get source database file
                    val dbPath = appContext.getDatabasePath(QuestFlowDatabase.DATABASE_NAME)

                    if (dbPath.exists()) {
                        // Copy file
                        dbPath.inputStream().use { input ->
                            backupFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    } else {
                        throw Exception("Datenbank-Datei nicht gefunden")
                    }
                }

                _backupProgress.value = "Backup abgeschlossen!"
                _isBackupInProgress.value = false

                val finalPath = backupFile.absolutePath

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Backup erfolgreich erstellt:\n$finalPath",
                        Toast.LENGTH_LONG
                    ).show()
                }

                onComplete(true, finalPath)
            } catch (e: Exception) {
                val appContext = getApplication<Application>().applicationContext
                _backupProgress.value = "Fehler: ${e.message}"
                _isBackupInProgress.value = false

                android.util.Log.e("SettingsViewModel", "Backup failed", e)

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Backup-Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                }

                onComplete(false, null)
            }
        }
    }

    /**
     * Sync/Repair calendar events for all tasks
     * Creates missing calendar events where conditions are met
     */
    fun syncCalendarEvents(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isCalendarSyncInProgress.value = true
            _calendarSyncProgress.value = "Kalender-Synchronisation wird gestartet..."

            try {
                val appContext = getApplication<Application>().applicationContext
                var createdCount = 0

                // Get all calendar links
                val allLinks = database.calendarEventLinkDao().getAllLinks().first()
                android.util.Log.d("SettingsViewModel", "Found ${allLinks.size} calendar links total")
                _calendarSyncProgress.value = "${allLinks.size} Kalender-Links gefunden..."

                for (link in allLinks) {
                    android.util.Log.d("SettingsViewModel", "Processing link: calendarEventId=${link.calendarEventId}, deleteOnClaim=${link.deleteOnClaim}, deleteOnExpiry=${link.deleteOnExpiry}, status=${link.status}")

                    val taskId = link.taskId
                    if (taskId != null) {
                        val task = withContext(Dispatchers.IO) {
                            database.taskDao().getTaskById(taskId)
                        }

                        if (task != null && task.dueDate != null) {
                            // Check if task should have calendar event based on UpdateTaskWithCalendarUseCase logic:
                            // - Not expired (status != EXPIRED)
                            // - OR expired but deleteOnExpiry is false
                            // - OR claimed but deleteOnClaim is false
                            val now = java.time.LocalDateTime.now()
                            val isExpired = link.endsAt <= now
                            val isClaimed = link.rewarded

                            val shouldHaveEvent = when {
                                // If claimed and deleteOnClaim is true -> should NOT have event
                                isClaimed && link.deleteOnClaim -> false
                                // If expired and deleteOnExpiry is true -> should NOT have event
                                isExpired && link.deleteOnExpiry -> false
                                // Otherwise should have event
                                else -> true
                            }

                            android.util.Log.d("SettingsViewModel", "Task ${task.title}: shouldHaveEvent=$shouldHaveEvent (isExpired=$isExpired, isClaimed=$isClaimed)")

                            // If should have event but doesn't, create it
                            if (shouldHaveEvent && link.calendarEventId == 0L) {
                                try {
                                    android.util.Log.d("SettingsViewModel", "Creating calendar event for: ${task.title}")

                                    // Create calendar event using the manager
                                    val eventId = calendarManager.createTaskEvent(
                                        taskTitle = task.title,
                                        taskDescription = task.description ?: "",
                                        startTime = link.startsAt,
                                        endTime = link.endsAt,
                                        xpReward = 0, // Placeholder
                                        xpPercentage = link.xpPercentage,
                                        taskId = taskId
                                    )

                                    if (eventId != null && eventId > 0) {
                                        android.util.Log.d("SettingsViewModel", "Created event with ID: $eventId")

                                        // Update link with new event ID
                                        val updatedLink = link.copy(calendarEventId = eventId)
                                        withContext(Dispatchers.IO) {
                                            database.calendarEventLinkDao().update(updatedLink)
                                        }
                                        createdCount++
                                        _calendarSyncProgress.value = "Erstellt: $createdCount Events..."
                                    } else {
                                        android.util.Log.e("SettingsViewModel", "Event creation returned null or 0 for task: ${task.title}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("SettingsViewModel", "Failed to create calendar event for task $taskId", e)
                                }
                            }
                        }
                    }
                }

                _calendarSyncProgress.value = "Synchronisation abgeschlossen: $createdCount Events erstellt"
                _isCalendarSyncInProgress.value = false

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Kalender-Sync abgeschlossen: $createdCount Events erstellt", Toast.LENGTH_LONG).show()
                }

                onComplete(createdCount)
            } catch (e: Exception) {
                val appContext = getApplication<Application>().applicationContext
                _calendarSyncProgress.value = "Fehler bei Synchronisation: ${e.message}"
                _isCalendarSyncInProgress.value = false

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Sync-Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                }

                onComplete(0)
            }
        }
    }

}
