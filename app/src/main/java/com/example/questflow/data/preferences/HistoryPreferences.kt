package com.example.questflow.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.questflow.domain.model.HistoryCategory
import com.example.questflow.domain.model.HistoryEventType
import com.example.questflow.domain.model.HistoryPriority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration state for which history events are tracked
 *
 * User can enable/disable individual events to control storage usage.
 * Each event type has a priority that indicates system relevance.
 */
data class HistoryEventConfig(
    val enabledEvents: Map<HistoryEventType, Boolean> = getDefaultConfig()
) {
    companion object {
        /**
         * Default configuration based on event priorities:
         * - CRITICAL: Always enabled (system-necessary for XP tracking)
         * - HIGH: Enabled by default (important for analytics)
         * - MEDIUM: Enabled by default (useful for complete history)
         * - LOW: Disabled by default (optional, saves storage)
         */
        fun getDefaultConfig(): Map<HistoryEventType, Boolean> {
            return HistoryEventType.values().associateWith { eventType ->
                when (eventType.priority) {
                    HistoryPriority.CRITICAL -> true  // Always enabled
                    HistoryPriority.HIGH -> true      // Enabled by default
                    HistoryPriority.MEDIUM -> true    // Enabled by default
                    HistoryPriority.LOW -> false      // Disabled by default to save storage
                }
            }
        }
    }

    /**
     * Calculate estimated storage usage per day based on enabled events
     * Assumes average user creates/modifies 10 tasks per day
     */
    fun estimatedDailyStorageBytes(): Int {
        val tasksPerDay = 10
        return enabledEvents.filter { it.value }.entries.sumOf { (eventType, _) ->
            eventType.estimatedBytesPerEvent * tasksPerDay
        }
    }

    /**
     * Calculate estimated monthly storage in KB
     */
    fun estimatedMonthlyStorageKB(): Double {
        return (estimatedDailyStorageBytes() * 30) / 1024.0
    }

    /**
     * Check if event is enabled
     */
    fun isEnabled(eventType: HistoryEventType): Boolean {
        return enabledEvents[eventType] ?: false
    }

    /**
     * Count of enabled events
     */
    fun enabledCount(): Int {
        return enabledEvents.count { it.value }
    }

    /**
     * Check if configuration has any changes from default
     */
    fun isModified(): Boolean {
        val defaultConfig = getDefaultConfig()
        return enabledEvents != defaultConfig
    }
}

/**
 * Manages persistent storage of history event configuration
 * Auto-saves on any change, uses StateFlow for reactive updates
 */
@Singleton
class HistoryPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "history_preferences"
        private const val KEY_PREFIX = "event_enabled_"
    }

    private val _settings = MutableStateFlow(loadConfig())

    fun getSettings(): StateFlow<HistoryEventConfig> = _settings.asStateFlow()

    /**
     * Load configuration from SharedPreferences
     * Falls back to default if not set
     */
    private fun loadConfig(): HistoryEventConfig {
        val enabledEvents = HistoryEventType.values().associateWith { eventType ->
            val key = KEY_PREFIX + eventType.name
            // Default depends on priority if not explicitly set
            val default = when (eventType.priority) {
                HistoryPriority.CRITICAL -> true
                HistoryPriority.HIGH -> true
                HistoryPriority.MEDIUM -> true
                HistoryPriority.LOW -> false
            }
            prefs.getBoolean(key, default)
        }
        return HistoryEventConfig(enabledEvents)
    }

    /**
     * Save configuration to SharedPreferences
     * Called automatically after any change
     */
    private fun saveConfig(config: HistoryEventConfig) {
        prefs.edit().apply {
            config.enabledEvents.forEach { (eventType, enabled) ->
                putBoolean(KEY_PREFIX + eventType.name, enabled)
            }
            apply() // Async save
        }
        _settings.value = config
    }

    /**
     * Update full configuration
     */
    fun updateSettings(config: HistoryEventConfig) {
        saveConfig(config)
    }

    /**
     * Toggle a single event type on/off
     * Saves immediately and updates StateFlow
     */
    fun toggleEvent(eventType: HistoryEventType, enabled: Boolean) {
        val currentConfig = _settings.value
        val updatedEvents = currentConfig.enabledEvents.toMutableMap()
        updatedEvents[eventType] = enabled

        val newConfig = HistoryEventConfig(updatedEvents)
        saveConfig(newConfig)
    }

    /**
     * Enable/disable entire category
     * Useful for bulk operations
     */
    fun toggleCategory(category: HistoryCategory, enabled: Boolean) {
        val currentConfig = _settings.value
        val updatedEvents = currentConfig.enabledEvents.toMutableMap()

        HistoryEventType.getByCategory(category).forEach { eventType ->
            updatedEvents[eventType] = enabled
        }

        val newConfig = HistoryEventConfig(updatedEvents)
        saveConfig(newConfig)
    }

    /**
     * Enable/disable all events by priority level
     */
    fun togglePriority(priority: HistoryPriority, enabled: Boolean) {
        val currentConfig = _settings.value
        val updatedEvents = currentConfig.enabledEvents.toMutableMap()

        HistoryEventType.values().filter { it.priority == priority }.forEach { eventType ->
            updatedEvents[eventType] = enabled
        }

        val newConfig = HistoryEventConfig(updatedEvents)
        saveConfig(newConfig)
    }

    /**
     * Reset to default configuration
     */
    fun resetToDefaults() {
        val defaultConfig = HistoryEventConfig(HistoryEventConfig.getDefaultConfig())
        saveConfig(defaultConfig)
    }

    /**
     * Check if an event type should be recorded
     * This is the main method called by UseCases before recording history
     */
    fun shouldRecordEvent(eventType: HistoryEventType): Boolean {
        return _settings.value.isEnabled(eventType)
    }

    /**
     * Get current configuration value
     */
    val currentConfig: HistoryEventConfig
        get() = _settings.value
}
