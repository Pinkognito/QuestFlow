package com.example.questflow.domain.usecase

import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

class CalculateXpRewardUseCase @Inject constructor() {

    /**
     * Calculate XP required for next level
     * Formula: level² × 100
     */
    fun getXpRequiredForLevel(level: Int): Int {
        return level * level * 100
    }

    /**
     * Calculate XP required to reach the next level from current level
     */
    fun getXpRequiredForNextLevel(currentLevel: Int): Int {
        val currentLevelTotal = currentLevel * currentLevel * 100
        val nextLevelTotal = (currentLevel + 1) * (currentLevel + 1) * 100
        return nextLevelTotal - currentLevelTotal
    }

    /**
     * Calculate XP reward based on percentage of level requirement
     *
     * @param percentage The percentage of level XP requirement (20, 40, 60, 80, 100)
     * @param currentLevel The player's current level
     * @return The actual XP amount to award
     */
    operator fun invoke(percentage: Int, currentLevel: Int): Int {
        val xpRequiredForNext = getXpRequiredForNextLevel(currentLevel)
        val xpReward = (xpRequiredForNext * percentage / 100.0).roundToInt()

        // Round to nearest 5 for clean numbers
        return ((xpReward + 2) / 5) * 5
    }

    /**
     * Calculate XP reward from difficulty string (for backward compatibility)
     */
    fun fromDifficulty(difficulty: String, currentLevel: Int): Int {
        val percentage = when (difficulty) {
            "TRIVIAL" -> 20
            "EASY" -> 40
            "MEDIUM" -> 60
            "HARD" -> 80
            "EPIC" -> 100
            else -> 60 // Default to MEDIUM
        }
        return invoke(percentage, currentLevel)
    }

    /**
     * Get display text for XP reward at a given level
     */
    fun getDisplayText(percentage: Int, currentLevel: Int): String {
        val xp = invoke(percentage, currentLevel)
        return "$xp XP"
    }
}