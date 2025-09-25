package com.example.questflow.domain.usecase

import com.example.questflow.data.database.TaskDifficulty
import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.domain.usecase.category.GrantCategoryXpUseCase
import java.time.LocalDateTime
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val grantXpUseCase: GrantXpUseCase,
    private val grantCategoryXpUseCase: GrantCategoryXpUseCase,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase
) {
    suspend operator fun invoke(taskId: Long): CompleteTaskResult {
        val task = taskRepository.getTaskById(taskId)
            ?: return CompleteTaskResult(success = false, message = "Task not found")

        if (task.isCompleted) {
            return CompleteTaskResult(success = false, message = "Task already completed")
        }

        // Determine which level to use for XP calculation
        val (currentLevel, categoryName) = if (task.categoryId != null) {
            val category = categoryRepository.getCategoryById(task.categoryId)
                ?: categoryRepository.getOrCreateDefaultCategory()
            category.currentLevel to category.name
        } else {
            val currentStats = statsRepository.getOrCreateStats()
            currentStats.level to null
        }

        // Calculate XP amount based on percentage and current level
        val xpAmount = calculateXpRewardUseCase(task.xpPercentage, currentLevel)

        // Update task completion
        val updatedTask = task.copy(
            isCompleted = true,
            completedAt = LocalDateTime.now()
        )
        taskRepository.updateTask(updatedTask)

        // Grant XP to category or general stats
        val (xpResult, categoryXpResult) = if (task.categoryId != null) {
            // Grant to category
            val catResult = grantCategoryXpUseCase(
                categoryId = task.categoryId,
                baseXpAmount = xpAmount,
                source = "TASK",
                sourceId = taskId
            )
            null to catResult
        } else {
            // Grant to general stats
            val genResult = grantXpUseCase(xpAmount, XpSource.TASK, taskId)
            genResult to null
        }

        return CompleteTaskResult(
            success = true,
            xpGranted = xpResult?.xpGranted ?: categoryXpResult?.xpGranted,
            leveledUp = (xpResult?.levelsGained ?: categoryXpResult?.levelsGained ?: 0) > 0,
            newLevel = xpResult?.newLevel ?: categoryXpResult?.newLevel,
            unlockedMemes = xpResult?.unlockedMemes ?: emptyList(),
            categoryName = categoryName
        )
    }
}

data class CompleteTaskResult(
    val success: Boolean,
    val message: String? = null,
    val xpGranted: Int? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int? = null,
    val unlockedMemes: List<String> = emptyList(),
    val categoryName: String? = null
)