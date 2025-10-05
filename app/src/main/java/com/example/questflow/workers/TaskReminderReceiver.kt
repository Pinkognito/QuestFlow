package com.example.questflow.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.questflow.domain.notification.TaskNotificationManager

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("TaskReminderReceiver", "onReceive() called")

        val taskId = intent.getLongExtra("taskId", -1L)
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val xpReward = intent.getIntExtra("xpReward", 0)

        android.util.Log.d("TaskReminderReceiver", "Task data: id=$taskId, title=$title, xp=$xpReward")

        if (taskId == -1L || title.isEmpty()) {
            android.util.Log.e("TaskReminderReceiver", "Invalid task data")
            return
        }

        try {
            val notificationManager = TaskNotificationManager(context)
            notificationManager.showTaskNotification(
                taskId = taskId,
                title = title,
                description = description,
                xpReward = xpReward
            )
            android.util.Log.d("TaskReminderReceiver", "Notification shown successfully")
        } catch (e: Exception) {
            android.util.Log.e("TaskReminderReceiver", "Error showing notification", e)
        }
    }
}
