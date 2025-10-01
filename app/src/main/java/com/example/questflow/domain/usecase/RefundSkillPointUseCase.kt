package com.example.questflow.domain.usecase

import com.example.questflow.data.repository.SkillRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import javax.inject.Inject

class RefundSkillPointUseCase @Inject constructor(
    private val skillRepository: SkillRepository,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(nodeId: String, categoryId: Long? = null): RefundSkillResult {
        android.util.Log.d("RefundSkillUC", "Refunding skill $nodeId, categoryId=$categoryId")

        val refundResult = skillRepository.refundSkillPoint(nodeId)

        return if (refundResult.success) {
            // Refund skill point to appropriate source
            if (categoryId != null) {
                val category = categoryRepository.getCategoryById(categoryId)
                if (category == null) {
                    return RefundSkillResult(success = false, message = "Kategorie nicht gefunden")
                }
                categoryRepository.updateCategory(category.copy(skillPoints = category.skillPoints + 1))
                android.util.Log.d("RefundSkillUC", "Refunded to category, new total: ${category.skillPoints + 1}")

                RefundSkillResult(
                    success = true,
                    remainingPoints = category.skillPoints + 1,
                    currentInvestment = refundResult.newInvestment ?: 0
                )
            } else {
                val stats = statsRepository.getOrCreateStats()
                statsRepository.updateStats(stats.copy(points = stats.points + 1))
                android.util.Log.d("RefundSkillUC", "Refunded to global, new total: ${stats.points + 1}")

                RefundSkillResult(
                    success = true,
                    remainingPoints = stats.points + 1,
                    currentInvestment = refundResult.newInvestment ?: 0
                )
            }
        } else {
            RefundSkillResult(success = false, message = refundResult.message ?: "Refund fehlgeschlagen")
        }
    }
}

data class RefundSkillResult(
    val success: Boolean,
    val message: String? = null,
    val remainingPoints: Int? = null,
    val currentInvestment: Int? = null
)
