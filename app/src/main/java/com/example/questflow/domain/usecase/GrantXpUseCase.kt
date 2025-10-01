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
        // Calculate all applicable multipliers
        var totalMultiplier = 1.0f

        // Base XP multiplier from skills
        val xpMultBonus = skillRepository.calculateTotalEffect(com.example.questflow.data.database.entity.SkillEffectType.XP_MULTIPLIER)
        totalMultiplier += xpMultBonus / 100f

        // Source-specific bonuses
        when (source) {
            XpSource.TASK, XpSource.SUBTASK -> {
                val taskBonus = skillRepository.calculateTotalEffect(com.example.questflow.data.database.entity.SkillEffectType.TASK_XP_BONUS)
                totalMultiplier += taskBonus / 100f
            }
            XpSource.CALENDAR -> {
                val calendarBonus = skillRepository.calculateTotalEffect(com.example.questflow.data.database.entity.SkillEffectType.CALENDAR_XP_BONUS)
                totalMultiplier += calendarBonus / 100f
            }
            else -> {}
        }

        val amount = (amountRaw * totalMultiplier).toInt().coerceAtLeast(1)

        android.util.Log.d("GrantXpUseCase", "Raw XP: $amountRaw, Total Multiplier: $totalMultiplier, Final XP: $amount")

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

        // Calculate skill points gained (base + bonus from skills)
        val baseSkillPoints = gainedLevels
        val skillPointBonus = skillRepository.calculateTotalEffect(com.example.questflow.data.database.entity.SkillEffectType.SKILL_POINT_GAIN).toInt()
        val totalSkillPointsGained = baseSkillPoints + skillPointBonus

        android.util.Log.d("GrantXpUseCase", "Skill Points: base=$baseSkillPoints, bonus=$skillPointBonus, total=$totalSkillPointsGained")

        val newPoints = currentStats.points + totalSkillPointsGained

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

            // Extra meme/collection if EXTRA_COLLECTION_UNLOCK skill is active
            if (skillRepository.hasEffectActive(com.example.questflow.data.database.entity.SkillEffectType.EXTRA_COLLECTION_UNLOCK)) {
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
            skillPointsGained = totalSkillPointsGained,
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