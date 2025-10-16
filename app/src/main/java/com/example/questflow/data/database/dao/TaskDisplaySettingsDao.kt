package com.example.questflow.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.questflow.data.database.entity.TaskDisplaySettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDisplaySettingsDao {
    @Query("SELECT * FROM task_display_settings WHERE id = 1")
    fun getSettings(): Flow<TaskDisplaySettingsEntity>

    @Query("SELECT * FROM task_display_settings WHERE id = 1")
    suspend fun getSettingsSync(): TaskDisplaySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: TaskDisplaySettingsEntity)

    @Update
    suspend fun update(settings: TaskDisplaySettingsEntity)

    @Query("DELETE FROM task_display_settings")
    suspend fun deleteAll()
}
