package com.example.questflow.domain.usecase.timeline

import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.presentation.screens.timeline.TaskSortOption
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * UseCase for batch task positioning with auto-arrangement.
 * Takes selected tasks and arranges them sequentially within a time range.
 */
class BatchTaskPositioningUseCase @Inject constructor(
    private val updateTaskTimeUseCase: UpdateTaskTimeUseCase
) {

    /**
     * Result of batch positioning operation
     */
    sealed class Result {
        data class Success(val updatedTasks: List<TimelineTask>) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Position tasks within time range with optional sorting.
     *
     * @param tasks List of tasks to position (should be pre-ordered if CUSTOM_ORDER)
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param sortOption Sort option (default: CUSTOM_ORDER)
     * @param gapMinutes Gap between tasks in minutes (default: 15)
     * @return Result with updated task list or error
     */
    suspend operator fun invoke(
        tasks: List<TimelineTask>,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        sortOption: TaskSortOption = TaskSortOption.CUSTOM_ORDER,
        gapMinutes: Long = 15
    ): Result {
        android.util.Log.d("BatchTaskPositioning", "Starting batch positioning: ${tasks.size} tasks, range: $startTime - $endTime, sortOption: $sortOption")

        if (tasks.isEmpty()) {
            android.util.Log.w("BatchTaskPositioning", "No tasks to insert")
            return Result.Error("Keine Tasks zum Einfügen ausgewählt")
        }

        // Filter out external calendar events (they can't be modified)
        val modifiableTasks = tasks.filter { !it.isExternal }
        val externalTasksCount = tasks.size - modifiableTasks.size

        if (externalTasksCount > 0) {
            android.util.Log.w("BatchTaskPositioning", "Filtered out $externalTasksCount external calendar events (can't be modified)")
        }

        if (modifiableTasks.isEmpty()) {
            android.util.Log.w("BatchTaskPositioning", "No modifiable tasks to insert (all were external)")
            return Result.Error("Keine verschiebbaren Tasks ausgewählt (externe Kalender-Events können nicht verschoben werden)")
        }

        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            android.util.Log.w("BatchTaskPositioning", "Invalid time range: start=$startTime, end=$endTime")
            return Result.Error("Ungültiger Zeitbereich: Start muss vor Ende liegen")
        }

        return try {
            // 1. Sort tasks according to option
            val sortedTasks = sortTasks(modifiableTasks, sortOption)
            android.util.Log.d("BatchTaskPositioning", "Tasks sorted (${sortedTasks.size} modifiable): ${sortedTasks.map { it.title }}")

            // 2. Calculate total duration needed
            val totalDuration = sortedTasks.sumOf { it.durationMinutes() } + (gapMinutes * (sortedTasks.size - 1))
            val availableMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime)

            android.util.Log.d("BatchTaskPositioning", "Duration check: ${totalDuration}min needed, ${availableMinutes}min available")

            if (totalDuration > availableMinutes) {
                val errorMsg = "Tasks passen nicht in Zeitbereich: ${totalDuration}min benötigt, ${availableMinutes}min verfügbar"
                android.util.Log.w("BatchTaskPositioning", errorMsg)
                return Result.Error(errorMsg)
            }

            // 3. Position tasks sequentially
            var currentTime = startTime
            val updatedTasks = mutableListOf<TimelineTask>()

            for ((index, task) in sortedTasks.withIndex()) {
                val taskDuration = task.durationMinutes()
                val newEndTime = currentTime.plusMinutes(taskDuration)

                android.util.Log.d("BatchTaskPositioning", "Updating task ${index + 1}/${sortedTasks.size}: '${task.title}' to $currentTime - $newEndTime")

                // Update task time
                val updateResult = updateTaskTimeUseCase(
                    taskId = task.taskId,
                    linkId = task.linkId,
                    newStartTime = currentTime,
                    newEndTime = newEndTime
                )

                when (updateResult) {
                    is UpdateTaskTimeUseCase.Result.Error -> {
                        val errorMsg = "Fehler beim Aktualisieren von Task '${task.title}': ${updateResult.message}"
                        android.util.Log.e("BatchTaskPositioning", errorMsg)
                        return Result.Error(errorMsg)
                    }
                    UpdateTaskTimeUseCase.Result.Success -> {
                        android.util.Log.d("BatchTaskPositioning", "Task '${task.title}' updated successfully")
                        // Create updated task representation
                        updatedTasks.add(
                            task.copy(
                                startTime = currentTime,
                                endTime = newEndTime
                            )
                        )
                    }
                }

                // Move to next time slot (with gap)
                currentTime = newEndTime.plusMinutes(gapMinutes)
            }

            android.util.Log.d("BatchTaskPositioning", "Batch positioning completed successfully: ${updatedTasks.size} tasks updated")
            Result.Success(updatedTasks)

        } catch (e: Exception) {
            val errorMsg = "Batch-Positionierung fehlgeschlagen: ${e.message}\n${e.stackTraceToString()}"
            android.util.Log.e("BatchTaskPositioning", errorMsg, e)
            Result.Error("Batch-Positionierung fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Sort tasks according to selected option
     */
    private fun sortTasks(tasks: List<TimelineTask>, sortOption: TaskSortOption): List<TimelineTask> {
        return when (sortOption) {
            TaskSortOption.CUSTOM_ORDER -> {
                // Already ordered by caller (manual order from selection list)
                tasks
            }

            TaskSortOption.PRIORITY -> {
                // Sort by priority (HIGH → LOW)
                // Note: TimelineTask doesn't have priority field, so we skip for now
                // TODO: Add priority field to TimelineTask or fetch from Task entity
                tasks
            }

            TaskSortOption.XP_PERCENTAGE -> {
                // Sort by XP percentage (Epic 100% → Trivial 20%)
                tasks.sortedByDescending { it.xpPercentage }
            }

            TaskSortOption.DURATION -> {
                // Sort by duration ascending (short → long)
                tasks.sortedBy { it.durationMinutes() }
            }

            TaskSortOption.DURATION_DESC -> {
                // Sort by duration descending (long → short)
                tasks.sortedByDescending { it.durationMinutes() }
            }

            TaskSortOption.ALPHABETICAL -> {
                // Sort alphabetically by title
                tasks.sortedBy { it.title.lowercase() }
            }

            TaskSortOption.CATEGORY -> {
                // Sort by category (grouped)
                tasks.sortedBy { it.categoryId ?: Long.MAX_VALUE }
            }
        }
    }

    /**
     * Validate if tasks fit into time range
     */
    fun validateFit(
        tasks: List<TimelineTask>,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        gapMinutes: Long = 15
    ): ValidationResult {
        val totalDuration = tasks.sumOf { it.durationMinutes() } + (gapMinutes * (tasks.size - 1))
        val availableMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime)

        return if (totalDuration <= availableMinutes) {
            ValidationResult.Valid(totalDuration, availableMinutes)
        } else {
            ValidationResult.Invalid(totalDuration, availableMinutes)
        }
    }

    /**
     * Validation result
     */
    sealed class ValidationResult {
        data class Valid(val requiredMinutes: Long, val availableMinutes: Long) : ValidationResult()
        data class Invalid(val requiredMinutes: Long, val availableMinutes: Long) : ValidationResult()
    }
}
