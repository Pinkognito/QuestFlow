package com.example.questflow.presentation.components.metadata

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.TaskMetadataItem

/**
 * Displays a single metadata item with type-specific rendering
 */
@Composable
fun MetadataItemCard(
    item: TaskMetadataItem,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon based on type
                Icon(
                    imageVector = when (item) {
                        is TaskMetadataItem.Location -> Icons.Default.LocationOn
                        is TaskMetadataItem.Contact -> Icons.Default.Person
                        is TaskMetadataItem.Phone -> Icons.Default.Phone
                        is TaskMetadataItem.Address -> Icons.Default.Home
                        is TaskMetadataItem.Email -> Icons.Default.Email
                        is TaskMetadataItem.Url -> Icons.Default.Star
                        is TaskMetadataItem.Note -> Icons.Default.Info
                        is TaskMetadataItem.FileAttachment -> Icons.Default.Settings
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                // Content based on type
                Column {
                    when (item) {
                        is TaskMetadataItem.Location -> {
                            Text(
                                text = item.location.placeName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            item.location.formattedAddress?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is TaskMetadataItem.Contact -> {
                            Text(
                                text = item.contact.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            item.contact.primaryPhone?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is TaskMetadataItem.Phone -> {
                            Text(
                                text = item.phone.phoneNumber,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.phone.phoneType.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is TaskMetadataItem.Address -> {
                            Text(
                                text = "${item.address.street}, ${item.address.city}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.address.postalCode}, ${item.address.country}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is TaskMetadataItem.Email -> {
                            Text(
                                text = item.email.emailAddress,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.email.emailType.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is TaskMetadataItem.Url -> {
                            Text(
                                text = item.url.title ?: "Link",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.url.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        is TaskMetadataItem.Note -> {
                            Text(
                                text = "Notiz",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.note.content.take(50) + if (item.note.content.length > 50) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is TaskMetadataItem.FileAttachment -> {
                            Text(
                                text = item.file.fileName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.file.fileSize / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
