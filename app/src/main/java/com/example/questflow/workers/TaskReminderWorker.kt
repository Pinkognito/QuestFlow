package com.example.questflow.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import com.example.questflow.domain.notification.TaskNotificationManager

class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Create foreground notification for the worker itself
        // This allows the worker to run reliably in background
        val notification = NotificationCompat.Builder(applicationContext, "task_reminders")
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("QuestFlow")
            .setContentText("Verarbeite Task-Erinnerung...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                999, // Notification ID for worker itself
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(999, notification)
        }
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("TaskReminderWorker", "doWork() started")

        val taskId = inputData.getLong("taskId", -1L)
        val title = inputData.getString("title") ?: ""
        val description = inputData.getString("description") ?: ""
        val xpReward = inputData.getInt("xpReward", 0)

        android.util.Log.d("TaskReminderWorker", "Task data: id=$taskId, title=$title, xp=$xpReward")

        if (taskId == -1L) {
            android.util.Log.e("TaskReminderWorker", "Invalid taskId, returning failure")
            return Result.failure()
        }

        if (title.isEmpty()) {
            android.util.Log.e("TaskReminderWorker", "Empty title, returning failure")
            return Result.failure()
        }

        try {
            // Set as foreground to ensure execution
            setForeground(getForegroundInfo())

            // Create notification manager and show notification
            android.util.Log.d("TaskReminderWorker", "Creating TaskNotificationManager...")
            val notificationManager = TaskNotificationManager(applicationContext)

            android.util.Log.d("TaskReminderWorker", "Showing notification...")
            notificationManager.showTaskNotification(
                taskId = taskId,
                title = title,
                description = description,
                xpReward = xpReward
            )

            android.util.Log.d("TaskReminderWorker", "Notification shown successfully")
            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("TaskReminderWorker", "Error showing notification", e)
            return Result.failure()
        }
    }
}
