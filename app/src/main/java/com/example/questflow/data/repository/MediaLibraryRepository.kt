package com.example.questflow.data.repository

import android.net.Uri
import android.util.Log
import com.example.questflow.data.database.dao.MediaLibraryDao
import com.example.questflow.data.database.dao.MediaUsageDao
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import com.example.questflow.data.database.entity.MediaUsageEntity
import com.example.questflow.data.database.entity.MediaUsageType
import com.example.questflow.data.manager.FileStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class MediaWithUsage(
    val media: MediaLibraryEntity,
    val usageCount: Int,
    val usages: List<MediaUsageEntity>
)

@Singleton
class MediaLibraryRepository @Inject constructor(
    private val mediaLibraryDao: MediaLibraryDao,
    private val mediaUsageDao: MediaUsageDao,
    private val fileStorageManager: FileStorageManager,
    private val collectionRepository: CollectionRepository
) {
    companion object {
        private const val TAG = "MediaLibraryRepository"
    }

    fun getAllMedia(): Flow<List<MediaLibraryEntity>> =
        mediaLibraryDao.getAllMedia()

    fun getMediaByType(type: MediaType): Flow<List<MediaLibraryEntity>> =
        mediaLibraryDao.getMediaByType(type)

    fun getMediaByTag(tag: String): Flow<List<MediaLibraryEntity>> =
        mediaLibraryDao.getMediaByTag(tag)

    fun getMediaByTypeAndTag(type: MediaType, tag: String): Flow<List<MediaLibraryEntity>> =
        mediaLibraryDao.getMediaByTypeAndTag(type, tag)

    suspend fun getAllTags(): List<String> {
        val rawTags = mediaLibraryDao.getAllTags()
        return rawTags.flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    suspend fun getMediaById(id: String): MediaLibraryEntity? =
        mediaLibraryDao.getMediaById(id)

    suspend fun updateMediaTags(mediaId: String, tags: List<String>) {
        val media = getMediaById(mediaId) ?: return
        val tagsString = tags.joinToString(",")
        val updatedMedia = media.copy(tags = tagsString)
        mediaLibraryDao.updateMedia(updatedMedia)
        Log.d(TAG, "Updated tags for media $mediaId: $tagsString")
    }

    /**
     * Update media metadata (displayName, description, tags)
     */
    suspend fun updateMediaMetadata(
        mediaId: String,
        displayName: String? = null,
        description: String? = null,
        tags: String? = null
    ) {
        try {
            val media = getMediaById(mediaId)
            if (media == null) {
                Log.w(TAG, "Cannot update metadata: Media $mediaId not found")
                return
            }

            val updatedMedia = media.copy(
                displayName = displayName ?: media.displayName,
                description = description ?: media.description,
                tags = tags ?: media.tags
            )

            mediaLibraryDao.updateMedia(updatedMedia)
            Log.d(TAG, "✅ [METADATA_UPDATE] Updated metadata for media $mediaId - displayName: '${updatedMedia.displayName}', description: '${updatedMedia.description}', tags: '${updatedMedia.tags}'")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [METADATA_UPDATE] Failed to update metadata for media $mediaId", e)
        }
    }

    /**
     * Search media with partial string matching
     */
    fun searchMedia(query: String): Flow<List<MediaLibraryEntity>> {
        Log.d(TAG, "🔍 [SEARCH] Searching media with query: '$query'")
        return mediaLibraryDao.searchMedia(query)
    }

    fun searchMediaByType(query: String, type: MediaType): Flow<List<MediaLibraryEntity>> {
        Log.d(TAG, "🔍 [SEARCH] Searching media by type $type with query: '$query'")
        return mediaLibraryDao.searchMediaByType(query, type)
    }

    fun getMediaByDateRange(startDate: Long, endDate: Long): Flow<List<MediaLibraryEntity>> {
        Log.d(TAG, "🔍 [SEARCH] Getting media by date range: $startDate - $endDate")
        return mediaLibraryDao.getMediaByDateRange(startDate, endDate)
    }

    fun getMediaByCategory(categoryId: Long?): Flow<List<MediaLibraryEntity>> {
        Log.d(TAG, "🔍 [FILTER] Getting media by category: ${categoryId ?: "Global"}")
        return if (categoryId == null) {
            mediaLibraryDao.getGlobalMedia()
        } else {
            mediaLibraryDao.getMediaByCategory(categoryId)
        }
    }

    /**
     * Add media from URI - handles file storage and database entry
     */
    suspend fun addMediaFromUri(
        uri: Uri,
        fileName: String,
        mediaType: MediaType,
        mimeType: String,
        displayName: String = "",
        description: String = "",
        tags: String = ""
    ): String? {
        try {
            Log.d(TAG, "📤 [UPLOAD] Adding media from URI: $uri, fileName: $fileName")
            Log.d(TAG, "📤 [UPLOAD] Metadata - displayName: '$displayName', description: '$description', tags: '$tags'")

            // Save file to media library storage
            val (filePath, fileSize) = fileStorageManager.saveToMediaLibrary(uri) ?: run {
                Log.e(TAG, "❌ [UPLOAD] Failed to save file to storage")
                return null
            }

            // Create database entry
            val id = UUID.randomUUID().toString()
            val media = MediaLibraryEntity(
                id = id,
                fileName = fileName,
                filePath = filePath,
                mediaType = mediaType,
                uploadedAt = System.currentTimeMillis(),
                fileSize = fileSize,
                mimeType = mimeType,
                thumbnailPath = null,
                displayName = displayName,
                description = description,
                tags = tags
            )

            mediaLibraryDao.insertMedia(media)
            Log.d(TAG, "✅ [UPLOAD] Media added successfully with ID: $id")
            return id
        } catch (e: Exception) {
            Log.e(TAG, "❌ [UPLOAD] Error adding media from URI", e)
            return null
        }
    }

    /**
     * Add media manually (if file already exists)
     */
    suspend fun addMedia(
        fileName: String,
        filePath: String,
        mediaType: MediaType,
        fileSize: Long,
        mimeType: String,
        thumbnailPath: String? = null,
        displayName: String = "",
        description: String = "",
        tags: String = ""
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
            thumbnailPath = thumbnailPath,
            displayName = displayName,
            description = description,
            tags = tags
        )
        mediaLibraryDao.insertMedia(media)
        Log.d(TAG, "📤 [UPLOAD] Media added manually with ID: $id")
        return id
    }

    /**
     * Delete media and associated file, including all collection items that reference it
     */
    suspend fun deleteMedia(media: MediaLibraryEntity) {
        try {
            Log.d(TAG, "🗑️ [MEDIA_DELETE] Deleting media: ${media.id}")

            // Get all usages for this media
            val usages = mediaUsageDao.getUsagesForMediaSync(media.id)
            Log.d(TAG, "🗑️ [MEDIA_DELETE] Found ${usages.size} usages")

            // Delete all associated collection items
            usages.forEach { usage ->
                if (usage.usageType == MediaUsageType.COLLECTION_ITEM) {
                    try {
                        val collectionItem = collectionRepository.getItemById(usage.referenceId)
                        if (collectionItem != null) {
                            collectionRepository.deleteCollectionItem(collectionItem)
                            Log.d(TAG, "✅ [MEDIA_DELETE] Deleted CollectionItem ${usage.referenceId}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [MEDIA_DELETE] Failed to delete CollectionItem ${usage.referenceId}", e)
                    }
                }
            }

            // Delete file from storage
            fileStorageManager.deleteMediaLibraryFile(media.filePath)

            // Delete database entry (cascade will delete usages)
            mediaLibraryDao.deleteMedia(media)

            Log.d(TAG, "✅ [MEDIA_DELETE] Media deleted successfully: ${media.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [MEDIA_DELETE] Error deleting media", e)
            throw e
        }
    }

    suspend fun deleteMediaById(id: String) {
        val media = getMediaById(id)
        if (media != null) {
            deleteMedia(media)
        } else {
            Log.w(TAG, "Cannot delete media: ID $id not found")
        }
    }

    suspend fun getMediaCount(): Int =
        mediaLibraryDao.getMediaCount()

    suspend fun getTotalMediaSize(): Long =
        mediaLibraryDao.getTotalMediaSize() ?: 0L

    /**
     * Check if media is being referenced by any collection items
     */
    suspend fun isMediaInUse(mediaId: String): Boolean {
        return mediaUsageDao.getUsageCount(mediaId) > 0
    }

    /**
     * Get media with usage information
     */
    suspend fun getMediaWithUsage(mediaId: String): MediaWithUsage? {
        val media = getMediaById(mediaId) ?: return null
        val usages = mediaUsageDao.getUsagesForMediaSync(mediaId)
        return MediaWithUsage(
            media = media,
            usageCount = usages.size,
            usages = usages
        )
    }

    /**
     * Track media usage
     */
    suspend fun trackUsage(
        mediaId: String,
        usageType: MediaUsageType,
        referenceId: Long,
        categoryId: Long? = null
    ) {
        val usage = MediaUsageEntity(
            mediaLibraryId = mediaId,
            usageType = usageType,
            referenceId = referenceId,
            categoryId = categoryId,
            createdAt = System.currentTimeMillis()
        )
        mediaUsageDao.insertUsage(usage)
        Log.d(TAG, "Tracked usage: $usageType for media $mediaId (ref: $referenceId)")
    }

    /**
     * Remove usage tracking and delete associated collection item if applicable
     */
    suspend fun removeUsageTracking(
        mediaId: String,
        usageType: MediaUsageType,
        referenceId: Long
    ) {
        Log.d(TAG, "🗑️ [USAGE_DELETE] Removing usage: $usageType for media $mediaId (ref: $referenceId)")

        // If this is a collection item usage, delete the actual collection item
        if (usageType == MediaUsageType.COLLECTION_ITEM) {
            try {
                val collectionItem = collectionRepository.getItemById(referenceId)
                if (collectionItem != null) {
                    collectionRepository.deleteCollectionItem(collectionItem)
                    Log.d(TAG, "✅ [USAGE_DELETE] Deleted CollectionItem $referenceId")
                } else {
                    Log.w(TAG, "⚠️ [USAGE_DELETE] CollectionItem $referenceId not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [USAGE_DELETE] Failed to delete CollectionItem $referenceId", e)
            }
        }

        // Remove usage tracking
        mediaUsageDao.deleteUsageByReference(mediaId, usageType, referenceId)
        Log.d(TAG, "✅ [USAGE_DELETE] Removed usage tracking for media $mediaId")
    }

    /**
     * Add multiple media from URIs (bulk upload)
     */
    suspend fun addMultipleMediaFromUris(
        uris: List<Uri>,
        displayName: String = "",
        description: String = "",
        tags: String = ""
    ): List<String> {
        Log.d(TAG, "📤 [BULK_UPLOAD] Starting bulk upload of ${uris.size} files")
        Log.d(TAG, "📤 [BULK_UPLOAD] Shared metadata - displayName: '$displayName', description: '$description', tags: '$tags'")

        val addedIds = mutableListOf<String>()

        uris.forEach { uri ->
            try {
                // Auto-detect MIME type and file name
                val mimeType = fileStorageManager.getMimeType(uri) ?: "application/octet-stream"
                val fileName = fileStorageManager.getFileName(uri) ?: "unknown_${System.currentTimeMillis()}"

                // Determine media type from MIME type
                val mediaType = when {
                    mimeType.startsWith("image/gif") -> MediaType.GIF
                    mimeType.startsWith("image/") -> MediaType.IMAGE
                    mimeType.startsWith("audio/") -> MediaType.AUDIO
                    else -> MediaType.IMAGE // Default to image
                }

                val mediaId = addMediaFromUri(
                    uri = uri,
                    fileName = fileName,
                    mediaType = mediaType,
                    mimeType = mimeType,
                    displayName = displayName,
                    description = description,
                    tags = tags
                )

                if (mediaId != null) {
                    addedIds.add(mediaId)
                    Log.d(TAG, "✅ [BULK_UPLOAD] Added media: $fileName (type: $mediaType)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [BULK_UPLOAD] Failed to add media from URI: $uri", e)
            }
        }

        Log.d(TAG, "✅ [BULK_UPLOAD] Bulk upload complete: ${addedIds.size}/${uris.size} files added")
        return addedIds
    }

    /**
     * Extract and save files from ZIP
     */
    suspend fun extractAndSaveZip(
        zipUri: Uri,
        displayName: String = "",
        description: String = "",
        tags: String = ""
    ): List<String> {
        Log.d(TAG, "📦 [ZIP_UPLOAD] Starting ZIP extraction from: $zipUri")
        Log.d(TAG, "📦 [ZIP_UPLOAD] Shared metadata - displayName: '$displayName', description: '$description', tags: '$tags'")

        val addedIds = mutableListOf<String>()

        try {
            val savedFiles = fileStorageManager.extractZipToMediaLibrary(zipUri)
            Log.d(TAG, "📦 [ZIP_UPLOAD] Extracted ${savedFiles.size} files from ZIP")

            savedFiles.forEach { (filePath, fileSize) ->
                try {
                    val fileName = filePath.substringAfterLast('/')
                    val mimeType = when (fileName.substringAfterLast('.').lowercase()) {
                        "gif" -> "image/gif"
                        "png" -> "image/png"
                        "jpg", "jpeg" -> "image/jpeg"
                        else -> "image/jpeg"
                    }

                    val mediaType = if (mimeType == "image/gif") MediaType.GIF else MediaType.IMAGE

                    val id = addMedia(
                        fileName = fileName,
                        filePath = filePath,
                        mediaType = mediaType,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        displayName = displayName,
                        description = description,
                        tags = tags
                    )

                    addedIds.add(id)
                    Log.d(TAG, "✅ [ZIP_UPLOAD] Added file from ZIP: $fileName")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [ZIP_UPLOAD] Failed to add media from ZIP file: $filePath", e)
                }
            }

            Log.d(TAG, "✅ [ZIP_UPLOAD] ZIP extraction complete: ${addedIds.size} files added")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [ZIP_UPLOAD] Error extracting ZIP", e)
        }

        return addedIds
    }

    /**
     * Add multiple media to collection as collection items
     * Creates actual CollectionItemEntity entries and tracks usage
     */
    suspend fun addMediaToCollection(
        mediaIds: List<String>,
        categoryId: Long?,
        name: String,
        description: String,
        rarity: String
    ): Int {
        Log.d(TAG, "📦 [COLLECTION_TRANSFER] Adding ${mediaIds.size} media to collection")
        Log.d(TAG, "📦 [COLLECTION_TRANSFER] Category: ${categoryId ?: "Global"}, Name: '$name', Rarity: $rarity")

        var count = 0
        mediaIds.forEach { mediaId ->
            try {
                // Get media to check if it exists
                val media = getMediaById(mediaId)
                if (media == null) {
                    Log.w(TAG, "⚠️ [COLLECTION_TRANSFER] Media $mediaId not found, skipping")
                    return@forEach
                }

                // Create CollectionItemEntity
                val itemName = if (name.isNotBlank()) name else media.displayName.ifBlank { media.fileName }
                val itemDescription = if (description.isNotBlank()) description else media.description

                val itemId = collectionRepository.addCollectionItem(
                    name = itemName,
                    description = itemDescription,
                    mediaLibraryId = mediaId,
                    rarity = rarity,
                    requiredLevel = 1, // Default level requirement
                    categoryId = categoryId
                )

                Log.d(TAG, "✅ [COLLECTION_TRANSFER] Created CollectionItem $itemId for media $mediaId")

                // Track usage
                trackUsage(
                    mediaId = mediaId,
                    usageType = MediaUsageType.COLLECTION_ITEM,
                    referenceId = itemId,
                    categoryId = categoryId
                )

                count++
                Log.d(TAG, "✅ [COLLECTION_TRANSFER] Successfully added media $mediaId to collection")
            } catch (e: Exception) {
                Log.e(TAG, "❌ [COLLECTION_TRANSFER] Failed to add media $mediaId to collection", e)
            }
        }

        Log.d(TAG, "✅ [COLLECTION_TRANSFER] Transfer complete: $count/${mediaIds.size} items created")
        return count
    }
}
