package com.example.questflow.presentation.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.database.entity.CategoryEntity
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
    private val categoryRepository: CategoryRepository,
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

    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory.asStateFlow()

    val categories = categoryRepository.getActiveCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadTasks()
        loadStats()
        checkCalendarPermission()
        loadSelectedCategory()
    }

    private fun checkCalendarPermission() {
        viewModelScope.launch {
            _hasCalendarPermission.value = calendarManager.hasCalendarPermission()
        }
    }

    fun getXpForPercentage(percentage: Int): String {
        // Use category level if selected, otherwise general level
        val currentLevel = _selectedCategory.value?.currentLevel ?: _uiState.value.level
        val xp = calculateXpRewardUseCase(percentage, currentLevel)
        return "$xp XP"
    }

    private fun loadSelectedCategory() {
        viewModelScope.launch {
            // Observe selected category from shared preferences or default
            categoryRepository.getOrCreateDefaultCategory().let { defaultCategory ->
                _selectedCategory.value = defaultCategory
            }
        }
    }

    fun syncSelectedCategory(category: CategoryEntity?) {
        _selectedCategory.value = category
        // Reload tasks for this category if needed
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getActiveTasks().collect { allTasks ->
                // Filter tasks by selected category
                val filteredTasks = if (_selectedCategory.value != null) {
                    allTasks.filter { task ->
                        task.categoryId == _selectedCategory.value?.id
                    }
                } else {
                    // Show tasks without category when "Allgemein" is selected
                    allTasks.filter { task ->
                        task.categoryId == null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    tasks = filteredTasks,
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
                xpReward = 10,
                categoryId = _selectedCategory.value?.id
            )
            taskRepository.insertTask(task)
        }
    }

    fun createTaskWithCalendar(
        title: String,
        description: String,
        xpPercentage: Int,
        dateTime: LocalDateTime,
        addToCalendar: Boolean,
        categoryId: Long? = null,
        deleteOnClaim: Boolean = false,
        deleteOnExpiry: Boolean = false,
        isRecurring: Boolean = false,
        recurringConfig: com.example.questflow.presentation.components.RecurringConfig? = null
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            // Check permission again before creating
            checkCalendarPermission()

            // Get current level for XP calculation (from category or general)
            val effectiveCategoryId = categoryId ?: _selectedCategory.value?.id
            val currentLevel = if (effectiveCategoryId != null) {
                categoryRepository.getCategoryById(effectiveCategoryId)?.currentLevel ?: 1
            } else {
                _uiState.value.level
            }
            val xpReward = calculateXpRewardUseCase(xpPercentage, currentLevel)

            // Create calendar event if requested (but delay link creation until after task)
            var calendarEventId: Long? = null
            if (addToCalendar && calendarManager.hasCalendarPermission()) {
                val category = effectiveCategoryId?.let { categoryRepository.getCategoryById(it) }
                val eventTitle = if (category != null) {
                    "${category.emoji} $title"
                } else {
                    "🎯 $title"
                }

                calendarEventId = calendarManager.createTaskEvent(
                    taskTitle = eventTitle,
                    taskDescription = description,
                    startTime = dateTime,
                    endTime = dateTime.plusHours(1),
                    xpReward = xpReward,
                    xpPercentage = xpPercentage,
                    categoryColor = category?.color
                )
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

            // Convert RecurringConfig to task parameters
            val recurringType = recurringConfig?.let {
                when (it.mode) {
                    com.example.questflow.presentation.components.RecurringMode.DAILY -> "DAILY"
                    com.example.questflow.presentation.components.RecurringMode.WEEKLY -> "WEEKLY"
                    com.example.questflow.presentation.components.RecurringMode.MONTHLY -> "MONTHLY"
                    com.example.questflow.presentation.components.RecurringMode.CUSTOM -> "CUSTOM"
                }
            }

            val recurringInterval = recurringConfig?.let {
                when (it.mode) {
                    com.example.questflow.presentation.components.RecurringMode.DAILY -> it.dailyInterval * 24 * 60
                    com.example.questflow.presentation.components.RecurringMode.WEEKLY -> 7 * 24 * 60 // Weekly uses days field instead
                    com.example.questflow.presentation.components.RecurringMode.MONTHLY -> it.monthlyDay * 24 * 60
                    com.example.questflow.presentation.components.RecurringMode.CUSTOM -> it.customHours * 60 + it.customMinutes
                }
            }

            val recurringDays = recurringConfig?.let {
                if (it.mode == com.example.questflow.presentation.components.RecurringMode.WEEKLY) {
                    it.weeklyDays.map { day -> day.value }.joinToString(",")
                } else null
            }

            // Create task with calendar event ID and percentage
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = dateTime,
                xpReward = xpReward,
                xpPercentage = xpPercentage,
                categoryId = effectiveCategoryId,
                calendarEventId = calendarEventId,
                isRecurring = isRecurring,
                recurringType = recurringType,
                recurringInterval = recurringInterval,
                recurringDays = recurringDays,
                triggerMode = recurringConfig?.triggerMode?.name
            )

            val taskId = taskRepository.insertTask(task)

            // Now create calendar link with taskId for XP tracking
            if (addToCalendar && calendarEventId != null) {
                createCalendarLinkUseCase(
                    calendarEventId = calendarEventId,
                    title = title,
                    startsAt = dateTime,
                    endsAt = dateTime.plusHours(1),
                    xp = xpReward,
                    xpPercentage = xpPercentage,
                    categoryId = effectiveCategoryId,
                    deleteOnClaim = deleteOnClaim,
                    deleteOnExpiry = deleteOnExpiry,
                    taskId = taskId
                )
            }
        }
    }

    fun updateCalendarTask(
        linkId: Long,
        taskId: Long,
        title: String,
        description: String,
        xpPercentage: Int,
        dateTime: LocalDateTime,
        categoryId: Long?,
        calendarEventId: Long?,
        shouldReactivate: Boolean = false,
        deleteOnClaim: Boolean = false,
        deleteOnExpiry: Boolean = false,
        isRecurring: Boolean = false,
        recurringConfig: com.example.questflow.presentation.components.RecurringConfig? = null
    ) {
        viewModelScope.launch {
            try {
                // Get the task to update
                val existingTask = taskRepository.getTaskById(taskId) ?: return@launch

                // Calculate new XP based on new percentage
                val currentLevel = if (categoryId != null) {
                    categoryRepository.getCategoryById(categoryId)?.currentLevel ?: 1
                } else {
                    _uiState.value.level
                }
                val newXpReward = calculateXpRewardUseCase(xpPercentage, currentLevel)

                // Map percentage to Priority enum
                val priority = when (xpPercentage) {
                    20, 40 -> Priority.LOW
                    60 -> Priority.MEDIUM
                    80, 100 -> Priority.HIGH
                    else -> Priority.MEDIUM
                }

                // Convert RecurringConfig to task parameters
                val recurringType = recurringConfig?.let {
                    when (it.mode) {
                        com.example.questflow.presentation.components.RecurringMode.DAILY -> "DAILY"
                        com.example.questflow.presentation.components.RecurringMode.WEEKLY -> "WEEKLY"
                        com.example.questflow.presentation.components.RecurringMode.MONTHLY -> "MONTHLY"
                        com.example.questflow.presentation.components.RecurringMode.CUSTOM -> "CUSTOM"
                    }
                }

                val recurringInterval = recurringConfig?.let {
                    when (it.mode) {
                        com.example.questflow.presentation.components.RecurringMode.DAILY -> it.dailyInterval * 24 * 60
                        com.example.questflow.presentation.components.RecurringMode.WEEKLY -> 7 * 24 * 60
                        com.example.questflow.presentation.components.RecurringMode.MONTHLY -> it.monthlyDay * 24 * 60
                        com.example.questflow.presentation.components.RecurringMode.CUSTOM -> it.customHours * 60 + it.customMinutes
                    }
                }

                val recurringDays = recurringConfig?.let {
                    if (it.mode == com.example.questflow.presentation.components.RecurringMode.WEEKLY) {
                        it.weeklyDays.map { day -> day.value }.joinToString(",")
                    } else null
                }

                // Update the task
                val updatedTask = existingTask.copy(
                    title = title,
                    description = description,
                    xpPercentage = xpPercentage,
                    xpReward = newXpReward,
                    dueDate = dateTime,
                    priority = priority,
                    categoryId = categoryId,
                    isRecurring = isRecurring,
                    recurringType = recurringType,
                    recurringInterval = recurringInterval,
                    recurringDays = recurringDays,
                    triggerMode = recurringConfig?.triggerMode?.name,
                    // Reactivate if needed
                    isCompleted = if (shouldReactivate) false else existingTask.isCompleted
                )
                taskRepository.updateTask(updatedTask)

                // Get the calendar link to update
                val existingLink = calendarLinkRepository.getLinkById(linkId)
                if (existingLink != null) {
                    val updatedLink = existingLink.copy(
                        title = title,
                        xpPercentage = xpPercentage,
                        startsAt = dateTime,
                        endsAt = dateTime.plusHours(1),
                        categoryId = categoryId,
                        deleteOnClaim = deleteOnClaim,
                        deleteOnExpiry = deleteOnExpiry,
                        isRecurring = isRecurring,
                        // Reset status and rewarded flag if reactivating
                        status = if (shouldReactivate) "PENDING" else existingLink.status,
                        rewarded = if (shouldReactivate) false else existingLink.rewarded
                    )
                    calendarLinkRepository.updateLink(updatedLink)
                }

                // Update the calendar event if it exists
                if (calendarEventId != null && calendarManager.hasCalendarPermission()) {
                    val category = categoryId?.let { categoryRepository.getCategoryById(it) }
                    val eventTitle = if (category != null) {
                        "${category.emoji} $title"
                    } else {
                        "🎯 $title"
                    }

                    calendarManager.updateTaskEvent(
                        eventId = calendarEventId,
                        taskTitle = eventTitle,
                        taskDescription = description,
                        startTime = dateTime,
                        endTime = dateTime.plusHours(1)
                    )
                }

                // Reload tasks to reflect changes
                loadTasks()

            } catch (e: Exception) {
                android.util.Log.e("TodayViewModel", "Failed to update calendar task", e)
            }
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
                        unlockedMemes = result.unlockedMemes,
                        categoryName = result.categoryName
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
    val unlockedMemes: List<String> = emptyList(),
    val categoryName: String? = null
)