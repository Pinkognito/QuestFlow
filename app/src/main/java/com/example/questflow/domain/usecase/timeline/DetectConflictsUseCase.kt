package com.example.questflow.domain.usecase.timeline

import com.example.questflow.domain.model.TimelineTask
import com.example.questflow.domain.model.ConflictState
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Detects conflicts and overlaps between timeline tasks.
 * Calculates ConflictState for each task based on overlap and tolerance settings.
 */
class DetectConflictsUseCase @Inject constructor() {

    /**
     * Detect conflicts for all tasks and return updated tasks with conflict states.
     *
     * @param tasks List of timeline tasks (should be tasks from the same day)
     * @param toleranceMinutes Minimum gap between tasks in minutes
     * @return List of tasks with updated conflict states
     */
    operator fun invoke(tasks: List<TimelineTask>, toleranceMinutes: Int): List<TimelineTask> {
        if (tasks.isEmpty()) return emptyList()

        return tasks.map { task ->
            val conflictState = calculateConflictState(task, tasks, toleranceMinutes)
            task.copy(conflictState = conflictState)
        }
    }

    /**
     * Calculate conflict state for a single task against all other tasks.
     *
     * Priority: OVERLAP > TOLERANCE_WARNING > NO_CONFLICT
     * (If a task has both overlap and tolerance warning with different tasks,
     * OVERLAP takes precedence)
     */
    private fun calculateConflictState(
        task: TimelineTask,
        allTasks: List<TimelineTask>,
        toleranceMinutes: Int
    ): ConflictState {
        val otherTasks = allTasks.filter { it.id != task.id }

        var hasOverlap = false
        var hasToleranceWarning = false

        for (otherTask in otherTasks) {
            when {
                // Check for direct overlap
                tasksOverlap(task, otherTask) -> {
                    hasOverlap = true
                    break // Overlap is highest priority, no need to check further
                }
                // Check for tolerance violation
                violatesTolerance(task, otherTask, toleranceMinutes) -> {
                    hasToleranceWarning = true
                }
            }
        }

        return when {
            hasOverlap -> ConflictState.OVERLAP
            hasToleranceWarning -> ConflictState.TOLERANCE_WARNING
            else -> ConflictState.NO_CONFLICT
        }
    }

    /**
     * Check if two tasks overlap in time.
     *
     * Tasks overlap if:
     * - Task A ends after Task B starts AND
     * - Task A starts before Task B ends
     */
    private fun tasksOverlap(taskA: TimelineTask, taskB: TimelineTask): Boolean {
        return taskA.endTime > taskB.startTime && taskA.startTime < taskB.endTime
    }

    /**
     * Check if gap between two tasks is less than tolerance.
     *
     * Tolerance is violated if:
     * - Tasks don't overlap (gap >= 0) AND
     * - Gap is less than tolerance minutes
     */
    private fun violatesTolerance(
        taskA: TimelineTask,
        taskB: TimelineTask,
        toleranceMinutes: Int
    ): Boolean {
        // Don't check tolerance if tasks overlap
        if (tasksOverlap(taskA, taskB)) return false

        // Calculate gap in both directions (before and after)
        val gapBefore = if (taskA.startTime > taskB.endTime) {
            ChronoUnit.MINUTES.between(taskB.endTime, taskA.startTime)
        } else Long.MAX_VALUE

        val gapAfter = if (taskB.startTime > taskA.endTime) {
            ChronoUnit.MINUTES.between(taskA.endTime, taskB.startTime)
        } else Long.MAX_VALUE

        val minGap = minOf(gapBefore, gapAfter)

        return minGap in 0 until toleranceMinutes
    }

    /**
     * Get all overlapping task pairs.
     * Useful for debugging or visualization.
     */
    fun getOverlappingPairs(tasks: List<TimelineTask>): Set<Pair<Long, Long>> {
        val overlaps = mutableSetOf<Pair<Long, Long>>()

        for (i in tasks.indices) {
            for (j in i + 1 until tasks.size) {
                if (tasksOverlap(tasks[i], tasks[j])) {
                    val pair = if (tasks[i].id < tasks[j].id) {
                        tasks[i].id to tasks[j].id
                    } else {
                        tasks[j].id to tasks[i].id
                    }
                    overlaps.add(pair)
                }
            }
        }

        return overlaps
    }

    /**
     * Get tasks with tolerance warnings.
     */
    fun getToleranceWarnings(tasks: List<TimelineTask>, toleranceMinutes: Int): List<TimelineTask> {
        return invoke(tasks, toleranceMinutes)
            .filter { it.conflictState == ConflictState.TOLERANCE_WARNING }
    }
}
