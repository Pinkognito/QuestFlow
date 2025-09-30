package com.example.questflow.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class DateFilterType {
    ALL, TODAY, THIS_WEEK, THIS_MONTH, CUSTOM_RANGE
}

data class CalendarFilterSettings(
    val showCompleted: Boolean = true,
    val showOpen: Boolean = true,
    val showExpired: Boolean = false,  // Filter for expired events
    val filterByCategory: Boolean = false,
    val dateFilterType: DateFilterType = DateFilterType.ALL,
    val customRangeStart: Long = 0,
    val customRangeEnd: Long = 0
) {
    fun isActive(): Boolean {
        return !showCompleted || !showOpen || showExpired || filterByCategory || dateFilterType != DateFilterType.ALL
    }
}

@Singleton
class CalendarFilterPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "calendar_filter_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SHOW_COMPLETED = "show_completed"
        private const val KEY_SHOW_OPEN = "show_open"
        private const val KEY_SHOW_EXPIRED = "show_expired"
        private const val KEY_FILTER_BY_CATEGORY = "filter_by_category"
        private const val KEY_DATE_FILTER_TYPE = "date_filter_type"
        private const val KEY_CUSTOM_START_DATE = "custom_start_date"
        private const val KEY_CUSTOM_END_DATE = "custom_end_date"
    }

    private val _settings = MutableStateFlow(loadSettings())

    fun getSettings(): StateFlow<CalendarFilterSettings> = _settings.asStateFlow()

    private fun loadSettings(): CalendarFilterSettings {
        return CalendarFilterSettings(
            showCompleted = prefs.getBoolean(KEY_SHOW_COMPLETED, true),
            showOpen = prefs.getBoolean(KEY_SHOW_OPEN, true),
            showExpired = prefs.getBoolean(KEY_SHOW_EXPIRED, false),
            filterByCategory = prefs.getBoolean(KEY_FILTER_BY_CATEGORY, false),
            dateFilterType = DateFilterType.valueOf(
                prefs.getString(KEY_DATE_FILTER_TYPE, DateFilterType.ALL.name) ?: DateFilterType.ALL.name
            ),
            customRangeStart = prefs.getLong(KEY_CUSTOM_START_DATE, 0),
            customRangeEnd = prefs.getLong(KEY_CUSTOM_END_DATE, 0)
        )
    }

    fun updateSettings(settings: CalendarFilterSettings) {
        prefs.edit()
            .putBoolean(KEY_SHOW_COMPLETED, settings.showCompleted)
            .putBoolean(KEY_SHOW_OPEN, settings.showOpen)
            .putBoolean(KEY_SHOW_EXPIRED, settings.showExpired)
            .putBoolean(KEY_FILTER_BY_CATEGORY, settings.filterByCategory)
            .putString(KEY_DATE_FILTER_TYPE, settings.dateFilterType.name)
            .putLong(KEY_CUSTOM_START_DATE, settings.customRangeStart)
            .putLong(KEY_CUSTOM_END_DATE, settings.customRangeEnd)
            .apply()

        _settings.value = settings
    }

    var showCompleted: Boolean
        get() = _settings.value.showCompleted
        set(value) {
            updateSettings(_settings.value.copy(showCompleted = value))
        }

    var showOpen: Boolean
        get() = _settings.value.showOpen
        set(value) {
            updateSettings(_settings.value.copy(showOpen = value))
        }

    var showExpired: Boolean
        get() = _settings.value.showExpired
        set(value) {
            updateSettings(_settings.value.copy(showExpired = value))
        }

    var filterByCategory: Boolean
        get() = _settings.value.filterByCategory
        set(value) {
            updateSettings(_settings.value.copy(filterByCategory = value))
        }

    var dateFilterType: DateFilterType
        get() = _settings.value.dateFilterType
        set(value) {
            updateSettings(_settings.value.copy(dateFilterType = value))
        }

    var customStartDate: Long
        get() = _settings.value.customRangeStart
        set(value) {
            updateSettings(_settings.value.copy(customRangeStart = value))
        }

    var customEndDate: Long
        get() = _settings.value.customRangeEnd
        set(value) {
            updateSettings(_settings.value.copy(customRangeEnd = value))
        }
}