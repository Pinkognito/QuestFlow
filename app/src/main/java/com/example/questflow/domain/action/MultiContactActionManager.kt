package com.example.questflow.domain.action

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.questflow.MainActivity
import com.example.questflow.data.database.entity.MetadataContactEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages multi-contact actions with notification-based flow
 * Allows user to process contacts one by one with notifications between them
 */
@Singleton
class MultiContactActionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "multi_contact_actions"
        private const val CHANNEL_NAME = "Multi-Kontakt Aktionen"
        private const val NOTIFICATION_ID = 12345
        const val ACTION_NEXT_CONTACT = "com.example.questflow.ACTION_NEXT_CONTACT"
    }

    private val _currentSession = MutableStateFlow<MultiContactSession?>(null)
    val currentSession: StateFlow<MultiContactSession?> = _currentSession

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Benachrichtigungen für Multi-Kontakt Aktionen"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start a new multi-contact WhatsApp session
     */
    fun startWhatsAppSession(
        taskId: Long,
        contacts: List<MetadataContactEntity>,
        messages: Map<Long, String>,
        templateName: String?
    ) {
        android.util.Log.d("MultiContactActionManager", "Starting WhatsApp session with ${contacts.size} contacts")

        val session = MultiContactSession(
            taskId = taskId,
            contacts = contacts,
            messages = messages,
            templateName = templateName,
            currentIndex = 0
        )

        _currentSession.value = session
    }

    /**
     * Get current contact to process
     */
    fun getCurrentContact(): ContactToProcess? {
        val session = _currentSession.value ?: return null
        if (session.currentIndex >= session.contacts.size) return null

        val contact = session.contacts[session.currentIndex]
        return ContactToProcess(
            contact = contact,
            message = session.messages[contact.id] ?: "",
            index = session.currentIndex,
            total = session.contacts.size
        )
    }

    /**
     * Mark current contact as processed and show notification for next
     */
    fun processedCurrentContact() {
        val session = _currentSession.value ?: return
        val newIndex = session.currentIndex + 1

        android.util.Log.d("MultiContactActionManager", "Processed contact $newIndex/${session.contacts.size}")

        if (newIndex < session.contacts.size) {
            // Update session
            _currentSession.value = session.copy(currentIndex = newIndex)

            // Show notification for next contact
            showNextContactNotification(session.contacts[newIndex], newIndex, session.contacts.size)
        } else {
            // All done
            android.util.Log.d("MultiContactActionManager", "All contacts processed!")
            clearSession()
        }
    }

    /**
     * Show notification to continue with next contact
     */
    private fun showNextContactNotification(contact: MetadataContactEntity, index: Int, total: Int) {
        android.util.Log.d("MultiContactActionManager", "Showing notification for contact ${index + 1}/$total: ${contact.displayName}")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open app and continue
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_NEXT_CONTACT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("📱 Nächster Kontakt bereit")
            .setContentText("${index + 1}/$total: WhatsApp an ${contact.displayName} senden")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                "WhatsApp öffnen",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Clear current session
     */
    fun clearSession() {
        android.util.Log.d("MultiContactActionManager", "Clearing session")
        _currentSession.value = null

        // Cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Check if there's an active session
     */
    fun hasActiveSession(): Boolean = _currentSession.value != null
}

data class MultiContactSession(
    val taskId: Long,
    val contacts: List<MetadataContactEntity>,
    val messages: Map<Long, String>,
    val templateName: String?,
    val currentIndex: Int
)

data class ContactToProcess(
    val contact: MetadataContactEntity,
    val message: String,
    val index: Int,
    val total: Int
)
