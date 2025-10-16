package com.example.questflow.domain.model

/**
 * Represents a configurable display element in the task card
 */
data class TaskDisplayElementConfig(
    val type: DisplayElementType,
    val enabled: Boolean = true,
    val column: DisplayColumn = DisplayColumn.LEFT,
    val priority: Int = 0
)

enum class DisplayElementType {
    TITLE,              // Task title (always enabled)
    DESCRIPTION,        // Task description preview
    PARENT_PATH,        // Parent task breadcrumb
    DUE_DATE,           // Due date/time
    CREATED_DATE,       // Creation date
    COMPLETED_DATE,     // Completion date
    DIFFICULTY,         // XP percentage/difficulty
    CATEGORY,           // Category badge
    PRIORITY,           // Priority indicator
    XP_REWARD,          // XP reward amount
    EXPIRED_BADGE,      // "Abgelaufen" badge
    COMPLETED_BADGE,    // Completion badge
    SUBTASK_COUNT,      // Subtask count badge
    RECURRING_ICON,     // Recurring indicator
    LINKED_CONTACTS,    // Contact chips
    MATCH_BADGES,       // Search match indicators
    CLAIM_BUTTON        // XP claim button
}

enum class DisplayColumn {
    LEFT,   // 2/3 width column (main content)
    RIGHT   // 1/3 width column (metadata/actions)
}

/**
 * Helper to get display name for UI
 */
fun DisplayElementType.getDisplayName(): String {
    return when (this) {
        DisplayElementType.TITLE -> "Titel"
        DisplayElementType.DESCRIPTION -> "Beschreibung"
        DisplayElementType.PARENT_PATH -> "Übergeordnete Task"
        DisplayElementType.DUE_DATE -> "Fälligkeitsdatum"
        DisplayElementType.CREATED_DATE -> "Erstellungsdatum"
        DisplayElementType.COMPLETED_DATE -> "Abschlussdatum"
        DisplayElementType.DIFFICULTY -> "Schwierigkeitsgrad"
        DisplayElementType.CATEGORY -> "Kategorie"
        DisplayElementType.PRIORITY -> "Priorität"
        DisplayElementType.XP_REWARD -> "XP Belohnung"
        DisplayElementType.EXPIRED_BADGE -> "Abgelaufen Badge"
        DisplayElementType.COMPLETED_BADGE -> "Erledigt Badge"
        DisplayElementType.SUBTASK_COUNT -> "Subtask-Anzahl"
        DisplayElementType.RECURRING_ICON -> "Wiederholend Icon"
        DisplayElementType.LINKED_CONTACTS -> "Verknüpfte Kontakte"
        DisplayElementType.MATCH_BADGES -> "Such-Match Badges"
        DisplayElementType.CLAIM_BUTTON -> "Claim Button"
    }
}

/**
 * Default configuration for all elements
 */
fun getDefaultDisplayConfig(): List<TaskDisplayElementConfig> {
    return listOf(
        // Left column (main content)
        TaskDisplayElementConfig(DisplayElementType.TITLE, true, DisplayColumn.LEFT, 1),
        TaskDisplayElementConfig(DisplayElementType.PARENT_PATH, true, DisplayColumn.LEFT, 2),
        TaskDisplayElementConfig(DisplayElementType.DESCRIPTION, true, DisplayColumn.LEFT, 3),
        TaskDisplayElementConfig(DisplayElementType.DUE_DATE, true, DisplayColumn.LEFT, 4),
        TaskDisplayElementConfig(DisplayElementType.DIFFICULTY, true, DisplayColumn.LEFT, 5),
        TaskDisplayElementConfig(DisplayElementType.CREATED_DATE, false, DisplayColumn.LEFT, 6),
        TaskDisplayElementConfig(DisplayElementType.COMPLETED_DATE, false, DisplayColumn.LEFT, 7),
        TaskDisplayElementConfig(DisplayElementType.LINKED_CONTACTS, true, DisplayColumn.LEFT, 8),
        TaskDisplayElementConfig(DisplayElementType.MATCH_BADGES, true, DisplayColumn.LEFT, 9),

        // Right column (metadata/actions)
        TaskDisplayElementConfig(DisplayElementType.EXPIRED_BADGE, true, DisplayColumn.RIGHT, 1),
        TaskDisplayElementConfig(DisplayElementType.SUBTASK_COUNT, true, DisplayColumn.RIGHT, 2),
        TaskDisplayElementConfig(DisplayElementType.COMPLETED_BADGE, true, DisplayColumn.RIGHT, 3),
        TaskDisplayElementConfig(DisplayElementType.RECURRING_ICON, true, DisplayColumn.RIGHT, 4),
        TaskDisplayElementConfig(DisplayElementType.CATEGORY, true, DisplayColumn.RIGHT, 5),
        TaskDisplayElementConfig(DisplayElementType.PRIORITY, true, DisplayColumn.RIGHT, 6),
        TaskDisplayElementConfig(DisplayElementType.XP_REWARD, false, DisplayColumn.RIGHT, 7),
        TaskDisplayElementConfig(DisplayElementType.CLAIM_BUTTON, true, DisplayColumn.RIGHT, 10)
    )
}
