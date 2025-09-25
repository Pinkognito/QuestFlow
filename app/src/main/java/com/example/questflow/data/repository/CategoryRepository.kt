package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.CategoryDao
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.database.entity.CategoryXpTransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getActiveCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getActiveCategories()

    fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    fun observeCategoryById(categoryId: Long): Flow<CategoryEntity?> =
        categoryDao.observeCategoryById(categoryId)

    suspend fun getCategoryById(categoryId: Long): CategoryEntity? =
        categoryDao.getCategoryById(categoryId)

    suspend fun createCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    suspend fun updateCategoryStats(
        categoryId: Long,
        xp: Int,
        level: Int,
        totalXp: Int,
        skillPoints: Int
    ) = categoryDao.updateCategoryStats(categoryId, xp, level, totalXp, skillPoints)

    suspend fun recordXpTransaction(transaction: CategoryXpTransactionEntity) =
        categoryDao.insertXpTransaction(transaction)

    fun getCategoryXpTransactions(categoryId: Long): Flow<List<CategoryXpTransactionEntity>> =
        categoryDao.getCategoryXpTransactions(categoryId)

    suspend fun getCategoryCount(): Int =
        categoryDao.getCategoryCount()

    suspend fun getOrCreateDefaultCategory(): CategoryEntity {
        val defaultCategory = categoryDao.getDefaultCategory()
        if (defaultCategory != null) {
            return defaultCategory
        }

        // Create default category if none exists
        val newCategory = CategoryEntity(
            name = "Allgemein",
            description = "Standard-Kategorie für allgemeine Aufgaben",
            color = "#2196F3",
            emoji = "🎯"
        )
        val categoryId = categoryDao.insertCategory(newCategory)
        return newCategory.copy(id = categoryId)
    }
}