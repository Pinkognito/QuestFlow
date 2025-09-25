package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.UserStatsDao
import com.example.questflow.data.database.entity.UserStatsEntity
import com.example.questflow.domain.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StatsRepository @Inject constructor(
    private val userStatsDao: UserStatsDao
) {
    suspend fun getOrCreateStats(): UserStats {
        val entity = userStatsDao.getStats()
            ?: UserStatsEntity(id = 0, xp = 0, level = 1, points = 0).also {
                userStatsDao.upsert(it)
            }
        return entity.toDomainModel()
    }

    fun getStatsFlow(): Flow<UserStats> = userStatsDao.getStatsFlow().map { entity ->
        entity?.toDomainModel() ?: UserStats()
    }

    suspend fun updateStats(stats: UserStats) {
        userStatsDao.upsert(stats.toEntity())
    }

    private fun UserStatsEntity.toDomainModel() = UserStats(
        xp = xp,
        level = level,
        points = points
    )

    private fun UserStats.toEntity() = UserStatsEntity(
        id = 0,
        xp = xp,
        level = level,
        points = points
    )
}