package com.example.questflow.presentation.screens.timeblock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.data.database.dao.TimeBlockWithTags
import com.example.questflow.data.database.entity.TimeBlockEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBlockListScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimeBlockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredTimeBlocks by viewModel.filteredTimeBlocks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zeitblockierungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowOnlyActive(!uiState.showOnlyActive) }) {
                        Icon(
                            imageVector = if (uiState.showOnlyActive) Icons.Default.Settings else Icons.Default.Settings,
                            contentDescription = "Filter aktive"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, "Hinzufügen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            // Type Filter Row            Row(                modifier = Modifier                    .fillMaxWidth()                    .padding(horizontal = 16.dp),                horizontalArrangement = Arrangement.spacedBy(8.dp)            ) {                listOf("WORK", "BREAK", "MEETING", "VACATION", "PERSONAL").forEach { type ->                    FilterChip(                        selected = uiState.filterType == type,                        onClick = { viewModel.setFilterType(if (uiState.filterType == type) null else type) },                        label = { Text(type) }                    )                }            }
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Suchen...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            // List
            if (filteredTimeBlocks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Keine Zeitblockierungen vorhanden",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTimeBlocks, key = { it.timeBlock.id }) { item ->
                        TimeBlockCard(
                            timeBlockWithTags = item,
                            onEdit = { viewModel.showEditDialog(it) },
                            onDelete = { viewModel.showDeleteConfirmation(it) },
                            onToggleActive = { viewModel.toggleActiveStatus(it) }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (uiState.showDialog) {
            TimeBlockDialog(
                timeBlock = uiState.editingTimeBlock,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { timeBlock, tagIds ->
                    viewModel.saveTimeBlock(timeBlock, tagIds)
                }
            )
        }

        if (uiState.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirmation() },
                title = { Text("Zeitblockierung löschen?") },
                text = { Text("Diese Aktion kann nicht rückgängig gemacht werden.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteTimeBlock() }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        // Error Snackbar
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar (simplified)
                viewModel.clearError()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeBlockCard(
    timeBlockWithTags: TimeBlockWithTags,
    onEdit: (TimeBlockEntity) -> Unit,
    onDelete: (TimeBlockEntity) -> Unit,
    onToggleActive: (TimeBlockEntity) -> Unit
) {
    val timeBlock = timeBlockWithTags.timeBlock
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (timeBlock.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        timeBlock.type?.let { type ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = timeBlock.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    timeBlock.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Time Info
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (timeBlock.allDay) {
                            Chip("Ganztägig")
                        } else if (timeBlock.startTime != null && timeBlock.endTime != null) {
                            Chip("${timeBlock.startTime.take(5)} - ${timeBlock.endTime.take(5)}")
                        }

                        if (timeBlock.daysOfWeek != null) {
                            Chip("Wochentage")
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = timeBlock.isActive,
                        onCheckedChange = { onToggleActive(timeBlock) }
                    )

                    Row {
                        IconButton(onClick = { onEdit(timeBlock) }) {
                            Icon(Icons.Default.Edit, "Bearbeiten", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDelete(timeBlock) }) {
                            Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Tags
            if (timeBlockWithTags.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    timeBlockWithTags.tags.take(3).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (timeBlockWithTags.tags.size > 3) {
                        Text("+${timeBlockWithTags.tags.size - 3}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
