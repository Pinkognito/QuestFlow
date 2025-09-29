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
import com.example.questflow.domain.usecase.CheckExpiredEventsUseCase
import com.example.questflow.domain.usecase.CalculateXpRewardUseCase
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
class CalendarXpViewModel @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val recordCalendarXpUseCase: RecordCalendarXpUseCase,
    private val calendarManager: CalendarManager,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val filterPreferences: CalendarFilterPreferences,
    private val taskRepository: TaskRepository,
    private val checkExpiredEventsUseCase: CheckExpiredEventsUseCase,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase
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
            // Check for expired events first
            try {
                checkExpiredEventsUseCase()
            } catch (e: Exception) {
                android.util.Log.e("CalendarXpViewModel", "Failed to check expired events", e)
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
        taskId: Long,
        title: String,
        description: String,
        xpPercentage: Int,
        dateTime: LocalDateTime,
        categoryId: Long?,
        calendarEventId: Long,
        shouldReactivate: Boolean = false,
        deleteOnClaim: Boolean = false,
        deleteOnExpiry: Boolean = false,
        isRecurring: Boolean = false,
        recurringConfig: RecurringConfig? = null
    ) {
        viewModelScope.launch {
            try {
                val link = calendarLinkRepository.getLinkById(linkId) ?: return@launch

                val now = LocalDateTime.now()
                val isExpiredNow = dateTime.plusHours(1) <= now

                // Check if we need to restore an expired event
                val shouldRestoreExpired = link.status == "EXPIRED" &&
                    link.deleteOnExpiry && !deleteOnExpiry

                // Add debug logging
                android.util.Log.d("CalendarXpViewModel", "updateCalendarTask: $title")
                android.util.Log.d("CalendarXpViewModel", "  isExpiredNow: $isExpiredNow, deleteOnExpiry: $deleteOnExpiry")
                android.util.Log.d("CalendarXpViewModel", "  link.deleteOnExpiry: ${link.deleteOnExpiry}")
                android.util.Log.d("CalendarXpViewModel", "  Time changed: ${dateTime != link.startsAt}")

                // Handle calendar event operations - DELETION has priority over update!
                var calendarEventDeleted = false
                if (calendarManager.hasCalendarPermission()) {
                    when {
                        // Event is expired and deleteOnExpiry is ON -> delete calendar event (PRIORITY!)
                        isExpiredNow && deleteOnExpiry -> {
                            try {
                                android.util.Log.d("CalendarXpViewModel", "Attempting to delete calendar event ID: $calendarEventId")
                                val deleted = calendarManager.deleteCalendarEvent(calendarEventId)
                                calendarEventDeleted = deleted
                                android.util.Log.d("CalendarXpViewModel", "Delete result: $deleted for calendar event: $title")
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarXpViewModel", "Failed to delete calendar event: ${e.message}", e)
                            }
                        }
                        // Event was expired with deleteOnExpiry ON, now deleteOnExpiry is OFF -> recreate
                        shouldRestoreExpired -> {
                            try {
                                val eventExists = try {
                                    calendarManager.getCalendarEvent(calendarEventId) != null
                                } catch (e: Exception) {
                                    false
                                }

                                if (!eventExists) {
                                    // Calculate XP reward based on current level
                                    val currentStats = statsRepository.getStatsFlow().first()
                                    val xpReward = calculateXpRewardUseCase(xpPercentage, currentStats.level)
                                    val newEventId = calendarManager.createTaskEvent(
                                        taskTitle = title,
                                        taskDescription = description,
                                        startTime = dateTime,
                                        endTime = dateTime.plusHours(1),
                                        xpReward = xpReward,
                                        xpPercentage = xpPercentage
                                    )
                                    android.util.Log.d("CalendarXpViewModel", "Recreated calendar event: $title with ID: $newEventId")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarXpViewModel", "Failed to recreate calendar event", e)
                            }
                        }
                        // Update calendar event ONLY if not deleted and not being recreated
                        !calendarEventDeleted && !shouldRestoreExpired -> {
                            try {
                                android.util.Log.d("CalendarXpViewModel", "Updating calendar event for: $title")
                                calendarManager.updateTaskEvent(
                                    eventId = calendarEventId,
                                    taskTitle = title,
                                    taskDescription = description,
                                    startTime = dateTime,
                                    endTime = dateTime.plusHours(1)
                                )
                                android.util.Log.d("CalendarXpViewModel", "Successfully updated calendar event")
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarXpViewModel", "Failed to update calendar event", e)
                            }
                        }
                        else -> {
                            android.util.Log.d("CalendarXpViewModel", "No calendar operation needed (deleted or recreating)")
                        }
                    }
                }

                // Determine new status
                val newStatus = when {
                    shouldReactivate -> "PENDING"
                    shouldRestoreExpired -> "PENDING"
                    isExpiredNow && !link.rewarded -> "EXPIRED"
                    else -> link.status
                }

                // Update link with proper status handling
                val updatedLink = link.copy(
                    title = title,
                    startsAt = dateTime,
                    endsAt = dateTime.plusHours(1),
                    xpPercentage = xpPercentage,
                    categoryId = categoryId,
                    deleteOnClaim = deleteOnClaim,
                    deleteOnExpiry = deleteOnExpiry,
                    isRecurring = isRecurring,
                    status = newStatus,
                    rewarded = if (shouldReactivate || shouldRestoreExpired) false else link.rewarded
                )
                calendarLinkRepository.updateLink(updatedLink)

                // Update associated task
                val task = taskRepository.getTaskEntityById(taskId)
                task?.let {
                    taskRepository.updateTaskEntity(
                        it.copy(
                            title = title,
                            description = description,
                            dueDate = dateTime,
                            xpPercentage = xpPercentage,
                            categoryId = categoryId,
                            isCompleted = if (shouldReactivate || shouldRestoreExpired) false else it.isCompleted,
                            isRecurring = isRecurring,
                            recurringType = recurringConfig?.mode?.name,
                            recurringInterval = when(recurringConfig?.mode) {
                                RecurringMode.DAILY -> recurringConfig.dailyInterval * 24 * 60
                                RecurringMode.CUSTOM -> recurringConfig.customMinutes + recurringConfig.customHours * 60
                                else -> null
                            },
                            recurringDays = if (recurringConfig?.mode == RecurringMode.WEEKLY) {
                                recurringConfig.weeklyDays.map { it.value }.joinToString(",")
                            } else null
                        )
                    )
                }

                // Refresh
                loadCalendarEvents()
                loadCalendarLinks()
            } catch (e: Exception) {
                android.util.Log.e("CalendarXpViewModel", "Failed to update task: ${e.message}")
            }
        }
    }

    fun updateCalendarLink(
        linkId: Long,
        title: String,
        xpPercentage: Int,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        categoryId: Long?
    ) {
        android.util.Log.e("CalendarXpViewModel", "!!! updateCalendarLink CALLED (SIMPLE VERSION) !!!")
        android.util.Log.e("CalendarXpViewModel", "!!! THIS SHOULD NOT BE CALLED FOR TASK EDITING !!!")
        android.util.Log.e("CalendarXpViewModel", "  linkId: $linkId")
        android.util.Log.e("CalendarXpViewModel", "  title: $title")
        android.util.Log.e("CalendarXpViewModel", "  startsAt: $startsAt")
        android.util.Log.e("CalendarXpViewModel", "  endsAt: $endsAt")

        viewModelScope.launch {
            try {
                calendarLinkRepository.updateLink(
                    linkId = linkId,
                    title = title,
                    xpPercentage = xpPercentage,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    categoryId = categoryId
                )

                // Reload links to show changes
                loadCalendarLinks()
            } catch (e: Exception) {
                android.util.Log.e("CalendarXpViewModel", "Failed to update calendar link", e)
            }
        }
    }

    fun updateCalendarLinkWithReactivation(
        linkId: Long,
        title: String,
        xpPercentage: Int,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        categoryId: Long?,
        shouldReactivate: Boolean = false,
        deleteOnClaim: Boolean = false,
        deleteOnExpiry: Boolean = false,
        isRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val existingLink = calendarLinkRepository.getLinkById(linkId) ?: return@launch
                val now = LocalDateTime.now()

                // Check if the event is expired based on the new time
                val isExpiredNow = endsAt <= now

                // Check if we need to restore an expired event
                val shouldRestoreExpired = existingLink.status == "EXPIRED" &&
                    existingLink.deleteOnExpiry && !deleteOnExpiry

                // Add debug logging
                android.util.Log.e("CalendarXpViewModel", "========================================")
                android.util.Log.e("CalendarXpViewModel", "=== updateCalendarLinkWithReactivation CALLED ===")
                android.util.Log.e("CalendarXpViewModel", "  title: $title")
                android.util.Log.e("CalendarXpViewModel", "  linkId: $linkId")
                android.util.Log.e("CalendarXpViewModel", "  NEW startsAt: $startsAt")
                android.util.Log.e("CalendarXpViewModel", "  NEW endsAt: $endsAt")
                android.util.Log.e("CalendarXpViewModel", "  OLD startsAt: ${existingLink.startsAt}")
                android.util.Log.e("CalendarXpViewModel", "  OLD endsAt: ${existingLink.endsAt}")
                android.util.Log.e("CalendarXpViewModel", "  isExpiredNow: $isExpiredNow")
                android.util.Log.e("CalendarXpViewModel", "  deleteOnExpiry NEW: $deleteOnExpiry")
                android.util.Log.e("CalendarXpViewModel", "  deleteOnExpiry OLD: ${existingLink.deleteOnExpiry}")
                android.util.Log.e("CalendarXpViewModel", "  shouldRestoreExpired: $shouldRestoreExpired")
                android.util.Log.e("CalendarXpViewModel", "  Time changed: ${startsAt != existingLink.startsAt || endsAt != existingLink.endsAt}")
                android.util.Log.e("CalendarXpViewModel", "  calendarEventId: ${existingLink.calendarEventId}")
                android.util.Log.e("CalendarXpViewModel", "========================================")

                // Check if we need to handle calendar event deletion or recreation
                // IMPORTANT: Handle deletion FIRST and skip update if we're deleting
                var calendarEventDeleted = false
                if (calendarManager.hasCalendarPermission()) {
                    android.util.Log.e("CalendarXpViewModel", ">>> ENTERING WHEN BLOCK <<<")
                    when {
                        // Case 1: Event is expired and deleteOnExpiry is ON -> delete calendar event (PRIORITY)
                        isExpiredNow && deleteOnExpiry -> {
                            android.util.Log.e("CalendarXpViewModel", ">>> CASE 1: SHOULD DELETE EVENT <<<")
                            android.util.Log.e("CalendarXpViewModel", "  Reason: isExpiredNow=$isExpiredNow AND deleteOnExpiry=$deleteOnExpiry")
                            try {
                                android.util.Log.e("CalendarXpViewModel", "  CALLING deleteCalendarEvent with ID: ${existingLink.calendarEventId}")
                                val deleted = calendarManager.deleteCalendarEvent(existingLink.calendarEventId)
                                calendarEventDeleted = deleted
                                android.util.Log.e("CalendarXpViewModel", "  DELETE RESULT: $deleted")
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarXpViewModel", "  DELETE FAILED: ${e.message}", e)
                            }
                        }
                        // Case 2: Event was expired with deleteOnExpiry ON, now deleteOnExpiry is OFF -> recreate calendar event
                        shouldRestoreExpired -> {
                            try {
                                // Check if calendar event still exists
                                val eventExists = try {
                                    calendarManager.getCalendarEvent(existingLink.calendarEventId) != null
                                } catch (e: Exception) {
                                    false
                                }

                                if (!eventExists) {
                                    // Calculate XP reward based on current level
                                    val currentStats = statsRepository.getStatsFlow().first()
                                    val xpReward = calculateXpRewardUseCase(xpPercentage, currentStats.level)
                                    // Recreate the calendar event
                                    val newEventId = calendarManager.createTaskEvent(
                                        taskTitle = title,
                                        taskDescription = "",
                                        startTime = startsAt,
                                        endTime = endsAt,
                                        xpReward = xpReward,
                                        xpPercentage = xpPercentage
                                    )
                                    // We'll update the link with the new event ID below
                                    android.util.Log.d("CalendarXpViewModel", "Recreated calendar event for: $title with ID: $newEventId")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarXpViewModel", "Failed to recreate calendar event", e)
                            }
                        }
                        // Case 3: Update the calendar event time if it changed (ONLY if not deleted)
                        !calendarEventDeleted && (startsAt != existingLink.startsAt || endsAt != existingLink.endsAt) -> {
                            android.util.Log.e("CalendarXpViewModel", ">>> CASE 3: UPDATING EVENT TIME <<<")
                            android.util.Log.e("CalendarXpViewModel", "  Reason: NOT deleted ($calendarEventDeleted) AND time changed")
                            android.util.Log.e("CalendarXpViewModel", "  THIS SHOULD NOT HAPPEN IF EVENT IS EXPIRED WITH deleteOnExpiry!")
                            try {
                                android.util.Log.e("CalendarXpViewModel", "  CALLING updateTaskEvent with ID: ${existingLink.calendarEventId}")
                                calendarManager.updateTaskEvent(
                                    eventId = existingLink.calendarEventId,
                                    taskTitle = title,
                                    taskDescription = "",
                                    startTime = startsAt,
                                    endTime = endsAt
                                )
                                android.util.Log.e("CalendarXpViewModel", "  UPDATE COMPLETED")
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarXpViewModel", "  UPDATE FAILED: ${e.message}", e)
                            }
                        }
                        else -> {
                            android.util.Log.e("CalendarXpViewModel", ">>> NO CALENDAR OPERATION <<<")
                            android.util.Log.e("CalendarXpViewModel", "  calendarEventDeleted: $calendarEventDeleted")
                            android.util.Log.e("CalendarXpViewModel", "  Time changed: ${startsAt != existingLink.startsAt || endsAt != existingLink.endsAt}")
                        }
                    }
                }

                // Determine new status based on conditions
                val newStatus = when {
                    shouldReactivate -> "PENDING"
                    shouldRestoreExpired -> "PENDING"
                    isExpiredNow && !existingLink.rewarded -> "EXPIRED"
                    !isExpiredNow -> "PENDING"  // If not expired anymore, set to PENDING
                    else -> existingLink.status
                }

                // Update link with all changes
                val updatedLink = existingLink.copy(
                    title = title,
                    xpPercentage = xpPercentage,
                    startsAt = startsAt,
                    endsAt = endsAt,
                    categoryId = categoryId,
                    deleteOnClaim = deleteOnClaim,
                    deleteOnExpiry = deleteOnExpiry,
                    isRecurring = isRecurring,
                    status = newStatus,
                    rewarded = if (shouldReactivate || shouldRestoreExpired) false else existingLink.rewarded
                )

                calendarLinkRepository.updateLink(updatedLink)

                // Reload links to show changes
                loadCalendarLinks()

                // Log status changes
                if (shouldReactivate) {
                    android.util.Log.d("CalendarXpViewModel", "Reactivated calendar link: $title")
                }
                if (shouldRestoreExpired) {
                    android.util.Log.d("CalendarXpViewModel", "Restored expired event: $title")
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarXpViewModel", "Failed to update calendar link with reactivation", e)
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