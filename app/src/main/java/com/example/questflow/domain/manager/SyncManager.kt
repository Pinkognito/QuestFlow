package com.example.questflow.domain.manager

import com.example.questflow.data.preferences.SyncPreferences
import com.example.questflow.domain.usecase.CheckExpiredEventsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class SyncManager @Inject constructor(
    private val checkExpiredEventsUseCase: CheckExpiredEventsUseCase,
    private val syncPreferences: SyncPreferences
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<CheckExpiredEventsUseCase.CheckResult?>(null)
    val lastSyncResult: StateFlow<CheckExpiredEventsUseCase.CheckResult?> = _lastSyncResult.asStateFlow()

    private var syncJob: Job? = null
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startPeriodicSync() {
        stopPeriodicSync()

        Log.d("SyncManager", "Starting periodic sync")

        syncJob = syncScope.launch {
            // Initial sync on startup
            performSync(forceFullCheck = false)

            // Then sync every minute
            while (isActive) {
                delay(60_000) // 1 minute
                performSync(forceFullCheck = false)
            }
        }
    }

    fun stopPeriodicSync() {
        Log.d("SyncManager", "Stopping periodic sync")
        syncJob?.cancel()
        syncJob = null
    }

    suspend fun performSync(forceFullCheck: Boolean = false) {
        try {
            _isSyncing.value = true
            Log.d("SyncManager", "Performing sync check...")

            val result = checkExpiredEventsUseCase(forceFullCheck)
            _lastSyncResult.value = result

            Log.d("SyncManager", "Sync complete: ${result.expiredCount} expired, ${result.deletedCount} deleted, ${result.recurringCreated} recurring")
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed", e)
        } finally {
            _isSyncing.value = false
        }
    }

    fun onDestroy() {
        syncScope.cancel()
    }
}