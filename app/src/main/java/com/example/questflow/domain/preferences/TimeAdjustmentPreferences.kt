package com.example.questflow.domain.preferences

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences for automatic end-time adjustment
 *
 * When creating a new task, the end time can be automatically adjusted based on:
 * - INDEPENDENT: Start and End times are independent (no automatic adjustment)
 * - FIXED_DURATION: End time = Start time + fixed duration (e.g., 60 minutes)
 * - CURRENT_DISTANCE: Uses the existing distance between start and end
 */
@Singleton
class TimeAdjustmentPreferences @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "time_adjustment_prefs",
        Context.MODE_PRIVATE
    )

    enum class AdjustmentMode {
        INDEPENDENT,       // Start and End are fully independent
        FIXED_DURATION,    // End = Start + fixed duration
        CURRENT_DISTANCE   // End = Start + current distance (dynamic)
    }

    /**
     * Get the current adjustment mode
     */
    fun getAdjustmentMode(): AdjustmentMode {
        val modeString = prefs.getString(KEY_ADJUSTMENT_MODE, AdjustmentMode.INDEPENDENT.name)
        return try {
            AdjustmentMode.valueOf(modeString ?: AdjustmentMode.INDEPENDENT.name)
        } catch (e: IllegalArgumentException) {
            AdjustmentMode.INDEPENDENT
        }
    }

    /**
     * Set the adjustment mode
     */
    fun setAdjustmentMode(mode: AdjustmentMode) {
        prefs.edit().putString(KEY_ADJUSTMENT_MODE, mode.name).apply()
    }

    /**
     * Get the fixed duration in minutes (for FIXED_DURATION mode)
     * Default: 60 minutes
     */
    fun getFixedDurationMinutes(): Int {
        return prefs.getInt(KEY_FIXED_DURATION_MINUTES, 60)
    }

    /**
     * Set the fixed duration in minutes
     */
    fun setFixedDurationMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_FIXED_DURATION_MINUTES, minutes).apply()
    }

    companion object {
        private const val KEY_ADJUSTMENT_MODE = "adjustment_mode"
        private const val KEY_FIXED_DURATION_MINUTES = "fixed_duration_minutes"
    }
}
