package com.example.questflow.presentation.screens.tasks

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
import com.example.questflow.data.preferences.UIPreferences
import com.example.questflow.domain.usecase.RecordCalendarXpUseCase
import com.example.questflow.domain.usecase.CheckExpiredEventsUseCase
import com.example.questflow.domain.usecase.CalculateXpRewardUseCase
import com.example.questflow.domain.usecase.UpdateTaskWithCalendarUseCase
import com.example.questflow.presentation.components.RecurringConfig
import com.example.questflow.presentation.components.RecurringMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val recordCalendarXpUseCase: RecordCalendarXpUseCase,
    private val calendarManager: CalendarManager,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val filterPreferences: CalendarFilterPreferences,
    private val uiPreferences: UIPreferences,
    private val taskRepository: TaskRepository,
    private val checkExpiredEventsUseCase: CheckExpiredEventsUseCase,
    private val updateTaskWithCalendarUseCase: UpdateTaskWithCalendarUseCase,
    private val taskContactTagRepository: com.example.questflow.data.repository.TaskContactTagRepository
) : ViewModel() {
    // NOTE: calculateXpRewardUseCase entfernt - wird jetzt in UpdateTaskWithCalendarUseCase verwendet
    // NOTE: calendarManager, statsRepository, categoryRepository, taskRepository still needed for:
    // - loadCalendarEvents()
    // - claimXp()
    // - createCalendarTask()
    // - checkExpiredEventsUseCase()

    private var selectedCategoryId: Long? = null

    private val _uiState = MutableStateFlow(CalendarXpUiState())
    val uiState: StateFlow<CalendarXpUiState> = _uiState.asStateFlow()

    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog.asStateFlow()

    val filterSettings: StateFlow<CalendarFilterSettings> = filterPreferences.getSettings()
    val uiSettings: StateFlow<com.example.questflow.data.preferences.UISettings> = uiPreferences.getSettings()

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

    fun updateUISettings(settings: com.example.questflow.data.preferences.UISettings) {
        uiPreferences.updateSettings(settings)
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
            // Check for expired events first
            try {
                checkExpiredEventsUseCase()
            } catch (e: Exception) {
                android.util.Log.e("TasksViewModel", "Failed to check expired events", e)
            }

            calendarLinkRepository.getAllLinks().collect { allLinks ->
                val filteredLinks = filterLinks(allLinks)
                _uiState.value = _uiState.value.copy(links = filteredLinks)
            }
        }
    }

    private fun filterLinks(links: List<CalendarEventLinkEntity>): List<CalendarEventLinkEntity> {
        var filtered = links

        // Update status based on rewarded flag only (not expiry date)
        val now = LocalDateTime.now()
        filtered = filtered.map { link ->
            if (link.rewarded && link.status != "CLAIMED") {
                // Mark as claimed if rewarded
                link.copy(status = "CLAIMED")
            } else {
                link
            }
        }

        // Filter by status and expiry
        filtered = filtered.filter { link ->
            val showOpen = filterSettings.value.showOpen
            val showCompleted = filterSettings.value.showCompleted
            val showExpired = filterSettings.value.showExpired
            val isExpired = link.endsAt < now

            when {
                link.status == "CLAIMED" -> showCompleted
                isExpired && !link.rewarded -> showExpired  // Expired but not claimed
                !isExpired && !link.rewarded -> showOpen    // Open and not expired
                else -> showOpen
            }
        }

        // Filter by category
        if (_uiState.value.filterByCategory && selectedCategoryId != null) {
            filtered = filtered.filter { it.categoryId == selectedCategoryId }
        }

        // Filter by date
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
                val startTimestamp = filterSettings.value.customRangeStart
                val endTimestamp = filterSettings.value.customRangeEnd

                if (startTimestamp > 0 && endTimestamp > 0) {
                    val startDateTime = LocalDateTime.ofEpochSecond(startTimestamp, 0, java.time.ZoneOffset.UTC)
                    val endDateTime = LocalDateTime.ofEpochSecond(endTimestamp, 0, java.time.ZoneOffset.UTC)

                    filtered.filter { link ->
                        // Task must start within or overlap with the custom range
                        (link.startsAt >= startDateTime && link.startsAt <= endDateTime) ||
                        (link.endsAt >= startDateTime && link.endsAt <= endDateTime) ||
                        (link.startsAt <= startDateTime && link.endsAt >= endDateTime)
                    }
                } else {
                    filtered
                }
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

                // Get category for emoji and color
                val category = selectedCategoryId?.let { categoryRepository.getCategoryById(it) }
                val eventTitle = if (category != null) {
                    "${category.emoji} $title"
                } else {
                    "🎯 $title"
                }

                // Create calendar event WITH taskId for deep linking
                val eventId = calendarManager.createTaskEvent(
                    taskTitle = eventTitle,
                    taskDescription = description,
                    startTime = dueDate,
                    endTime = dueDate.plusHours(1),
                    xpReward = 10,
                    xpPercentage = xpPercentage,
                    categoryColor = category?.color,
                    taskId = taskId
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
                android.util.Log.e("TasksViewModel", "Failed to create task: ${e.message}")
            }
        }
    }

    /**
     * Unified update method for calendar tasks.
     * Works for both tasks with and without taskId (calendar-only events).
     * Uses the central UpdateTaskWithCalendarUseCase for consistency.
     */
    fun updateCalendarTask(
        linkId: Long,
        taskId: Long?,
        title: String,
        description: String,
        xpPercentage: Int,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        categoryId: Long?,
        shouldReactivate: Boolean = false,
        addToCalendar: Boolean = true,
        deleteOnClaim: Boolean = false,
        deleteOnExpiry: Boolean = false,
        isRecurring: Boolean = false,
        recurringConfig: RecurringConfig? = null,
        parentTaskId: Long? = null,
        autoCompleteParent: Boolean = false
    ) {
        android.util.Log.d("DescriptionFlow-ViewModel", "📥 RECEIVED in ViewModel: taskId=$taskId, linkId=$linkId, description='$description'")
        viewModelScope.launch {
            val params = UpdateTaskWithCalendarUseCase.UpdateParams(
                taskId = taskId, // Can be null for calendar-only events
                linkId = linkId,
                title = title,
                description = description,
                xpPercentage = xpPercentage,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                categoryId = categoryId,
                shouldReactivate = shouldReactivate,
                addToCalendar = addToCalendar,
                deleteOnClaim = deleteOnClaim,
                deleteOnExpiry = deleteOnExpiry,
                isRecurring = isRecurring,
                recurringConfig = recurringConfig,
                parentTaskId = parentTaskId,
                autoCompleteParent = autoCompleteParent
            )
            android.util.Log.d("DescriptionFlow-ViewModel", "📦 SENDING UpdateParams to UseCase: description='${params.description}'")

            when (val result = updateTaskWithCalendarUseCase(params)) {
                is UpdateTaskWithCalendarUseCase.UpdateResult.Success -> {
                    android.util.Log.d("TasksViewModel", "Task updated successfully via UseCase")
                    loadCalendarEvents()
                    loadCalendarLinks()
                }
                is UpdateTaskWithCalendarUseCase.UpdateResult.Error -> {
                    android.util.Log.e("TasksViewModel", "Update failed: ${result.message}", result.throwable)
                }
            }
        }
    }

    fun findLinkByTaskId(taskId: Long): CalendarEventLinkEntity? {
        return _uiState.value.links.find { it.taskId == taskId }
    }

    suspend fun findTaskById(taskId: Long): com.example.questflow.domain.model.Task? {
        return taskRepository.getTaskById(taskId)
    }

    fun openTaskFromDeepLink(taskId: Long, onLinkFound: (CalendarEventLinkEntity?) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("TasksViewModel", "openTaskFromDeepLink: $taskId")

                // First try to find calendar link
                val link = _uiState.value.links.find { it.taskId == taskId }
                if (link != null) {
                    android.util.Log.d("TasksViewModel", "Found calendar link")
                    onLinkFound(link)
                    return@launch
                }

                // If no link, find task and create temp link
                android.util.Log.d("TasksViewModel", "No link, finding task...")
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    android.util.Log.d("TasksViewModel", "Found task: ${task.title}")
                    val tempLink = CalendarEventLinkEntity(
                        id = 0,
                        calendarEventId = task.calendarEventId ?: 0,
                        title = task.title,
                        startsAt = task.dueDate ?: java.time.LocalDateTime.now(),
                        endsAt = (task.dueDate ?: java.time.LocalDateTime.now()).plusHours(1),
                        xp = task.xpReward,
                        xpPercentage = task.xpPercentage ?: 60,
                        categoryId = task.categoryId,
                        taskId = task.id
                    )
                    onLinkFound(tempLink)
                } else {
                    android.util.Log.w("TasksViewModel", "Task not found")
                    onLinkFound(null)
                }
            } catch (e: Exception) {
                android.util.Log.e("TasksViewModel", "Error in openTaskFromDeepLink", e)
                onLinkFound(null)
            }
        }
    }

    fun getAvailableTasksFlow() = taskRepository.getActiveTasks()

    /**
     * Get calendar link by task ID
     * Returns the existing link or creates a temporary one from task data
     */
    suspend fun getLinkByTaskId(taskId: Long): CalendarEventLinkEntity? {
        return try {
            // First try to find existing calendar link
            val link = _uiState.value.links.find { it.taskId == taskId }
            if (link != null) {
                return link
            }

            // If no link exists, find task and create temp link
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                CalendarEventLinkEntity(
                    id = 0,
                    calendarEventId = task.calendarEventId ?: 0,
                    title = task.title,
                    startsAt = task.dueDate ?: LocalDateTime.now(),
                    endsAt = (task.dueDate ?: LocalDateTime.now()).plusHours(1),
                    xp = task.xpReward,
                    xpPercentage = task.xpPercentage ?: 60,
                    categoryId = task.categoryId,
                    taskId = task.id,
                    rewarded = false,
                    deleteOnClaim = false,
                    deleteOnExpiry = false,
                    status = "ACTIVE",
                    isRecurring = task.isRecurring,
                    recurringTaskId = task.id
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("TasksViewModel", "Error getting link by task ID", e)
            null
        }
    }

    /**
     * Load task-specific contact tags
     */
    suspend fun loadTaskContactTags(taskId: Long): Map<Long, List<String>> {
        return taskContactTagRepository.loadTaskContactTagsMap(taskId)
    }

    /**
     * Save task-specific contact tags
     */
    fun saveTaskContactTags(taskId: Long, contactTagMap: Map<Long, List<String>>) {
        viewModelScope.launch {
            taskContactTagRepository.saveTaskContactTags(taskId, contactTagMap)
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