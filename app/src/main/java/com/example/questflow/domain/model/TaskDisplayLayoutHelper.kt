package com.example.questflow.domain.model

/**
 * Helper for serializing/deserializing task display layout configuration
 * Uses simple pipe-delimited format: type|enabled|column|priority
 */
object TaskDisplayLayoutHelper {

    /**
     * Serialize layout config to string
     * Format: type|enabled|column|priority;type|enabled|column|priority;...
     */
    fun toJson(config: List<TaskDisplayElementConfig>): String {
        return config.joinToString(";") { element ->
            "${element.type.name}|${element.enabled}|${element.column.name}|${element.priority}"
        }
    }

    /**
     * Deserialize string to layout config
     * Returns default config if string is empty or invalid
     */
    fun fromJson(json: String): List<TaskDisplayElementConfig> {
        if (json.isBlank()) {
            return getDefaultDisplayConfig()
        }

        return try {
            json.split(";").mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size == 4) {
                    TaskDisplayElementConfig(
                        type = DisplayElementType.valueOf(parts[0]),
                        enabled = parts[1].toBoolean(),
                        column = DisplayColumn.valueOf(parts[2]),
                        priority = parts[3].toInt()
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskDisplayLayoutHelper", "Failed to parse layout config, using defaults", e)
            getDefaultDisplayConfig()
        }
    }

    /**
     * Get configuration for specific element type
     */
    fun getElementConfig(
        config: List<TaskDisplayElementConfig>,
        type: DisplayElementType
    ): TaskDisplayElementConfig? {
        return config.find { it.type == type }
    }

    /**
     * Get all enabled elements for a specific column, sorted by priority
     */
    fun getElementsForColumn(
        config: List<TaskDisplayElementConfig>,
        column: DisplayColumn
    ): List<TaskDisplayElementConfig> {
        return config
            .filter { it.enabled && it.column == column }
            .sortedBy { it.priority }
    }

    /**
     * Update specific element configuration
     * When priority changes, automatically adjusts other priorities to avoid duplicates
     */
    fun updateElement(
        config: List<TaskDisplayElementConfig>,
        type: DisplayElementType,
        enabled: Boolean? = null,
        column: DisplayColumn? = null,
        priority: Int? = null
    ): List<TaskDisplayElementConfig> {
        // Find the element being updated
        val targetElement = config.find { it.type == type } ?: return config
        val oldPriority = targetElement.priority
        val oldColumn = targetElement.column
        val newPriority = priority ?: targetElement.priority
        val newColumn = column ?: targetElement.column

        // If priority or column changed, need to adjust others
        val needsAdjustment = priority != null || (column != null && column != oldColumn)

        return if (needsAdjustment) {
            config.map { element ->
                when {
                    // The target element itself
                    element.type == type -> element.copy(
                        enabled = enabled ?: element.enabled,
                        column = newColumn,
                        priority = newPriority
                    )
                    // Same column as new position: adjust priorities
                    element.column == newColumn -> {
                        // If priority conflict, shift others up
                        if (element.priority >= newPriority) {
                            element.copy(priority = element.priority + 1)
                        } else {
                            element
                        }
                    }
                    // Old column (if column changed): close gaps
                    column != null && element.column == oldColumn && element.priority > oldPriority -> {
                        element.copy(priority = element.priority - 1)
                    }
                    // Other columns: unchanged
                    else -> element
                }
            }.let { normalizeColumnPriorities(it) }
        } else {
            // Only enabled changed, no priority adjustment needed
            config.map { element ->
                if (element.type == type) {
                    element.copy(enabled = enabled ?: element.enabled)
                } else {
                    element
                }
            }
        }
    }

    /**
     * Normalize priorities within each column to be consecutive (1, 2, 3, ...)
     */
    private fun normalizeColumnPriorities(config: List<TaskDisplayElementConfig>): List<TaskDisplayElementConfig> {
        // Group by column
        val leftElements = config.filter { it.column == DisplayColumn.LEFT }.sortedBy { it.priority }
        val rightElements = config.filter { it.column == DisplayColumn.RIGHT }.sortedBy { it.priority }

        // Reassign consecutive priorities
        val normalizedLeft = leftElements.mapIndexed { index, element ->
            element.copy(priority = index + 1)
        }
        val normalizedRight = rightElements.mapIndexed { index, element ->
            element.copy(priority = index + 1)
        }

        // Merge back
        return (normalizedLeft + normalizedRight)
    }
}
