package com.example.questflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.questflow.domain.model.CalendarColorConfig
import com.example.questflow.domain.model.ColorMode
import com.example.questflow.domain.model.TaskColorSetting
import com.example.questflow.domain.model.TaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Calendar Color Configuration
 * Uses SharedPreferences for persistence
 */
@Singleton
class CalendarColorConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "calendar_color_config",
        Context.MODE_PRIVATE
    )

    private val _colorConfig = MutableStateFlow(loadConfig())
    val colorConfig: StateFlow<CalendarColorConfig> = _colorConfig.asStateFlow()

    /**
     * Load configuration from SharedPreferences
     */
    private fun loadConfig(): CalendarColorConfig {
        return CalendarColorConfig(
            ownTask = loadTaskColorSetting("own_task", CalendarColorConfig.default().ownTask),
            parentTask = loadTaskColorSetting("parent_task", CalendarColorConfig.default().parentTask),
            subtask = loadTaskColorSetting("subtask", CalendarColorConfig.default().subtask),
            sameCategory = loadTaskColorSetting("same_category", CalendarColorConfig.default().sameCategory),
            otherCategory = loadTaskColorSetting("other_category", CalendarColorConfig.default().otherCategory),
            noCategory = loadTaskColorSetting("no_category", CalendarColorConfig.default().noCategory),
            expiredTask = loadTaskColorSetting("expired_task", CalendarColorConfig.default().expiredTask),
            completedTask = loadTaskColorSetting("completed_task", CalendarColorConfig.default().completedTask),
            externalEvent = loadTaskColorSetting("external_event", CalendarColorConfig.default().externalEvent),
            overlap = loadTaskColorSetting("overlap", CalendarColorConfig.default().overlap)
        )
    }

    /**
     * Load single task color setting
     */
    private fun loadTaskColorSetting(key: String, default: TaskColorSetting): TaskColorSetting {
        return TaskColorSetting(
            enabled = prefs.getBoolean("${key}_enabled", default.enabled),
            colorMode = ColorMode.valueOf(
                prefs.getString("${key}_color_mode", default.colorMode.name) ?: default.colorMode.name
            ),
            fixedColor = prefs.getString("${key}_fixed_color", default.fixedColor) ?: default.fixedColor,
            alpha = prefs.getFloat("${key}_alpha", default.alpha)
        )
    }

    /**
     * Save configuration to SharedPreferences
     */
    fun saveConfig(config: CalendarColorConfig) {
        val editor = prefs.edit()

        saveTaskColorSetting(editor, "own_task", config.ownTask)
        saveTaskColorSetting(editor, "parent_task", config.parentTask)
        saveTaskColorSetting(editor, "subtask", config.subtask)
        saveTaskColorSetting(editor, "same_category", config.sameCategory)
        saveTaskColorSetting(editor, "other_category", config.otherCategory)
        saveTaskColorSetting(editor, "no_category", config.noCategory)
        saveTaskColorSetting(editor, "expired_task", config.expiredTask)
        saveTaskColorSetting(editor, "completed_task", config.completedTask)
        saveTaskColorSetting(editor, "external_event", config.externalEvent)
        saveTaskColorSetting(editor, "overlap", config.overlap)

        editor.apply()
        _colorConfig.value = config
    }

    /**
     * Save single task color setting
     */
    private fun saveTaskColorSetting(
        editor: SharedPreferences.Editor,
        key: String,
        setting: TaskColorSetting
    ) {
        editor.putBoolean("${key}_enabled", setting.enabled)
        editor.putString("${key}_color_mode", setting.colorMode.name)
        editor.putString("${key}_fixed_color", setting.fixedColor)
        editor.putFloat("${key}_alpha", setting.alpha)
    }

    /**
     * Update specific task type setting
     */
    fun updateTaskTypeSetting(taskType: TaskType, setting: TaskColorSetting) {
        val currentConfig = _colorConfig.value
        val updatedConfig = when (taskType) {
            TaskType.OWN_TASK -> currentConfig.copy(ownTask = setting)
            TaskType.PARENT_TASK -> currentConfig.copy(parentTask = setting)
            TaskType.SUBTASK -> currentConfig.copy(subtask = setting)
            TaskType.SAME_CATEGORY -> currentConfig.copy(sameCategory = setting)
            TaskType.OTHER_CATEGORY -> currentConfig.copy(otherCategory = setting)
            TaskType.NO_CATEGORY -> currentConfig.copy(noCategory = setting)
            TaskType.EXPIRED_TASK -> currentConfig.copy(expiredTask = setting)
            TaskType.COMPLETED_TASK -> currentConfig.copy(completedTask = setting)
            TaskType.EXTERNAL_EVENT -> currentConfig.copy(externalEvent = setting)
            TaskType.OVERLAP -> currentConfig.copy(overlap = setting)
        }
        saveConfig(updatedConfig)
    }

    /**
     * Reset to default configuration
     */
    fun resetToDefaults() {
        saveConfig(CalendarColorConfig.default())
    }
}
