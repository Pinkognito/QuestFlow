package com.example.questflow.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.questflow.domain.notification.TaskNotificationManager

class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1L)
        val title = inputData.getString("title") ?: return Result.failure()
        val description = inputData.getString("description") ?: ""
        val xpReward = inputData.getInt("xpReward", 0)

        if (taskId == -1L) {
            return Result.failure()
        }

        // Create notification manager and show notification
        val notificationManager = TaskNotificationManager(applicationContext)
        notificationManager.showTaskNotification(
            taskId = taskId,
            title = title,
            description = description,
            xpReward = xpReward
        )

        return Result.success()
    }
}
