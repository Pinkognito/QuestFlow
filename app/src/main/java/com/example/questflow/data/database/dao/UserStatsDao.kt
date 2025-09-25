package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 0 LIMIT 1")
    suspend fun getStats(): UserStatsEntity?

    @Query("SELECT * FROM user_stats WHERE id = 0 LIMIT 1")
    fun getStatsFlow(): Flow<UserStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStatsEntity)

    @Query("UPDATE user_stats SET xp = :xp, level = :level, points = :points WHERE id = 0")
    suspend fun updateStats(xp: Long, level: Int, points: Int)
}