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
    placeholderResolver: PlaceholderResolver
) {
    var selectedAction by remember { mutableStateOf<ActionType?>(null) }
    var selectedTemplate by remember { mutableStateOf<TextTemplateEntity?>(null) }
    var messageText by remember { mutableStateOf("") }
    var personalizeMessages by remember { mutableStateOf(true) }
    var emailSubject by remember { mutableStateOf("") }

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
                    if (selectedAction in listOf(ActionType.WHATSAPP, ActionType.SMS, ActionType.EMAIL)) {
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
                                            expanded = false
                                        }
                                    )
                                    availableTemplates.forEach { template ->
                                        DropdownMenuItem(
                                            text = { Text(template.title) },
                                            onClick = {
                                                selectedTemplate = template
                                                messageText = template.content
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
                                                // Send personalized messages
                                                selectedContacts.forEach { contact ->
                                                    android.util.Log.d("ActionDialog", "Sending WhatsApp to ${contact.displayName}")
                                                    val result = actionExecutor.sendWhatsAppMessage(
                                                        taskId,
                                                        listOf(contact),
                                                        messages[contact.id] ?: messageText,
                                                        selectedTemplate?.title
                                                    )
                                                    android.util.Log.d("ActionDialog", "Result: ${result.success}")
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
                                            actionExecutor.sendEmail(
                                                taskId,
                                                selectedContacts,
                                                emailSubject,
                                                messageText,
                                                selectedTemplate?.title
                                            )
                                        }
                                        ActionType.CALL -> {
                                            actionExecutor.makeCall(taskId, selectedContacts.first())
                                        }
                                        ActionType.MEETING -> {
                                            actionExecutor.createMeeting(
                                                taskId,
                                                selectedContacts,
                                                "Meeting", // Would need UI input
                                                messageText,
                                                LocalDateTime.now().plusHours(1),
                                                LocalDateTime.now().plusHours(2),
                                                null
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
