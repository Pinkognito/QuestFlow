package com.example.questflow.presentation.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

/**
 * ViewModel for Timeline Screen with infinite scroll and vertical time axis.
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

    fun onTaskDragStart(task: TimelineTask) {
        _uiState.update { it.copy(
            dragState = DragState(
                task = task,
                originalStartTime = task.startTime,
                originalEndTime = task.endTime,
                currentOffsetY = 0f,
                previewStartTime = task.startTime,
                previewEndTime = task.endTime
            )
        )}
    }

    fun onTaskDrag(offsetY: Float) {
        val currentDrag = _uiState.value.dragState ?: return
        val (newStart, newEnd) = TimelineCalculator.calculateDraggedTaskTimes(
            currentDrag.originalStartTime,
            currentDrag.originalEndTime,
            offsetY,
            _uiState.value.pixelsPerMinute,
            _uiState.value.snapToGridMinutes
        )
        _uiState.update { it.copy(
            dragState = currentDrag.copy(
                currentOffsetY = offsetY,
                previewStartTime = newStart,
                previewEndTime = newEnd
            )
        )}
    }

    fun onTaskDragEnd() {
        val dragState = _uiState.value.dragState ?: return
        if (dragState.hasChanged()) {
            viewModelScope.launch {
                updateTaskTimeUseCase(
                    taskId = dragState.task.taskId,
                    linkId = dragState.task.linkId,
                    newStartTime = dragState.previewStartTime,
                    newEndTime = dragState.previewEndTime
                )
            }
        }
        _uiState.update { it.copy(dragState = null) }
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
}
