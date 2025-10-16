package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores user preferences for task search filtering
 * Determines which fields from related entities should be included in search
 */
@Entity(tableName = "task_search_filter_settings")
data class TaskSearchFilterSettingsEntity(
    @PrimaryKey
    val id: Long = 1, // Only one settings row needed

    // Task direct fields (always enabled)
    val taskTitle: Boolean = true,
    val taskDescription: Boolean = true,
    val taskTags: Boolean = true,

    // Task -> Category
    val categoryName: Boolean = true,

    // Task -> Contacts
    val contactEnabled: Boolean = true,
    val contactDisplayName: Boolean = true,
    val contactGivenName: Boolean = true,
    val contactFamilyName: Boolean = true,
    val contactPrimaryPhone: Boolean = true,
    val contactPrimaryEmail: Boolean = true,
    val contactOrganization: Boolean = false,
    val contactJobTitle: Boolean = false,
    val contactNote: Boolean = false,

    // Task -> Parent Task
    val parentTaskEnabled: Boolean = true,
    val parentTaskTitle: Boolean = true,
    val parentTaskDescription: Boolean = false,

    // Task Metadata Filters
    val xpPercentageEnabled: Boolean = false,
    val xpPercentage20: Boolean = true,  // Trivial
    val xpPercentage40: Boolean = true,  // Einfach
    val xpPercentage60: Boolean = true,  // Mittel
    val xpPercentage80: Boolean = true,  // Schwer
    val xpPercentage100: Boolean = true, // Episch

    // Time-based Filters
    val timeFilterEnabled: Boolean = false,
    val filterByStartTime: Boolean = false,
    val filterByEndTime: Boolean = false,
    val filterByDueDate: Boolean = true,
    val filterByCreatedDate: Boolean = false,
    val filterByCompletedDate: Boolean = false,

    // Date Range Filters (for time-based searches)
    val dateRangeEnabled: Boolean = false,
    val includePastTasks: Boolean = true,
    val includeFutureTasks: Boolean = true,
    val includeOverdueTasks: Boolean = true,

    // Future extension: Task -> Locations, Actions, etc.
    val locationEnabled: Boolean = false,
    val locationName: Boolean = false,
    val locationAddress: Boolean = false,

    // UI State: Collapsed/Expanded state for each section
    val taskFieldsSectionExpanded: Boolean = true,
    val contactsSectionExpanded: Boolean = true,
    val parentTaskSectionExpanded: Boolean = true,
    val xpPercentageSectionExpanded: Boolean = false,
    val timeFilterSectionExpanded: Boolean = false,
    val dateRangeSectionExpanded: Boolean = false
)
