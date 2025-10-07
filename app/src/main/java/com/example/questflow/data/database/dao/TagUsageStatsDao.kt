package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.TagUsageStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagUsageStatsDao {
    @Query("SELECT * FROM tag_usage_stats ORDER BY usageCount DESC, lastUsedAt DESC")
    fun getAllStatsFlow(): Flow<List<TagUsageStatsEntity>>

    @Query("SELECT * FROM tag_usage_stats WHERE tag LIKE '%' || :query || '%' ORDER BY usageCount DESC LIMIT :limit")
    suspend fun searchTags(query: String, limit: Int = 10): List<TagUsageStatsEntity>

    @Query("SELECT * FROM tag_usage_stats ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getTopTags(limit: Int = 10): List<TagUsageStatsEntity>

    @Query("SELECT * FROM tag_usage_stats WHERE tag = :tag")
    suspend fun getStatsByTag(tag: String): TagUsageStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: TagUsageStatsEntity)

    @Update
    suspend fun updateStats(stats: TagUsageStatsEntity)

    @Query("UPDATE tag_usage_stats SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE tag = :tag")
    suspend fun incrementUsage(tag: String, timestamp: java.time.LocalDateTime)

    @Transaction
    suspend fun incrementOrCreateTag(tag: String, timestamp: java.time.LocalDateTime) {
        val existing = getStatsByTag(tag)
        if (existing != null) {
            incrementUsage(tag, timestamp)
        } else {
            insertStats(
                TagUsageStatsEntity(
                    tag = tag,
                    usageCount = 1,
                    lastUsedAt = timestamp,
                    createdAt = timestamp
                )
            )
        }
    }
}
