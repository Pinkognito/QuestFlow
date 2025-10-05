package com.example.questflow.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.questflow.MainActivity
import com.example.questflow.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "task_reminders"
        private const val CHANNEL_NAME = "Task Erinnerungen"
        private const val CHANNEL_DESC = "Benachrichtigungen für anstehende Tasks"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskNotification(
        taskId: Long,
        title: String,
        description: String,
        xpReward: Int
    ) {
        android.util.Log.d("TaskNotificationManager", "showTaskNotification called: taskId=$taskId, title=$title")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create deep link intent
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("questflow://task/$taskId"),
            context,
            MainActivity::class.java
        ).apply {
            // SINGLE_TOP ensures onNewIntent is called if activity is already running
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today) // TODO: Replace with app icon
            .setContentTitle("🎯 $title")
            .setContentText("$description\n🎮 $xpReward XP")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$description\n\n🎮 XP Reward: $xpReward"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Task öffnen",
                pendingIntent
            )
            .build()

        android.util.Log.d("TaskNotificationManager", "Calling notify() with id=${taskId.toInt()}")
        notificationManager.notify(taskId.toInt(), notification)
        android.util.Log.d("TaskNotificationManager", "Notification sent!")
    }

    fun cancelTaskNotification(taskId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())
    }
}
