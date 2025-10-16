package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.TaskFilterPresetDao
import com.example.questflow.data.database.entity.TaskFilterPresetEntity
import com.example.questflow.domain.model.AdvancedTaskFilter
import com.example.questflow.domain.model.TaskFilterSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskFilterRepository @Inject constructor(
    private val presetDao: TaskFilterPresetDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {

    private val sharedPreferences = context.getSharedPreferences("task_filter_prefs", android.content.Context.MODE_PRIVATE)
    private val CURRENT_FILTER_KEY = "current_filter"

    /**
     * Get all presets as Flow
     */
    fun getAllPresetsFlow(): Flow<List<TaskFilterPresetEntity>> {
        return presetDao.getAllPresetsFlow()
    }

    /**
     * Get all presets as list
     */
    suspend fun getAllPresets(): List<TaskFilterPresetEntity> {
        return presetDao.getAllPresets()
    }

    /**
     * Get preset by ID
     */
    suspend fun getPresetById(id: Long): TaskFilterPresetEntity? {
        return presetDao.getPresetById(id)
    }

    /**
     * Get default preset (the one applied on app start)
     */
    suspend fun getDefaultPreset(): TaskFilterPresetEntity? {
        return presetDao.getDefaultPreset()
    }

    /**
     * Get default preset as Flow
     */
    fun getDefaultPresetFlow(): Flow<TaskFilterPresetEntity?> {
        return presetDao.getDefaultPresetFlow()
    }

    /**
     * Get default filter or empty filter
     */
    suspend fun getDefaultFilter(): AdvancedTaskFilter {
        val preset = getDefaultPreset()
        return if (preset != null) {
            deserializeFilter(preset.filterJson)
        } else {
            AdvancedTaskFilter()
        }
    }

    /**
     * Save filter as new preset
     */
    suspend fun saveFilterAsPreset(
        filter: AdvancedTaskFilter,
        name: String,
        description: String = "",
        setAsDefault: Boolean = false
    ): Long {
        val filterJson = TaskFilterSerializer.serialize(filter)

        // If setting as default, clear all other default flags first
        if (setAsDefault) {
            presetDao.clearAllDefaultFlags()
        }

        val preset = TaskFilterPresetEntity(
            name = name,
            description = description,
            isDefault = setAsDefault,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            filterJson = filterJson
        )

        return presetDao.insertPreset(preset)
    }

    /**
     * Update existing preset
     */
    suspend fun updatePreset(
        presetId: Long,
        filter: AdvancedTaskFilter,
        name: String? = null,
        description: String? = null,
        setAsDefault: Boolean? = null
    ) {
        val existingPreset = presetDao.getPresetById(presetId) ?: return

        // If setting as default, clear all other default flags first
        if (setAsDefault == true) {
            presetDao.clearAllDefaultFlags()
        }

        val filterJson = TaskFilterSerializer.serialize(filter)

        val updatedPreset = existingPreset.copy(
            name = name ?: existingPreset.name,
            description = description ?: existingPreset.description,
            isDefault = setAsDefault ?: existingPreset.isDefault,
            updatedAt = LocalDateTime.now(),
            filterJson = filterJson
        )

        presetDao.updatePreset(updatedPreset)
    }

    /**
     * Delete preset
     */
    suspend fun deletePreset(presetId: Long) {
        presetDao.deletePresetById(presetId)
    }

    /**
     * Set preset as default
     */
    suspend fun setPresetAsDefault(presetId: Long) {
        val preset = presetDao.getPresetById(presetId) ?: return

        // Clear all default flags
        presetDao.clearAllDefaultFlags()

        // Set this preset as default
        val updatedPreset = preset.copy(
            isDefault = true,
            updatedAt = LocalDateTime.now()
        )
        presetDao.updatePreset(updatedPreset)
    }

    /**
     * Clear default preset (no preset will be auto-applied)
     */
    suspend fun clearDefaultPreset() {
        presetDao.clearAllDefaultFlags()
    }

    /**
     * Get preset count
     */
    suspend fun getPresetCount(): Int {
        return presetDao.getPresetCount()
    }

    /**
     * Deserialize filter from JSON string
     */
    fun deserializeFilter(filterJson: String): AdvancedTaskFilter {
        return TaskFilterSerializer.deserialize(filterJson)
    }

    /**
     * Serialize filter to JSON string
     */
    fun serializeFilter(filter: AdvancedTaskFilter): String {
        return TaskFilterSerializer.serialize(filter)
    }

    /**
     * Save current filter to SharedPreferences (persists across app restarts)
     */
    fun saveCurrentFilter(filter: AdvancedTaskFilter) {
        val filterJson = TaskFilterSerializer.serialize(filter)
        sharedPreferences.edit().putString(CURRENT_FILTER_KEY, filterJson).apply()
    }

    /**
     * Get current filter from SharedPreferences
     */
    fun getCurrentFilter(): AdvancedTaskFilter {
        val filterJson = sharedPreferences.getString(CURRENT_FILTER_KEY, null)
        return if (filterJson != null) {
            TaskFilterSerializer.deserialize(filterJson)
        } else {
            AdvancedTaskFilter()
        }
    }

    /**
     * Clear current filter
     */
    fun clearCurrentFilter() {
        sharedPreferences.edit().remove(CURRENT_FILTER_KEY).apply()
    }

    /**
     * Create default "Alle Tasks" preset if no presets exist
     */
    suspend fun ensureDefaultPresetsExist() {
        val count = getPresetCount()
        if (count == 0) {
            // Create "Alle Tasks" preset
            val allTasksFilter = AdvancedTaskFilter()
            saveFilterAsPreset(
                filter = allTasksFilter,
                name = "Alle Tasks",
                description = "Zeigt alle Tasks ohne Filter",
                setAsDefault = true
            )

            // Create "Nur Offene" preset
            val openTasksFilter = AdvancedTaskFilter(
                statusFilters = com.example.questflow.domain.model.StatusFilter(
                    enabled = true,
                    showCompleted = false,
                    showOpen = true,
                    showExpired = true,
                    showClaimed = false,
                    showUnclaimed = true
                )
            )
            saveFilterAsPreset(
                filter = openTasksFilter,
                name = "Nur Offene",
                description = "Zeigt nur offene/nicht erledigte Tasks"
            )

            // Create "Heute Fällig" preset
            val todayFilter = AdvancedTaskFilter(
                dateFilters = com.example.questflow.domain.model.DateFilter(
                    enabled = true,
                    filterType = com.example.questflow.domain.model.DateFilterType.TODAY
                )
            )
            saveFilterAsPreset(
                filter = todayFilter,
                name = "Heute Fällig",
                description = "Zeigt nur Tasks die heute fällig sind"
            )

            // Create "Hohe Priorität" preset
            val highPriorityFilter = AdvancedTaskFilter(
                priorityFilters = com.example.questflow.domain.model.PriorityFilter(
                    enabled = true,
                    showUrgent = true,
                    showHigh = true,
                    showMedium = false,
                    showLow = false
                ),
                sortOptions = listOf(com.example.questflow.domain.model.SortOption.PRIORITY_DESC)
            )
            saveFilterAsPreset(
                filter = highPriorityFilter,
                name = "Hohe Priorität",
                description = "Zeigt nur dringende und wichtige Tasks"
            )
        }
    }
}
