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
}
