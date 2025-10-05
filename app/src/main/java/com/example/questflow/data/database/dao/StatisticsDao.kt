package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.StatisticsConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Query("SELECT * FROM statistics_config WHERE id = 1")
    fun getConfig(): Flow<StatisticsConfigEntity?>

    @Query("SELECT * FROM statistics_config WHERE id = 1")
    suspend fun getConfigOnce(): StatisticsConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: StatisticsConfigEntity)

    @Update
    suspend fun updateConfig(config: StatisticsConfigEntity)
}
