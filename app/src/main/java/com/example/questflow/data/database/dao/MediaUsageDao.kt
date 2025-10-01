package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MediaUsageEntity
import com.example.questflow.data.database.entity.MediaUsageType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: MediaUsageEntity)

    @Delete
    suspend fun deleteUsage(usage: MediaUsageEntity)

    @Query("DELETE FROM media_usage WHERE mediaLibraryId = :mediaId AND usageType = :type AND referenceId = :refId")
    suspend fun deleteUsageByReference(mediaId: String, type: MediaUsageType, refId: Long)

    @Query("SELECT * FROM media_usage WHERE mediaLibraryId = :mediaId")
    fun getUsagesForMedia(mediaId: String): Flow<List<MediaUsageEntity>>

    @Query("SELECT * FROM media_usage WHERE mediaLibraryId = :mediaId")
    suspend fun getUsagesForMediaSync(mediaId: String): List<MediaUsageEntity>

    @Query("SELECT COUNT(*) FROM media_usage WHERE mediaLibraryId = :mediaId")
    suspend fun getUsageCount(mediaId: String): Int

    @Query("SELECT * FROM media_usage WHERE usageType = :type AND referenceId = :refId")
    suspend fun getUsageByReference(type: MediaUsageType, refId: Long): MediaUsageEntity?
}
