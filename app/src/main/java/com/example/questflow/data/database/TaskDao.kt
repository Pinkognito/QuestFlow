package com.example.questflow.data.database

import androidx.room.*
import com.example.questflow.data.database.entity.TaskCompletionByDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE calendarEventId = :calendarEventId")
    suspend fun getTaskByCalendarEventId(calendarEventId: Long): TaskEntity?

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean)

    // Statistics queries
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC LIMIT 100")
    suspend fun getRecentCompletedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLastCompletedTask(): TaskEntity?
}