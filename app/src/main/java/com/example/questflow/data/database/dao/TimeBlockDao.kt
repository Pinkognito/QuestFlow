package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataTagEntity
import com.example.questflow.data.database.entity.TimeBlockEntity
import com.example.questflow.data.database.entity.TimeBlockTagEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * TimeBlock mit zugehörigen Tags
 */
data class TimeBlockWithTags(
    @Embedded val timeBlock: TimeBlockEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TimeBlockTagEntity::class,
            parentColumn = "timeBlockId",
            entityColumn = "tagId"
        )
    )
    val tags: List<MetadataTagEntity>
)

@Dao
interface TimeBlockDao {
    // === BASIC QUERIES ===

    @Query("SELECT * FROM time_blocks ORDER BY name ASC")
    fun getAllTimeBlocksFlow(): Flow<List<TimeBlockEntity>>

    @Query("SELECT * FROM time_blocks WHERE id = :id")
    fun getTimeBlockByIdFlow(id: Long): Flow<TimeBlockEntity?>

    @Query("SELECT * FROM time_blocks WHERE id = :id")
    suspend fun getTimeBlockById(id: Long): TimeBlockEntity?

    // === QUERIES WITH TAGS ===

    @Transaction
    @Query("SELECT * FROM time_blocks ORDER BY name ASC")
    fun getAllTimeBlocksWithTagsFlow(): Flow<List<TimeBlockWithTags>>

    @Transaction
    @Query("SELECT * FROM time_blocks WHERE id = :id")
    fun getTimeBlockWithTagsByIdFlow(id: Long): Flow<TimeBlockWithTags?>

    @Transaction
    @Query("SELECT * FROM time_blocks WHERE id = :id")
    suspend fun getTimeBlockWithTagsById(id: Long): TimeBlockWithTags?

    // === ACTIVE TIME BLOCKS ===

    @Query("""
        SELECT * FROM time_blocks
        WHERE isActive = 1
        AND (activationDate IS NULL OR activationDate <= :now)
        AND (deactivationDate IS NULL OR deactivationDate > :now)
        ORDER BY name ASC
    """)
    fun getActiveTimeBlocksFlow(now: String = LocalDateTime.now().toString()): Flow<List<TimeBlockEntity>>

    @Query("""
        SELECT * FROM time_blocks
        WHERE isActive = 1
        AND (activationDate IS NULL OR activationDate <= :now)
        AND (deactivationDate IS NULL OR deactivationDate > :now)
    """)
    suspend fun getActiveTimeBlocks(now: String = LocalDateTime.now().toString()): List<TimeBlockEntity>

    @Transaction
    @Query("""
        SELECT * FROM time_blocks
        WHERE isActive = 1
        AND (activationDate IS NULL OR activationDate <= :now)
        AND (deactivationDate IS NULL OR deactivationDate > :now)
        ORDER BY name ASC
    """)
    fun getActiveTimeBlocksWithTagsFlow(now: String = LocalDateTime.now().toString()): Flow<List<TimeBlockWithTags>>

    // === FILTERING ===

    @Query("SELECT * FROM time_blocks WHERE type = :type ORDER BY name ASC")
    fun getTimeBlocksByTypeFlow(type: String): Flow<List<TimeBlockEntity>>

    @Query("SELECT * FROM time_blocks WHERE contactId = :contactId ORDER BY name ASC")
    fun getTimeBlocksByContactFlow(contactId: Long): Flow<List<TimeBlockEntity>>

    @Query("""
        SELECT tb.* FROM time_blocks tb
        INNER JOIN time_block_tags tbt ON tb.id = tbt.timeBlockId
        WHERE tbt.tagId = :tagId
        ORDER BY tb.name ASC
    """)
    fun getTimeBlocksByTagFlow(tagId: Long): Flow<List<TimeBlockEntity>>

    // === TIME RANGE QUERIES ===

    /**
     * Findet alle TimeBlocks, die potenziell im gegebenen Datumsbereich aktiv sein könnten
     * (Detaillierte Matching-Logik erfolgt in der Business-Schicht)
     */
    @Query("""
        SELECT * FROM time_blocks
        WHERE isActive = 1
        AND (activationDate IS NULL OR activationDate <= :endDate)
        AND (deactivationDate IS NULL OR deactivationDate >= :startDate)
        AND (validFrom IS NULL OR validFrom <= :endDate)
        AND (validUntil IS NULL OR validUntil >= :startDate)
        ORDER BY name ASC
    """)
    suspend fun getTimeBlocksForDateRange(
        startDate: String,
        endDate: String
    ): List<TimeBlockEntity>

    @Transaction
    @Query("""
        SELECT * FROM time_blocks
        WHERE isActive = 1
        AND (activationDate IS NULL OR activationDate <= :endDate)
        AND (deactivationDate IS NULL OR deactivationDate >= :startDate)
        AND (validFrom IS NULL OR validFrom <= :endDate)
        AND (validUntil IS NULL OR validUntil >= :startDate)
        ORDER BY name ASC
    """)
    suspend fun getTimeBlocksWithTagsForDateRange(
        startDate: String,
        endDate: String
    ): List<TimeBlockWithTags>

    // === SEARCH ===

    @Query("""
        SELECT * FROM time_blocks
        WHERE name LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR type LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchTimeBlocksFlow(query: String): Flow<List<TimeBlockEntity>>

    // === INSERT / UPDATE / DELETE ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timeBlock: TimeBlockEntity): Long

    @Update
    suspend fun update(timeBlock: TimeBlockEntity)

    @Delete
    suspend fun delete(timeBlock: TimeBlockEntity)

    @Query("DELETE FROM time_blocks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE time_blocks SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean, updatedAt: String = LocalDateTime.now().toString())

    // === TAG MANAGEMENT ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTimeBlockTag(tag: TimeBlockTagEntity)

    @Query("DELETE FROM time_block_tags WHERE timeBlockId = :timeBlockId")
    suspend fun deleteTimeBlockTags(timeBlockId: Long)

    @Query("DELETE FROM time_block_tags WHERE timeBlockId = :timeBlockId AND tagId = :tagId")
    suspend fun deleteTimeBlockTag(timeBlockId: Long, tagId: Long)

    @Query("SELECT COUNT(*) FROM time_block_tags WHERE timeBlockId = :timeBlockId AND tagId = :tagId")
    suspend fun isTagLinked(timeBlockId: Long, tagId: Long): Int

    // === STATISTICS ===

    @Query("SELECT COUNT(*) FROM time_blocks")
    suspend fun getTimeBlockCount(): Int

    @Query("SELECT COUNT(*) FROM time_blocks WHERE isActive = 1")
    suspend fun getActiveTimeBlockCount(): Int

    @Query("SELECT COUNT(*) FROM time_blocks WHERE type = :type")
    suspend fun getTimeBlockCountByType(type: String): Int
}
