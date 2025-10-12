package com.example.questflow.domain.usecase.timeline

import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.domain.model.ConflictState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Loads tasks for the timeline view within a specific date range.
 * Combines Task and CalendarEventLink data to create TimelineTask objects.
 * Also loads external calendar events (Google Calendar, etc.) for read-only display.
 */
class LoadTimelineTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarLinkRepository: CalendarLinkRepository,
    private val categoryRepository: CategoryRepository,
    private val calendarManager: CalendarManager
) {
    /**
     * Load timeline tasks for the specified date range.
     *
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return Flow of TimelineTask list
     */
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<TimelineTask>> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)

        return combine(
            taskRepository.getActiveTasks(),
            calendarLinkRepository.getAllLinks(),
            categoryRepository.getAllCategories(),
            flow { emit(calendarManager.getAllCalendarEvents(startDate, endDate)) }
        ) { tasks, links, categories, externalEvents ->
            val timelineTasks = mutableListOf<TimelineTask>()

            // Create map for quick category lookup
            val categoryMap = categories.associateBy { it.id }

            // Process tasks with calendar links
            links.filter { link ->
                link.startsAt in startDateTime..endDateTime ||
                link.endsAt in startDateTime..endDateTime
            }.forEach { link ->
                val task = link.taskId?.let { taskId ->
                    tasks.find { it.id == taskId }
                }

                val category = categoryMap[link.categoryId]

                timelineTasks.add(
                    TimelineTask(
                        id = link.id,
                        taskId = link.taskId,
                        linkId = link.id,
                        title = link.title,
                        description = task?.description ?: "",
                        startTime = link.startsAt,
                        endTime = link.endsAt,
                        xpPercentage = link.xpPercentage,
                        categoryId = link.categoryId,
                        categoryColor = category?.color,
                        categoryEmoji = category?.emoji,
                        conflictState = ConflictState.NO_CONFLICT,
                        isCompleted = link.rewarded,
                        calendarEventId = link.calendarEventId
                    )
                )
            }

            // Process tasks without calendar links but with dueDate in range
            tasks.filter { task ->
                task.dueDate != null &&
                task.dueDate in startDateTime..endDateTime &&
                !timelineTasks.any { it.taskId == task.id } // Not already added via link
            }.forEach { task ->
                val category = categoryMap[task.categoryId]
                val duration = calculateDurationFromXpPercentage(task.xpPercentage)

                timelineTasks.add(
                    TimelineTask(
                        id = task.id * -1, // Negative ID to distinguish from link-based tasks
                        taskId = task.id,
                        linkId = null,
                        title = task.title,
                        description = task.description,
                        startTime = task.dueDate!!,
                        endTime = task.dueDate.plusMinutes(duration.toLong()),
                        xpPercentage = task.xpPercentage,
                        categoryId = task.categoryId,
                        categoryColor = category?.color,
                        categoryEmoji = category?.emoji,
                        conflictState = ConflictState.NO_CONFLICT,
                        isCompleted = task.isCompleted,
                        calendarEventId = task.calendarEventId
                    )
                )
            }

            // Add external calendar events (Google Calendar, Outlook, etc.)
            externalEvents.filter { event ->
                event.isExternal // Only add external events (not QuestFlow calendar)
            }.forEach { event ->
                timelineTasks.add(
                    TimelineTask(
                        id = event.id + 1000000000L, // Large offset to avoid ID conflicts
                        taskId = null,
                        linkId = null,
                        title = event.title,
                        description = event.description,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        xpPercentage = 0, // No XP for external events
                        categoryId = null,
                        categoryColor = "#9E9E9E", // Grey for external events
                        categoryEmoji = "📅", // Calendar emoji
                        conflictState = ConflictState.NO_CONFLICT,
                        isCompleted = false,
                        calendarEventId = event.id,
                        isExternal = true,
                        calendarName = event.calendarName
                    )
                )
            }

            // Sort by start time
            timelineTasks.sortedBy { it.startTime }
        }
    }

    /**
     * Calculate task duration in minutes based on XP percentage (difficulty).
     *
     * Mapping:
     * - 20% (Trivial): 30 minutes
     * - 40% (Easy): 60 minutes
     * - 60% (Medium): 120 minutes
     * - 80% (Hard): 180 minutes
     * - 100% (Epic): 240 minutes
     */
    private fun calculateDurationFromXpPercentage(xpPercentage: Int): Int {
        return when (xpPercentage) {
            in 0..25 -> 30     // Trivial
            in 26..45 -> 60    // Easy
            in 46..70 -> 120   // Medium
            in 71..90 -> 180   // Hard
            else -> 240        // Epic
        }
    }
}
