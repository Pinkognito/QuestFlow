package com.example.questflow.domain.usecase

import com.example.questflow.data.database.TaskDifficulty
import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import java.time.LocalDateTime
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val statsRepository: StatsRepository,
    private val grantXpUseCase: GrantXpUseCase,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase
) {
    suspend operator fun invoke(taskId: Long): CompleteTaskResult {
        val task = taskRepository.getTaskById(taskId)
            ?: return CompleteTaskResult(success = false, message = "Task not found")

        if (task.isCompleted) {
            return CompleteTaskResult(success = false, message = "Task already completed")
        }

        // Get current level for XP calculation
        val currentStats = statsRepository.getOrCreateStats()
        val currentLevel = currentStats.level

        // Calculate XP amount based on percentage and current level
        val xpAmount = calculateXpRewardUseCase(task.xpPercentage, currentLevel)

        // Update task completion
        val updatedTask = task.copy(
            isCompleted = true,
            completedAt = LocalDateTime.now()
        )
        taskRepository.updateTask(updatedTask)

        // Grant XP
        val xpResult = grantXpUseCase(xpAmount, XpSource.TASK, taskId)

        return CompleteTaskResult(
            success = true,
            xpGranted = xpResult.xpGranted,
            leveledUp = xpResult.levelsGained > 0,
            newLevel = xpResult.newLevel,
            unlockedMemes = xpResult.unlockedMemes
        )
    }
}

data class CompleteTaskResult(
    val success: Boolean,
    val message: String? = null,
    val xpGranted: Int? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int? = null,
    val unlockedMemes: List<String> = emptyList()
)