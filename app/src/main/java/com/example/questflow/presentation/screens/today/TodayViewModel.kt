package com.example.questflow.presentation.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.domain.model.Priority
import com.example.questflow.domain.model.Task
import com.example.questflow.domain.usecase.CalculateXpRewardUseCase
import com.example.questflow.domain.usecase.CompleteTaskUseCase
import com.example.questflow.domain.usecase.CreateCalendarLinkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val statsRepository: StatsRepository,
    private val calendarManager: CalendarManager,
    private val calendarLinkRepository: CalendarLinkRepository,
    private val createCalendarLinkUseCase: CreateCalendarLinkUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(false)
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    init {
        loadTasks()
        loadStats()
        checkCalendarPermission()
    }

    private fun checkCalendarPermission() {
        viewModelScope.launch {
            _hasCalendarPermission.value = calendarManager.hasCalendarPermission()
        }
    }

    fun getXpForPercentage(percentage: Int): String {
        val currentLevel = _uiState.value.level
        val xp = calculateXpRewardUseCase(percentage, currentLevel)
        return "$xp XP"
    }

    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getActiveTasks().collect { tasks ->
                _uiState.value = _uiState.value.copy(
                    tasks = tasks,
                    isLoading = false
                )
            }
        }
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
    
    fun addQuickTask(title: String) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val task = Task(
                title = title,
                priority = Priority.MEDIUM,
                xpReward = 10
            )
            taskRepository.insertTask(task)
        }
    }

    fun createTaskWithCalendar(
        title: String,
        description: String,
        xpPercentage: Int,
        dateTime: LocalDateTime,
        addToCalendar: Boolean
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            // Check permission again before creating
            checkCalendarPermission()

            // Get current level for XP calculation
            val currentLevel = _uiState.value.level
            val xpReward = calculateXpRewardUseCase(xpPercentage, currentLevel)

            // Create calendar event if requested
            var calendarEventId: Long? = null
            if (addToCalendar && calendarManager.hasCalendarPermission()) {
                calendarEventId = calendarManager.createTaskEvent(
                    taskTitle = title,
                    taskDescription = description,
                    startTime = dateTime,
                    endTime = dateTime.plusHours(1),
                    xpReward = xpReward,
                    xpPercentage = xpPercentage
                )

                // Also create calendar link for XP tracking
                calendarEventId?.let { eventId ->
                    createCalendarLinkUseCase(
                        calendarEventId = eventId,
                        title = title,
                        startsAt = dateTime,
                        endsAt = dateTime.plusHours(1),
                        xp = xpReward,
                        xpPercentage = xpPercentage
                    )
                }
            }

            // Map percentage to Priority enum for visual distinction
            val priority = when (xpPercentage) {
                20 -> Priority.LOW
                40 -> Priority.LOW
                60 -> Priority.MEDIUM
                80 -> Priority.HIGH
                100 -> Priority.URGENT
                else -> Priority.MEDIUM
            }

            // Create task with calendar event ID and percentage
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = dateTime,
                xpReward = xpReward,
                xpPercentage = xpPercentage,
                calendarEventId = calendarEventId
            )

            taskRepository.insertTask(task)
        }
    }
    
    fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            if (!isCompleted) {
                // Complete task and grant XP
                val result = completeTaskUseCase(taskId)
                // Show XP animation
                _uiState.value = _uiState.value.copy(
                    xpAnimationData = XpAnimationData(
                        xpAmount = result.xpGranted ?: 0,
                        leveledUp = result.leveledUp,
                        newLevel = result.newLevel ?: 0,
                        unlockedMemes = result.unlockedMemes
                    )
                )
            } else {
                // Uncomplete task
                taskRepository.toggleTaskCompletion(taskId, false)
            }
        }
    }

    fun clearXpAnimation() {
        _uiState.value = _uiState.value.copy(xpAnimationData = null)
    }
}

data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val totalXp: Long = 0,
    val level: Int = 1,
    val levelUpNotification: String? = null,
    val xpAnimationData: XpAnimationData? = null
)

data class XpAnimationData(
    val xpAmount: Int,
    val leveledUp: Boolean,
    val newLevel: Int,
    val unlockedMemes: List<String> = emptyList()
)