package com.example.questflow.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LAST_CHECK_TIMESTAMP = stringPreferencesKey("last_check_timestamp")
        private val SYNC_IN_PROGRESS = booleanPreferencesKey("sync_in_progress")
    }

    private val dataStore = context.syncDataStore
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun saveLastCheckTime(timestamp: LocalDateTime) {
        dataStore.edit { preferences ->
            preferences[LAST_CHECK_TIMESTAMP] = timestamp.format(formatter)
        }
    }

    fun getLastCheckTime(): Flow<LocalDateTime?> {
        return dataStore.data.map { preferences ->
            preferences[LAST_CHECK_TIMESTAMP]?.let {
                try {
                    LocalDateTime.parse(it, formatter)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    suspend fun setSyncInProgress(inProgress: Boolean) {
        dataStore.edit { preferences ->
            preferences[SYNC_IN_PROGRESS] = inProgress
        }
    }

    fun isSyncInProgress(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[SYNC_IN_PROGRESS] ?: false
        }
    }
}