package com.example.questflow.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Calendar Color Configuration System
 *
 * Allows users to customize colors and visibility for different task types
 * in the calendar month view.
 */

/**
 * Color mode for task visualization
 */
enum class ColorMode {
    FIXED,           // Use fixed color (hex string)
    CATEGORY,        // Use category color
    TRANSPARENT      // Hide/make transparent
}

/**
 * Task type classification for color configuration
 */
enum class TaskType {
    OWN_TASK,           // Currently selected/edited task (white default)
    PARENT_TASK,        // Parent of current task
    SUBTASK,            // Subtask of current task
    SAME_CATEGORY,      // Task from same category (yellow default)
    OTHER_CATEGORY,     // Task from different category (blue default)
    NO_CATEGORY,        // Task without category
    EXPIRED_TASK,       // Task past due date
    COMPLETED_TASK,     // Completed task
    EXTERNAL_EVENT,     // Google Calendar event (red default)
    OVERLAP             // Overlap/conflict (black default)
}

/**
 * Color setting for a specific task type
 */
data class TaskColorSetting(
    val enabled: Boolean = true,                // Show this task type?
    val colorMode: ColorMode = ColorMode.FIXED, // How to determine color
    val fixedColor: String = "#FFFFFF",         // Hex color when mode = FIXED
    val alpha: Float = 1.0f                     // Transparency (0.0 - 1.0)
) {
    /**
     * Get the actual color to use, optionally using category color
     */
    fun getColor(categoryColor: Color? = null): Color {
        return when (colorMode) {
            ColorMode.FIXED -> {
                try {
                    Color(android.graphics.Color.parseColor(fixedColor)).copy(alpha = alpha)
                } catch (e: Exception) {
                    Color.White.copy(alpha = alpha)
                }
            }
            ColorMode.CATEGORY -> {
                categoryColor?.copy(alpha = alpha) ?: Color.Gray.copy(alpha = alpha)
            }
            ColorMode.TRANSPARENT -> Color.Transparent
        }
    }
}

/**
 * Complete calendar color configuration
 */
data class CalendarColorConfig(
    val ownTask: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#FFFFFF",  // White
        alpha = 1.0f
    ),
    val parentTask: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#FFA726",  // Orange
        alpha = 1.0f
    ),
    val subtask: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#AB47BC",  // Purple
        alpha = 1.0f
    ),
    val sameCategory: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.CATEGORY,  // Use category color by default
        fixedColor = "#FFEB3B",          // Yellow fallback
        alpha = 1.0f
    ),
    val otherCategory: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#42A5F5",  // Blue
        alpha = 1.0f
    ),
    val noCategory: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#78909C",  // Blue Grey
        alpha = 1.0f
    ),
    val expiredTask: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#EF5350",  // Red
        alpha = 0.6f             // Slightly transparent
    ),
    val completedTask: TaskColorSetting = TaskColorSetting(
        enabled = false,          // Hidden by default
        colorMode = ColorMode.FIXED,
        fixedColor = "#66BB6A",  // Green
        alpha = 0.4f
    ),
    val externalEvent: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#EF5350",  // Red
        alpha = 1.0f
    ),
    val overlap: TaskColorSetting = TaskColorSetting(
        enabled = true,
        colorMode = ColorMode.FIXED,
        fixedColor = "#000000",  // Black
        alpha = 1.0f
    )
) {
    /**
     * Get color setting for a specific task type
     */
    fun getSettingForType(type: TaskType): TaskColorSetting {
        return when (type) {
            TaskType.OWN_TASK -> ownTask
            TaskType.PARENT_TASK -> parentTask
            TaskType.SUBTASK -> subtask
            TaskType.SAME_CATEGORY -> sameCategory
            TaskType.OTHER_CATEGORY -> otherCategory
            TaskType.NO_CATEGORY -> noCategory
            TaskType.EXPIRED_TASK -> expiredTask
            TaskType.COMPLETED_TASK -> completedTask
            TaskType.EXTERNAL_EVENT -> externalEvent
            TaskType.OVERLAP -> overlap
        }
    }

    /**
     * Get default/fallback configuration
     */
    companion object {
        fun default() = CalendarColorConfig()
    }
}
