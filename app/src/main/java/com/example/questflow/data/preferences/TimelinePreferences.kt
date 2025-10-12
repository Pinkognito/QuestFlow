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
 * Time range options for timeline view
 */
enum class TimeRange(val days: Int, val displayName: String) {
    ONE_DAY(1, "1 Tag"),
    THREE_DAYS(3, "3 Tage"),
    SEVEN_DAYS(7, "1 Woche"),
    FOURTEEN_DAYS(14, "2 Wochen")
}

/**
 * Settings for timeline view display and behavior
 */
data class TimelineSettings(
    val toleranceMinutes: Int = 30,
    val defaultTimeRange: TimeRange = TimeRange.THREE_DAYS,
    val hourRangeStart: Int = 6,
    val hourRangeEnd: Int = 22,
    val pixelsPerMinute: Float = 2f,
    val snapToGridMinutes: Int = 15
) {
    /**
     * Validate settings and return corrected version if needed
     */
    fun validated(): TimelineSettings {
        return copy(
            toleranceMinutes = toleranceMinutes.coerceIn(0, 120),
            hourRangeStart = hourRangeStart.coerceIn(0, 23),
            hourRangeEnd = hourRangeEnd.coerceIn(hourRangeStart + 1, 24),
            pixelsPerMinute = pixelsPerMinute.coerceIn(0.5f, 10f),
            snapToGridMinutes = snapToGridMinutes.coerceIn(1, 60)
        )
    }
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
        private const val KEY_TIME_RANGE = "time_range"
        private const val KEY_HOUR_START = "hour_start"
        private const val KEY_HOUR_END = "hour_end"
        private const val KEY_PX_PER_MINUTE = "px_per_minute"
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
            defaultTimeRange = TimeRange.valueOf(
                prefs.getString(KEY_TIME_RANGE, TimeRange.THREE_DAYS.name)
                    ?: TimeRange.THREE_DAYS.name
            ),
            hourRangeStart = prefs.getInt(KEY_HOUR_START, 6),
            hourRangeEnd = prefs.getInt(KEY_HOUR_END, 22),
            pixelsPerMinute = prefs.getFloat(KEY_PX_PER_MINUTE, 2f),
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
            .putString(KEY_TIME_RANGE, validated.defaultTimeRange.name)
            .putInt(KEY_HOUR_START, validated.hourRangeStart)
            .putInt(KEY_HOUR_END, validated.hourRangeEnd)
            .putFloat(KEY_PX_PER_MINUTE, validated.pixelsPerMinute)
            .putInt(KEY_SNAP_TO_GRID, validated.snapToGridMinutes)
            .apply()

        _settings.value = validated
    }

    /**
     * Update tolerance in minutes
     */
    var toleranceMinutes: Int
        get() = _settings.value.toleranceMinutes
        set(value) {
            updateSettings(_settings.value.copy(toleranceMinutes = value))
        }

    /**
     * Update default time range
     */
    var defaultTimeRange: TimeRange
        get() = _settings.value.defaultTimeRange
        set(value) {
            updateSettings(_settings.value.copy(defaultTimeRange = value))
        }

    /**
     * Update hour range start (0-23)
     */
    var hourRangeStart: Int
        get() = _settings.value.hourRangeStart
        set(value) {
            updateSettings(_settings.value.copy(hourRangeStart = value))
        }

    /**
     * Update hour range end (1-24)
     */
    var hourRangeEnd: Int
        get() = _settings.value.hourRangeEnd
        set(value) {
            updateSettings(_settings.value.copy(hourRangeEnd = value))
        }

    /**
     * Update pixels per minute (zoom level)
     */
    var pixelsPerMinute: Float
        get() = _settings.value.pixelsPerMinute
        set(value) {
            updateSettings(_settings.value.copy(pixelsPerMinute = value))
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
