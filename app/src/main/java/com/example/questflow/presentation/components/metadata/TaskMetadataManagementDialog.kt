package com.example.questflow.presentation.components.metadata

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.presentation.viewmodels.TaskMetadataViewModel

/**
 * Full-screen dialog for managing task metadata
 * Can be called from edit screens or detail views
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskMetadataManagementDialog(
    taskId: Long,
    taskTitle: String,
    onDismiss: () -> Unit,
    viewModel: TaskMetadataViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val metadata by viewModel.metadataItems.collectAsState()

    // Set task ID when dialog opens
    LaunchedEffect(taskId) {
        viewModel.setTaskId(taskId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Metadaten verwalten")
                Text(
                    text = taskTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(min = 200.dp, max = 500.dp)) {
                TaskMetadataSection(
                    metadata = metadata,
                    onAddClick = { showAddDialog = true },
                    onDeleteClick = { item ->
                        viewModel.deleteMetadata(item)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fertig")
            }
        }
    )

    // Add metadata dialog
    if (showAddDialog) {
        AddMetadataDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { item ->
                viewModel.addMetadata(item)
            }
        )
    }
}
