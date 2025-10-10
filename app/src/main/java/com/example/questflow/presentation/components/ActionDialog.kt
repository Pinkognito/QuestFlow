package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.TextTemplateEntity
import com.example.questflow.domain.action.ActionExecutor
import com.example.questflow.domain.action.MultiContactActionManager
import com.example.questflow.domain.placeholder.PlaceholderResolver
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Wiederverwendbarer Dialog für Aktionen auf Kontakte
 *
 * Unterstützt:
 * - WhatsApp, SMS, Email, Anruf, Meeting
 * - Template-Auswahl
 * - Platzhalter-Ersetzung
 * - Personalisierte vs. Gruppen-Nachrichten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionDialog(
    taskId: Long,
    selectedContacts: List<MetadataContactEntity>,
    availableTemplates: List<TextTemplateEntity>,
    onDismiss: () -> Unit,
    onActionExecuted: () -> Unit,
    actionExecutor: ActionExecutor,
    placeholderResolver: PlaceholderResolver,
    multiContactActionManager: MultiContactActionManager
) {
    var selectedAction by remember { mutableStateOf<ActionType?>(null) }
    var selectedTemplate by remember { mutableStateOf<TextTemplateEntity?>(null) }
    var messageText by remember { mutableStateOf("") }
    var personalizeMessages by remember { mutableStateOf(true) }
    var emailSubject by remember { mutableStateOf("") }

    // Meeting-spezifische States
    var meetingTitle by remember { mutableStateOf("") }
    var meetingLocation by remember { mutableStateOf("") }
    var meetingDate by remember { mutableStateOf(LocalDateTime.now().plusDays(1)) }
    var meetingDuration by remember { mutableStateOf(60) } // Minuten

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Aktion ausführen",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Schließen")
                        }
                    }
                }

                // Selected Contacts Summary
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${selectedContacts.size} Kontakt${if (selectedContacts.size != 1) "e" else ""} ausgewählt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                selectedContacts.joinToString(", ") { it.displayName },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Action Selection
                if (selectedAction == null) {
                    item {
                        Text(
                            "Aktion wählen:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        ActionCard(
                            title = "WhatsApp",
                            icon = Icons.Default.Send,
                            onClick = { selectedAction = ActionType.WHATSAPP }
                        )
                    }

                    item {
                        ActionCard(
                            title = "SMS",
                            icon = Icons.Default.Send,
                            onClick = { selectedAction = ActionType.SMS }
                        )
                    }

                    item {
                        ActionCard(
                            title = "Email",
                            icon = Icons.Default.Email,
                            onClick = { selectedAction = ActionType.EMAIL }
                        )
                    }

                    item {
                        ActionCard(
                            title = "Anrufen",
                            icon = Icons.Default.Call,
                            onClick = { selectedAction = ActionType.CALL },
                            enabled = selectedContacts.size == 1
                        )
                    }

                    item {
                        ActionCard(
                            title = "Meeting erstellen",
                            icon = Icons.Default.DateRange,
                            onClick = { selectedAction = ActionType.MEETING }
                        )
                    }
                } else {
                    // Action Configuration
                    val currentAction = selectedAction // Local val for smart cast
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { selectedAction = null }) {
                                Icon(Icons.Default.ArrowBack, "Zurück")
                            }
                            Text(
                                currentAction?.displayName ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Template Selection
                    if (selectedAction in listOf(ActionType.WHATSAPP, ActionType.SMS, ActionType.EMAIL, ActionType.MEETING)) {
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedTemplate?.title ?: "Kein Template",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Textbaustein (optional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Kein Template") },
                                        onClick = {
                                            selectedTemplate = null
                                            messageText = ""
                                            emailSubject = ""
                                            meetingTitle = ""
                                            expanded = false
                                        }
                                    )
                                    availableTemplates.forEach { template ->
                                        DropdownMenuItem(
                                            text = { Text(template.title) },
                                            onClick = {
                                                selectedTemplate = template
                                                messageText = template.content
                                                // Übernehme Betreff wenn vorhanden
                                                if (template.subject != null) {
                                                    when (selectedAction) {
                                                        ActionType.EMAIL -> emailSubject = template.subject
                                                        ActionType.MEETING -> meetingTitle = template.subject
                                                        else -> {}
                                                    }
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Email Subject
                    if (selectedAction == ActionType.EMAIL) {
                        item {
                            OutlinedTextField(
                                value = emailSubject,
                                onValueChange = { emailSubject = it },
                                label = { Text("Betreff") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Meeting Inputs
                    if (selectedAction == ActionType.MEETING) {
                        item {
                            OutlinedTextField(
                                value = meetingTitle,
                                onValueChange = { meetingTitle = it },
                                label = { Text("Meeting-Titel") },
                                placeholder = { Text("z.B. Projekt-Besprechung") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = meetingLocation,
                                onValueChange = { meetingLocation = it },
                                label = { Text("Ort (optional)") },
                                placeholder = { Text("z.B. Raum 204, Zoom Link") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Text("Datum & Uhrzeit", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = meetingDate.toLocalDate().toString(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Datum") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = String.format("%02d:%02d", meetingDate.hour, meetingDate.minute),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Zeit") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Text("Dauer", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(30, 60, 90, 120).forEach { duration ->
                                    FilterChip(
                                        selected = meetingDuration == duration,
                                        onClick = { meetingDuration = duration },
                                        label = { Text("${duration}min") }
                                    )
                                }
                            }
                        }
                    }

                    // Message Text
                    if (selectedAction in listOf(ActionType.WHATSAPP, ActionType.SMS, ActionType.EMAIL)) {
                        item {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                label = { Text("Nachricht") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                maxLines = 8
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = personalizeMessages,
                                    onCheckedChange = { personalizeMessages = it }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Personalisierte Nachrichten", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Jeder Kontakt erhält individuelle Nachricht mit seinem Namen",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // WhatsApp Multi-Contact Hinweis
                        if (selectedAction == ActionType.WHATSAPP && selectedContacts.size > 1) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Column {
                                            Text(
                                                "WhatsApp Mehrfachversand",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Öffnet WhatsApp ${selectedContacts.size}x nacheinander. " +
                                                        "Nachricht wird vorbefüllt, bitte manuell senden.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Execute Button
                    item {
                        Button(
                            onClick = {
                                android.util.Log.d("ActionDialog", "=== EXECUTE BUTTON CLICKED ===")
                                android.util.Log.d("ActionDialog", "selectedAction: $selectedAction")
                                android.util.Log.d("ActionDialog", "selectedContacts: ${selectedContacts.size}")
                                android.util.Log.d("ActionDialog", "messageText: '$messageText'")
                                android.util.Log.d("ActionDialog", "personalizeMessages: $personalizeMessages")
                                android.util.Log.d("ActionDialog", "selectedTemplate: ${selectedTemplate?.title}")

                                coroutineScope.launch {
                                    android.util.Log.d("ActionDialog", "Coroutine launched, executing action...")
                                    when (selectedAction) {
                                        ActionType.WHATSAPP -> {
                                            android.util.Log.d("ActionDialog", "WHATSAPP action selected")
                                            if (personalizeMessages) {
                                                android.util.Log.d("ActionDialog", "Personalizing messages...")
                                                val messages = placeholderResolver.resolveForContacts(
                                                    messageText,
                                                    taskId,
                                                    selectedContacts.map { it.id }
                                                )
                                                android.util.Log.d("ActionDialog", "Resolved ${messages.size} personalized messages")

                                                // Start multi-contact session
                                                multiContactActionManager.startWhatsAppSession(
                                                    taskId = taskId,
                                                    contacts = selectedContacts,
                                                    messages = messages,
                                                    templateName = selectedTemplate?.title
                                                )

                                                // Send first contact immediately
                                                val firstContact = multiContactActionManager.getCurrentContact()
                                                if (firstContact != null) {
                                                    android.util.Log.d("ActionDialog", "Sending WhatsApp to ${firstContact.contact.displayName} (${firstContact.index + 1}/${firstContact.total})")
                                                    val result = actionExecutor.sendWhatsAppMessage(
                                                        taskId,
                                                        listOf(firstContact.contact),
                                                        firstContact.message,
                                                        selectedTemplate?.title
                                                    )
                                                    android.util.Log.d("ActionDialog", "Result: ${result.success}")

                                                    // Mark as processed - will show notification for next contact
                                                    multiContactActionManager.processedCurrentContact()
                                                }
                                            } else {
                                                android.util.Log.d("ActionDialog", "Sending group WhatsApp message to ${selectedContacts.size} contacts")
                                                val result = actionExecutor.sendWhatsAppMessage(
                                                    taskId,
                                                    selectedContacts,
                                                    messageText,
                                                    selectedTemplate?.title
                                                )
                                                android.util.Log.d("ActionDialog", "Result: ${result.success}, contactResults: ${result.contactResults.size}")
                                            }
                                        }
                                        ActionType.SMS -> {
                                            actionExecutor.sendSMS(
                                                taskId,
                                                selectedContacts,
                                                messageText,
                                                selectedTemplate?.title
                                            )
                                        }
                                        ActionType.EMAIL -> {
                                            android.util.Log.d("ActionDialog", "EMAIL action selected")
                                            if (personalizeMessages) {
                                                android.util.Log.d("ActionDialog", "Personalizing email messages...")
                                                val messages = placeholderResolver.resolveForContacts(
                                                    messageText,
                                                    taskId,
                                                    selectedContacts.map { it.id }
                                                )
                                                android.util.Log.d("ActionDialog", "Resolved ${messages.size} personalized messages")
                                                // For email, send to all with first personalized message
                                                // (Email can't send different messages to different recipients in one intent)
                                                val personalizedBody = messages[selectedContacts.first().id] ?: messageText
                                                android.util.Log.d("ActionDialog", "Using personalized body: $personalizedBody")
                                                actionExecutor.sendEmail(
                                                    taskId,
                                                    selectedContacts,
                                                    emailSubject,
                                                    personalizedBody,
                                                    selectedTemplate?.title
                                                )
                                            } else {
                                                android.util.Log.d("ActionDialog", "Sending non-personalized email")
                                                actionExecutor.sendEmail(
                                                    taskId,
                                                    selectedContacts,
                                                    emailSubject,
                                                    messageText,
                                                    selectedTemplate?.title
                                                )
                                            }
                                        }
                                        ActionType.CALL -> {
                                            actionExecutor.makeCall(taskId, selectedContacts.first())
                                        }
                                        ActionType.MEETING -> {
                                            android.util.Log.d("ActionDialog", "MEETING action selected")
                                            android.util.Log.d("ActionDialog", "  Title: $meetingTitle")
                                            android.util.Log.d("ActionDialog", "  Location: $meetingLocation")
                                            android.util.Log.d("ActionDialog", "  Date: $meetingDate")
                                            android.util.Log.d("ActionDialog", "  Duration: $meetingDuration min")
                                            android.util.Log.d("ActionDialog", "  Attendees: ${selectedContacts.map { it.displayName }}")

                                            val endTime = meetingDate.plusMinutes(meetingDuration.toLong())
                                            actionExecutor.createMeeting(
                                                taskId,
                                                selectedContacts,
                                                meetingTitle.ifBlank { "Meeting" },
                                                messageText.ifBlank { null },
                                                meetingDate,
                                                endTime,
                                                meetingLocation.ifBlank { null }
                                            )
                                        }
                                        null -> {}
                                    }
                                    onActionExecuted()
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ausführen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

enum class ActionType(val displayName: String) {
    WHATSAPP("WhatsApp"),
    SMS("SMS"),
    EMAIL("Email"),
    CALL("Anrufen"),
    MEETING("Meeting")
}
