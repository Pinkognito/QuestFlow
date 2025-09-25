package com.example.questflow.domain.usecase

import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.database.entity.SkillType
import com.example.questflow.data.repository.*
import javax.inject.Inject

class GrantXpUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val xpTransactionRepository: XpTransactionRepository,
    private val skillRepository: SkillRepository,
    private val memeRepository: MemeRepository
) {
    suspend operator fun invoke(
        amountRaw: Int,
        source: XpSource,
        referenceId: Long? = null
    ): GrantXpResult {
        // Calculate multiplied amount
        val multiplier = skillRepository.getActiveXpMultiplier()
        val amount = (amountRaw * multiplier).toInt().coerceAtLeast(1)

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
        if (gainedLevels > 0) {
            repeat(gainedLevels) {
                val meme = memeRepository.unlockNextMeme(newLevel)
                meme?.let { unlockedMemes.add(it.name) }
            }

            // Extra meme if skill is unlocked
            if (skillRepository.hasUnlockedPerk(SkillType.EXTRA_MEME)) {
                val extraMeme = memeRepository.unlockNextMeme(newLevel)
                extraMeme?.let { unlockedMemes.add("${it.name} (Bonus!)") }
            }
        }

        return GrantXpResult(
            xpGranted = amount,
            totalXp = newXp,
            previousLevel = beforeLevel,
            newLevel = newLevel,
            levelsGained = gainedLevels,
            skillPointsGained = gainedLevels,
            unlockedMemes = unlockedMemes
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
    val unlockedMemes: List<String>
)