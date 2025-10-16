package com.example.questflow.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.questflow.data.database.entity.TaskSearchFilterSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskSearchFilterSettingsDao {
    @Query("SELECT * FROM task_search_filter_settings WHERE id = 1")
    fun getSettings(): Flow<TaskSearchFilterSettingsEntity?>

    @Query("SELECT * FROM task_search_filter_settings WHERE id = 1")
    suspend fun getSettingsSync(): TaskSearchFilterSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: TaskSearchFilterSettingsEntity)

    @Update
    suspend fun update(settings: TaskSearchFilterSettingsEntity)

    @Query("DELETE FROM task_search_filter_settings")
    suspend fun deleteAll()
}
