package com.example.questflow.domain.usecase.category

import android.util.Log
import com.example.questflow.data.database.entity.CategoryXpTransactionEntity
import com.example.questflow.data.repository.CategoryRepository
import javax.inject.Inject
import kotlin.math.pow

class GrantCategoryXpUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val collectionRepository: com.example.questflow.data.repository.CollectionRepository
) {
    suspend operator fun invoke(
        categoryId: Long,
        baseXpAmount: Int,
        source: String,
        sourceId: Long? = null,
        multiplier: Float = 1.0f
    ): XpGrantResult {
        val category = categoryRepository.getCategoryById(categoryId)
            ?: throw IllegalArgumentException("Category not found")

        // Apply level scaling factor
        val scaledXp = (baseXpAmount * multiplier / category.levelScalingFactor).toInt()

        val previousLevel = category.currentLevel
        val previousTotalXp = category.totalXp
        val newTotalXp = previousTotalXp + scaledXp

        // Calculate new level based on total XP
        var newLevel = 1
        var totalXpRequired = 0
        while (totalXpRequired <= newTotalXp) {
            newLevel++
            totalXpRequired = calculateTotalXpForLevel(newLevel)
        }
        newLevel-- // Step back one level since we went over

        // Calculate current XP within the level
        val xpForCurrentLevel = calculateTotalXpForLevel(newLevel)
        val xpForPreviousLevel = if (newLevel > 1) calculateTotalXpForLevel(newLevel - 1) else 0
        val currentLevelXp = newTotalXp - xpForPreviousLevel
        val xpNeededForCurrentLevel = xpForCurrentLevel - xpForPreviousLevel

        // Calculate skill points gained
        val levelsGained = newLevel - previousLevel
        val newSkillPoints = category.skillPoints + levelsGained

        // Update category stats
        categoryRepository.updateCategoryStats(
            categoryId = categoryId,
            xp = currentLevelXp,
            level = newLevel,
            totalXp = newTotalXp,
            skillPoints = newSkillPoints
        )

        // Record transaction
        val transaction = CategoryXpTransactionEntity(
            categoryId = categoryId,
            xpAmount = scaledXp,
            source = source,
            sourceId = sourceId,
            multiplier = multiplier,
            previousLevel = previousLevel,
            newLevel = newLevel,
            previousTotalXp = previousTotalXp,
            newTotalXp = newTotalXp
        )
        categoryRepository.recordXpTransaction(transaction)

        Log.d("GrantCategoryXpUseCase",
            "Category ${category.name}: Granted $scaledXp XP (base: $baseXpAmount, multiplier: $multiplier, scaling: ${category.levelScalingFactor})")
        Log.d("GrantCategoryXpUseCase",
            "Level progress: $previousLevel → $newLevel, Total XP: $previousTotalXp → $newTotalXp")

        // Handle level-up rewards: unlock collection items
        val unlockedCollectionItems = mutableListOf<String>()
        if (levelsGained > 0) {
            Log.d("GrantCategoryXpUseCase", "Category level up! Gained $levelsGained levels. Unlocking collection items...")

            repeat(levelsGained) {
                // Unlock collection item for this category
                val collectionItem = collectionRepository.unlockNextItem(newLevel, categoryId = categoryId)
                collectionItem?.let {
                    unlockedCollectionItems.add(it.name)
                    Log.d("GrantCategoryXpUseCase", "Unlocked collection item: ${it.name} (category: $categoryId)")
                }
            }
        }

        return XpGrantResult(
            categoryId = categoryId,
            categoryName = category.name,
            xpGranted = scaledXp,
            previousLevel = previousLevel,
            newLevel = newLevel,
            levelsGained = levelsGained,
            previousTotalXp = previousTotalXp,
            newTotalXp = newTotalXp,
            currentLevelXp = currentLevelXp,
            xpNeededForCurrentLevel = xpNeededForCurrentLevel,
            skillPointsGained = levelsGained,
            unlockedCollectionItems = unlockedCollectionItems
        )
    }

    private fun calculateTotalXpForLevel(level: Int): Int {
        return (level * level * 100.0).toInt()
    }

    data class XpGrantResult(
        val categoryId: Long,
        val categoryName: String,
        val xpGranted: Int,
        val previousLevel: Int,
        val newLevel: Int,
        val levelsGained: Int,
        val previousTotalXp: Int,
        val newTotalXp: Int,
        val currentLevelXp: Int,
        val xpNeededForCurrentLevel: Int,
        val skillPointsGained: Int,
        val unlockedCollectionItems: List<String> = emptyList()
    )
}