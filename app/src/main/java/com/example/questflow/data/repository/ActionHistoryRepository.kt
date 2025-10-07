package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.ActionHistoryDao
import com.example.questflow.data.database.entity.ActionHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionHistoryRepository @Inject constructor(
    private val actionHistoryDao: ActionHistoryDao
) {
    fun getHistoryForTask(taskId: Long): Flow<List<ActionHistoryEntity>> =
        actionHistoryDao.getHistoryForTask(taskId)

    suspend fun getHistoryForTaskSync(taskId: Long): List<ActionHistoryEntity> =
        actionHistoryDao.getHistoryForTaskSync(taskId)

    suspend fun getHistoryByType(taskId: Long, actionType: String): List<ActionHistoryEntity> =
        actionHistoryDao.getHistoryByType(taskId, actionType)

    suspend fun getActionCount(taskId: Long): Int = actionHistoryDao.getActionCount(taskId)

    suspend fun getActionById(id: Long): ActionHistoryEntity? = actionHistoryDao.getActionById(id)

    suspend fun insertAction(action: ActionHistoryEntity): Long = actionHistoryDao.insertAction(action)

    suspend fun deleteAction(action: ActionHistoryEntity) = actionHistoryDao.deleteAction(action)

    suspend fun deleteAllActionsForTask(taskId: Long) = actionHistoryDao.deleteAllActionsForTask(taskId)

    suspend fun getRecentActions(startDate: LocalDateTime): List<ActionHistoryEntity> =
        actionHistoryDao.getRecentActions(startDate)

    /**
     * Record a new action with all details
     */
    suspend fun recordAction(
        taskId: Long,
        actionType: String,
        targetContactIds: List<Long>,
        targetContactNames: List<String>,
        message: String? = null,
        templateUsed: String? = null,
        detailsJson: String? = null,
        success: Boolean = true,
        errorMessage: String? = null
    ): Long {
        val action = ActionHistoryEntity(
            taskId = taskId,
            actionType = actionType,
            targetContactIds = targetContactIds.joinToString(","),
            targetContactNames = targetContactNames.joinToString(", "),
            message = message,
            templateUsed = templateUsed,
            detailsJson = detailsJson,
            success = success,
            errorMessage = errorMessage,
            executedAt = LocalDateTime.now()
        )
        return insertAction(action)
    }
}
