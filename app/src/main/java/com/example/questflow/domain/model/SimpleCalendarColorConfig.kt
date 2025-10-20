package com.example.questflow.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Simplified Calendar Color Configuration (MVP)
 *
 * Configurable colors for the 5 existing task types
 */
data class SimpleCalendarColorConfig(
    val ownTaskColor: String = "#FFFFFF",       // White - Currently selected task
    val sameCategoryColor: String = "#FFEB3B",  // Yellow - Same category
    val otherTaskColor: String = "#42A5F5",     // Blue - Other own tasks
    val externalEventColor: String = "#EF5350", // Red - Google Calendar events
    val overlapColor: String = "#000000"        // Black - Overlaps
) {
    /**
     * Parse hex color to Compose Color
     */
    fun getOwnTaskColor(): Color = parseColor(ownTaskColor)
    fun getSameCategoryColor(): Color = parseColor(sameCategoryColor)
    fun getOtherTaskColor(): Color = parseColor(otherTaskColor)
    fun getExternalEventColor(): Color = parseColor(externalEventColor)
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
        private const val KEY_OVERLAP = "simple_cal_overlap"
    }

    fun loadConfig(): SimpleCalendarColorConfig {
        return SimpleCalendarColorConfig(
            ownTaskColor = prefs.getString(KEY_OWN_TASK, "#FFFFFF") ?: "#FFFFFF",
            sameCategoryColor = prefs.getString(KEY_SAME_CATEGORY, "#FFEB3B") ?: "#FFEB3B",
            otherTaskColor = prefs.getString(KEY_OTHER_TASK, "#42A5F5") ?: "#42A5F5",
            externalEventColor = prefs.getString(KEY_EXTERNAL_EVENT, "#EF5350") ?: "#EF5350",
            overlapColor = prefs.getString(KEY_OVERLAP, "#000000") ?: "#000000"
        )
    }

    fun saveConfig(config: SimpleCalendarColorConfig) {
        prefs.edit()
            .putString(KEY_OWN_TASK, config.ownTaskColor)
            .putString(KEY_SAME_CATEGORY, config.sameCategoryColor)
            .putString(KEY_OTHER_TASK, config.otherTaskColor)
            .putString(KEY_EXTERNAL_EVENT, config.externalEventColor)
            .putString(KEY_OVERLAP, config.overlapColor)
            .apply()
    }

    fun resetToDefaults() {
        saveConfig(SimpleCalendarColorConfig.default())
    }
}
