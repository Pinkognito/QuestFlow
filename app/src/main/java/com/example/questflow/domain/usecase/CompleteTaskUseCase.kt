package com.example.questflow.domain.usecase

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

        // TODO: Recurring tasks feature
        // UI and data structures are ready (RecurringConfigDialog, entity fields)
        // Backend processing is disabled pending full implementation
        // When enabled, uncomment below to create next recurring instance:
        /*
        if (task.isRecurring) {
            processRecurringTasksUseCase(taskId)
        }
        */

        // Handle subtask completion logic
        handleSubtaskCompletion(task)

        return CompleteTaskResult(
            success = true,
            xpGranted = xpResult?.xpGranted ?: categoryXpResult?.xpGranted,
            leveledUp = (xpResult?.levelsGained ?: categoryXpResult?.levelsGained ?: 0) > 0,
            newLevel = xpResult?.newLevel ?: categoryXpResult?.newLevel,
            unlockedMemes = xpResult?.unlockedMemes ?: emptyList(),
            categoryName = categoryName
        )
    }

    private suspend fun handleSubtaskCompletion(completedTask: com.example.questflow.domain.model.Task) {
        // Case 1: If completed task has subtasks → complete all subtasks
        val subtasks = taskRepository.getSubtasksSync(completedTask.id)
        if (subtasks.isNotEmpty()) {
            subtasks.filter { !it.isCompleted }.forEach { subtask ->
                val updatedSubtask = subtask.copy(
                    isCompleted = true,
                    completedAt = LocalDateTime.now()
                )
                taskRepository.updateTaskEntity(updatedSubtask)
            }
        }

        // Case 2: If completed task is a subtask → check if parent should be auto-completed
        completedTask.parentTaskId?.let { parentId ->
            val parentTask = taskRepository.getTaskById(parentId)
            if (parentTask != null && !parentTask.isCompleted && parentTask.autoCompleteParent) {
                // Check if all subtasks are now completed
                val incompleteCount = taskRepository.getIncompleteSubtaskCount(parentId)
                if (incompleteCount == 0) {
                    // All subtasks completed → complete parent
                    val updatedParent = parentTask.copy(
                        isCompleted = true,
                        completedAt = LocalDateTime.now()
                    )
                    taskRepository.updateTask(updatedParent)

                    // Grant XP for parent task
                    val (parentLevel, _) = if (parentTask.categoryId != null) {
                        val category = categoryRepository.getCategoryById(parentTask.categoryId)
                            ?: categoryRepository.getOrCreateDefaultCategory()
                        category.currentLevel to category.name
                    } else {
                        val currentStats = statsRepository.getOrCreateStats()
                        currentStats.level to null
                    }

                    val parentXp = calculateXpRewardUseCase(parentTask.xpPercentage, parentLevel)

                    if (parentTask.categoryId != null) {
                        grantCategoryXpUseCase(
                            categoryId = parentTask.categoryId,
                            baseXpAmount = parentXp,
                            source = "TASK",
                            sourceId = parentId
                        )
                    } else {
                        grantXpUseCase(parentXp, XpSource.TASK, parentId)
                    }
                }
            }
        }
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