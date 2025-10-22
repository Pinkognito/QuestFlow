package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.TimeBlockDao
import com.example.questflow.data.database.dao.TimeBlockWithTags
import com.example.questflow.data.database.entity.TimeBlockEntity
import com.example.questflow.data.database.entity.TimeBlockTagEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeBlockRepository @Inject constructor(
    private val timeBlockDao: TimeBlockDao
) {
    fun getAllTimeBlocksFlow(): Flow<List<TimeBlockEntity>> =
        timeBlockDao.getAllTimeBlocksFlow()

    fun getAllTimeBlocksWithTagsFlow(): Flow<List<TimeBlockWithTags>> =
        timeBlockDao.getAllTimeBlocksWithTagsFlow()

    fun getActiveTimeBlocksWithTagsFlow(): Flow<List<TimeBlockWithTags>> =
        timeBlockDao.getActiveTimeBlocksWithTagsFlow()

    suspend fun getActiveTimeBlocks(): List<TimeBlockEntity> =
        timeBlockDao.getActiveTimeBlocks()

    suspend fun getTimeBlocksForDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TimeBlockEntity> =
        timeBlockDao.getTimeBlocksForDateRange(
            startDate = startDate.toString(),
            endDate = endDate.toString()
        )

    suspend fun insertTimeBlock(timeBlock: TimeBlockEntity): Long =
        timeBlockDao.insert(timeBlock)

    suspend fun updateTimeBlock(timeBlock: TimeBlockEntity) =
        timeBlockDao.update(timeBlock.copy(updatedAt = LocalDateTime.now().toString()))

    suspend fun deleteTimeBlockById(id: Long) =
        timeBlockDao.deleteById(id)

    suspend fun updateActiveStatus(id: Long, isActive: Boolean) =
        timeBlockDao.updateActiveStatus(id, isActive)

    suspend fun saveTimeBlockWithTags(
        timeBlock: TimeBlockEntity,
        tagIds: List<Long>
    ): Long {
        val timeBlockId = if (timeBlock.id == 0L) {
            insertTimeBlock(timeBlock)
        } else {
            updateTimeBlock(timeBlock)
            timeBlock.id
        }

        timeBlockDao.deleteTimeBlockTags(timeBlockId)
        tagIds.forEach { tagId ->
            timeBlockDao.insertTimeBlockTag(TimeBlockTagEntity(timeBlockId = timeBlockId, tagId = tagId))
        }

        return timeBlockId
    }
}
