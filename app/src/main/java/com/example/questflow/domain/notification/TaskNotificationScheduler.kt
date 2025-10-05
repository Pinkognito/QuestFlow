package com.example.questflow.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.questflow.workers.TaskReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules a notification for a task at the specified time.
     * Uses AlarmManager for guaranteed delivery even when app is closed.
     */
    fun scheduleNotification(
        taskId: Long,
        title: String,
        description: String,
        xpReward: Int,
        notificationTime: LocalDateTime
    ) {
        val now = LocalDateTime.now()

        // Only schedule if notification time is in the future
        if (notificationTime.isAfter(now)) {
            val triggerTimeMillis = notificationTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("title", title)
                putExtra("description", description)
                putExtra("xpReward", xpReward)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use setExactAndAllowWhileIdle for guaranteed delivery even in Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )

            android.util.Log.d(TAG, "Scheduled notification for task $taskId at $notificationTime (triggerTime=$triggerTimeMillis)")
        } else {
            android.util.Log.d(TAG, "Task $taskId notification time is in the past, not scheduling")
        }
    }

    /**
     * Cancels a scheduled notification for a task.
     */
    fun cancelNotification(taskId: Long) {
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        android.util.Log.d(TAG, "Cancelled notification for task $taskId")
    }

    /**
     * Reschedules a notification (cancels old, schedules new).
     */
    fun rescheduleNotification(
        taskId: Long,
        title: String,
        description: String,
        xpReward: Int,
        notificationTime: LocalDateTime
    ) {
        cancelNotification(taskId)
        scheduleNotification(taskId, title, description, xpReward, notificationTime)
    }

    companion object {
        private const val TAG = "TaskNotificationScheduler"
    }
}
