package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.TaskDisplaySettingsDao
import com.example.questflow.data.database.entity.TaskDisplaySettingsEntity
import com.example.questflow.domain.model.TaskDisplayElementConfig
import com.example.questflow.domain.model.TaskDisplayLayoutHelper
import com.example.questflow.domain.model.DisplayElementType
import com.example.questflow.domain.model.DisplayColumn
import com.example.questflow.domain.model.getDefaultDisplayConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskDisplaySettingsRepository @Inject constructor(
    private val dao: TaskDisplaySettingsDao
) {
    /**
     * Get display settings as Flow for reactive UI updates
     * Ensures settings exist before returning
     */
    suspend fun getSettings(): Flow<TaskDisplaySettingsEntity> {
        // Initialize if needed
        if (dao.getSettingsSync() == null) {
            dao.insert(TaskDisplaySettingsEntity())
        }
        return dao.getSettings()
    }

    /**
     * Get display settings synchronously
     * Returns default settings if none exist
     */
    suspend fun getSettingsSync(): TaskDisplaySettingsEntity {
        return dao.getSettingsSync() ?: TaskDisplaySettingsEntity().also {
            dao.insert(it)
        }
    }

    /**
     * Update display settings
     */
    suspend fun updateSettings(settings: TaskDisplaySettingsEntity) {
        android.util.Log.d("DisplaySettings", "Repository.updateSettings: showDueDate=${settings.showDueDate}, showDifficulty=${settings.showDifficulty}")
        dao.update(settings)
        android.util.Log.d("DisplaySettings", "DAO.update completed")
        // Verify update
        val updated = dao.getSettingsSync()
        android.util.Log.d("DisplaySettings", "After update: showDueDate=${updated?.showDueDate}, showDifficulty=${updated?.showDifficulty}")
    }

    /**
     * Reset to default settings
     */
    suspend fun resetToDefaults() {
        val defaults = TaskDisplaySettingsEntity()
        dao.deleteAll()
        dao.insert(defaults)
    }

    /**
     * Initialize settings with defaults if not exists
     */
    suspend fun initializeIfNeeded() {
        if (dao.getSettingsSync() == null) {
            dao.insert(TaskDisplaySettingsEntity())
        }
    }

    // === V2: Layout Configuration Methods ===

    /**
     * Get layout configuration (deserializes from JSON)
     * Returns default config if not set or invalid
     */
    suspend fun getLayoutConfig(): List<TaskDisplayElementConfig> {
        val settings = getSettingsSync()
        return TaskDisplayLayoutHelper.fromJson(settings.elementLayoutConfig)
    }

    /**
     * Update layout configuration (serializes to JSON)
     */
    suspend fun updateLayoutConfig(config: List<TaskDisplayElementConfig>) {
        val settings = getSettingsSync()
        val json = TaskDisplayLayoutHelper.toJson(config)
        updateSettings(settings.copy(elementLayoutConfig = json))
        android.util.Log.d("DisplaySettings", "Layout config updated: ${config.size} elements")
    }

    /**
     * Update specific element configuration
     */
    suspend fun updateElement(
        type: DisplayElementType,
        enabled: Boolean? = null,
        column: DisplayColumn? = null,
        priority: Int? = null
    ) {
        val currentConfig = getLayoutConfig()
        val updatedConfig = TaskDisplayLayoutHelper.updateElement(
            currentConfig, type, enabled, column, priority
        )
        updateLayoutConfig(updatedConfig)
    }

    /**
     * Reset layout config to defaults
     */
    suspend fun resetLayoutToDefaults() {
        val defaultConfig = getDefaultDisplayConfig()
        updateLayoutConfig(defaultConfig)
    }
}
