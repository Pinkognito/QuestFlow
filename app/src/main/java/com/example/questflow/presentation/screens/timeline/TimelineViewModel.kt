package com.example.questflow.presentation.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.preferences.TimelinePreferences
import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.domain.usecase.timeline.DetectConflictsUseCase
import com.example.questflow.domain.usecase.timeline.LoadTimelineTasksUseCase
import com.example.questflow.domain.usecase.timeline.UpdateTaskTimeUseCase
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for Timeline Screen with infinite scroll and vertical time axis.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val loadTimelineTasksUseCase: LoadTimelineTasksUseCase,
    private val detectConflictsUseCase: DetectConflictsUseCase,
    private val updateTaskTimeUseCase: UpdateTaskTimeUseCase,
    private val batchTaskPositioningUseCase: com.example.questflow.domain.usecase.timeline.BatchTaskPositioningUseCase,
    private val timelinePreferences: TimelinePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    enum class LoadDirection { PAST, FUTURE }

    private var screenHeightDp: Float = 800f // Will be updated from UI

    init {
        viewModelScope.launch {
            timelinePreferences.getSettings().collect { settings ->
                val calculatedPixelsPerMinute = settings.calculatePixelsPerMinute(screenHeightDp)
                _uiState.update { it.copy(
                    toleranceMinutes = settings.toleranceMinutes,
                    visibleHours = settings.visibleHours,
                    pixelsPerMinute = calculatedPixelsPerMinute,
                    snapToGridMinutes = settings.snapToGridMinutes
                )}
            }
        }
        loadInitialDays()
    }

    /**
     * Update pixelsPerMinute based on screen height and visibleHours
     */
    fun updateScreenHeight(screenHeightDp: Float) {
        this.screenHeightDp = screenHeightDp
        val settings = timelinePreferences.getSettings().value
        val calculatedPixelsPerMinute = settings.calculatePixelsPerMinute(screenHeightDp)
        _uiState.update { it.copy(pixelsPerMinute = calculatedPixelsPerMinute) }
    }

    private fun loadInitialDays() {
        val today = LocalDate.now()
        val start = today.minusDays(3)
        val end = today.plusDays(3)
        loadTasksForRange(start, end)
    }

    fun loadMore(direction: LoadDirection) {
        when (direction) {
            LoadDirection.PAST -> loadMorePast()
            LoadDirection.FUTURE -> loadMoreFuture()
        }
    }

    private fun loadMorePast() {
        if (_uiState.value.isLoadingPast) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPast = true) }
            try {
                val currentStart = _uiState.value.viewStart
                val newStart = currentStart.minusDays(7)
                loadAndPrependDays(newStart, currentStart.minusDays(1))
            } finally {
                _uiState.update { it.copy(isLoadingPast = false) }
            }
        }
    }

    private fun loadMoreFuture() {
        if (_uiState.value.isLoadingFuture) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFuture = true) }
            try {
                val currentEnd = _uiState.value.viewEnd
                val newEnd = currentEnd.plusDays(7)
                loadAndAppendDays(currentEnd.plusDays(1), newEnd)
            } finally {
                _uiState.update { it.copy(isLoadingFuture = false) }
            }
        }
    }

    private fun loadTasksForRange(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                loadTimelineTasksUseCase(start, end)
                    .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) }}
                    .collect { tasks ->
                        val days = processTasksIntoDays(tasks, start, end)
                        _uiState.update { it.copy(
                            days = days,
                            viewStart = start,
                            viewEnd = end,
                            isLoading = false
                        )}
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadAndPrependDays(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            loadTimelineTasksUseCase(start, end).first().let { tasks ->
                val newDays = processTasksIntoDays(tasks, start, end)
                _uiState.update { it.copy(
                    days = newDays + it.days,
                    viewStart = start
                )}
            }
        }
    }

    private fun loadAndAppendDays(start: LocalDate, end: LocalDate) {
        viewModelScope.launch {
            loadTimelineTasksUseCase(start, end).first().let { tasks ->
                val newDays = processTasksIntoDays(tasks, start, end)
                _uiState.update { it.copy(
                    days = it.days + newDays,
                    viewEnd = end
                )}
            }
        }
    }

    private fun processTasksIntoDays(tasks: List<TimelineTask>, start: LocalDate, end: LocalDate): List<DayTimeline> {
        val tasksByDay = tasks.groupBy { it.startTime.toLocalDate() }
        val tolerance = _uiState.value.toleranceMinutes

        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { date ->
                val dayTasks = tasksByDay[date] ?: emptyList()
                val tasksWithConflicts = detectConflictsUseCase(dayTasks, tolerance)
                DayTimeline(date, tasksWithConflicts, date == LocalDate.now())
            }
            .toList()
    }

    fun onTaskClick(task: TimelineTask?) {
        _uiState.update { it.copy(selectedTask = task) }
    }

    fun setFocusedTask(task: TimelineTask?) {
        _uiState.update { it.copy(focusedTask = task) }
    }


    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun updateTolerance(minutes: Int) {
        timelinePreferences.toleranceMinutes = minutes
    }

    fun updateVisibleHours(hours: Float) {
        timelinePreferences.visibleHours = hours
    }

    fun refresh() {
        loadTasksForRange(_uiState.value.viewStart, _uiState.value.viewEnd)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Shift the 3-day window left (past) or right (future)
     */
    fun shiftDayWindow(direction: Int) {
        val newOffset = _uiState.value.dayWindowOffset + direction
        _uiState.update { it.copy(dayWindowOffset = newOffset) }

        // Load more days if approaching edges
        val visibleDays = _uiState.value.getVisibleDays()
        if (visibleDays.isNotEmpty()) {
            val firstVisible = visibleDays.first().date
            val lastVisible = visibleDays.last().date

            // If first visible day is close to start, load more past
            if (firstVisible <= _uiState.value.viewStart.plusDays(1)) {
                loadMorePast()
            }

            // If last visible day is close to end, load more future
            if (lastVisible >= _uiState.value.viewEnd.minusDays(1)) {
                loadMoreFuture()
            }
        }
    }

    // ====== Multi-Selection Methods ======

    /**
     * Toggle task selection (add or remove from selection)
     * External calendar events cannot be selected (they cannot be modified)
     */
    fun toggleTaskSelection(taskId: Long) {
        _uiState.update { state ->
            // Find the task to check if it's external
            val task = state.getAllTasks().find { it.id == taskId }

            if (task?.isExternal == true) {
                android.util.Log.w("TimelineViewModel", "Cannot select external calendar event: ${task.title}")
                // Show a brief error or ignore
                return@update state.copy(
                    error = "Externe Kalender-Events können nicht ausgewählt werden"
                )
            }

            val newSelection = if (taskId in state.selectedTaskIds) {
                // Remove from selection
                state.selectedTaskIds - taskId
            } else {
                // Add to selection
                state.selectedTaskIds + taskId
            }

            // Update custom order if needed
            val newOrder = if (taskId in state.selectedTaskIds) {
                // Remove from order
                state.customTaskOrder.filter { it != taskId }
            } else {
                // Add to end of order
                state.customTaskOrder + taskId
            }

            state.copy(
                selectedTaskIds = newSelection,
                customTaskOrder = newOrder
            )
        }
    }

    /**
     * Clear all selections
     */
    fun clearSelection() {
        _uiState.update { it.copy(
            selectedTaskIds = emptySet(),
            customTaskOrder = emptyList()
        )}
    }

    /**
     * Remove specific task from selection
     */
    fun removeFromSelection(taskId: Long) {
        _uiState.update { state ->
            state.copy(
                selectedTaskIds = state.selectedTaskIds - taskId,
                customTaskOrder = state.customTaskOrder.filter { it != taskId }
            )
        }
    }

    /**
     * Set custom task order (manual ordering in selection list)
     */
    fun setCustomTaskOrder(orderedIds: List<Long>) {
        _uiState.update { it.copy(customTaskOrder = orderedIds) }
    }

    /**
     * Toggle selection list visibility
     */
    fun toggleSelectionList() {
        _uiState.update { it.copy(showSelectionList = !it.showSelectionList) }
    }

    /**
     * Set selection box time range
     */
    fun setSelectionBox(startTime: LocalDateTime, endTime: LocalDateTime) {
        _uiState.update { it.copy(
            selectionBox = com.example.questflow.presentation.screens.timeline.model.SelectionBox(
                startTime = startTime,
                endTime = endTime
            )
        )}
    }

    /**
     * Clear selection box
     */
    fun clearSelectionBox() {
        _uiState.update { it.copy(selectionBox = null) }
    }

    /**
     * Select all tasks within the selection box time range
     */
    fun selectAllInRange() {
        val tasksInBox = _uiState.value.getTasksInSelectionBox()
        val taskIds = tasksInBox.map { it.id }.toSet()

        _uiState.update { state ->
            val newSelection = state.selectedTaskIds + taskIds
            val newOrder = state.customTaskOrder + taskIds.filter { it !in state.customTaskOrder }

            state.copy(
                selectedTaskIds = newSelection,
                customTaskOrder = newOrder
            )
        }
    }

    /**
     * Insert selected tasks into selection box with auto-positioning
     * Uses BatchTaskPositioningUseCase for smart arrangement
     */
    fun insertSelectedIntoRange(sortOption: TaskSortOption = TaskSortOption.CUSTOM_ORDER) {
        val box = _uiState.value.selectionBox ?: return
        val selectedTasks = _uiState.value.getSelectedTasksOrdered()

        if (selectedTasks.isEmpty()) return

        viewModelScope.launch {
            when (val result = batchTaskPositioningUseCase(
                tasks = selectedTasks,
                startTime = box.startTime,
                endTime = box.endTime,
                sortOption = sortOption
            )) {
                is com.example.questflow.domain.usecase.timeline.BatchTaskPositioningUseCase.Result.Success -> {
                    android.util.Log.d("TimelineViewModel", "Batch positioning successful: ${result.updatedTasks.size} tasks updated")
                    // Clear selection and box after successful insert
                    clearSelection()
                    clearSelectionBox()
                    // Refresh timeline to show updates
                    refresh()
                }
                is com.example.questflow.domain.usecase.timeline.BatchTaskPositioningUseCase.Result.Error -> {
                    android.util.Log.e("TimelineViewModel", "Batch positioning failed: ${result.message}")
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    // ====== Drag-to-Select Methods ======

    /**
     * Start drag selection at given time
     * Clears any existing SelectionBox first
     */
    fun onDragSelectionStart(startTime: LocalDateTime) {
        _uiState.update { it.copy(
            selectionBox = null, // Clear old selection box
            dragSelectionState = com.example.questflow.presentation.screens.timeline.model.DragSelectionState(
                startTime = startTime,
                currentTime = startTime,
                isActive = true
            )
        )}
    }

    /**
     * Update drag selection with current drag position
     */
    fun onDragSelectionUpdate(currentTime: LocalDateTime) {
        _uiState.update { state ->
            state.dragSelectionState?.let { drag ->
                state.copy(
                    dragSelectionState = drag.copy(currentTime = currentTime)
                )
            } ?: state
        }
    }

    /**
     * Finalize drag selection and create SelectionBox
     * Automatically orders start/end and enforces minimum duration
     */
    fun onDragSelectionEnd() {
        _uiState.value.dragSelectionState?.let { drag ->
            // Sort start/end
            val (start, end) = if (drag.startTime.isBefore(drag.currentTime)) {
                drag.startTime to drag.currentTime
            } else {
                drag.currentTime to drag.startTime
            }

            // Enforce minimum duration of 15 minutes
            val duration = java.time.temporal.ChronoUnit.MINUTES.between(start, end)
            val finalEnd = if (duration < 15) {
                start.plusMinutes(15)
            } else {
                end
            }

            // Create final SelectionBox
            setSelectionBox(start, finalEnd)
        }

        // Clear drag state
        _uiState.update { it.copy(dragSelectionState = null) }
    }

    /**
     * Cancel drag selection without creating SelectionBox
     */
    fun onDragSelectionCancel() {
        _uiState.update { it.copy(dragSelectionState = null) }
    }

    /**
     * Update gesture debug info for visual feedback with history
     * Smart history: Only adds entry if gesture type changes OR direction changes (for swipes)
     */
    fun updateGestureDebug(gestureType: String, elapsedMs: Long, dragX: Float, dragY: Float, message: String) {
        _uiState.update { state ->
            val oldHistory = state.gestureDebugInfo?.history ?: emptyList()
            val lastGestureType = state.gestureDebugInfo?.gestureType

            // Determine if we should add to history
            val shouldAddToHistory = when {
                // Always add if gesture type changed
                gestureType != lastGestureType -> true

                // For SWIPING: Only add if direction changed significantly
                gestureType == "SWIPING" -> {
                    val oldDragX = state.gestureDebugInfo?.dragX ?: 0f
                    val oldDragY = state.gestureDebugInfo?.dragY ?: 0f

                    // Check if direction changed (sign flip on X or Y axis)
                    val xDirectionChanged = (oldDragX * dragX < 0) && kotlin.math.abs(dragX) > 20f
                    val yDirectionChanged = (oldDragY * dragY < 0) && kotlin.math.abs(dragY) > 20f

                    xDirectionChanged || yDirectionChanged
                }

                // Don't add repeated same gestures
                else -> false
            }

            val newHistory = if (shouldAddToHistory && lastGestureType != null) {
                // Add direction indicator for SWIPING gestures
                val oldDragX = state.gestureDebugInfo?.dragX ?: 0f
                val oldDragY = state.gestureDebugInfo?.dragY ?: 0f

                val directionIndicator = if (lastGestureType == "SWIPING") {
                    // Determine dominant axis and direction
                    if (kotlin.math.abs(oldDragY) > kotlin.math.abs(oldDragX)) {
                        if (oldDragY < 0) "↑" else "↓" // Vertical dominant
                    } else {
                        if (oldDragX < 0) "←" else "→" // Horizontal dominant
                    }
                } else ""

                val entry = "$lastGestureType$directionIndicator (${state.gestureDebugInfo?.elapsedMs ?: 0}ms)"
                (oldHistory + entry).takeLast(5)
            } else {
                oldHistory
            }

            state.copy(
                gestureDebugInfo = com.example.questflow.presentation.screens.timeline.model.GestureDebugInfo(
                    gestureType = gestureType,
                    elapsedMs = elapsedMs,
                    dragX = dragX,
                    dragY = dragY,
                    message = message,
                    history = newHistory
                )
            )
        }
    }

    /**
     * Clear gesture debug info
     */
    fun clearGestureDebug() {
        _uiState.update { it.copy(gestureDebugInfo = null) }
    }

    /**
     * Set SelectionBox based on task time range (for long-press on task)
     */
    fun setSelectionBoxFromTask(task: TimelineTask) {
        if (task.isExternal) {
            _uiState.update { it.copy(
                error = "Externe Kalender-Events können nicht als Zeitbereich genutzt werden"
            )}
            return
        }

        setSelectionBox(task.startTime, task.endTime)
    }
}

/**
 * Sort options for batch task positioning
 */
enum class TaskSortOption {
    CUSTOM_ORDER,      // Use manual order from selection list (DEFAULT)
    PRIORITY,          // Sort by priority (HIGH → LOW)
    XP_PERCENTAGE,     // Sort by XP/difficulty
    DURATION,          // Sort by duration (short → long)
    DURATION_DESC,     // Sort by duration (long → short)
    ALPHABETICAL,      // Sort alphabetically
    CATEGORY           // Sort by category
}
