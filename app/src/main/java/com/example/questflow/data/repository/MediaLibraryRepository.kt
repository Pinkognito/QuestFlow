package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.MediaLibraryDao
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaLibraryRepository @Inject constructor(
    private val mediaLibraryDao: MediaLibraryDao
) {
    fun getAllMedia(): Flow<List<MediaLibraryEntity>> =
        mediaLibraryDao.getAllMedia()

    fun getMediaByType(type: MediaType): Flow<List<MediaLibraryEntity>> =
        mediaLibraryDao.getMediaByType(type)

    suspend fun getMediaById(id: String): MediaLibraryEntity? =
        mediaLibraryDao.getMediaById(id)

    suspend fun addMedia(
        fileName: String,
        filePath: String,
        mediaType: MediaType,
        fileSize: Long,
        mimeType: String,
        thumbnailPath: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val media = MediaLibraryEntity(
            id = id,
            fileName = fileName,
            filePath = filePath,
            mediaType = mediaType,
            uploadedAt = System.currentTimeMillis(),
            fileSize = fileSize,
            mimeType = mimeType,
            thumbnailPath = thumbnailPath
        )
        mediaLibraryDao.insertMedia(media)
        return id
    }

    suspend fun deleteMedia(media: MediaLibraryEntity) {
        mediaLibraryDao.deleteMedia(media)
    }

    suspend fun deleteMediaById(id: String) {
        mediaLibraryDao.deleteMediaById(id)
    }

    suspend fun getMediaCount(): Int =
        mediaLibraryDao.getMediaCount()

    suspend fun getTotalMediaSize(): Long =
        mediaLibraryDao.getTotalMediaSize()
}
