package com.example.questflow.domain.usecase

object LevelCurve {
    fun requiredXp(level: Int): Long {
        return (level.toLong() * level) * 100L
    }

    fun getLevelFromXp(xp: Long): Int {
        var level = 1
        while (xp >= requiredXp(level + 1)) {
            level++
        }
        return level
    }

    fun getProgressToNextLevel(xp: Long, currentLevel: Int): Float {
        val currentLevelXp = requiredXp(currentLevel)
        val nextLevelXp = requiredXp(currentLevel + 1)
        val progress = xp - currentLevelXp
        val total = nextLevelXp - currentLevelXp
        return (progress.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}