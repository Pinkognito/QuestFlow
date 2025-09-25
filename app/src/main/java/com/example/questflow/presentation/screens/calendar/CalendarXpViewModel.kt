package com.example.questflow.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.calendar.CalendarEvent
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.domain.usecase.RecordCalendarXpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarXpViewModel @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val recordCalendarXpUseCase: RecordCalendarXpUseCase,
    private val calendarManager: CalendarManager,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarXpUiState())
    val uiState: StateFlow<CalendarXpUiState> = _uiState.asStateFlow()

    init {
        loadCalendarLinks()
        loadCalendarEvents()
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            statsRepository.getStatsFlow().collect { stats ->
                _uiState.value = _uiState.value.copy(
                    totalXp = stats.xp,
                    level = stats.level
                )
            }
        }
    }

    private fun loadCalendarLinks() {
        viewModelScope.launch {
            calendarLinkRepository.getAllLinks().collect { links ->
                _uiState.value = _uiState.value.copy(links = links)
            }
        }
    }

    private fun loadCalendarEvents() {
        viewModelScope.launch {
            val events = calendarManager.getCalendarEvents()
            _uiState.value = _uiState.value.copy(calendarEvents = events)
        }
    }

    fun refreshCalendarEvents() {
        loadCalendarEvents()
    }

    fun claimXp(linkId: Long) {
        viewModelScope.launch {
            val result = recordCalendarXpUseCase(linkId)
            if (result.success) {
                // Show XP animation
                _uiState.value = _uiState.value.copy(
                    xpAnimationData = XpAnimationData(
                        xpAmount = result.xpGranted ?: 0,
                        leveledUp = result.leveledUp,
                        newLevel = result.newLevel ?: 0
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(notification = result.message)
            }
        }
    }

    fun clearXpAnimation() {
        _uiState.value = _uiState.value.copy(xpAnimationData = null)
    }
}

data class CalendarXpUiState(
    val links: List<CalendarEventLinkEntity> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val notification: String? = null,
    val xpAnimationData: XpAnimationData? = null,
    val totalXp: Long = 0,
    val level: Int = 1
)

data class XpAnimationData(
    val xpAmount: Int,
    val leveledUp: Boolean,
    val newLevel: Int
)