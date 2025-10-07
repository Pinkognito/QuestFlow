package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.ActionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Query("SELECT * FROM action_history WHERE taskId = :taskId ORDER BY executedAt DESC")
    fun getHistoryForTask(taskId: Long): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE taskId = :taskId ORDER BY executedAt DESC")
    suspend fun getHistoryForTaskSync(taskId: Long): List<ActionHistoryEntity>

    @Query("SELECT * FROM action_history WHERE taskId = :taskId AND actionType = :actionType ORDER BY executedAt DESC")
    suspend fun getHistoryByType(taskId: Long, actionType: String): List<ActionHistoryEntity>

    @Query("SELECT COUNT(*) FROM action_history WHERE taskId = :taskId")
    suspend fun getActionCount(taskId: Long): Int

    @Query("SELECT * FROM action_history WHERE id = :id")
    suspend fun getActionById(id: Long): ActionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionHistoryEntity): Long

    @Delete
    suspend fun deleteAction(action: ActionHistoryEntity)

    @Query("DELETE FROM action_history WHERE taskId = :taskId")
    suspend fun deleteAllActionsForTask(taskId: Long)

    @Query("""
        SELECT * FROM action_history
        WHERE executedAt >= :startDate
        ORDER BY executedAt DESC
    """)
    suspend fun getRecentActions(startDate: java.time.LocalDateTime): List<ActionHistoryEntity>
}
