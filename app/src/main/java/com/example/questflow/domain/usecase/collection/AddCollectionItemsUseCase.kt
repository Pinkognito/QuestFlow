package com.example.questflow.domain.usecase.collection

import android.net.Uri
import android.util.Log
import com.example.questflow.data.database.entity.MediaUsageType
import com.example.questflow.data.manager.FileStorageManager
import com.example.questflow.data.repository.CollectionRepository
import com.example.questflow.data.repository.MediaLibraryRepository
import javax.inject.Inject

class AddCollectionItemsUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val fileStorageManager: FileStorageManager
) {
    companion object {
        private const val TAG = "AddCollectionItemsUseCase"
    }

    /**
     * Add a single collection item with media library reference
     */
    suspend fun addSingleItem(
        mediaLibraryId: String,
        name: String,
        description: String,
        rarity: String,
        requiredLevel: Int,
        categoryId: Long?
    ): Result {
        Log.d(TAG, "Adding single collection item: $name with mediaLibraryId: $mediaLibraryId")

        try {
            val itemId = collectionRepository.addCollectionItem(
                name = name,
                description = description,
                mediaLibraryId = mediaLibraryId,
                rarity = rarity,
                requiredLevel = requiredLevel,
                categoryId = categoryId
            )

            // Track media usage
            mediaLibraryRepository.trackUsage(
                mediaId = mediaLibraryId,
                usageType = MediaUsageType.COLLECTION_ITEM,
                referenceId = itemId,
                categoryId = categoryId
            )

            Log.d(TAG, "Successfully added collection item with ID: $itemId and tracked usage")
            return Result.Success(1)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add collection item", e)
            return Result.Error("Failed to add collection item: ${e.message}")
        }
    }

    /**
     * Add a single image to collection (legacy method - still used for bulk operations)
     */
    suspend fun addSingleImage(
        uri: Uri,
        name: String,
        description: String,
        rarity: String,
        requiredLevel: Int,
        categoryId: Long?
    ): Result {
        Log.d(TAG, "Adding single image: $name")

        val savedUri = fileStorageManager.saveImage(uri, categoryId)
        if (savedUri == null) {
            Log.e(TAG, "Failed to save image")
            return Result.Error("Failed to save image")
        }

        val itemId = collectionRepository.addCollectionItem(
            name = name,
            description = description,
            mediaLibraryId = "", // Legacy: empty for old workflow
            rarity = rarity,
            requiredLevel = requiredLevel,
            categoryId = categoryId
        )

        Log.d(TAG, "Successfully added collection item with ID: $itemId")
        return Result.Success(1)
    }

    /**
     * Add multiple images to collection
     */
    suspend fun addMultipleImages(
        uris: List<Uri>,
        baseNamePrefix: String,
        rarity: String,
        startRequiredLevel: Int,
        categoryId: Long?
    ): Result {
        Log.d(TAG, "Adding ${uris.size} images with prefix: $baseNamePrefix")

        val savedUris = fileStorageManager.saveImages(uris, categoryId)
        if (savedUris.isEmpty()) {
            Log.e(TAG, "No images were saved successfully")
            return Result.Error("Failed to save images")
        }

        var addedCount = 0
        savedUris.forEachIndexed { index, savedUri ->
            try {
                collectionRepository.addCollectionItem(
                    name = "$baseNamePrefix ${index + 1}",
                    description = "",
                    mediaLibraryId = "", // Legacy: empty for old workflow
                    rarity = rarity,
                    requiredLevel = startRequiredLevel + index,
                    categoryId = categoryId
                )
                addedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add collection item ${index + 1}", e)
            }
        }

        Log.d(TAG, "Successfully added $addedCount collection items")
        return if (addedCount > 0) {
            Result.Success(addedCount)
        } else {
            Result.Error("Failed to add collection items to database")
        }
    }

    /**
     * Add images from ZIP file to collection
     */
    suspend fun addFromZip(
        zipUri: Uri,
        baseNamePrefix: String,
        rarity: String,
        startRequiredLevel: Int,
        categoryId: Long?
    ): Result {
        Log.d(TAG, "Extracting and adding images from ZIP")

        val savedUris = fileStorageManager.extractAndSaveZip(zipUri, categoryId)
        if (savedUris.isEmpty()) {
            Log.e(TAG, "No images were extracted from ZIP")
            return Result.Error("Failed to extract images from ZIP")
        }

        var addedCount = 0
        savedUris.forEachIndexed { index, savedUri ->
            try {
                collectionRepository.addCollectionItem(
                    name = "$baseNamePrefix ${index + 1}",
                    description = "",
                    mediaLibraryId = "", // Legacy: empty for old workflow
                    rarity = rarity,
                    requiredLevel = startRequiredLevel + index,
                    categoryId = categoryId
                )
                addedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add collection item ${index + 1} from ZIP", e)
            }
        }

        Log.d(TAG, "Successfully added $addedCount collection items from ZIP")
        return if (addedCount > 0) {
            Result.Success(addedCount)
        } else {
            Result.Error("Failed to add collection items to database")
        }
    }

    sealed class Result {
        data class Success(val itemsAdded: Int) : Result()
        data class Error(val message: String) : Result()
    }
}
