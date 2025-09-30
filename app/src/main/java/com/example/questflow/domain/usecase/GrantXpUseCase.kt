package com.example.questflow.domain.usecase

import android.content.Context
import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.database.entity.SkillType
import com.example.questflow.data.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GrantXpUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val xpTransactionRepository: XpTransactionRepository,
    private val skillRepository: SkillRepository,
    private val memeRepository: MemeRepository,
    private val collectionRepository: CollectionRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        amountRaw: Int,
        source: XpSource,
        referenceId: Long? = null
    ): GrantXpResult {
        // Calculate multiplied amount
        val multiplier = skillRepository.getActiveXpMultiplier()
        val amount = (amountRaw * multiplier).toInt().coerceAtLeast(1)

        // Debug logging
        android.util.Log.d("GrantXpUseCase", "Raw XP: $amountRaw, Multiplier: $multiplier, Final XP: $amount")

        // Record transaction
        xpTransactionRepository.recordTransaction(source, amount, referenceId)

        // Update stats
        val currentStats = statsRepository.getOrCreateStats()
        val beforeLevel = currentStats.level
        val newXp = currentStats.xp + amount

        // Calculate new level
        var newLevel = beforeLevel
        while (newXp >= LevelCurve.requiredXp(newLevel + 1)) {
            newLevel++
        }

        val gainedLevels = newLevel - beforeLevel
        val newPoints = currentStats.points + gainedLevels

        // Update stats
        statsRepository.updateStats(
            currentStats.copy(
                xp = newXp,
                level = newLevel,
                points = newPoints
            )
        )

        // Handle level-up rewards
        val unlockedMemes = mutableListOf<String>()
        val unlockedCollectionItems = mutableListOf<String>()

        if (gainedLevels > 0) {
            android.util.Log.d("GrantXpUseCase", "Level up! Gained $gainedLevels levels. Unlocking rewards...")

            // Get selected category from SharedPreferences
            val prefs = context.getSharedPreferences("quest_flow_prefs", Context.MODE_PRIVATE)
            val selectedCategoryId = if (prefs.contains("selected_category_id")) {
                prefs.getLong("selected_category_id", -1L).takeIf { it != -1L }
            } else {
                null
            }

            android.util.Log.d("GrantXpUseCase", "Selected category for unlock: $selectedCategoryId")

            repeat(gainedLevels) {
                // Legacy meme unlock (kept for backward compatibility)
                val meme = memeRepository.unlockNextMeme(newLevel)
                meme?.let { unlockedMemes.add(it.name) }

                // New collection item unlock (uses selected category)
                val collectionItem = collectionRepository.unlockNextItem(newLevel, categoryId = selectedCategoryId)
                collectionItem?.let {
                    unlockedCollectionItems.add(it.name)
                    android.util.Log.d("GrantXpUseCase", "Unlocked collection item: ${it.name} (category: $selectedCategoryId)")
                }
            }

            // Extra meme if skill is unlocked
            if (skillRepository.hasUnlockedPerk(SkillType.EXTRA_MEME)) {
                val extraMeme = memeRepository.unlockNextMeme(newLevel)
                extraMeme?.let { unlockedMemes.add("${it.name} (Bonus!)") }

                // Extra collection item
                val extraCollectionItem = collectionRepository.unlockNextItem(newLevel, categoryId = selectedCategoryId)
                extraCollectionItem?.let {
                    unlockedCollectionItems.add("${it.name} (Bonus!)")
                    android.util.Log.d("GrantXpUseCase", "Unlocked bonus collection item: ${it.name} (category: $selectedCategoryId)")
                }
            }
        }

        return GrantXpResult(
            xpGranted = amount,
            totalXp = newXp,
            previousLevel = beforeLevel,
            newLevel = newLevel,
            levelsGained = gainedLevels,
            skillPointsGained = gainedLevels,
            unlockedMemes = unlockedMemes,
            unlockedCollectionItems = unlockedCollectionItems
        )
    }
}

data class GrantXpResult(
    val xpGranted: Int,
    val totalXp: Long,
    val previousLevel: Int,
    val newLevel: Int,
    val levelsGained: Int,
    val skillPointsGained: Int,
    val unlockedMemes: List<String>,
    val unlockedCollectionItems: List<String> = emptyList()
)