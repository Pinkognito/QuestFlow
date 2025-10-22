package com.example.questflow.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Simplified Calendar Color Configuration (MVP)
 *
 * Configurable colors for the 5 existing task types with visibility toggle
 */
data class SimpleCalendarColorConfig(
    val ownTaskColor: String = "#FFFFFF",       // White - Currently selected task
    val ownTaskEnabled: Boolean = true,
    val sameCategoryColor: String = "#FFEB3B",  // Yellow - Same category
    val sameCategoryEnabled: Boolean = true,
    val otherTaskColor: String = "#42A5F5",     // Blue - Other own tasks
    val otherTaskEnabled: Boolean = true,
    val externalEventColor: String = "#EF5350", // Red - Google Calendar events
    val externalEventEnabled: Boolean = true,
    val timeBlockColor: String = "#FF9800",     // Orange - Time Blocks
    val timeBlockEnabled: Boolean = true,
    val overlapColor: String = "#000000",       // Black - Overlaps
    val overlapEnabled: Boolean = true
) {
    /**
     * Parse hex color to Compose Color
     */
    fun getOwnTaskColor(): Color = parseColor(ownTaskColor)
    fun getSameCategoryColor(): Color = parseColor(sameCategoryColor)
    fun getOtherTaskColor(): Color = parseColor(otherTaskColor)
    fun getExternalEventColor(): Color = parseColor(externalEventColor)
    fun getTimeBlockColor(): Color = parseColor(timeBlockColor)
    fun getOverlapColor(): Color = parseColor(overlapColor)

    private fun parseColor(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    companion object {
        fun default() = SimpleCalendarColorConfig()
    }
}

/**
 * Repository for Simple Calendar Color Configuration
 */
class SimpleCalendarColorRepository(
    private val prefs: android.content.SharedPreferences
) {
    companion object {
        private const val KEY_OWN_TASK = "simple_cal_own_task"
        private const val KEY_SAME_CATEGORY = "simple_cal_same_category"
        private const val KEY_OTHER_TASK = "simple_cal_other_task"
        private const val KEY_EXTERNAL_EVENT = "simple_cal_external_event"
        private const val KEY_TIME_BLOCK = "simple_cal_time_block"
        private const val KEY_OVERLAP = "simple_cal_overlap"
    }

    fun loadConfig(): SimpleCalendarColorConfig {
        return SimpleCalendarColorConfig(
            ownTaskColor = prefs.getString(KEY_OWN_TASK, "#FFFFFF") ?: "#FFFFFF",
            ownTaskEnabled = prefs.getBoolean("${KEY_OWN_TASK}_enabled", true),
            sameCategoryColor = prefs.getString(KEY_SAME_CATEGORY, "#FFEB3B") ?: "#FFEB3B",
            sameCategoryEnabled = prefs.getBoolean("${KEY_SAME_CATEGORY}_enabled", true),
            otherTaskColor = prefs.getString(KEY_OTHER_TASK, "#42A5F5") ?: "#42A5F5",
            otherTaskEnabled = prefs.getBoolean("${KEY_OTHER_TASK}_enabled", true),
            externalEventColor = prefs.getString(KEY_EXTERNAL_EVENT, "#EF5350") ?: "#EF5350",
            externalEventEnabled = prefs.getBoolean("${KEY_EXTERNAL_EVENT}_enabled", true),
            timeBlockColor = prefs.getString(KEY_TIME_BLOCK, "#FF9800") ?: "#FF9800",
            timeBlockEnabled = prefs.getBoolean("${KEY_TIME_BLOCK}_enabled", true),
            overlapColor = prefs.getString(KEY_OVERLAP, "#000000") ?: "#000000",
            overlapEnabled = prefs.getBoolean("${KEY_OVERLAP}_enabled", true)
        )
    }

    fun saveConfig(config: SimpleCalendarColorConfig) {
        prefs.edit()
            .putString(KEY_OWN_TASK, config.ownTaskColor)
            .putBoolean("${KEY_OWN_TASK}_enabled", config.ownTaskEnabled)
            .putString(KEY_SAME_CATEGORY, config.sameCategoryColor)
            .putBoolean("${KEY_SAME_CATEGORY}_enabled", config.sameCategoryEnabled)
            .putString(KEY_OTHER_TASK, config.otherTaskColor)
            .putBoolean("${KEY_OTHER_TASK}_enabled", config.otherTaskEnabled)
            .putString(KEY_EXTERNAL_EVENT, config.externalEventColor)
            .putBoolean("${KEY_EXTERNAL_EVENT}_enabled", config.externalEventEnabled)
            .putString(KEY_TIME_BLOCK, config.timeBlockColor)
            .putBoolean("${KEY_TIME_BLOCK}_enabled", config.timeBlockEnabled)
            .putString(KEY_OVERLAP, config.overlapColor)
            .putBoolean("${KEY_OVERLAP}_enabled", config.overlapEnabled)
            .apply()
    }

    fun resetToDefaults() {
        saveConfig(SimpleCalendarColorConfig.default())
    }
}
