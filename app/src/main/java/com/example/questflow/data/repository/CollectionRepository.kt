package com.example.questflow.data.repository

import android.util.Log
import com.example.questflow.data.database.dao.CollectionDao
import com.example.questflow.data.database.entity.CollectionItemEntity
import com.example.questflow.data.database.entity.CollectionUnlockEntity
import com.example.questflow.data.manager.FileStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import javax.inject.Inject

data class CollectionItemWithUnlock(
    val item: CollectionItemEntity,
    val isUnlocked: Boolean,
    val unlockedAt: LocalDateTime? = null
)

class CollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao,
    private val fileStorageManager: FileStorageManager
) {
    companion object {
        private const val TAG = "CollectionRepository"
    }

    /**
     * Get all collection items with unlock status
     */
    fun getAllItemsWithUnlocks(): Flow<List<CollectionItemWithUnlock>> {
        return combine(
            collectionDao.getAllCollectionItemsFlow(),
            collectionDao.getAllUnlocksFlow()
        ) { items, unlocks ->
            val unlockMap = unlocks.associateBy { it.collectionItemId }
            items.map { item ->
                val unlock = unlockMap[item.id]
                CollectionItemWithUnlock(
                    item = item,
                    isUnlocked = unlock != null,
                    unlockedAt = unlock?.unlockedAt
                )
            }
        }
    }

    /**
     * Get global collection items with unlock status
     */
    fun getGlobalItemsWithUnlocks(): Flow<List<CollectionItemWithUnlock>> {
        return combine(
            collectionDao.getGlobalCollectionItemsFlow(),
            collectionDao.getAllUnlocksFlow()
        ) { items, unlocks ->
            val unlockMap = unlocks.associateBy { it.collectionItemId }
            items.map { item ->
                val unlock = unlockMap[item.id]
                CollectionItemWithUnlock(
                    item = item,
                    isUnlocked = unlock != null,
                    unlockedAt = unlock?.unlockedAt
                )
            }
        }
    }

    /**
     * Get category-specific collection items with unlock status
     */
    fun getCategoryItemsWithUnlocks(categoryId: Long): Flow<List<CollectionItemWithUnlock>> {
        return combine(
            collectionDao.getCategoryCollectionItemsFlow(categoryId),
            collectionDao.getAllUnlocksFlow()
        ) { items, unlocks ->
            val unlockMap = unlocks.associateBy { it.collectionItemId }
            items.map { item ->
                val unlock = unlockMap[item.id]
                CollectionItemWithUnlock(
                    item = item,
                    isUnlocked = unlock != null,
                    unlockedAt = unlock?.unlockedAt
                )
            }
        }
    }

    /**
     * Get global + category-specific collection items with unlock status
     * Shows global items AND items specific to the given category
     */
    fun getGlobalAndCategoryItemsWithUnlocks(categoryId: Long): Flow<List<CollectionItemWithUnlock>> {
        return combine(
            collectionDao.getAllCollectionItemsFlow(),
            collectionDao.getAllUnlocksFlow()
        ) { items, unlocks ->
            val unlockMap = unlocks.associateBy { it.collectionItemId }

            // Filter: global items (categoryId == null) OR items matching the category
            items.filter { it.categoryId == null || it.categoryId == categoryId }
                .map { item ->
                    val unlock = unlockMap[item.id]
                    CollectionItemWithUnlock(
                        item = item,
                        isUnlocked = unlock != null,
                        unlockedAt = unlock?.unlockedAt
                    )
                }
        }
    }

    /**
     * Add a new collection item
     */
    suspend fun addCollectionItem(
        name: String,
        description: String,
        imageUri: String,
        rarity: String,
        requiredLevel: Int,
        categoryId: Long?
    ): Long {
        Log.d(TAG, "Adding collection item: $name (category: $categoryId)")

        val item = CollectionItemEntity(
            name = name,
            description = description,
            imageUri = imageUri,
            rarity = rarity,
            requiredLevel = requiredLevel,
            categoryId = categoryId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val id = collectionDao.insertCollectionItem(item)
        Log.d(TAG, "Collection item added with ID: $id")
        return id
    }

    /**
     * Update an existing collection item
     */
    suspend fun updateCollectionItem(item: CollectionItemEntity) {
        Log.d(TAG, "Updating collection item: ${item.id}")
        collectionDao.updateCollectionItem(item.copy(updatedAt = LocalDateTime.now()))
    }

    /**
     * Delete a collection item and its associated image
     */
    suspend fun deleteCollectionItem(item: CollectionItemEntity) {
        Log.d(TAG, "Deleting collection item: ${item.id}")

        // Delete the image file
        fileStorageManager.deleteImage(item.imageUri)

        // Delete from database (cascade will remove unlock)
        collectionDao.deleteCollectionItem(item)
    }

    /**
     * Unlock the next available collection item
     */
    suspend fun unlockNextItem(levelAtUnlock: Int, categoryId: Long?): CollectionItemEntity? {
        val nextItem = collectionDao.getNextLockedItem(categoryId)

        if (nextItem != null) {
            Log.d(TAG, "Unlocking collection item: ${nextItem.id} at level $levelAtUnlock")

            collectionDao.insertUnlock(
                CollectionUnlockEntity(
                    collectionItemId = nextItem.id,
                    levelAtUnlock = levelAtUnlock,
                    unlockedAt = LocalDateTime.now()
                )
            )
        } else {
            Log.d(TAG, "No more locked items to unlock for category: $categoryId")
        }

        return nextItem
    }

    /**
     * Check if an item is unlocked
     */
    suspend fun isItemUnlocked(itemId: Long): Boolean {
        return collectionDao.getUnlockForItem(itemId) != null
    }

    /**
     * Get unlock count
     */
    suspend fun getUnlockedCount(): Int {
        return collectionDao.getUnlockedCount()
    }

    /**
     * Get global unlock count
     */
    suspend fun getGlobalUnlockedCount(): Int {
        return collectionDao.getGlobalUnlockedCount()
    }

    /**
     * Get category unlock count
     */
    suspend fun getCategoryUnlockedCount(categoryId: Long): Int {
        return collectionDao.getCategoryUnlockedCount(categoryId)
    }

    /**
     * Get collection item by ID
     */
    suspend fun getItemById(id: Long): CollectionItemEntity? {
        return collectionDao.getCollectionItemById(id)
    }
}
