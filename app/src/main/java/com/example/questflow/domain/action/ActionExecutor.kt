package com.example.questflow.domain.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.repository.ActionHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes actions on contacts (WhatsApp, SMS, Email, Call, Meeting)
 * Uses Android Intents to launch native apps
 */
@Singleton
class ActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val actionHistoryRepository: ActionHistoryRepository,
    private val metadataPhoneDao: com.example.questflow.data.database.dao.MetadataPhoneDao,
    private val metadataEmailDao: com.example.questflow.data.database.dao.MetadataEmailDao
) {
    /**
     * Send WhatsApp message to contacts
     * Opens WhatsApp for each contact individually
     */
    suspend fun sendWhatsAppMessage(
        taskId: Long,
        contacts: List<MetadataContactEntity>,
        message: String,
        templateName: String? = null
    ): ActionResult {
        android.util.Log.d("ActionExecutor", "=== sendWhatsAppMessage CALLED ===")
        android.util.Log.d("ActionExecutor", "taskId: $taskId")
        android.util.Log.d("ActionExecutor", "contacts: ${contacts.size}")
        android.util.Log.d("ActionExecutor", "message: '$message'")
        android.util.Log.d("ActionExecutor", "templateName: $templateName")

        val results = mutableListOf<ContactActionResult>()

        for (contact in contacts) {
            android.util.Log.d("ActionExecutor", "Processing contact: ${contact.displayName}")
            try {
                val phoneNumber = getPhoneNumber(contact)
                android.util.Log.d("ActionExecutor", "  Phone number: $phoneNumber")
                if (phoneNumber == null) {
                    android.util.Log.e("ActionExecutor", "  ERROR: No phone number for ${contact.displayName}")
                    results.add(ContactActionResult(contact.id, contact.displayName, false, "Keine Telefonnummer"))
                    continue
                }

                val whatsappUrl = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
                android.util.Log.d("ActionExecutor", "  WhatsApp URL: $whatsappUrl")

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(whatsappUrl)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                android.util.Log.d("ActionExecutor", "  Starting WhatsApp activity...")
                context.startActivity(intent)
                android.util.Log.d("ActionExecutor", "  SUCCESS: WhatsApp opened for ${contact.displayName}")
                results.add(ContactActionResult(contact.id, contact.displayName, true))
            } catch (e: Exception) {
                android.util.Log.e("ActionExecutor", "  EXCEPTION: ${e.message}", e)
                results.add(ContactActionResult(contact.id, contact.displayName, false, e.message))
            }
        }

        // Record in history
        val allSuccess = results.all { it.success }
        actionHistoryRepository.recordAction(
            taskId = taskId,
            actionType = "WHATSAPP",
            targetContactIds = contacts.map { it.id },
            targetContactNames = contacts.map { it.displayName },
            message = message,
            templateUsed = templateName,
            success = allSuccess,
            errorMessage = if (allSuccess) null else results.filter { !it.success }.joinToString { it.error ?: "" }
        )

        return ActionResult(
            actionType = "WHATSAPP",
            success = allSuccess,
            contactResults = results
        )
    }

    /**
     * Send SMS to contacts
     */
    suspend fun sendSMS(
        taskId: Long,
        contacts: List<MetadataContactEntity>,
        message: String,
        templateName: String? = null
    ): ActionResult {
        val results = mutableListOf<ContactActionResult>()

        for (contact in contacts) {
            try {
                val phoneNumber = getPhoneNumber(contact)
                if (phoneNumber == null) {
                    results.add(ContactActionResult(contact.id, contact.displayName, false, "Keine Telefonnummer"))
                    continue
                }

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$phoneNumber")
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                context.startActivity(intent)
                results.add(ContactActionResult(contact.id, contact.displayName, true))
            } catch (e: Exception) {
                results.add(ContactActionResult(contact.id, contact.displayName, false, e.message))
            }
        }

        // Record in history
        val allSuccess = results.all { it.success }
        actionHistoryRepository.recordAction(
            taskId = taskId,
            actionType = "SMS",
            targetContactIds = contacts.map { it.id },
            targetContactNames = contacts.map { it.displayName },
            message = message,
            templateUsed = templateName,
            success = allSuccess,
            errorMessage = if (allSuccess) null else results.filter { !it.success }.joinToString { it.error ?: "" }
        )

        return ActionResult(
            actionType = "SMS",
            success = allSuccess,
            contactResults = results
        )
    }

    /**
     * Send Email to contacts
     */
    suspend fun sendEmail(
        taskId: Long,
        contacts: List<MetadataContactEntity>,
        subject: String,
        body: String,
        templateName: String? = null
    ): ActionResult {
        val results = mutableListOf<ContactActionResult>()

        try {
            // For email, we can send to multiple recipients at once
            val emails = contacts.mapNotNull { getEmailAddress(it) }

            if (emails.isEmpty()) {
                return ActionResult(
                    actionType = "EMAIL",
                    success = false,
                    contactResults = contacts.map {
                        ContactActionResult(it.id, it.displayName, false, "Keine Email-Adresse")
                    }
                )
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(Intent.createChooser(intent, "Email senden"))

            results.addAll(contacts.map { ContactActionResult(it.id, it.displayName, true) })
        } catch (e: Exception) {
            results.addAll(contacts.map { ContactActionResult(it.id, it.displayName, false, e.message) })
        }

        // Record in history
        val allSuccess = results.all { it.success }
        actionHistoryRepository.recordAction(
            taskId = taskId,
            actionType = "EMAIL",
            targetContactIds = contacts.map { it.id },
            targetContactNames = contacts.map { it.displayName },
            message = body,
            templateUsed = templateName,
            success = allSuccess,
            errorMessage = if (allSuccess) null else results.filter { !it.success }.joinToString { it.error ?: "" }
        )

        return ActionResult(
            actionType = "EMAIL",
            success = allSuccess,
            contactResults = results
        )
    }

    /**
     * Make phone call to contact
     * Note: Only supports single contact at a time
     */
    suspend fun makeCall(
        taskId: Long,
        contact: MetadataContactEntity
    ): ActionResult {
        val results = mutableListOf<ContactActionResult>()

        try {
            val phoneNumber = getPhoneNumber(contact)
            if (phoneNumber == null) {
                results.add(ContactActionResult(contact.id, contact.displayName, false, "Keine Telefonnummer"))
            } else {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                context.startActivity(intent)
                results.add(ContactActionResult(contact.id, contact.displayName, true))
            }
        } catch (e: Exception) {
            results.add(ContactActionResult(contact.id, contact.displayName, false, e.message))
        }

        // Record in history
        val success = results.first().success
        actionHistoryRepository.recordAction(
            taskId = taskId,
            actionType = "CALL",
            targetContactIds = listOf(contact.id),
            targetContactNames = listOf(contact.displayName),
            success = success,
            errorMessage = if (success) null else results.first().error
        )

        return ActionResult(
            actionType = "CALL",
            success = success,
            contactResults = results
        )
    }

    /**
     * Create calendar meeting with contacts
     */
    suspend fun createMeeting(
        taskId: Long,
        contacts: List<MetadataContactEntity>,
        title: String,
        description: String?,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        location: String?
    ): ActionResult {
        val results = mutableListOf<ContactActionResult>()

        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description ?: "")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                putExtra(CalendarContract.Events.EVENT_LOCATION, location ?: "")

                // Add attendees (emails)
                val emails = contacts.mapNotNull { getEmailAddress(it) }
                if (emails.isNotEmpty()) {
                    putExtra(Intent.EXTRA_EMAIL, emails.joinToString(","))
                }

                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            results.addAll(contacts.map { ContactActionResult(it.id, it.displayName, true) })
        } catch (e: Exception) {
            results.addAll(contacts.map { ContactActionResult(it.id, it.displayName, false, e.message) })
        }

        // Record in history
        val allSuccess = results.all { it.success }
        actionHistoryRepository.recordAction(
            taskId = taskId,
            actionType = "MEETING",
            targetContactIds = contacts.map { it.id },
            targetContactNames = contacts.map { it.displayName },
            message = "Meeting: $title",
            success = allSuccess,
            errorMessage = if (allSuccess) null else results.filter { !it.success }.joinToString { it.error ?: "" }
        )

        return ActionResult(
            actionType = "MEETING",
            success = allSuccess,
            contactResults = results
        )
    }

    /**
     * Helper: Extract phone number from contact
     * In real implementation, would query phone DAO
     */
    private suspend fun getPhoneNumber(contact: MetadataContactEntity): String? {
        android.util.Log.d("ActionExecutor", "  getPhoneNumber for contact ${contact.id}")
        // Query first phone number for this contact
        val phones = metadataPhoneDao.getByContactId(contact.id).first()
        android.util.Log.d("ActionExecutor", "  Found ${phones.size} phone numbers")
        val phoneNumber = phones.firstOrNull()?.phoneNumber
        android.util.Log.d("ActionExecutor", "  Selected phone: $phoneNumber")
        return phoneNumber
    }

    /**
     * Helper: Extract email address from contact
     */
    private suspend fun getEmailAddress(contact: MetadataContactEntity): String? {
        android.util.Log.d("ActionExecutor", "  getEmailAddress for contact ${contact.id}")
        // Query first email for this contact
        val emails = metadataEmailDao.getByContactId(contact.id).first()
        android.util.Log.d("ActionExecutor", "  Found ${emails.size} emails")
        val email = emails.firstOrNull()?.emailAddress
        android.util.Log.d("ActionExecutor", "  Selected email: $email")
        return email
    }
}

data class ActionResult(
    val actionType: String,
    val success: Boolean,
    val contactResults: List<ContactActionResult>
)

data class ContactActionResult(
    val contactId: Long,
    val contactName: String,
    val success: Boolean,
    val error: String? = null
)
