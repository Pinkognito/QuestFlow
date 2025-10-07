package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.TagUsageStatsDao
import com.example.questflow.data.database.entity.TagUsageStatsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagUsageRepository @Inject constructor(
    private val tagUsageStatsDao: TagUsageStatsDao
) {
    fun getAllStatsFlow(): Flow<List<TagUsageStatsEntity>> = tagUsageStatsDao.getAllStatsFlow()

    suspend fun searchTags(query: String, limit: Int = 10): List<TagUsageStatsEntity> =
        tagUsageStatsDao.searchTags(query, limit)

    suspend fun getTopTags(limit: Int = 10): List<TagUsageStatsEntity> =
        tagUsageStatsDao.getTopTags(limit)

    suspend fun getStatsByTag(tag: String): TagUsageStatsEntity? =
        tagUsageStatsDao.getStatsByTag(tag)

    suspend fun incrementUsage(tag: String) {
        tagUsageStatsDao.incrementOrCreateTag(tag, LocalDateTime.now())
    }

    suspend fun incrementUsageForMultiple(tags: List<String>) {
        val now = LocalDateTime.now()
        tags.forEach { tag ->
            tagUsageStatsDao.incrementOrCreateTag(tag, now)
        }
    }

    /**
     * Get ranked tag suggestions based on query and usage
     * Scoring: Exact match > Starts with > Contains, then by usage count
     */
    suspend fun getRankedSuggestions(query: String, limit: Int = 10): List<TagSuggestion> {
        if (query.isBlank()) {
            return getTopTags(limit).map { TagSuggestion(it.tag, it.usageCount, 0) }
        }

        val allMatches = searchTags(query, limit * 3) // Get more to rank properly
        val normalizedQuery = query.lowercase().trim()

        return allMatches.map { stats ->
            val normalizedTag = stats.tag.lowercase()
            val score = when {
                normalizedTag == normalizedQuery -> 1000 + stats.usageCount
                normalizedTag.startsWith(normalizedQuery) -> 500 + stats.usageCount
                normalizedTag.contains(normalizedQuery) -> 100 + stats.usageCount
                else -> stats.usageCount
            }
            TagSuggestion(stats.tag, stats.usageCount, score)
        }.sortedByDescending { it.score }.take(limit)
    }
}

data class TagSuggestion(
    val tag: String,
    val usageCount: Int,
    val score: Int
)
