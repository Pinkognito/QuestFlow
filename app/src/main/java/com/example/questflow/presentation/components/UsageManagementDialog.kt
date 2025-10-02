package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.MediaUsageEntity
import com.example.questflow.data.database.entity.MediaUsageType

/**
 * Dialog to manage media usages (view, edit collection item metadata, delete)
 *
 * @param mediaFileName File name for display
 * @param usages List of usages
 * @param categories Map of category IDs to names
 * @param onDismiss Called when dialog is dismissed
 * @param onDeleteUsage Called when a usage should be deleted
 * @param onEditCollectionItem Called when collection item metadata should be edited
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageManagementDialog(
    mediaFileName: String,
    usages: List<MediaUsageEntity>,
    categories: Map<Long, String> = emptyMap(),
    onDismiss: () -> Unit,
    onDeleteUsage: (MediaUsageEntity) -> Unit,
    onEditCollectionItem: (referenceId: Long, categoryId: Long?) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf<MediaUsageEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
        title = { Text("Verwendungen verwalten") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                Text(
                    text = "Datei: $mediaFileName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${usages.size} Verwendung${if (usages.size != 1) "en" else ""}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usages) { usage ->
                        UsageItem(
                            usage = usage,
                            categoryName = usage.categoryId?.let { categories[it] } ?: "Global",
                            onDelete = { showDeleteConfirmation = usage },
                            onEdit = {
                                if (usage.usageType == MediaUsageType.COLLECTION_ITEM) {
                                    onEditCollectionItem(usage.referenceId, usage.categoryId)
                                }
                            }
                        )
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

    // Delete confirmation dialog
    showDeleteConfirmation?.let { usage ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Verwendung entfernen?") },
            text = {
                Text("Möchtest du diese Verwendung wirklich entfernen? Die Datei bleibt in der Mediathek erhalten.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUsage(usage)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Entfernen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun UsageItem(
    usage: MediaUsageEntity,
    categoryName: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (usage.usageType) {
                        MediaUsageType.COLLECTION_ITEM -> "Collection Item"
                        MediaUsageType.SKILL_ICON -> "Skill Icon"
                        MediaUsageType.CATEGORY_ICON -> "Category Icon"
                        MediaUsageType.OTHER -> "Andere"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kategorie: $categoryName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ID: ${usage.referenceId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Edit button (only for collection items)
                if (usage.usageType == MediaUsageType.COLLECTION_ITEM) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Bearbeiten",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
