package com.example.questflow.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.example.questflow.data.preferences.HistoryEventConfig
import com.example.questflow.data.preferences.HistoryPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for History Configuration
 *
 * Provides access to history event preferences and allows updating configuration.
 */
@HiltViewModel
class HistoryConfigViewModel @Inject constructor(
    private val historyPreferences: HistoryPreferences
) : ViewModel() {

    /**
     * Current history configuration as StateFlow
     */
    val config: StateFlow<HistoryEventConfig> = historyPreferences.getSettings()

    /**
     * Update the history configuration
     */
    fun updateConfig(newConfig: HistoryEventConfig) {
        historyPreferences.updateSettings(newConfig)
    }
}
