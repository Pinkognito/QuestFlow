package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.DynamicChartDao
import com.example.questflow.domain.model.DynamicChartConfig
import com.example.questflow.domain.model.toEntity
import com.example.questflow.domain.model.toDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicChartRepository @Inject constructor(
    private val dynamicChartDao: DynamicChartDao
) {
    fun getAllCharts(): Flow<List<DynamicChartConfig>> =
        dynamicChartDao.getAllCharts().map { list -> list.map { it.toDomainModel() } }

    suspend fun getChartById(id: Long): DynamicChartConfig? =
        dynamicChartDao.getChartById(id)?.toDomainModel()

    suspend fun addChart(chart: DynamicChartConfig): Long =
        dynamicChartDao.insertChart(chart.toEntity())

    suspend fun updateChart(chart: DynamicChartConfig) =
        dynamicChartDao.updateChart(chart.toEntity())

    suspend fun deleteChart(chart: DynamicChartConfig) =
        dynamicChartDao.deleteChartById(chart.id)

    suspend fun reorderCharts(chartIds: List<Long>) =
        dynamicChartDao.reorderCharts(chartIds)
}
