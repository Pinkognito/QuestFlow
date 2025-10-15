package com.example.questflow.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for app-wide settings using SharedPreferences.
 * Provides reactive Flow for settings changes.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Long-Press Delay (in milliseconds)
    private val _longPressDelayMs = MutableStateFlow(getLongPressDelay())
    val longPressDelayMs: StateFlow<Long> = _longPressDelayMs.asStateFlow()

    companion object {
        private const val KEY_LONG_PRESS_DELAY = "long_press_delay_ms"
        private const val DEFAULT_LONG_PRESS_DELAY = 250L // Default: 250ms
    }

    /**
     * Get current long press delay in milliseconds.
     */
    fun getLongPressDelay(): Long {
        return prefs.getLong(KEY_LONG_PRESS_DELAY, DEFAULT_LONG_PRESS_DELAY)
    }

    /**
     * Set long press delay in milliseconds.
     * @param delayMs Delay between 50ms and 1000ms
     */
    fun setLongPressDelay(delayMs: Long) {
        val clampedDelay = delayMs.coerceIn(50L, 1000L)
        prefs.edit().putLong(KEY_LONG_PRESS_DELAY, clampedDelay).apply()
        _longPressDelayMs.value = clampedDelay
    }
}
