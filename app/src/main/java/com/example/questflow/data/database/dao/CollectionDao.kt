package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.CollectionItemEntity
import com.example.questflow.data.database.entity.CollectionUnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionItem(item: CollectionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionItems(items: List<CollectionItemEntity>)

    @Update
    suspend fun updateCollectionItem(item: CollectionItemEntity)

    @Delete
    suspend fun deleteCollectionItem(item: CollectionItemEntity)

    @Query("SELECT * FROM collection_items WHERE categoryId IS NULL ORDER BY requiredLevel, id")
    fun getGlobalCollectionItemsFlow(): Flow<List<CollectionItemEntity>>

    @Query("SELECT * FROM collection_items WHERE categoryId = :categoryId ORDER BY requiredLevel, id")
    fun getCategoryCollectionItemsFlow(categoryId: Long): Flow<List<CollectionItemEntity>>

    @Query("SELECT * FROM collection_items WHERE id = :id")
    suspend fun getCollectionItemById(id: Long): CollectionItemEntity?

    @Query("SELECT * FROM collection_items ORDER BY requiredLevel, id")
    fun getAllCollectionItemsFlow(): Flow<List<CollectionItemEntity>>

    @Query("SELECT * FROM collection_items ORDER BY requiredLevel, id")
    suspend fun getAllCollectionItems(): List<CollectionItemEntity>

    @Query("SELECT ci.* FROM collection_items ci LEFT JOIN collection_unlocks cu ON ci.id = cu.collectionItemId WHERE cu.id IS NULL AND (ci.categoryId IS NULL OR ci.categoryId = :categoryId) ORDER BY ci.requiredLevel LIMIT 1")
    suspend fun getNextLockedItem(categoryId: Long?): CollectionItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnlock(unlock: CollectionUnlockEntity)

    @Query("SELECT * FROM collection_unlocks ORDER BY unlockedAt DESC")
    fun getAllUnlocksFlow(): Flow<List<CollectionUnlockEntity>>

    @Query("SELECT * FROM collection_unlocks WHERE collectionItemId = :itemId")
    suspend fun getUnlockForItem(itemId: Long): CollectionUnlockEntity?

    @Query("SELECT COUNT(*) FROM collection_unlocks")
    suspend fun getUnlockedCount(): Int

    @Query("SELECT COUNT(*) FROM collection_unlocks cu INNER JOIN collection_items ci ON cu.collectionItemId = ci.id WHERE ci.categoryId IS NULL")
    suspend fun getGlobalUnlockedCount(): Int

    @Query("SELECT COUNT(*) FROM collection_unlocks cu INNER JOIN collection_items ci ON cu.collectionItemId = ci.id WHERE ci.categoryId = :categoryId")
    suspend fun getCategoryUnlockedCount(categoryId: Long): Int
}
