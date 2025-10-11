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
    
    suspend fun getTaskById(id: Long): Task? {
        android.util.Log.d("DescriptionFlow-Repository", "🔍 CALLING DAO.getTaskById($id)")
        val entity = taskDao.getTaskById(id)
        val task = entity?.toDomainModel()
        android.util.Log.d("DescriptionFlow-Repository", "📖 LOADED from DB: taskId=$id, entity=${if(entity==null) "NULL" else "present"}, description='${task?.description ?: "N/A"}'")
        return task
    }

    suspend fun getTaskByCalendarEventId(calendarEventId: Long): Task? =
        taskDao.getTaskByCalendarEventId(calendarEventId)?.toDomainModel()

    suspend fun insertTask(task: Task): Long =
        taskDao.insertTask(task.toEntity())
    
    suspend fun updateTask(task: Task) {
        android.util.Log.d("DescriptionFlow-Repository", "💾 CALLING DAO.updateTask() for taskId=${task.id}, description='${task.description}'")
        val entity = task.toEntity()
        android.util.Log.d("DescriptionFlow-Repository", "🔄 TaskEntity created: id=${entity.id}, description='${entity.description}'")
        taskDao.updateTask(entity)
        android.util.Log.d("DescriptionFlow-Repository", "✅ DAO.updateTask() completed for taskId=${task.id}")
    }

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

    // Subtask methods
    fun getSubtasks(parentId: Long): Flow<List<Task>> =
        taskDao.getSubtasks(parentId).map { entities ->
            entities.map { it.toDomainModel() }
        }

    suspend fun getSubtasksSync(parentId: Long): List<TaskEntity> =
        taskDao.getSubtasksSync(parentId)

    fun getTaskByIdFlow(taskId: Long): Flow<Task?> =
        taskDao.getTaskByIdFlow(taskId).map { it?.toDomainModel() }

    suspend fun getIncompleteSubtaskCount(parentId: Long): Int =
        taskDao.getIncompleteSubtaskCount(parentId)
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
    specificTime = specificTime,
    triggerMode = triggerMode,
    lastCompletedAt = lastCompletedAt,
    nextDueDate = nextDueDate,
    isEditable = isEditable,
    parentTaskId = parentTaskId,
    autoCompleteParent = autoCompleteParent
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
    specificTime = specificTime,
    triggerMode = triggerMode,
    lastCompletedAt = lastCompletedAt,
    nextDueDate = nextDueDate,
    isEditable = isEditable,
    parentTaskId = parentTaskId,
    autoCompleteParent = autoCompleteParent
)