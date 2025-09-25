package com.example.questflow.domain.usecase

import com.example.questflow.data.repository.SkillRepository
import com.example.questflow.data.repository.StatsRepository
import javax.inject.Inject

class UnlockSkillNodeUseCase @Inject constructor(
    private val skillRepository: SkillRepository,
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(nodeId: String): UnlockSkillResult {
        val stats = statsRepository.getOrCreateStats()

        if (stats.points < 1) {
            return UnlockSkillResult(success = false, message = "Not enough skill points")
        }

        val unlocked = skillRepository.unlockNode(nodeId)

        return if (unlocked) {
            // Deduct skill point
            statsRepository.updateStats(stats.copy(points = stats.points - 1))
            UnlockSkillResult(success = true, remainingPoints = stats.points - 1)
        } else {
            UnlockSkillResult(success = false, message = "Prerequisites not met")
        }
    }
}

data class UnlockSkillResult(
    val success: Boolean,
    val message: String? = null,
    val remainingPoints: Int? = null
)