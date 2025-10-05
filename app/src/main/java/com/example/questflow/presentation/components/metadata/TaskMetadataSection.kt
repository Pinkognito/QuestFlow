package com.example.questflow.presentation.components.metadata

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.TaskMetadataItem

/**
 * Displays all metadata for a task with add/delete functionality
 */
@Composable
fun TaskMetadataSection(
    metadata: List<TaskMetadataItem>,
    onAddClick: () -> Unit,
    onDeleteClick: (TaskMetadataItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Metadaten (${metadata.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Metadaten hinzufügen"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata items
        if (metadata.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Keine Metadaten vorhanden.\nFüge Kontakte, Orte, Notizen und mehr hinzu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(
                    items = metadata,
                    key = { it.metadataId }
                ) { item ->
                    MetadataItemCard(
                        item = item,
                        onDeleteClick = { onDeleteClick(item) }
                    )
                }
            }
        }
    }
}
