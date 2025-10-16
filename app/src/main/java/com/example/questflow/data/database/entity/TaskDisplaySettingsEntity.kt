package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Settings for customizing which fields are displayed in the task list
 * Allows users to configure their preferred task card layout
 *
 * V2 (since DB v40): Added elementLayoutConfig for 2-column configurable layout
 */
@Entity(tableName = "task_display_settings")
data class TaskDisplaySettingsEntity(
    @PrimaryKey val id: Long = 1, // Singleton settings row

    // V2: Advanced layout configuration (JSON string)
    // Contains list of TaskDisplayElementConfig for each element
    val elementLayoutConfig: String = "",  // Empty = use legacy fields below

    // === LEGACY FIELDS (kept for backward compatibility) ===
    // Will be migrated to elementLayoutConfig in v40

    // Basic Task Information
    val showTaskTitle: Boolean = true,           // Always visible (cannot be disabled)
    val showTaskDescription: Boolean = true,     // Show description preview
    val showParentTaskPath: Boolean = true,      // Show parent task breadcrumb for subtasks

    // Date & Time Information
    val showDueDate: Boolean = true,             // Show due date/time
    val showCreatedDate: Boolean = false,        // Show creation date
    val showCompletedDate: Boolean = false,      // Show completion date (for completed tasks)

    // Task Metadata
    val showCategory: Boolean = true,            // Show category badge/name
    val showPriority: Boolean = true,            // Show priority indicator
    val showDifficulty: Boolean = true,          // Show XP percentage/difficulty level
    val showXpReward: Boolean = false,           // Show calculated XP reward amount

    // Status Indicators
    val showExpiredBadge: Boolean = true,        // Show "Abgelaufen" badge
    val showCompletedBadge: Boolean = true,      // Show completion status
    val showSubtaskCount: Boolean = true,        // Show subtask count for parent tasks
    val showRecurringIcon: Boolean = true,       // Show recurring task indicator

    // Contact Information
    val showLinkedContacts: Boolean = true,      // Show contact chips/names
    val showContactAvatars: Boolean = true,      // Show contact profile pictures
    val maxContactsVisible: Int = 3,             // Maximum contacts to show (rest collapsed)

    // Search & Filter Match Info
    val showMatchBadges: Boolean = true,         // Show filter match indicators during search
    val maxMatchBadgesVisible: Int = 3,          // Maximum match badges to show

    // Card Layout Options
    val compactMode: Boolean = false,            // Compact layout (less spacing, smaller text)
    val showDescriptionPreview: Boolean = true,  // Show first line of description
    val descriptionMaxLines: Int = 2             // Max lines for description preview
)
