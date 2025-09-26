package com.example.questflow.data.repository

import com.example.questflow.data.database.TaskDao
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.domain.model.Priority
import com.example.questflow.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<Task>> = 
        taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    
    fun getActiveTasks(): Flow<List<Task>> = 
        taskDao.getActiveTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    
    suspend fun getTaskById(id: Long): Task? =
        taskDao.getTaskById(id)?.toDomainModel()

    suspend fun getTaskByCalendarEventId(calendarEventId: Long): Task? =
        taskDao.getTaskByCalendarEventId(calendarEventId)?.toDomainModel()

    suspend fun insertTask(task: Task): Long =
        taskDao.insertTask(task.toEntity())
    
    suspend fun updateTask(task: Task) =
        taskDao.updateTask(task.toEntity())
    
    suspend fun deleteTask(task: Task) =
        taskDao.deleteTask(task.toEntity())
    
    suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) =
        taskDao.updateTaskCompletion(taskId, isCompleted)

    suspend fun insertTaskEntity(task: TaskEntity): Long =
        taskDao.insertTask(task)

    suspend fun updateTaskEntity(task: TaskEntity) =
        taskDao.updateTask(task)

    suspend fun getTaskEntityById(id: Long): TaskEntity? =
        taskDao.getTaskById(id)
}

private fun TaskEntity.toDomainModel() = Task(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = Priority.valueOf(priority),
    dueDate = dueDate,
    xpReward = xpReward,
    xpPercentage = xpPercentage,
    categoryId = categoryId,
    calendarEventId = calendarEventId,
    createdAt = createdAt,
    completedAt = completedAt,
    isRecurring = isRecurring,
    recurringType = recurringType,
    recurringInterval = recurringInterval,
    recurringDays = recurringDays,
    lastCompletedAt = lastCompletedAt,
    nextDueDate = nextDueDate,
    isEditable = isEditable,
    parentTaskId = parentTaskId
)

private fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    isCompleted = isCompleted,
    priority = priority.name,
    dueDate = dueDate,
    difficulty = "MEDIUM",
    xpOverride = null,
    xpReward = xpReward,
    xpPercentage = xpPercentage,
    categoryId = categoryId,
    calendarEventId = calendarEventId,
    createdAt = createdAt,
    completedAt = completedAt,
    isRecurring = isRecurring,
    recurringType = recurringType,
    recurringInterval = recurringInterval,
    recurringDays = recurringDays,
    lastCompletedAt = lastCompletedAt,
    nextDueDate = nextDueDate,
    isEditable = isEditable,
    parentTaskId = parentTaskId
)