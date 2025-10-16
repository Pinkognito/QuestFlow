package com.example.questflow.domain.usecase

import com.example.questflow.data.database.dao.MetadataContactDao
import com.example.questflow.data.database.dao.TaskContactLinkDao
import com.example.questflow.data.database.entity.TaskSearchFilterSettingsEntity
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.TaskSearchFilterRepository
import com.example.questflow.domain.model.Task
import com.example.questflow.domain.model.TaskSearchResult
import com.example.questflow.domain.model.SearchMatchInfo
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Enhanced task search that respects user-configured filter settings
 * Searches in task fields and related entities (contacts, parent tasks, etc.)
 * Returns TaskSearchResult with match information for UI display
 */
class SearchTasksWithFiltersUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val filterRepository: TaskSearchFilterRepository,
    private val taskContactLinkDao: TaskContactLinkDao,
    private val metadataContactDao: MetadataContactDao
) {
    /**
     * Search tasks and return results with match information
     */
    suspend fun searchWithMatchInfo(
        tasks: List<Task>,
        searchQuery: String
    ): List<TaskSearchResult> {
        if (searchQuery.isBlank()) return tasks.map { TaskSearchResult(it, emptyList()) }

        val settings = filterRepository.getSettingsSync()
        val query = searchQuery.trim()

        return tasks.mapNotNull { task ->
            val matches = collectMatches(task, query, settings)
            if (matches.isNotEmpty()) {
                TaskSearchResult(task, matches)
            } else {
                null
            }
        }
    }

    /**
     * Legacy method for backwards compatibility
     * Returns only the tasks without match information
     */
    suspend operator fun invoke(
        tasks: List<Task>,
        searchQuery: String
    ): List<Task> {
        return searchWithMatchInfo(tasks, searchQuery).map { it.task }
    }

    /**
     * Collect all matching filter criteria for a task
     */
    private suspend fun collectMatches(
        task: Task,
        query: String,
        settings: TaskSearchFilterSettingsEntity
    ): List<SearchMatchInfo> {
        val matches = mutableListOf<SearchMatchInfo>()

        // Direct task fields
        if (settings.taskTitle && task.title.contains(query, ignoreCase = true)) {
            matches.add(SearchMatchInfo.TaskTitle(task.title))
        }

        if (settings.taskDescription && task.description.contains(query, ignoreCase = true)) {
            matches.add(SearchMatchInfo.TaskDescription(task.description))
        }

        // Search in linked contacts
        if (settings.contactEnabled) {
            val contactIds = taskContactLinkDao.getContactsByTaskId(task.id).first().map { it.id }
            for (contactId in contactIds) {
                val contact = metadataContactDao.getById(contactId) ?: continue

                if (settings.contactDisplayName && contact.displayName.contains(query, ignoreCase = true)) {
                    matches.add(SearchMatchInfo.ContactDisplayName(contact.displayName))
                }

                if (settings.contactGivenName && contact.givenName?.contains(query, ignoreCase = true) == true) {
                    matches.add(SearchMatchInfo.ContactGivenName(contact.givenName!!))
                }

                if (settings.contactFamilyName && contact.familyName?.contains(query, ignoreCase = true) == true) {
                    matches.add(SearchMatchInfo.ContactFamilyName(contact.familyName!!))
                }

                if (settings.contactPrimaryPhone && contact.primaryPhone?.contains(query, ignoreCase = true) == true) {
                    matches.add(SearchMatchInfo.ContactPhone(contact.primaryPhone!!))
                }

                if (settings.contactPrimaryEmail && contact.primaryEmail?.contains(query, ignoreCase = true) == true) {
                    matches.add(SearchMatchInfo.ContactEmail(contact.primaryEmail!!))
                }

                if (settings.contactOrganization && contact.organization?.contains(query, ignoreCase = true) == true) {
                    matches.add(SearchMatchInfo.ContactOrganization(contact.organization!!))
                }

                if (settings.contactJobTitle && contact.jobTitle?.contains(query, ignoreCase = true) == true) {
                    matches.add(SearchMatchInfo.ContactJobTitle(contact.jobTitle!!))
                }

                if (settings.contactNote && contact.note?.contains(query, ignoreCase = true) == true) {
                    matches.add(SearchMatchInfo.ContactNote(contact.note!!))
                }
            }
        }

        // Search in parent task
        if (settings.parentTaskEnabled && task.parentTaskId != null) {
            val parentTask = taskRepository.getTaskById(task.parentTaskId)

            if (parentTask != null) {
                if (settings.parentTaskTitle && parentTask.title.contains(query, ignoreCase = true)) {
                    matches.add(SearchMatchInfo.ParentTaskTitle(parentTask.title))
                }

                if (settings.parentTaskDescription && parentTask.description.contains(query, ignoreCase = true)) {
                    matches.add(SearchMatchInfo.ParentTaskDescription(parentTask.description))
                }
            }
        }

        // Search in difficulty/XP percentage (if enabled)
        if (settings.xpPercentageEnabled) {
            val difficultyMatch = matchXpPercentageSearch(task, query, settings)
            if (difficultyMatch != null) {
                matches.add(difficultyMatch)
            }
        }

        // Search in time-based fields (if enabled)
        if (settings.timeFilterEnabled) {
            matches.addAll(matchTimeFilters(task, query, settings))
        }

        return matches
    }


    /**
     * Search in difficulty/XP percentage field
     * Maps XP percentage to German difficulty names and searches for them
     * Returns SearchMatchInfo if matches, null otherwise
     */
    private fun matchXpPercentageSearch(
        task: Task,
        query: String,
        settings: TaskSearchFilterSettingsEntity
    ): SearchMatchInfo.DifficultyLevel? {
        val taskXpPercent = task.xpPercentage

        // Map XP percentage to difficulty names
        val difficultyName = when (taskXpPercent) {
            20 -> if (settings.xpPercentage20) "Trivial" else null
            40 -> if (settings.xpPercentage40) "Einfach" else null
            60 -> if (settings.xpPercentage60) "Mittel" else null
            80 -> if (settings.xpPercentage80) "Schwer" else null
            100 -> if (settings.xpPercentage100) "Episch" else null
            else -> null
        }

        // Check if the difficulty name matches the query
        return if (difficultyName?.contains(query, ignoreCase = true) == true) {
            SearchMatchInfo.DifficultyLevel(difficultyName)
        } else {
            null
        }
    }

    /**
     * Check if task matches the date range filter
     * Returns true if task should be included based on its temporal status
     */
    private fun matchesDateRangeFilter(
        task: Task,
        settings: TaskSearchFilterSettingsEntity
    ): Boolean {
        val now = LocalDateTime.now()

        // Determine task's temporal status
        val isPast = task.dueDate?.isBefore(now) == true && !task.isCompleted
        val isFuture = task.dueDate?.isAfter(now) == true
        val isOverdue = task.dueDate?.isBefore(now) == true && !task.isCompleted

        // Check if task matches any enabled date range
        if (isPast && settings.includePastTasks) return true
        if (isFuture && settings.includeFutureTasks) return true
        if (isOverdue && settings.includeOverdueTasks) return true

        // If task has no due date, include it by default
        if (task.dueDate == null) return true

        return false
    }

    /**
     * Search in time-based fields (dates/times formatted as strings)
     * Returns list of SearchMatchInfo for all matching time fields
     *
     * Note: Task model doesn't have separate start/end times currently,
     * so we search in due date, created date, and completed date only
     */
    private fun matchTimeFilters(
        task: Task,
        query: String,
        settings: TaskSearchFilterSettingsEntity
    ): List<SearchMatchInfo> {
        val matches = mutableListOf<SearchMatchInfo>()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        // Note: Task model doesn't have startTime/endTime fields
        // These settings are included for future compatibility
        // Currently they check dueDate as a fallback
        if ((settings.filterByStartTime || settings.filterByEndTime) && task.dueDate != null) {
            val dueDateStr = task.dueDate.format(formatter)
            if (dueDateStr.contains(query, ignoreCase = true)) {
                matches.add(SearchMatchInfo.DueDate(dueDateStr))
            }
        }

        if (settings.filterByDueDate && task.dueDate != null) {
            val dueDateStr = task.dueDate.format(dateFormatter)
            if (dueDateStr.contains(query, ignoreCase = true)) {
                matches.add(SearchMatchInfo.DueDate(dueDateStr))
            }
        }

        if (settings.filterByCreatedDate) {
            val createdDateStr = task.createdAt.format(formatter)
            if (createdDateStr.contains(query, ignoreCase = true)) {
                matches.add(SearchMatchInfo.CreatedDate(createdDateStr))
            }
        }

        if (settings.filterByCompletedDate && task.completedAt != null) {
            val completedDateStr = task.completedAt.format(formatter)
            if (completedDateStr.contains(query, ignoreCase = true)) {
                matches.add(SearchMatchInfo.CompletedDate(completedDateStr))
            }
        }

        return matches
    }
}
