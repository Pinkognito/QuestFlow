package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.TaskSearchFilterSettingsDao
import com.example.questflow.data.database.entity.TaskSearchFilterSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskSearchFilterRepository @Inject constructor(
    private val dao: TaskSearchFilterSettingsDao
) {
    fun getSettings(): Flow<TaskSearchFilterSettingsEntity> {
        return dao.getSettings().map { it ?: TaskSearchFilterSettingsEntity() }
    }

    suspend fun getSettingsSync(): TaskSearchFilterSettingsEntity {
        return dao.getSettingsSync() ?: TaskSearchFilterSettingsEntity()
    }

    suspend fun updateSettings(settings: TaskSearchFilterSettingsEntity) {
        dao.insertOrUpdate(settings)
    }

    suspend fun resetToDefaults() {
        dao.deleteAll()
        dao.insertOrUpdate(TaskSearchFilterSettingsEntity())
    }
}
