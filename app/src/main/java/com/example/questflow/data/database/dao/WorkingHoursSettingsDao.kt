package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.WorkingHoursSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkingHoursSettingsDao {

    @Query("SELECT * FROM working_hours_settings WHERE id = 1")
    fun getWorkingHoursSettings(): Flow<WorkingHoursSettingsEntity?>

    @Query("SELECT * FROM working_hours_settings WHERE id = 1")
    suspend fun getWorkingHoursSettingsOnce(): WorkingHoursSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: WorkingHoursSettingsEntity)

    @Update
    suspend fun update(settings: WorkingHoursSettingsEntity)

    @Query("DELETE FROM working_hours_settings")
    suspend fun deleteAll()
}
