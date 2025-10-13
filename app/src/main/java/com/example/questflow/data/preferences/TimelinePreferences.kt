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
 * Settings for timeline view display and behavior.
 *
 * UPDATED: After refactoring to vertical time axis and infinite scroll:
 * - Tolerance now 0-24 hours (0-1440 minutes)
 * - No more fixed time ranges (infinite scroll)
 * - visibleHours controls zoom (2-24 hours visible on screen)
 * - pixelsPerMinute calculated dynamically based on screen height and visibleHours
 */
data class TimelineSettings(
    val toleranceMinutes: Int = 30,
    val visibleHours: Float = 12f, // How many hours visible on screen (2-24)
    val snapToGridMinutes: Int = 15
) {
    /**
     * Validate settings and return corrected version if needed
     */
    fun validated(): TimelineSettings {
        return copy(
            toleranceMinutes = toleranceMinutes.coerceIn(0, 1440), // 0-24 hours
            visibleHours = visibleHours.coerceIn(2f, 24f),
            snapToGridMinutes = snapToGridMinutes.coerceIn(1, 60)
        )
    }

    /**
     * Calculate DP per minute based on screen height.
     * IMPORTANT: This MUST be calculated for the FULL 24-hour day (1440 minutes),
     * NOT for visibleHours! The coordinate system uses absolute positions from 00:00-23:59.
     *
     * screenHeightDp: Available screen height in DP (e.g., 800dp)
     * Returns: DP per minute for the full 24-hour timeline
     */
    fun calculatePixelsPerMinute(screenHeightDp: Float): Float {
        // CRITICAL: Use full day (24 hours = 1440 minutes) for coordinate calculations
        val fullDayMinutes = 24 * 60f  // Always 1440 minutes
        return (screenHeightDp / fullDayMinutes).coerceIn(0.5f, 20f)
    }

    /**
     * Get tolerance in hours (for display)
     */
    val toleranceHours: Float
        get() = toleranceMinutes / 60f
}

/**
 * Manages timeline view preferences with SharedPreferences backend
 */
@Singleton
class TimelinePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "timeline_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_TOLERANCE_MINUTES = "tolerance_minutes"
        private const val KEY_VISIBLE_HOURS = "visible_hours"
        private const val KEY_SNAP_TO_GRID = "snap_to_grid"
    }

    private val _settings = MutableStateFlow(loadSettings())

    /**
     * Get settings as StateFlow for reactive updates
     */
    fun getSettings(): StateFlow<TimelineSettings> = _settings.asStateFlow()

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings(): TimelineSettings {
        val settings = TimelineSettings(
            toleranceMinutes = prefs.getInt(KEY_TOLERANCE_MINUTES, 30),
            visibleHours = prefs.getFloat(KEY_VISIBLE_HOURS, 12f),
            snapToGridMinutes = prefs.getInt(KEY_SNAP_TO_GRID, 15)
        )
        return settings.validated()
    }

    /**
     * Update all settings at once
     */
    fun updateSettings(settings: TimelineSettings) {
        val validated = settings.validated()

        prefs.edit()
            .putInt(KEY_TOLERANCE_MINUTES, validated.toleranceMinutes)
            .putFloat(KEY_VISIBLE_HOURS, validated.visibleHours)
            .putInt(KEY_SNAP_TO_GRID, validated.snapToGridMinutes)
            .apply()

        _settings.value = validated
    }

    /**
     * Update tolerance in minutes (0-1440 = 0-24 hours)
     */
    var toleranceMinutes: Int
        get() = _settings.value.toleranceMinutes
        set(value) {
            updateSettings(_settings.value.copy(toleranceMinutes = value))
        }

    /**
     * Update visible hours (zoom level, 2-24 hours)
     */
    var visibleHours: Float
        get() = _settings.value.visibleHours
        set(value) {
            updateSettings(_settings.value.copy(visibleHours = value))
        }

    /**
     * Update snap-to-grid interval in minutes
     */
    var snapToGridMinutes: Int
        get() = _settings.value.snapToGridMinutes
        set(value) {
            updateSettings(_settings.value.copy(snapToGridMinutes = value))
        }

    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        updateSettings(TimelineSettings())
    }
}
