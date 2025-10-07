package com.example.questflow.presentation.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.data.database.entity.MetadataTagEntity
import com.example.questflow.data.database.entity.TagType
import com.example.questflow.presentation.viewmodels.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    viewModel: TagViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val tags by viewModel.filteredTags.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<MetadataTagEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags verwalten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Tag erstellen")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Type Filter Chips
            ScrollableTabRow(
                selectedTabIndex = TagType.values().indexOf(selectedType ?: TagType.CONTACT),
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedType == null,
                    onClick = { viewModel.filterByType(null) },
                    text = { Text("Alle") }
                )
                TagType.values().forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { viewModel.filterByType(type) },
                        text = { Text(type.name) }
                    )
                }
            }

            // Tags List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    TagCard(
                        tag = tag,
                        onEdit = { editingTag = tag },
                        onDelete = { viewModel.deleteTag(tag) }
                    )
                }
            }
        }
    }

    // Create/Edit Dialog
    if (showCreateDialog || editingTag != null) {
        TagEditDialog(
            tag = editingTag,
            onDismiss = {
                showCreateDialog = false
                editingTag = null
            },
            onSave = { name, type, color, description ->
                if (editingTag != null) {
                    viewModel.updateTag(
                        editingTag!!.copy(
                            name = name,
                            type = type,
                            color = color,
                            description = description
                        )
                    )
                } else {
                    viewModel.createTag(name, type, color = color, description = description)
                }
                showCreateDialog = false
                editingTag = null
            }
        )
    }

    // Snackbar for messages
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearMessages()
        }
    }
}

@Composable
fun TagCard(
    tag: MetadataTagEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    tag.color?.let { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color(android.graphics.Color.parseColor(colorHex)),
                                shape = MaterialTheme.shapes.small
                            ) {}
                        }
                    }
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = tag.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                tag.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Bearbeiten")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Löschen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditDialog(
    tag: MetadataTagEntity?,
    onDismiss: () -> Unit,
    onSave: (String, TagType, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(tag?.name ?: "") }
    var type by remember { mutableStateOf(tag?.type ?: TagType.GENERAL) }
    var color by remember { mutableStateOf(tag?.color ?: "#2196F3") }
    var description by remember { mutableStateOf(tag?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tag == null) "Tag erstellen" else "Tag bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Type Dropdown
                var expandedType by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Typ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        TagType.values().forEach { tagType ->
                            DropdownMenuItem(
                                text = { Text(tagType.name) },
                                onClick = {
                                    type = tagType
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Farbe (Hex)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, type, color, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
