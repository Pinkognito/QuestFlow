package com.example.questflow.domain.usecase

import com.example.questflow.data.repository.InvestmentResult
import com.example.questflow.data.repository.SkillRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import javax.inject.Inject

class InvestSkillPointUseCase @Inject constructor(
    private val skillRepository: SkillRepository,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(nodeId: String, categoryId: Long? = null): InvestSkillResult {
        android.util.Log.d("InvestSkillUC", "Investing in skill $nodeId, categoryId=$categoryId")

        // Check skill points based on context
        val availablePoints: Int
        if (categoryId != null) {
            // Category-specific skill points
            val category = categoryRepository.getCategoryById(categoryId)
            if (category == null) {
                return InvestSkillResult(success = false, message = "Kategorie nicht gefunden")
            }
            availablePoints = category.skillPoints
            android.util.Log.d("InvestSkillUC", "Category SP: $availablePoints")
        } else {
            // Global skill points
            val stats = statsRepository.getOrCreateStats()
            availablePoints = stats.points
            android.util.Log.d("InvestSkillUC", "Global SP: $availablePoints")
        }

        if (availablePoints < 1) {
            return InvestSkillResult(success = false, message = "Nicht genug Skillpunkte (verfügbar: $availablePoints)")
        }

        val investResult = skillRepository.investSkillPoint(nodeId)

        return if (investResult.success) {
            // Deduct skill point from appropriate source
            if (categoryId != null) {
                val category = categoryRepository.getCategoryById(categoryId)!!
                categoryRepository.updateCategory(category.copy(skillPoints = category.skillPoints - 1))
                android.util.Log.d("InvestSkillUC", "Deducted from category, remaining: ${category.skillPoints - 1}")
            } else {
                val stats = statsRepository.getOrCreateStats()
                statsRepository.updateStats(stats.copy(points = stats.points - 1))
                android.util.Log.d("InvestSkillUC", "Deducted from global, remaining: ${stats.points - 1}")
            }

            InvestSkillResult(
                success = true,
                remainingPoints = availablePoints - 1,
                currentInvestment = investResult.newInvestment ?: 0,
                maxInvestment = investResult.maxInvestment ?: 0
            )
        } else {
            InvestSkillResult(success = false, message = investResult.message ?: "Unbekannter Fehler")
        }
    }
}

data class InvestSkillResult(
    val success: Boolean,
    val message: String? = null,
    val remainingPoints: Int? = null,
    val currentInvestment: Int? = null,
    val maxInvestment: Int? = null
)
