package com.example.questflow.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.repository.TimeBlockRepository
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.TaskContactLinkEntity
import com.example.questflow.data.database.entity.TaskHistoryEntity
import com.example.questflow.data.database.dao.MetadataContactDao
import com.example.questflow.data.database.dao.TaskContactLinkDao
import com.example.questflow.data.database.dao.TaskHistoryDao
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
import com.example.questflow.domain.usecase.DetectScheduleConflictsUseCase
import com.example.questflow.domain.usecase.FindFreeTimeSlotsUseCase
import com.example.questflow.data.preferences.UIPreferences
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
    private val timeBlockRepository: TimeBlockRepository,
    private val createCalendarLinkUseCase: CreateCalendarLinkUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase,
    private val updateTaskWithCalendarUseCase: UpdateTaskWithCalendarUseCase,
    private val detectScheduleConflictsUseCase: DetectScheduleConflictsUseCase,
    private val findFreeTimeSlotsUseCase: FindFreeTimeSlotsUseCase,
    private val metadataContactDao: MetadataContactDao,
    private val taskContactLinkDao: TaskContactLinkDao,
    private val taskHistoryDao: TaskHistoryDao,
    private val textTemplateRepository: TextTemplateRepository,
    private val taskContactTagRepository: TaskContactTagRepository,
    private val tagUsageRepository: TagUsageRepository,
    val actionHistoryRepository: ActionHistoryRepository,
    val actionExecutor: ActionExecutor,
    val placeholderResolver: PlaceholderResolver,
    val multiContactActionManager: com.example.questflow.domain.action.MultiContactActionManager,
    private val uiPreferences: UIPreferences,
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

    // PERFORMANCE OPTIMIZATION: Month-based calendar data loading (shared with TasksViewModel)
    private val _currentMonth = MutableStateFlow(java.time.YearMonth.now())
    val currentMonth: StateFlow<java.time.YearMonth> = _currentMonth.asStateFlow()

    val categories = categoryRepository.getActiveCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTimeBlocks = timeBlockRepository.getActiveTimeBlocksWithTagsFlow()
        .map { list -> list.map { it.timeBlock } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiSettings: StateFlow<com.example.questflow.data.preferences.UISettings> = uiPreferences.getSettings()

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

    fun updateUISettings(settings: com.example.questflow.data.preferences.UISettings) {
        uiPreferences.updateSettings(settings)
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
                android.util.Log.d("TodayViewModel", "📋 loadTasks: Loaded ${allTasks.size} active tasks")
                android.util.Log.d("TodayViewModel", "📋 Selected category: ${_selectedCategory.value?.name} (ID: ${_selectedCategory.value?.id})")

                allTasks.forEachIndexed { index, task ->
                    android.util.Log.d("TodayViewModel", "📋   Task $index: '${task.title}' (categoryId=${task.categoryId}, isCompleted=${task.isCompleted})")
                }

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

                android.util.Log.d("TodayViewModel", "📋 After filtering: ${filteredTasks.size} tasks")

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
        contactIds: Set<Long> = emptySet(),
        calendarEventCustomTitle: String? = null,
        calendarEventCustomDescription: String? = null
    ) {
        if (title.isBlank()) return

        // Debug log incoming parameters
        android.util.Log.d("TodayViewModel", "createTaskWithCalendar called:")
        android.util.Log.d("TodayViewModel", "  isRecurring: $isRecurring")
        android.util.Log.d("TodayViewModel", "  recurringConfig: $recurringConfig")
        android.util.Log.d("TodayViewModel", "  recurringConfig.mode: ${recurringConfig?.mode}")
        android.util.Log.d("TodayViewModel", "  recurringConfig.triggerMode: ${recurringConfig?.triggerMode}")

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
                triggerMode = recurringConfig?.triggerMode?.name,
                calendarEventCustomTitle = calendarEventCustomTitle,
                calendarEventCustomDescription = calendarEventCustomDescription
            )

            android.util.Log.d("TodayViewModel", "Task entity before insert:")
            android.util.Log.d("TodayViewModel", "  task.isRecurring: ${task.isRecurring}")
            android.util.Log.d("TodayViewModel", "  task.recurringType: ${task.recurringType}")
            android.util.Log.d("TodayViewModel", "  task.recurringInterval: ${task.recurringInterval}")
            android.util.Log.d("TodayViewModel", "  task.recurringDays: ${task.recurringDays}")
            android.util.Log.d("TodayViewModel", "  task.triggerMode: ${task.triggerMode}")

            val taskId = taskRepository.insertTask(task)

            // STEP 1.5: Resolve placeholders in title and description using the first contact (if any)
            val contactId = contactIds.firstOrNull()
            val resolvedTitle = if (title.contains("{")) {
                placeholderResolver.resolve(title, taskId, contactId)
            } else {
                title
            }
            val resolvedDescription = if (description.contains("{")) {
                placeholderResolver.resolve(description, taskId, contactId)
            } else {
                description
            }

            // Resolve placeholders in calendar event custom fields
            val resolvedCalendarTitle = if (calendarEventCustomTitle != null && calendarEventCustomTitle.contains("{")) {
                placeholderResolver.resolve(calendarEventCustomTitle, taskId, contactId)
            } else {
                calendarEventCustomTitle
            }
            val resolvedCalendarDescription = if (calendarEventCustomDescription != null && calendarEventCustomDescription.contains("{")) {
                placeholderResolver.resolve(calendarEventCustomDescription, taskId, contactId)
            } else {
                calendarEventCustomDescription
            }

            // Update task with resolved values if placeholders were found
            if (resolvedTitle != title || resolvedDescription != description ||
                resolvedCalendarTitle != calendarEventCustomTitle ||
                resolvedCalendarDescription != calendarEventCustomDescription) {
                android.util.Log.d("TodayViewModel", "Resolved placeholders:")
                android.util.Log.d("TodayViewModel", "  Title: '$title' -> '$resolvedTitle'")
                android.util.Log.d("TodayViewModel", "  Description: '$description' -> '$resolvedDescription'")
                android.util.Log.d("TodayViewModel", "  Calendar Title: '$calendarEventCustomTitle' -> '$resolvedCalendarTitle'")
                android.util.Log.d("TodayViewModel", "  Calendar Description: '$calendarEventCustomDescription' -> '$resolvedCalendarDescription'")

                taskRepository.updateTask(task.copy(
                    id = taskId,
                    title = resolvedTitle,
                    description = resolvedDescription,
                    calendarEventCustomTitle = resolvedCalendarTitle,
                    calendarEventCustomDescription = resolvedCalendarDescription
                ))
            }

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
                        taskDescription = resolvedDescription,
                        startTime = startDateTime,
                        endTime = endDateTime,
                        xpReward = xpReward,
                        xpPercentage = xpPercentage,
                        categoryColor = category?.color,
                        taskId = taskId, // Deep link!
                        customTitle = resolvedCalendarTitle,
                        customDescription = resolvedCalendarDescription
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
                    title = resolvedTitle,
                    startsAt = startDateTime,
                    endsAt = endDateTime,
                    xp = xpReward,
                    xpPercentage = xpPercentage,
                    categoryId = effectiveCategoryId,
                    deleteOnClaim = deleteOnClaim,
                    deleteOnExpiry = deleteOnExpiry,
                    taskId = taskId,
                    isRecurring = isRecurring,
                    recurringTaskId = if (isRecurring) taskId else null  // FIX P1-002: Link to recurring task
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
                    title = resolvedTitle,
                    startsAt = startDateTime,
                    endsAt = endDateTime,
                    xp = xpReward,
                    xpPercentage = xpPercentage,
                    categoryId = effectiveCategoryId,
                    deleteOnClaim = deleteOnClaim,
                    deleteOnExpiry = deleteOnExpiry,
                    taskId = taskId,
                    isRecurring = isRecurring,
                    recurringTaskId = if (isRecurring) taskId else null  // FIX P1-002: Link to recurring task
                )

                // Mark as expired immediately
                calendarLinkRepository.updateLinkStatus(linkId, "EXPIRED")
            }

            // STEP 4: Schedule notification reminder 15 minutes before task
            if (addToCalendar && !isExpired) {
                scheduleTaskNotification(
                    taskId = taskId,
                    title = resolvedTitle,
                    description = resolvedDescription,
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

    suspend fun getDefaultCalendarTemplate() = textTemplateRepository.getDefaultByType("CALENDAR")

    fun getActionHistory(taskId: Long) = actionHistoryRepository.getHistoryForTask(taskId)

    /**
     * Get task history for a specific task
     */
    fun getTaskHistory(taskId: Long): Flow<List<TaskHistoryEntity>> {
        return taskHistoryDao.getHistoryForTask(taskId)
    }

    /**
     * Check for scheduling conflicts with existing events
     */
    suspend fun checkScheduleConflicts(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ) = detectScheduleConflictsUseCase(startTime, endTime, excludeEventId, excludeTaskId, excludeLinkId)

    /**
     * Find free time slots for task scheduling
     */
    suspend fun findFreeTimeSlots(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        minDurationMinutes: Long = 30,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ) = findFreeTimeSlotsUseCase(
        startDate = startDate,
        endDate = endDate,
        minDurationMinutes = minDurationMinutes,
        excludeEventId = excludeEventId,
        excludeTaskId = excludeTaskId,
        excludeLinkId = excludeLinkId
    )

    /**
     * Suggest optimal time slots based on task duration
     */
    suspend fun suggestTimeSlots(
        requiredDurationMinutes: Long,
        startSearchFrom: LocalDateTime = LocalDateTime.now(),
        maxSuggestions: Int = 5,
        excludeEventId: Long? = null,
        excludeTaskId: Long? = null,
        excludeLinkId: Long? = null
    ) = findFreeTimeSlotsUseCase.suggestTimeSlots(
        requiredDurationMinutes = requiredDurationMinutes,
        startSearchFrom = startSearchFrom,
        maxSuggestions = maxSuggestions,
        excludeEventId = excludeEventId,
        excludeTaskId = excludeTaskId,
        excludeLinkId = excludeLinkId
    )

    /**
     * PERFORMANCE OPTIMIZATION: Set current month for calendar display
     * Updates the month state which triggers re-composition in AddTaskDialog
     */
    fun setCurrentMonth(month: java.time.YearMonth) {
        android.util.Log.d("TodayViewModel", "📅 setCurrentMonth: $month")
        _currentMonth.value = month
    }

    /**
     * Get calendar events in a date range for month view occupancy visualization
     * COMBINES app-created events (CalendarEventLinkEntity) with external Google Calendar events
     */
    fun getCalendarEventsInRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate) = flow {
        // 1. Get app-created calendar event links
        calendarLinkRepository.getEventsInRange(startDate, endDate).collect { appEvents ->
            // 2. Get external Google Calendar events (using correct method with date range)
            val googleEvents = if (_hasCalendarPermission.value) {
                calendarManager.getAllCalendarEvents(startDate, endDate)
                    .filter { it.isExternal } // Only external events (not QuestFlow calendar)
            } else {
                emptyList()
            }

            // 3. Convert Google events to CalendarEventLinkEntity format (without taskId = external)
            val googleEventsAsLinks = googleEvents.map { googleEvent ->
                CalendarEventLinkEntity(
                    id = -googleEvent.id, // Negative ID to distinguish from app events
                    calendarEventId = googleEvent.id,
                    title = googleEvent.title,
                    startsAt = googleEvent.startTime,
                    endsAt = googleEvent.endTime,
                    xp = 0,
                    xpPercentage = 0,
                    rewarded = true, // External events are not for XP claiming
                    taskId = null,  // NULL = external Google Calendar event (RED)
                    status = "EXTERNAL"
                )
            }

            // 4. Combine both lists and sort by start time
            val combined = (appEvents + googleEventsAsLinks).sortedBy { it.startsAt }
            emit(combined)
        }
    }

    /**
     * Get tasks with due dates in a date range for month view occupancy visualization
     */
    fun getTasksInRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate) =
        taskRepository.getTasksInRange(startDate, endDate)
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