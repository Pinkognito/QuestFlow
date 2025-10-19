package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Data class for media metadata
 */
data class MediaMetadata(
    val displayName: String = "",
    val description: String = "",
    val tags: String = ""
)

/**
 * Dialog for entering/editing media metadata
 *
 * @param initialMetadata Initial metadata values (for editing existing media)
 * @param title Dialog title
 * @param confirmText Confirm button text
 * @param onDismiss Called when dialog is dismissed
 * @param onConfirm Called when user confirms with metadata
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaMetadataDialog(
    initialMetadata: MediaMetadata = MediaMetadata(),
    title: String = "Metadaten hinzufügen",
    confirmText: String = "Bestätigen",
    onDismiss: () -> Unit,
    onConfirm: (MediaMetadata) -> Unit
) {
    var displayName by remember { mutableStateOf(initialMetadata.displayName) }
    var description by remember { mutableStateOf(initialMetadata.description) }
    var tags by remember { mutableStateOf(initialMetadata.tags) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info text
                Text(
                    text = "Füge optionale Metadaten hinzu. Diese helfen dir später beim Filtern und Organisieren deiner Dateien.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Display Name Field
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Anzeigename",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("z.B. \"Mein Lieblingsbild\"") },
                        singleLine = true
                    )
                    Text(
                        text = "Benutzerdefinierter Name für die Datei",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Description Field
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Beschreibung",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("z.B. \"Aufgenommen im Urlaub 2025\"") },
                        minLines = 2,
                        maxLines = 4
                    )
                    Text(
                        text = "Zusätzliche Informationen zur Datei",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tags Field
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("z.B. \"urlaub, strand, sommer\"") },
                        singleLine = true
                    )
                    Text(
                        text = "Komma-getrennte Schlagwörter für die Suche",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Note about optional fields
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Alle Felder sind optional. Du kannst sie jederzeit später bearbeiten.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        MediaMetadata(
                            displayName = displayName.trim(),
                            description = description.trim(),
                            tags = tags.trim()
                        )
                    )
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
