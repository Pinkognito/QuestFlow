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
    }

    private val _settings = MutableStateFlow(loadSettings())

    fun getSettings(): StateFlow<UISettings> = _settings.asStateFlow()

    private fun loadSettings(): UISettings {
        return UISettings(
            taskFamilyExpanded = prefs.getBoolean(KEY_TASK_FAMILY_EXPANDED, true)
        )
    }

    fun updateSettings(settings: UISettings) {
        prefs.edit()
            .putBoolean(KEY_TASK_FAMILY_EXPANDED, settings.taskFamilyExpanded)
            .apply()

        _settings.value = settings
    }

    var taskFamilyExpanded: Boolean
        get() = _settings.value.taskFamilyExpanded
        set(value) {
            updateSettings(_settings.value.copy(taskFamilyExpanded = value))
        }
}
