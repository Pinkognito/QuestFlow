package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.database.entity.CategoryXpTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun observeCategoryById(categoryId: Long): Flow<CategoryEntity?>

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("UPDATE categories SET currentXp = :xp, currentLevel = :level, totalXp = :totalXp, skillPoints = :skillPoints WHERE id = :categoryId")
    suspend fun updateCategoryStats(categoryId: Long, xp: Int, level: Int, totalXp: Int, skillPoints: Int)

    @Insert
    suspend fun insertXpTransaction(transaction: CategoryXpTransactionEntity)

    @Query("SELECT * FROM category_xp_transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getCategoryXpTransactions(categoryId: Long): Flow<List<CategoryXpTransactionEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("SELECT * FROM categories LIMIT 1")
    suspend fun getDefaultCategory(): CategoryEntity?
}