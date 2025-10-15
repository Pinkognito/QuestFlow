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
        android.util.Log.d("TimelineViewModel", "🏁 init: screenHeightDp=$screenHeightDp")
        viewModelScope.launch {
            timelinePreferences.getSettings().collect { settings ->
                val calculatedPixelsPerMinute = settings.calculatePixelsPerMinute(screenHeightDp)
                android.util.Log.d("TimelineViewModel", "📊 Settings collected: visibleHours=${settings.visibleHours}, screenHeight=$screenHeightDp, pixelsPerMinute=$calculatedPixelsPerMinute, edgeBorder=${settings.edgeBorderWidthDp}dp")
                _uiState.update { it.copy(
                    toleranceMinutes = settings.toleranceMinutes,
                    visibleHours = settings.visibleHours,
                    pixelsPerMinute = calculatedPixelsPerMinute,
                    snapToGridMinutes = settings.snapToGridMinutes,
                    edgeBorderWidthDp = settings.edgeBorderWidthDp
                )}
            }
        }
        loadInitialDays()
    }

    /**
     * Update pixelsPerMinute based on screen height and visibleHours
     */
    fun updateScreenHeight(screenHeightDp: Float) {
        android.util.Log.d("TimelineViewModel", "🎯 updateScreenHeight() called: screenHeightDp=$screenHeightDp (old=${this.screenHeightDp})")
        this.screenHeightDp = screenHeightDp
        val settings = timelinePreferences.getSettings().value
        val calculatedPixelsPerMinute = settings.calculatePixelsPerMinute(screenHeightDp)
        android.util.Log.d("TimelineViewModel", "🎯 Calculated pixelsPerMinute=$calculatedPixelsPerMinute (old=${_uiState.value.pixelsPerMinute})")
        _uiState.update { it.copy(pixelsPerMinute = calculatedPixelsPerMinute) }
        android.util.Log.d("TimelineViewModel", "🎯 UI State updated with new pixelsPerMinute=${_uiState.value.pixelsPerMinute}")
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

    // ====== Absolute Coordinate Methods (for Multi-Day Drag) ======

    /**
     * Update layout metrics (header height, time column width, content width, density)
     * Called from TimelineGrid once layout is measured
     * NOTE: pixelsPerMinute is managed by settings flow and updateScreenHeight()
     */
    fun updateLayoutMetrics(
        headerHeightPx: Float,
        timeColumnWidthPx: Float,
        contentWidthPx: Float,
        density: Float
    ) {
        android.util.Log.d("TimelineViewModel", "📐 updateLayoutMetrics: headerHeight=${headerHeightPx}px, timeColWidth=${timeColumnWidthPx}px, contentWidth=${contentWidthPx}px, density=$density")
        _uiState.update { it.copy(
            headerHeightPx = headerHeightPx,
            timeColumnWidthPx = timeColumnWidthPx,
            contentWidthPx = contentWidthPx,
            density = density
        )}
    }

    /**
     * Update current scroll position (for absolute coordinate calculation)
     * Called from TimelineGrid on every scroll event
     */
    fun updateScrollPosition(scrollOffsetPx: Float) {
        // Only log if significantly changed (avoid spam)
        val oldOffset = _uiState.value.currentScrollOffsetPx
        if (kotlin.math.abs(scrollOffsetPx - oldOffset) > 50f) {
            android.util.Log.d("TimelineViewModel", "📜 updateScrollPosition: ${scrollOffsetPx}px (Δ${scrollOffsetPx - oldOffset}px)")
        }
        _uiState.update { it.copy(currentScrollOffsetPx = scrollOffsetPx) }
    }

    /**
     * Start drag selection using ABSOLUTE screen coordinates
     * Converts absolute X/Y to DateTime, then calls regular onDragSelectionStart
     */
    fun onDragSelectionStartAbsolute(absoluteX: Float, absoluteY: Float) {
        android.util.Log.d("TimelineViewModel", "🎯 onDragSelectionStartAbsolute: X=${absoluteX}px, Y=${absoluteY}px")

        val currentState = _uiState.value
        val dateTime = calculateDateTimeFromAbsolutePosition(
            absoluteX = absoluteX,
            absoluteY = absoluteY,
            visibleDays = currentState.getVisibleDays(),
            timeColumnWidthPx = currentState.timeColumnWidthPx,
            headerHeightPx = currentState.headerHeightPx,
            scrollOffsetPx = currentState.currentScrollOffsetPx,
            pixelsPerMinute = currentState.pixelsPerMinute
        )

        if (dateTime != null) {
            android.util.Log.d("TimelineViewModel", "✅ Converted to DateTime: $dateTime")
            onDragSelectionStart(dateTime)
        } else {
            android.util.Log.w("TimelineViewModel", "❌ Could not convert absolute position to DateTime (outside valid area)")
        }
    }

    /**
     * Update drag selection using ABSOLUTE screen coordinates
     * Converts absolute X/Y to DateTime, then calls regular onDragSelectionUpdate
     */
    fun onDragSelectionUpdateAbsolute(absoluteX: Float, absoluteY: Float) {
        val currentState = _uiState.value
        val dateTime = calculateDateTimeFromAbsolutePosition(
            absoluteX = absoluteX,
            absoluteY = absoluteY,
            visibleDays = currentState.getVisibleDays(),
            timeColumnWidthPx = currentState.timeColumnWidthPx,
            headerHeightPx = currentState.headerHeightPx,
            scrollOffsetPx = currentState.currentScrollOffsetPx,
            pixelsPerMinute = currentState.pixelsPerMinute
        )

        if (dateTime != null) {
            // Only log every ~100ms equivalent (reduce spam)
            if (System.currentTimeMillis() % 3 == 0L) {
                android.util.Log.d("TimelineViewModel", "📍 Drag update: X=${absoluteX.toInt()}px, Y=${absoluteY.toInt()}px → $dateTime")
            }
            onDragSelectionUpdate(dateTime)
        }
    }

    /**
     * Calculate DateTime from absolute screen position
     * This enables multi-day drag across different HourSlot cells
     */
    private fun calculateDateTimeFromAbsolutePosition(
        absoluteX: Float,
        absoluteY: Float,
        visibleDays: List<com.example.questflow.presentation.screens.timeline.model.DayTimeline>,
        timeColumnWidthPx: Float,
        headerHeightPx: Float,
        scrollOffsetPx: Float,
        pixelsPerMinute: Float
    ): LocalDateTime? {
        val contentWidthPx = _uiState.value.contentWidthPx
        android.util.Log.d("TimelineViewModel", "🔢 calculateDateTime: X=$absoluteX, Y=$absoluteY, scroll=$scrollOffsetPx, visibleDays=${visibleDays.size}, contentWidth=$contentWidthPx")

        // 1. Which day? (X-axis)
        val xRelativeToContent = absoluteX - timeColumnWidthPx
        android.util.Log.d("TimelineViewModel", "  ├─ X relative to content: $xRelativeToContent (after subtracting timeCol=$timeColumnWidthPx)")

        if (xRelativeToContent < 0) {
            android.util.Log.d("TimelineViewModel", "  └─ ❌ Touch in time column")
            return null // Touch in time column
        }

        if (visibleDays.isEmpty()) {
            android.util.Log.d("TimelineViewModel", "  └─ ❌ No visible days")
            return null
        }

        // Each day gets equal width from the total content area
        val dayColumnWidth = contentWidthPx / visibleDays.size
        val dayIndex = (xRelativeToContent / dayColumnWidth).toInt().coerceIn(0, visibleDays.size - 1)

        android.util.Log.d("TimelineViewModel", "  ├─ Content width: ${contentWidthPx}px, day column width: ${dayColumnWidth}px, dayIndex: $dayIndex")

        if (dayIndex !in visibleDays.indices) {
            android.util.Log.d("TimelineViewModel", "  └─ ❌ dayIndex $dayIndex out of range [0, ${visibleDays.size-1}]")
            return null
        }

        val targetDay = visibleDays[dayIndex]
        android.util.Log.d("TimelineViewModel", "  ├─ ✅ Target day: ${targetDay.date}")

        // 2. Which time? (Y-axis + scroll offset)
        val yRelativeToContent = absoluteY - headerHeightPx
        android.util.Log.d("TimelineViewModel", "  ├─ Y relative to content: $yRelativeToContent (after subtracting header=$headerHeightPx)")

        if (yRelativeToContent < 0) {
            android.util.Log.d("TimelineViewModel", "  └─ ❌ Touch in header")
            return null // Touch in header
        }

        // Add scroll offset to get absolute Y position in full timeline
        val absoluteYInTimeline = yRelativeToContent + scrollOffsetPx
        android.util.Log.d("TimelineViewModel", "  ├─ Absolute Y in timeline: $absoluteYInTimeline (Y=$yRelativeToContent + scroll=$scrollOffsetPx)")

        // Convert DP/min → PX/min using density
        val density = _uiState.value.density
        val pixelsPerMinuteActual = pixelsPerMinute * density
        android.util.Log.d("TimelineViewModel", "  ├─ Conversion: dpPerMin=$pixelsPerMinute, density=$density, pxPerMin=$pixelsPerMinuteActual")

        // Convert to minutes (0-1439 for full day)
        val totalMinutes = (absoluteYInTimeline / pixelsPerMinuteActual).toInt().coerceIn(0, 1439)
        val hour = (totalMinutes / 60).coerceIn(0, 23)
        val minute = (totalMinutes % 60).coerceIn(0, 59)

        android.util.Log.d("TimelineViewModel", "  └─ ✅ Time: $hour:${String.format("%02d", minute)} (totalMin=$totalMinutes)")

        return LocalDateTime.of(targetDay.date, java.time.LocalTime.of(hour, minute))
    }

    /**
     * Update gesture debug info for visual feedback with history
     * Smart history: Only adds entry if gesture type changes OR direction changes (for swipes)
     */
    fun updateGestureDebug(
        gestureType: String,
        elapsedMs: Long,
        dragX: Float,
        dragY: Float,
        message: String,
        atEdge: com.example.questflow.presentation.screens.timeline.model.EdgePosition? = null
    ) {
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
                    history = newHistory,
                    atEdge = atEdge
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

    // ====== Context Menu Methods ======

    /**
     * Show radial context menu at finger release position
     * Automatically determines menu type based on selection context
     */
    fun showContextMenu(centerX: Float, centerY: Float) {
        val selectionBox = _uiState.value.selectionBox ?: run {
            android.util.Log.w("TimelineViewModel", "showContextMenu called without SelectionBox")
            return
        }

        val tasksInBox = _uiState.value.getTasksInSelectionBox()
        val menuType = if (tasksInBox.isEmpty()) {
            com.example.questflow.presentation.screens.timeline.model.ContextMenuType.SELECTION_EMPTY
        } else {
            com.example.questflow.presentation.screens.timeline.model.ContextMenuType.SELECTION_WITH_TASKS
        }

        android.util.Log.d("TimelineViewModel", "🎯 Show context menu: center=($centerX, $centerY), type=$menuType, tasksInBox=${tasksInBox.size}")

        _uiState.update { it.copy(
            contextMenu = com.example.questflow.presentation.screens.timeline.model.ContextMenuState(
                centerX = centerX,
                centerY = centerY,
                selectionBox = selectionBox,
                selectedTasksInBox = tasksInBox.size,
                menuType = menuType
            )
        )}
    }

    /**
     * Dismiss context menu
     */
    fun dismissContextMenu() {
        android.util.Log.d("TimelineViewModel", "❌ Dismiss context menu")
        _uiState.update { it.copy(contextMenu = null) }
    }

    /**
     * Execute context menu action
     */
    fun executeContextMenuAction(actionId: String) {
        val contextMenu = _uiState.value.contextMenu ?: return

        android.util.Log.d("TimelineViewModel", "⚡ Execute context menu action: $actionId")

        when (actionId) {
            "insert" -> {
                // Insert selected tasks into selection box
                insertSelectedIntoRange()
                // SelectionBox wird in insertSelectedIntoRange() gelöscht
                dismissContextMenu()
            }
            "delete" -> {
                // Delete all tasks in selection box
                val tasksInBox = _uiState.value.getTasksInSelectionBox()
                android.util.Log.d("TimelineViewModel", "Delete ${tasksInBox.size} tasks in selection box")
                // TODO: Implement delete UseCase
                dismissContextMenu()
                // SelectionBox bleibt erhalten für weitere Aktionen
            }
            "edit" -> {
                // Edit selection box time range
                android.util.Log.d("TimelineViewModel", "Edit selection box - opening dialog")
                dismissContextMenu()
                // SelectionBox bleibt erhalten, Dialog wird vom Screen geöffnet
                // TODO: Signal to screen to open SelectionBoxDialog
            }
            "details" -> {
                // Show list of selected tasks
                toggleSelectionList()
                dismissContextMenu()
                // SelectionBox bleibt erhalten
            }
            "create" -> {
                // Create new task in selection box
                android.util.Log.d("TimelineViewModel", "Create new task in selection box")
                // TODO: Implement create task flow
                dismissContextMenu()
                // SelectionBox bleibt erhalten
            }
            "cancel" -> {
                // Cancel selection - ONLY action that clears SelectionBox
                clearSelectionBox()
                dismissContextMenu()
            }
            else -> {
                android.util.Log.w("TimelineViewModel", "Unknown context menu action: $actionId")
                dismissContextMenu()
                // SelectionBox bleibt erhalten bei unbekannten Aktionen
            }
        }
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
