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
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase,
    private val calendarLinkRepository: com.example.questflow.data.repository.CalendarLinkRepository,
    private val calendarManager: com.example.questflow.data.calendar.CalendarManager
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
        android.util.Log.d("CompleteTaskUseCase", "=== TASK COMPLETION ===")
        android.util.Log.d("CompleteTaskUseCase", "Task ID: ${task.id}, Title: ${task.title}")
        android.util.Log.d("CompleteTaskUseCase", "BEFORE completion - parentTaskId: ${task.parentTaskId}")

        val updatedTask = task.copy(
            isCompleted = true,
            completedAt = LocalDateTime.now()
        )
        android.util.Log.d("CompleteTaskUseCase", "AFTER copy() - parentTaskId: ${updatedTask.parentTaskId}")

        taskRepository.updateTask(updatedTask)

        // Verify after database update
        val verifyTask = taskRepository.getTaskById(task.id)
        android.util.Log.d("CompleteTaskUseCase", "AFTER DB update - parentTaskId: ${verifyTask?.parentTaskId}")
        android.util.Log.d("CompleteTaskUseCase", "Parent relationship preserved: ${verifyTask?.parentTaskId == task.parentTaskId}")

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
            categoryName = categoryName
        )
    }

    private suspend fun handleSubtaskCompletion(completedTask: com.example.questflow.domain.model.Task) {
        android.util.Log.d("CompleteTaskUseCase", "handleSubtaskCompletion START - Task: ${completedTask.title} (ID: ${completedTask.id})")

        // Case 1: If completed task has subtasks → complete all subtasks
        val subtasks = taskRepository.getSubtasksSync(completedTask.id)
        if (subtasks.isNotEmpty()) {
            android.util.Log.d("CompleteTaskUseCase", "  Task is a PARENT - has ${subtasks.size} subtasks")
            subtasks.filter { !it.isCompleted }.forEach { subtask ->
                android.util.Log.d("CompleteTaskUseCase", "    Completing subtask: ${subtask.title} (ID: ${subtask.id})")
                val updatedSubtask = subtask.copy(
                    isCompleted = true,
                    completedAt = LocalDateTime.now()
                )
                taskRepository.updateTaskEntity(updatedSubtask)
            }
        }

        // Case 2: If completed task is a subtask → check if parent should be auto-completed
        val parentId = completedTask.parentTaskId
        if (parentId != null) {
            android.util.Log.d("CompleteTaskUseCase", "  Task is a SUBTASK - Parent ID: $parentId")
            android.util.Log.d("CompleteTaskUseCase", "  This subtask has autoCompleteParent: ${completedTask.autoCompleteParent}")

            val parentTask = taskRepository.getTaskById(parentId)
            if (parentTask != null) {
                android.util.Log.d("CompleteTaskUseCase", "    Parent task found: ${parentTask.title} (ID: ${parentTask.id})")
                android.util.Log.d("CompleteTaskUseCase", "    Parent completed: ${parentTask.isCompleted}")

                // FIXED: Check autoCompleteParent on the SUBTASK (completedTask), not the parent!
                // The flag is stored on each subtask to indicate if it should trigger parent completion
                val shouldAutoComplete = !parentTask.isCompleted && completedTask.autoCompleteParent

                if (shouldAutoComplete) {
                    // Check if all subtasks are now completed
                    val incompleteCount = taskRepository.getIncompleteSubtaskCount(parentId)
                    android.util.Log.d("CompleteTaskUseCase", "    Incomplete subtask count: $incompleteCount")

                    val allSubtasksComplete = (incompleteCount == 0)

                    if (allSubtasksComplete) {
                        android.util.Log.d("CompleteTaskUseCase", "    ALL SUBTASKS COMPLETED → Auto-completing parent!")

                        // All subtasks completed → complete parent
                        val updatedParent = parentTask.copy(
                            isCompleted = true,
                            completedAt = LocalDateTime.now()
                        )
                        taskRepository.updateTask(updatedParent)

                        // Grant XP for parent task
                        val parentLevel: Int
                        val parentCategoryName: String?

                        if (parentTask.categoryId != null) {
                            val category = categoryRepository.getCategoryById(parentTask.categoryId)
                                ?: categoryRepository.getOrCreateDefaultCategory()
                            parentLevel = category.currentLevel
                            parentCategoryName = category.name
                        } else {
                            val currentStats = statsRepository.getOrCreateStats()
                            parentLevel = currentStats.level
                            parentCategoryName = null
                        }

                        val parentXp = calculateXpRewardUseCase(parentTask.xpPercentage, parentLevel)
                        android.util.Log.d("CompleteTaskUseCase", "    Parent XP granted: $parentXp")

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

                        // CRITICAL FIX: Also claim the parent's calendar link if it exists
                        if (parentTask.calendarEventId != null) {
                            val calendarEventId = parentTask.calendarEventId
                            android.util.Log.d("CompleteTaskUseCase", "    Parent has calendar event ID: $calendarEventId")

                            val calendarLink = calendarLinkRepository.getLinkByCalendarEventId(calendarEventId)
                            if (calendarLink != null) {
                                android.util.Log.d("CompleteTaskUseCase", "    Found calendar link for parent (ID: ${calendarLink.id})")

                                if (!calendarLink.rewarded) {
                                    android.util.Log.d("CompleteTaskUseCase", "    Marking parent calendar link as CLAIMED")
                                    calendarLinkRepository.updateLink(
                                        calendarLink.copy(rewarded = true, status = "CLAIMED")
                                    )

                                    // Delete calendar event if deleteOnClaim flag is set
                                    if (calendarLink.deleteOnClaim) {
                                        try {
                                            android.util.Log.d("CompleteTaskUseCase", "    Deleting parent calendar event (deleteOnClaim=true)")
                                            calendarManager.deleteEvent(calendarEventId)
                                            android.util.Log.d("CompleteTaskUseCase", "    Successfully deleted parent calendar event")
                                        } catch (e: Exception) {
                                            android.util.Log.e("CompleteTaskUseCase", "    Failed to delete parent calendar event: ${e.message}")
                                        }
                                    }
                                } else {
                                    android.util.Log.d("CompleteTaskUseCase", "    Parent calendar link already claimed")
                                }
                            } else {
                                android.util.Log.w("CompleteTaskUseCase", "    No calendar link found for parent event ID: $calendarEventId")
                            }
                        } else {
                            android.util.Log.d("CompleteTaskUseCase", "    Parent has no calendar event ID")
                        }
                    }
                } else {
                    if (parentTask.isCompleted) {
                        android.util.Log.d("CompleteTaskUseCase", "    Parent already completed - skipping")
                    } else {
                        android.util.Log.d("CompleteTaskUseCase", "    autoCompleteParent=false - skipping auto-complete")
                    }
                }
            } else {
                android.util.Log.w("CompleteTaskUseCase", "    Parent task NOT FOUND for ID: $parentId")
            }
        }

        android.util.Log.d("CompleteTaskUseCase", "handleSubtaskCompletion END")
    }
}

data class CompleteTaskResult(
    val success: Boolean,
    val message: String? = null,
    val xpGranted: Int? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int? = null,
    val categoryName: String? = null
)