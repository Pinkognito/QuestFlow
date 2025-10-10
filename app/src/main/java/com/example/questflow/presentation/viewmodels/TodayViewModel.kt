package com.example.questflow.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.TaskContactLinkEntity
import com.example.questflow.data.database.dao.MetadataContactDao
import com.example.questflow.data.database.dao.TaskContactLinkDao
import com.example.questflow.data.repository.TextTemplateRepository
import com.example.questflow.data.repository.TaskContactTagRepository
import com.example.questflow.data.repository.TagUsageRepository
import com.example.questflow.data.repository.ActionHistoryRepository
import com.example.questflow.domain.action.ActionExecutor
import com.example.questflow.domain.placeholder.PlaceholderResolver
import com.example.questflow.domain.model.Priority
import com.example.questflow.domain.model.Task
import com.example.questflow.domain.usecase.CalculateXpRewardUseCase
import com.example.questflow.domain.usecase.CompleteTaskUseCase
import com.example.questflow.domain.usecase.CreateCalendarLinkUseCase
import com.example.questflow.domain.usecase.UpdateTaskWithCalendarUseCase
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
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase,
    private val updateTaskWithCalendarUseCase: UpdateTaskWithCalendarUseCase,
    private val metadataContactDao: MetadataContactDao,
    private val taskContactLinkDao: TaskContactLinkDao,
    private val textTemplateRepository: TextTemplateRepository,
    private val taskContactTagRepository: TaskContactTagRepository,
    private val tagUsageRepository: TagUsageRepository,
    val actionHistoryRepository: ActionHistoryRepository,
    val actionExecutor: ActionExecutor,
    val placeholderResolver: PlaceholderResolver,
    val multiContactActionManager: com.example.questflow.domain.action.MultiContactActionManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    // NOTE: calendarManager & calendarLinkRepository still needed for:
    // - checkCalendarPermission()
    // - createTaskWithCalendar() (creates new tasks)
    // - toggleTaskCompletion() (unclaim)

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

    val contacts = metadataContactDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val textTemplates = textTemplateRepository.getAllTemplatesFlow()
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
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        addToCalendar: Boolean,
        categoryId: Long? = null,
        deleteOnClaim: Boolean = false,
        deleteOnExpiry: Boolean = false,
        isRecurring: Boolean = false,
        recurringConfig: com.example.questflow.presentation.components.RecurringConfig? = null,
        parentTaskId: Long? = null,
        autoCompleteParent: Boolean = false,
        contactIds: Set<Long> = emptySet()
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

            // Check if the task is already expired
            val isExpired = endDateTime <= LocalDateTime.now()

            android.util.Log.d("TodayViewModel", "Creating task: $title")
            android.util.Log.d("TodayViewModel", "  Start: $startDateTime, End: $endDateTime, IsExpired: $isExpired")
            android.util.Log.d("TodayViewModel", "  DeleteOnExpiry: $deleteOnExpiry, AddToCalendar: $addToCalendar")

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

            // STEP 1: Create task FIRST (without calendarEventId yet)
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                dueDate = startDateTime,
                xpReward = xpReward,
                xpPercentage = xpPercentage,
                categoryId = effectiveCategoryId,
                calendarEventId = null, // Will be set later
                isRecurring = isRecurring,
                recurringType = recurringType,
                parentTaskId = parentTaskId,
                autoCompleteParent = autoCompleteParent,
                recurringInterval = recurringInterval,
                recurringDays = recurringDays,
                triggerMode = recurringConfig?.triggerMode?.name
            )

            val taskId = taskRepository.insertTask(task)

            // STEP 2: Create calendar event WITH taskId for deep linking
            var calendarEventId: Long? = null
            if (addToCalendar && calendarManager.hasCalendarPermission()) {
                // Don't create calendar event if it's already expired and deleteOnExpiry is true
                if (isExpired && deleteOnExpiry) {
                    android.util.Log.d("TodayViewModel", "NOT creating calendar event - already expired with deleteOnExpiry=true")
                    calendarEventId = -1L // Placeholder
                } else {
                    val category = effectiveCategoryId?.let { categoryRepository.getCategoryById(it) }
                    val eventTitle = if (category != null) {
                        "${category.emoji} $title"
                    } else {
                        "🎯 $title"
                    }

                    android.util.Log.d("TodayViewModel", "Creating calendar event WITH taskId for deep linking")
                    calendarEventId = calendarManager.createTaskEvent(
                        taskTitle = eventTitle,
                        taskDescription = description,
                        startTime = startDateTime,
                        endTime = endDateTime,
                        xpReward = xpReward,
                        xpPercentage = xpPercentage,
                        categoryColor = category?.color,
                        taskId = taskId // Deep link!
                    )
                }
            }

            // STEP 3: Update task with calendarEventId
            if (calendarEventId != null && calendarEventId != -1L) {
                taskRepository.updateTask(task.copy(id = taskId, calendarEventId = calendarEventId))
            }

            // Now create calendar link with taskId for XP tracking
            if (addToCalendar && calendarEventId != null && calendarEventId != -1L) {
                android.util.Log.d("TodayViewModel", "Creating calendar link for task")

                val linkId = createCalendarLinkUseCase(
                    calendarEventId = calendarEventId,
                    title = title,
                    startsAt = startDateTime,
                    endsAt = endDateTime,
                    xp = xpReward,
                    xpPercentage = xpPercentage,
                    categoryId = effectiveCategoryId,
                    deleteOnClaim = deleteOnClaim,
                    deleteOnExpiry = deleteOnExpiry,
                    taskId = taskId
                )

                // If task is already expired, immediately update the link status
                if (isExpired) {
                    android.util.Log.d("TodayViewModel", "Task is expired, updating link status to EXPIRED")
                    calendarLinkRepository.updateLinkStatus(linkId, "EXPIRED")
                }
            } else if (addToCalendar && calendarEventId == -1L) {
                // Still create the link for expired events, but with no real calendar event
                android.util.Log.d("TodayViewModel", "Creating calendar link for expired task (no actual calendar event)")

                val linkId = createCalendarLinkUseCase(
                    calendarEventId = System.currentTimeMillis(), // Fake ID
                    title = title,
                    startsAt = startDateTime,
                    endsAt = endDateTime,
                    xp = xpReward,
                    xpPercentage = xpPercentage,
                    categoryId = effectiveCategoryId,
                    deleteOnClaim = deleteOnClaim,
                    deleteOnExpiry = deleteOnExpiry,
                    taskId = taskId
                )

                // Mark as expired immediately
                calendarLinkRepository.updateLinkStatus(linkId, "EXPIRED")
            }

            // STEP 4: Schedule notification reminder 15 minutes before task
            if (addToCalendar && !isExpired) {
                scheduleTaskNotification(
                    taskId = taskId,
                    title = title,
                    description = description,
                    xpReward = xpReward,
                    startDateTime = startDateTime
                )
            }

            // STEP 5: Save contact links if any
            if (contactIds.isNotEmpty()) {
                saveTaskContactLinks(taskId, contactIds)
            }
        }
    }

    private fun scheduleTaskNotification(
        taskId: Long,
        title: String,
        description: String,
        xpReward: Int,
        startDateTime: LocalDateTime
    ) {
        // Use centralized notification scheduler
        val notificationScheduler = com.example.questflow.domain.notification.TaskNotificationScheduler(context)
        notificationScheduler.scheduleNotification(
            taskId = taskId,
            title = title,
            description = description,
            xpReward = xpReward,
            notificationTime = startDateTime
        )
    }

    // NOTE: updateCalendarTask removed - use TasksViewModel.updateCalendarTask instead

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
                // Also unclaim any associated calendar events
                calendarLinkRepository.unclaimByTaskId(taskId)
            }
        }
    }

    fun clearXpAnimation() {
        _uiState.value = _uiState.value.copy(xpAnimationData = null)
    }

    fun saveTaskContactLinks(taskId: Long, contactIds: Set<Long>) {
        viewModelScope.launch {
            // Delete existing links for this task
            taskContactLinkDao.deleteAllLinksForTask(taskId)

            // Insert new links
            contactIds.forEach { contactId ->
                taskContactLinkDao.insert(
                    TaskContactLinkEntity(
                        taskId = taskId,
                        contactId = contactId
                    )
                )
            }
        }
    }

    fun getTaskContactIds(taskId: Long): Flow<Set<Long>> {
        return taskContactLinkDao.getContactsByTaskId(taskId)
            .map { contacts -> contacts.map { it.id }.toSet() }
    }

    suspend fun getTagSuggestions(query: String) = tagUsageRepository.getRankedSuggestions(query)

    suspend fun saveTaskContactTags(taskId: Long, contactTagMap: Map<Long, List<String>>) {
        viewModelScope.launch {
            taskContactTagRepository.saveTaskContactTags(taskId, contactTagMap)
            // Update tag usage statistics
            val allTags = contactTagMap.values.flatten().distinct()
            tagUsageRepository.incrementUsageForMultiple(allTags)
        }
    }

    fun getTextTemplates() = textTemplateRepository.getAllTemplatesFlow()

    fun getActionHistory(taskId: Long) = actionHistoryRepository.getHistoryForTask(taskId)
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