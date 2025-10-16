package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.TaskFilterPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskFilterPresetDao {

    @Query("SELECT * FROM task_filter_presets ORDER BY isDefault DESC, name ASC")
    fun getAllPresetsFlow(): Flow<List<TaskFilterPresetEntity>>

    @Query("SELECT * FROM task_filter_presets ORDER BY isDefault DESC, name ASC")
    suspend fun getAllPresets(): List<TaskFilterPresetEntity>

    @Query("SELECT * FROM task_filter_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): TaskFilterPresetEntity?

    @Query("SELECT * FROM task_filter_presets WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPreset(): TaskFilterPresetEntity?

    @Query("SELECT * FROM task_filter_presets WHERE isDefault = 1 LIMIT 1")
    fun getDefaultPresetFlow(): Flow<TaskFilterPresetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TaskFilterPresetEntity): Long

    @Update
    suspend fun updatePreset(preset: TaskFilterPresetEntity)

    @Delete
    suspend fun deletePreset(preset: TaskFilterPresetEntity)

    @Query("DELETE FROM task_filter_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)

    @Query("UPDATE task_filter_presets SET isDefault = 0")
    suspend fun clearAllDefaultFlags()

    @Query("SELECT COUNT(*) FROM task_filter_presets")
    suspend fun getPresetCount(): Int
}
