package com.example.questflow.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.calendar.CalendarEvent
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.preferences.CalendarFilterPreferences
import com.example.questflow.data.preferences.CalendarFilterSettings
import com.example.questflow.data.preferences.DateFilterType
import com.example.questflow.domain.usecase.RecordCalendarXpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class CalendarXpViewModel @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val recordCalendarXpUseCase: RecordCalendarXpUseCase,
    private val calendarManager: CalendarManager,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val filterPreferences: CalendarFilterPreferences,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private var selectedCategoryId: Long? = null

    private val _uiState = MutableStateFlow(CalendarXpUiState())
    val uiState: StateFlow<CalendarXpUiState> = _uiState.asStateFlow()

    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog.asStateFlow()

    val filterSettings: StateFlow<CalendarFilterSettings> = filterPreferences.getSettings()

    init {
        loadFilterSettings()
        loadCalendarLinks()
        loadCalendarEvents()
        loadStats()
    }

    private fun loadFilterSettings() {
        val settings = filterPreferences.getSettings().value
        _uiState.value = _uiState.value.copy(
            showCompleted = settings.showCompleted,
            showOpen = settings.showOpen,
            filterByCategory = settings.filterByCategory,
            dateFilterType = settings.dateFilterType
        )
    }

    fun updateSelectedCategory(categoryId: Long?) {
        selectedCategoryId = categoryId
        loadCalendarLinks()
    }

    fun toggleFilterDialog() {
        _showFilterDialog.value = !_showFilterDialog.value
    }

    fun updateFilterSettings(settings: CalendarFilterSettings) {
        filterPreferences.updateSettings(settings)
        _uiState.value = _uiState.value.copy(
            showCompleted = settings.showCompleted,
            showOpen = settings.showOpen,
            filterByCategory = settings.filterByCategory,
            dateFilterType = settings.dateFilterType
        )
        loadCalendarLinks()
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

    fun loadCalendarLinks() {
        viewModelScope.launch {
            calendarLinkRepository.getAllLinks().collect { allLinks ->
                val filteredLinks = filterLinks(allLinks)
                _uiState.value = _uiState.value.copy(links = filteredLinks)
            }
        }
    }

    private fun filterLinks(links: List<CalendarEventLinkEntity>): List<CalendarEventLinkEntity> {
        var filtered = links

        // Filter by completion status
        filtered = filtered.filter { link ->
            when {
                _uiState.value.showCompleted && _uiState.value.showOpen -> true
                _uiState.value.showCompleted -> link.rewarded
                _uiState.value.showOpen -> !link.rewarded
                else -> false
            }
        }

        // Filter by category
        if (_uiState.value.filterByCategory && selectedCategoryId != null) {
            filtered = filtered.filter { it.categoryId == selectedCategoryId }
        }

        // Filter by date
        val now = LocalDateTime.now()
        filtered = when (_uiState.value.dateFilterType) {
            DateFilterType.TODAY -> {
                filtered.filter { link ->
                    link.startsAt.toLocalDate() == LocalDate.now()
                }
            }
            DateFilterType.THIS_WEEK -> {
                val startOfWeek = now.minusDays(now.dayOfWeek.value.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(6)
                filtered.filter { link ->
                    link.startsAt >= startOfWeek && link.startsAt <= endOfWeek
                }
            }
            DateFilterType.THIS_MONTH -> {
                filtered.filter { link ->
                    link.startsAt.month == now.month && link.startsAt.year == now.year
                }
            }
            DateFilterType.CUSTOM_RANGE -> {
                // TODO: Implement custom range
                filtered
            }
            else -> filtered
        }

        return filtered
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

    fun claimXp(linkId: Long, onSuccess: (() -> Unit)? = null) {
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
                // Trigger refresh callback
                onSuccess?.invoke()
            } else {
                _uiState.value = _uiState.value.copy(notification = result.message)
            }
        }
    }

    fun clearXpAnimation() {
        _uiState.value = _uiState.value.copy(xpAnimationData = null)
    }

    fun createCalendarTask(
        title: String,
        description: String = "",
        dueDate: LocalDateTime,
        xpPercentage: Int,
        deleteOnClaim: Boolean,
        isRecurring: Boolean = false,
        recurringInterval: Int? = null
    ) {
        viewModelScope.launch {
            try {
                // Create task in database
                val task = TaskEntity(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    xpReward = 10,
                    xpPercentage = xpPercentage,
                    categoryId = selectedCategoryId,
                    isRecurring = isRecurring,
                    recurringType = if (isRecurring) "CUSTOM" else null,
                    recurringInterval = recurringInterval,
                    nextDueDate = if (isRecurring) dueDate else null
                )

                val taskId = taskRepository.insertTaskEntity(task)

                // Create calendar event
                val eventId = calendarManager.createTaskEvent(
                    taskTitle = title,
                    taskDescription = description,
                    startTime = dueDate,
                    endTime = dueDate.plusHours(1),
                    xpReward = 10
                )

                eventId?.let {
                    // Create calendar link
                    val link = CalendarEventLinkEntity(
                        calendarEventId = it,
                        title = title,
                        startsAt = dueDate,
                        endsAt = dueDate.plusHours(1),
                        xp = 0,
                        xpPercentage = xpPercentage,
                        categoryId = selectedCategoryId,
                        rewarded = false,
                        deleteOnClaim = deleteOnClaim,
                        taskId = taskId
                    )
                    calendarLinkRepository.insertLink(link)

                    // Update task with calendar event ID
                    taskRepository.updateTaskEntity(
                        task.copy(id = taskId, calendarEventId = it)
                    )
                }

                // Refresh calendar events
                loadCalendarEvents()
                loadCalendarLinks()
            } catch (e: Exception) {
                android.util.Log.e("CalendarXpViewModel", "Failed to create task: ${e.message}")
            }
        }
    }

    fun updateCalendarTask(
        linkId: Long,
        title: String,
        dueDate: LocalDateTime,
        xpPercentage: Int,
        deleteOnClaim: Boolean,
        reactivate: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val link = calendarLinkRepository.getLinkById(linkId) ?: return@launch

                // Update calendar event
                calendarManager.updateTaskEvent(
                    eventId = link.calendarEventId,
                    taskTitle = title,
                    taskDescription = "",
                    startTime = dueDate,
                    endTime = dueDate.plusHours(1)
                )

                // Update link
                val updatedLink = link.copy(
                    title = title,
                    startsAt = dueDate,
                    endsAt = dueDate.plusHours(1),
                    xpPercentage = xpPercentage,
                    deleteOnClaim = deleteOnClaim,
                    rewarded = if (reactivate) false else link.rewarded
                )
                calendarLinkRepository.updateLink(updatedLink)

                // Update associated task if exists
                link.taskId?.let { taskId ->
                    val task = taskRepository.getTaskEntityById(taskId)
                    task?.let {
                        taskRepository.updateTaskEntity(
                            it.copy(
                                title = title,
                                dueDate = dueDate,
                                xpPercentage = xpPercentage,
                                isCompleted = if (reactivate) false else it.isCompleted
                            )
                        )
                    }
                }

                // Refresh
                loadCalendarEvents()
                loadCalendarLinks()
            } catch (e: Exception) {
                android.util.Log.e("CalendarXpViewModel", "Failed to update task: ${e.message}")
            }
        }
    }
}

data class CalendarXpUiState(
    val links: List<CalendarEventLinkEntity> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val notification: String? = null,
    val xpAnimationData: XpAnimationData? = null,
    val totalXp: Long = 0,
    val level: Int = 1,
    val showCompleted: Boolean = true,
    val showOpen: Boolean = true,
    val filterByCategory: Boolean = false,
    val dateFilterType: DateFilterType = DateFilterType.ALL
)

data class XpAnimationData(
    val xpAmount: Int,
    val leveledUp: Boolean,
    val newLevel: Int
)