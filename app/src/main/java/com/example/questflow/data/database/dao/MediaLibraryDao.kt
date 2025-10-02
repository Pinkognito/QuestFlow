package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaLibraryDao {
    @Query("SELECT * FROM media_library ORDER BY uploadedAt DESC")
    fun getAllMedia(): Flow<List<MediaLibraryEntity>>

    @Query("SELECT * FROM media_library WHERE mediaType = :type ORDER BY uploadedAt DESC")
    fun getMediaByType(type: MediaType): Flow<List<MediaLibraryEntity>>

    @Query("SELECT * FROM media_library WHERE id = :id")
    suspend fun getMediaById(id: String): MediaLibraryEntity?

    @Query("SELECT * FROM media_library WHERE tags LIKE '%' || :tag || '%' ORDER BY uploadedAt DESC")
    fun getMediaByTag(tag: String): Flow<List<MediaLibraryEntity>>

    @Query("SELECT * FROM media_library WHERE mediaType = :type AND tags LIKE '%' || :tag || '%' ORDER BY uploadedAt DESC")
    fun getMediaByTypeAndTag(type: MediaType, tag: String): Flow<List<MediaLibraryEntity>>

    @Query("SELECT DISTINCT tags FROM media_library WHERE tags != ''")
    suspend fun getAllTags(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaLibraryEntity)

    @Update
    suspend fun updateMedia(media: MediaLibraryEntity)

    @Delete
    suspend fun deleteMedia(media: MediaLibraryEntity)

    @Query("DELETE FROM media_library WHERE id = :id")
    suspend fun deleteMediaById(id: String)

    @Query("SELECT COUNT(*) FROM media_library")
    suspend fun getMediaCount(): Int

    @Query("SELECT SUM(fileSize) FROM media_library")
    suspend fun getTotalMediaSize(): Long

    /**
     * Search media by query string (searches in displayName, fileName, description, and tags)
     * Uses partial matching with LIKE operator
     */
    @Query("""
        SELECT * FROM media_library
        WHERE displayName LIKE '%' || :query || '%'
           OR fileName LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY uploadedAt DESC
    """)
    fun searchMedia(query: String): Flow<List<MediaLibraryEntity>>

    /**
     * Search media by query and filter by media type
     */
    @Query("""
        SELECT * FROM media_library
        WHERE (displayName LIKE '%' || :query || '%'
           OR fileName LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%')
           AND mediaType = :type
        ORDER BY uploadedAt DESC
    """)
    fun searchMediaByType(query: String, type: MediaType): Flow<List<MediaLibraryEntity>>

    /**
     * Get media filtered by date range
     */
    @Query("""
        SELECT * FROM media_library
        WHERE uploadedAt >= :startDate AND uploadedAt <= :endDate
        ORDER BY uploadedAt DESC
    """)
    fun getMediaByDateRange(startDate: Long, endDate: Long): Flow<List<MediaLibraryEntity>>

    /**
     * Get media used in a specific category
     */
    @Query("""
        SELECT DISTINCT ml.* FROM media_library ml
        INNER JOIN media_usage mu ON ml.id = mu.mediaLibraryId
        WHERE mu.categoryId = :categoryId
        ORDER BY ml.uploadedAt DESC
    """)
    fun getMediaByCategory(categoryId: Long): Flow<List<MediaLibraryEntity>>

    /**
     * Get global media (no category filter)
     */
    @Query("""
        SELECT DISTINCT ml.* FROM media_library ml
        INNER JOIN media_usage mu ON ml.id = mu.mediaLibraryId
        WHERE mu.categoryId IS NULL
        ORDER BY ml.uploadedAt DESC
    """)
    fun getGlobalMedia(): Flow<List<MediaLibraryEntity>>
}
