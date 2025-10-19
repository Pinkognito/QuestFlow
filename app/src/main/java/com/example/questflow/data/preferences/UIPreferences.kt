package com.example.questflow.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI Preferences for persistent UI state
 * Stores user preferences like collapsed sections, view modes, etc.
 */
data class UISettings(
    val taskFamilyExpanded: Boolean = true,  // Task family section expanded/collapsed
    val taskContactListExpanded: Boolean = true,  // Task contact list expanded/collapsed
    val startDayIncrement: Int = 1,  // Start date adjustment increment in days
    val startMinuteIncrement: Int = 15,  // Start time adjustment increment in minutes
    val endDayIncrement: Int = 1,  // End date adjustment increment in days
    val endMinuteIncrement: Int = 15,  // End time adjustment increment in minutes
    val recurringTimeMinuteIncrement: Int = 15,  // Recurring time adjustment increment in minutes
    // Add more UI preferences here as needed
) {
    companion object {
        val DEFAULT = UISettings()
    }
}

@Singleton
class UIPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ui_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_TASK_FAMILY_EXPANDED = "task_family_expanded"
        private const val KEY_TASK_CONTACT_LIST_EXPANDED = "task_contact_list_expanded"
        private const val KEY_START_DAY_INCREMENT = "start_day_increment"
        private const val KEY_START_MINUTE_INCREMENT = "start_minute_increment"
        private const val KEY_END_DAY_INCREMENT = "end_day_increment"
        private const val KEY_END_MINUTE_INCREMENT = "end_minute_increment"
        private const val KEY_RECURRING_TIME_MINUTE_INCREMENT = "recurring_time_minute_increment"

        // Legacy keys for migration
        private const val KEY_MINUTE_INCREMENT = "minute_increment"
        private const val KEY_DAY_INCREMENT = "day_increment"
    }

    private val _settings = MutableStateFlow(loadSettings())

    fun getSettings(): StateFlow<UISettings> = _settings.asStateFlow()

    private fun loadSettings(): UISettings {
        // Migrate from old keys if new keys don't exist
        val legacyMinuteIncrement = prefs.getInt(KEY_MINUTE_INCREMENT, 15)
        val legacyDayIncrement = prefs.getInt(KEY_DAY_INCREMENT, 1)

        return UISettings(
            taskFamilyExpanded = prefs.getBoolean(KEY_TASK_FAMILY_EXPANDED, true),
            taskContactListExpanded = prefs.getBoolean(KEY_TASK_CONTACT_LIST_EXPANDED, true),
            startDayIncrement = prefs.getInt(KEY_START_DAY_INCREMENT, legacyDayIncrement),
            startMinuteIncrement = prefs.getInt(KEY_START_MINUTE_INCREMENT, legacyMinuteIncrement),
            endDayIncrement = prefs.getInt(KEY_END_DAY_INCREMENT, legacyDayIncrement),
            endMinuteIncrement = prefs.getInt(KEY_END_MINUTE_INCREMENT, legacyMinuteIncrement),
            recurringTimeMinuteIncrement = prefs.getInt(KEY_RECURRING_TIME_MINUTE_INCREMENT, 15)
        )
    }

    fun updateSettings(settings: UISettings) {
        prefs.edit()
            .putBoolean(KEY_TASK_FAMILY_EXPANDED, settings.taskFamilyExpanded)
            .putBoolean(KEY_TASK_CONTACT_LIST_EXPANDED, settings.taskContactListExpanded)
            .putInt(KEY_START_DAY_INCREMENT, settings.startDayIncrement)
            .putInt(KEY_START_MINUTE_INCREMENT, settings.startMinuteIncrement)
            .putInt(KEY_END_DAY_INCREMENT, settings.endDayIncrement)
            .putInt(KEY_END_MINUTE_INCREMENT, settings.endMinuteIncrement)
            .putInt(KEY_RECURRING_TIME_MINUTE_INCREMENT, settings.recurringTimeMinuteIncrement)
            .apply()

        _settings.value = settings
    }

    var taskFamilyExpanded: Boolean
        get() = _settings.value.taskFamilyExpanded
        set(value) {
            updateSettings(_settings.value.copy(taskFamilyExpanded = value))
        }

    var taskContactListExpanded: Boolean
        get() = _settings.value.taskContactListExpanded
        set(value) {
            updateSettings(_settings.value.copy(taskContactListExpanded = value))
        }
}
