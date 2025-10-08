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
        android.util.Log.d("ActionExecutor", "=== sendEmail CALLED ===")
        android.util.Log.d("ActionExecutor", "taskId: $taskId")
        android.util.Log.d("ActionExecutor", "contacts: ${contacts.size}")
        android.util.Log.d("ActionExecutor", "subject: '$subject'")
        android.util.Log.d("ActionExecutor", "body: '$body'")
        android.util.Log.d("ActionExecutor", "templateName: $templateName")

        val results = mutableListOf<ContactActionResult>()

        try {
            // For email, we can send to multiple recipients at once
            android.util.Log.d("ActionExecutor", "Collecting email addresses...")
            val emailMap = contacts.mapNotNull { contact ->
                val email = getEmailAddress(contact)
                android.util.Log.d("ActionExecutor", "  ${contact.displayName}: $email")
                if (email != null) contact to email else null
            }

            if (emailMap.isEmpty()) {
                android.util.Log.e("ActionExecutor", "ERROR: No email addresses found for any contact")
                return ActionResult(
                    actionType = "EMAIL",
                    success = false,
                    contactResults = contacts.map {
                        ContactActionResult(it.id, it.displayName, false, "Keine Email-Adresse")
                    }
                )
            }

            val emails = emailMap.map { it.second }
            android.util.Log.d("ActionExecutor", "Valid emails: ${emails.joinToString()}")

            // Build mailto URI with embedded addresses, subject, and body
            // Format: mailto:addr1,addr2?subject=xxx&body=yyy
            val mailtoUri = buildString {
                append("mailto:")
                append(emails.joinToString(","))
                append("?subject=")
                append(Uri.encode(subject))
                append("&body=")
                append(Uri.encode(body))
            }
            android.util.Log.d("ActionExecutor", "Mailto URI: $mailtoUri")

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse(mailtoUri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            android.util.Log.d("ActionExecutor", "Checking for email apps...")
            val packageManager = context.packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)
            android.util.Log.d("ActionExecutor", "Found ${activities.size} email apps: ${activities.map { it.activityInfo.packageName }}")

            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                android.util.Log.d("ActionExecutor", "Starting email activity...")
                context.startActivity(intent)
                android.util.Log.d("ActionExecutor", "SUCCESS: Email app opened")
                results.addAll(emailMap.map { (contact, _) ->
                    ContactActionResult(contact.id, contact.displayName, true)
                })
            } else {
                android.util.Log.e("ActionExecutor", "ERROR: No email app found (resolveActivity returned null)")
                android.util.Log.e("ActionExecutor", "  This might mean no email app is installed or configured")
                results.addAll(contacts.map {
                    ContactActionResult(it.id, it.displayName, false, "Keine Email-App gefunden oder konfiguriert")
                })
            }
        } catch (e: Exception) {
            android.util.Log.e("ActionExecutor", "EXCEPTION in sendEmail: ${e.message}", e)
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
     * Normalizes German numbers (0XXX -> +49XXX) for WhatsApp
     */
    private suspend fun getPhoneNumber(contact: MetadataContactEntity): String? {
        android.util.Log.d("ActionExecutor", "  getPhoneNumber for contact ${contact.id}")
        // Query first phone number for this contact
        val phones = metadataPhoneDao.getByContactId(contact.id).first()
        android.util.Log.d("ActionExecutor", "  Found ${phones.size} phone numbers")
        val rawPhone = phones.firstOrNull()?.phoneNumber
        android.util.Log.d("ActionExecutor", "  Raw phone: $rawPhone")

        if (rawPhone == null) return null

        // Normalize phone number for international format
        val normalized = normalizePhoneNumber(rawPhone)
        android.util.Log.d("ActionExecutor", "  Normalized phone: $normalized")
        return normalized
    }

    /**
     * Normalize phone number to international format
     * - Removes spaces, dashes, parentheses
     * - Converts German format (0XXX) to +49XXX
     */
    private fun normalizePhoneNumber(phone: String): String {
        // Remove all non-digit characters except +
        var cleaned = phone.replace(Regex("[^0-9+]"), "")
        android.util.Log.d("ActionExecutor", "    Cleaned: $cleaned")

        // If starts with 0 (German mobile/landline), replace with +49
        if (cleaned.startsWith("0") && !cleaned.startsWith("00")) {
            cleaned = "+49" + cleaned.substring(1)
            android.util.Log.d("ActionExecutor", "    Converted 0 -> +49: $cleaned")
        }
        // If starts with 00, replace with +
        else if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2)
            android.util.Log.d("ActionExecutor", "    Converted 00 -> +: $cleaned")
        }
        // If doesn't start with +, assume German and add +49
        else if (!cleaned.startsWith("+")) {
            cleaned = "+49$cleaned"
            android.util.Log.d("ActionExecutor", "    Added +49 prefix: $cleaned")
        }

        return cleaned
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
