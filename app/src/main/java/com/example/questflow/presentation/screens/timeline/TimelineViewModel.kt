package com.example.questflow.presentation.screens.timeline

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.preferences.TimeRange
import com.example.questflow.data.preferences.TimelinePreferences
import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.domain.usecase.timeline.DetectConflictsUseCase
import com.example.questflow.domain.usecase.timeline.LoadTimelineTasksUseCase
import com.example.questflow.domain.usecase.timeline.UpdateTaskTimeUseCase
import com.example.questflow.presentation.screens.timeline.model.DayTimeline
import com.example.questflow.presentation.screens.timeline.model.DragState
import com.example.questflow.presentation.screens.timeline.model.TimelineUiState
import com.example.questflow.presentation.screens.timeline.util.TimelineCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for Timeline Screen.
 * Manages timeline state, task loading, conflict detection, and drag & drop interactions.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val loadTimelineTasksUseCase: LoadTimelineTasksUseCase,
    private val detectConflictsUseCase: DetectConflictsUseCase,
    private val updateTaskTimeUseCase: UpdateTaskTimeUseCase,
    private val timelinePreferences: TimelinePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        // Load preferences
        viewModelScope.launch {
            timelinePreferences.getSettings().collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        toleranceMinutes = settings.toleranceMinutes,
                        timeRange = settings.defaultTimeRange,
                        hourRangeStart = settings.hourRangeStart,
                        hourRangeEnd = settings.hourRangeEnd,
                        pixelsPerMinute = settings.pixelsPerMinute,
                        snapToGridMinutes = settings.snapToGridMinutes
                    )
                }
            }
        }

        // Initialize with default time range
        updateTimeRange(_uiState.value.timeRange)

        // Load tasks
        loadTasks()
    }

    /**
     * Load tasks for the current time range
     */
    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val startDate = _uiState.value.viewStart
                val endDate = _uiState.value.viewEnd

                loadTimelineTasksUseCase(startDate, endDate)
                    .catch { e ->
                        android.util.Log.e("TimelineViewModel", "Failed to load tasks", e)
                        _uiState.update { it.copy(error = "Fehler beim Laden: ${e.message}", isLoading = false) }
                    }
                    .collect { tasks ->
                        val daysWithTasks = processTasksIntoDays(tasks, startDate, endDate)
                        _uiState.update { it.copy(days = daysWithTasks, isLoading = false) }
                    }
            } catch (e: Exception) {
                android.util.Log.e("TimelineViewModel", "Failed to load tasks", e)
                _uiState.update { it.copy(error = "Fehler beim Laden: ${e.message}", isLoading = false) }
            }
        }
    }

    /**
     * Process tasks into days with conflict detection
     */
    private fun processTasksIntoDays(
        tasks: List<TimelineTask>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayTimeline> {
        val days = mutableListOf<DayTimeline>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            // Get tasks for this day
            val tasksForDay = tasks.filter { task ->
                task.startTime.toLocalDate() == currentDate
            }

            // Detect conflicts for this day
            val tasksWithConflicts = detectConflictsUseCase(
                tasksForDay,
                _uiState.value.toleranceMinutes
            )

            days.add(
                DayTimeline(
                    date = currentDate,
                    tasks = tasksWithConflicts,
                    isToday = currentDate == LocalDate.now()
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        return days
    }

    /**
     * Update visible time range
     */
    fun updateTimeRange(timeRange: TimeRange) {
        val today = LocalDate.now()
        val (start, end) = when (timeRange) {
            TimeRange.ONE_DAY -> today to today
            TimeRange.THREE_DAYS -> today.minusDays(1) to today.plusDays(1)
            TimeRange.SEVEN_DAYS -> today.minusDays(3) to today.plusDays(3)
            TimeRange.FOURTEEN_DAYS -> today.minusDays(7) to today.plusDays(6)
        }

        _uiState.update { it.copy(timeRange = timeRange, viewStart = start, viewEnd = end) }
        loadTasks()
    }

    /**
     * Handle task drag start
     */
    fun onTaskDragStart(task: TimelineTask) {
        android.util.Log.d("TimelineViewModel", "Drag start: ${task.title}")

        _uiState.update { state ->
            state.copy(
                dragState = DragState(
                    task = task,
                    originalStartTime = task.startTime,
                    originalEndTime = task.endTime,
                    currentOffset = Offset.Zero,
                    previewStartTime = task.startTime,
                    previewEndTime = task.endTime
                )
            )
        }
    }

    /**
     * Handle task drag update
     */
    fun onTaskDrag(offsetX: Float) {
        val state = _uiState.value
        val dragState = state.dragState ?: return

        // Calculate new times based on drag offset
        val (newStart, newEnd) = TimelineCalculator.calculateDraggedTaskTimes(
            originalStart = dragState.originalStartTime,
            originalEnd = dragState.originalEndTime,
            dragDeltaX = offsetX,
            pixelsPerMinute = state.pixelsPerMinute,
            gridMinutes = state.snapToGridMinutes
        )

        // Update drag state with preview times
        _uiState.update {
            it.copy(
                dragState = dragState.copy(
                    currentOffset = Offset(offsetX, 0f),
                    previewStartTime = newStart,
                    previewEndTime = newEnd
                )
            )
        }
    }

    /**
     * Handle task drag end (save changes)
     */
    fun onTaskDragEnd() {
        val state = _uiState.value
        val dragState = state.dragState ?: return

        android.util.Log.d("TimelineViewModel", "Drag end: ${dragState.task.title}, new time: ${dragState.previewStartTime}")

        // Check if position actually changed
        if (!dragState.hasChanged()) {
            _uiState.update { it.copy(dragState = null) }
            return
        }

        // Save updated task times
        viewModelScope.launch {
            val result = updateTaskTimeUseCase(
                taskId = dragState.task.taskId,
                linkId = dragState.task.linkId,
                newStartTime = dragState.previewStartTime,
                newEndTime = dragState.previewEndTime
            )

            when (result) {
                is UpdateTaskTimeUseCase.Result.Success -> {
                    android.util.Log.d("TimelineViewModel", "Task time updated successfully")
                    // Reload tasks to reflect changes and recalculate conflicts
                    loadTasks()
                }
                is UpdateTaskTimeUseCase.Result.Error -> {
                    android.util.Log.e("TimelineViewModel", "Failed to update task time: ${result.message}")
                    _uiState.update { it.copy(error = "Fehler beim Speichern: ${result.message}") }
                }
            }

            // Clear drag state
            _uiState.update { it.copy(dragState = null) }
        }
    }

    /**
     * Cancel drag operation
     */
    fun onTaskDragCancel() {
        android.util.Log.d("TimelineViewModel", "Drag cancelled")
        _uiState.update { it.copy(dragState = null) }
    }

    /**
     * Handle task click (show details)
     */
    fun onTaskClick(task: TimelineTask?) {
        _uiState.update { it.copy(selectedTask = task) }
    }

    /**
     * Set focused task (for top bar display)
     */
    fun setFocusedTask(task: TimelineTask?) {
        _uiState.update { it.copy(focusedTask = task) }
    }

    /**
     * Toggle settings dialog
     */
    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    /**
     * Update tolerance minutes
     */
    fun updateTolerance(minutes: Int) {
        _uiState.update { it.copy(toleranceMinutes = minutes) }
        timelinePreferences.toleranceMinutes = minutes

        // Recalculate conflicts with new tolerance
        refreshConflicts()
    }

    /**
     * Update hour range
     */
    fun updateHourRange(start: Int, end: Int) {
        _uiState.update { it.copy(hourRangeStart = start, hourRangeEnd = end) }
        timelinePreferences.hourRangeStart = start
        timelinePreferences.hourRangeEnd = end
    }

    /**
     * Update zoom level (pixels per minute)
     */
    fun updateZoom(pixelsPerMinute: Float) {
        _uiState.update { it.copy(pixelsPerMinute = pixelsPerMinute) }
        timelinePreferences.pixelsPerMinute = pixelsPerMinute
    }

    /**
     * Update snap-to-grid interval
     */
    fun updateSnapToGrid(minutes: Int) {
        _uiState.update { it.copy(snapToGridMinutes = minutes) }
        timelinePreferences.snapToGridMinutes = minutes
    }

    /**
     * Refresh conflict detection without reloading tasks
     */
    private fun refreshConflicts() {
        val state = _uiState.value
        val updatedDays = state.days.map { day ->
            val tasksWithConflicts = detectConflictsUseCase(
                day.tasks,
                state.toleranceMinutes
            )
            day.copy(tasks = tasksWithConflicts)
        }

        _uiState.update { it.copy(days = updatedDays) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Refresh all tasks
     */
    fun refresh() {
        loadTasks()
    }
}
