package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaUsageEntity
import com.example.questflow.data.database.entity.MediaUsageType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsDialog(
    media: MediaLibraryEntity,
    usages: List<MediaUsageEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Datei-Details") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File information
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Dateiinformationen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            if (media.displayName.isNotBlank()) {
                                DetailRow("Anzeigename", media.displayName)
                            }
                            DetailRow("Dateiname", media.fileName)
                            if (media.description.isNotBlank()) {
                                DetailRow("Beschreibung", media.description)
                            }
                            if (media.tags.isNotBlank()) {
                                DetailRow("Tags", media.tags)
                            }
                            DetailRow("Typ", when (media.mediaType) {
                                com.example.questflow.data.database.entity.MediaType.IMAGE -> "Bild"
                                com.example.questflow.data.database.entity.MediaType.GIF -> "GIF"
                                com.example.questflow.data.database.entity.MediaType.AUDIO -> "Audio"
                            })
                            DetailRow("Größe", formatFileSize(media.fileSize))
                            DetailRow("Hochgeladen", dateFormat.format(Date(media.uploadedAt)))
                            DetailRow("MIME-Type", media.mimeType)
                        }
                    }
                }

                // Usage information
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Verwendung",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            if (usages.isEmpty()) {
                                Text(
                                    text = "Wird noch nicht verwendet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Text(
                                    text = "${usages.size} Verwendung${if (usages.size != 1) "en" else ""}:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // List of usages
                items(usages) { usage ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (usage.usageType) {
                                    MediaUsageType.COLLECTION_ITEM -> Icons.Default.Info
                                    MediaUsageType.SKILL_ICON -> Icons.Default.Info
                                    MediaUsageType.CATEGORY_ICON -> Icons.Default.Info
                                    MediaUsageType.OTHER -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (usage.usageType) {
                                        MediaUsageType.COLLECTION_ITEM -> "Collection Item"
                                        MediaUsageType.SKILL_ICON -> "Skill Icon"
                                        MediaUsageType.CATEGORY_ICON -> "Category Icon"
                                        MediaUsageType.OTHER -> "Andere"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "ID: ${usage.referenceId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                usage.categoryId?.let { catId ->
                                    Text(
                                        text = "Kategorie-ID: $catId",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
