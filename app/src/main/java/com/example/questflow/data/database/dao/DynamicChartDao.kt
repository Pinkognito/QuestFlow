package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.DynamicChartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DynamicChartDao {
    @Query("SELECT * FROM dynamic_charts WHERE isVisible = 1 ORDER BY position ASC")
    fun getAllCharts(): Flow<List<DynamicChartEntity>>

    @Query("SELECT * FROM dynamic_charts WHERE id = :chartId")
    suspend fun getChartById(chartId: Long): DynamicChartEntity?

    @Insert
    suspend fun insertChart(chart: DynamicChartEntity): Long

    @Update
    suspend fun updateChart(chart: DynamicChartEntity)

    @Delete
    suspend fun deleteChart(chart: DynamicChartEntity)

    @Query("DELETE FROM dynamic_charts WHERE id = :chartId")
    suspend fun deleteChartById(chartId: Long)

    @Query("UPDATE dynamic_charts SET position = :newPosition WHERE id = :chartId")
    suspend fun updateChartPosition(chartId: Long, newPosition: Int)

    @Transaction
    suspend fun reorderCharts(chartIds: List<Long>) {
        chartIds.forEachIndexed { index, chartId ->
            updateChartPosition(chartId, index)
        }
    }
}
