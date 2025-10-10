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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.questflow.data.database.entity.TextTemplateEntity
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.viewmodels.TextTemplateViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTemplateLibraryScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: TextTemplateViewModel = hiltViewModel()
) {
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()
    var previousXp by remember { mutableStateOf(globalStats?.xp ?: 0L) }

    val filteredTemplates by viewModel.filteredTemplates.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TextTemplateEntity?>(null) }

    Scaffold(
        topBar = {
            QuestFlowTopBar(
                title = "Textbausteine",
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = appViewModel::selectCategory,
                onManageCategoriesClick = { navController.navigate("categories") },
                level = globalStats?.level ?: 1,
                totalXp = globalStats?.xp ?: 0,
                previousXp = previousXp,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Erstellen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Suchen...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, "Löschen")
                        }
                    }
                },
                singleLine = true
            )

            // Templates List
            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Keine Textbausteine vorhanden"
                        else "Keine Ergebnisse gefunden",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onEdit = { editingTemplate = template },
                            onDelete = { viewModel.deleteTemplate(template) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Create/Edit Dialog
    if (showCreateDialog || editingTemplate != null) {
        TextTemplateEditorDialog(
            template = editingTemplate,
            viewModel = viewModel,
            onDismiss = {
                showCreateDialog = false
                editingTemplate = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCard(
    template: TextTemplateEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    viewModel: TextTemplateViewModel
) {
    var showMenu by remember { mutableStateOf(false) }
    var templateTags by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(template.id) {
        templateTags = viewModel.getTagsForTemplate(template.id)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Optionen")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Bearbeiten") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Löschen") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }

            template.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = template.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Tags
            if (templateTags.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    templateTags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { viewModel.setTagFilter(tag) },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (templateTags.size > 3) {
                        Text("+${templateTags.size - 3}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Usage Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Verwendet: ${template.usageCount}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                template.lastUsedAt?.let {
                    Text(
                        text = "Zuletzt: ${it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTemplateEditorDialog(
    template: TextTemplateEntity?,
    viewModel: TextTemplateViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(template?.title ?: "") }
    var subject by remember { mutableStateOf(template?.subject ?: "") }
    var contentField by remember { mutableStateOf(TextFieldValue(template?.content ?: "")) }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }
    var showPlaceholderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(template) {
        if (template != null) {
            tags = viewModel.getTagsForTemplate(template.id)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "Textbaustein erstellen" else "Bearbeiten") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titel") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Betreff (optional, für E-Mails/Termine)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Beschreibung (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = contentField,
                        onValueChange = { contentField = it },
                        label = { Text("Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 8,
                        trailingIcon = {
                            IconButton(onClick = { showPlaceholderDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Platzhalter einfügen",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
                item {
                    Text(
                        "Tipp: Drücke '+' um Platzhalter einzufügen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            label = { Text("Tag hinzufügen") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (tagInput.isNotBlank() && tagInput !in tags) {
                                    tags = tags + tagInput
                                    tagInput = ""
                                }
                            }
                        ) {
                            Text("+ ")
                        }
                    }
                }
                items(tags) { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, modifier = Modifier.weight(1f))
                        IconButton(onClick = { tags = tags - tag }) {
                            Icon(Icons.Default.Close, "Entfernen")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val content = contentField.text
                    if (title.isNotBlank() && content.isNotBlank()) {
                        if (template == null) {
                            viewModel.createTemplate(
                                title = title,
                                content = content,
                                description = description.ifBlank { null },
                                subject = subject.ifBlank { null },
                                tags = tags
                            )
                        } else {
                            viewModel.updateTemplate(
                                template.copy(
                                    title = title,
                                    content = content,
                                    description = description.ifBlank { null },
                                    subject = subject.ifBlank { null }
                                ),
                                tags
                            )
                        }
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && contentField.text.isNotBlank()
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

    // Platzhalter-Auswahl Dialog
    if (showPlaceholderDialog) {
        com.example.questflow.presentation.components.PlaceholderSelectorDialog(
            onDismiss = { showPlaceholderDialog = false },
            onPlaceholderSelected = { placeholder ->
                // Füge Platzhalter an Cursor-Position ein
                val currentText = contentField.text
                val selection = contentField.selection
                val newText = currentText.substring(0, selection.start) +
                              placeholder +
                              currentText.substring(selection.end)

                contentField = TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(
                        selection.start + placeholder.length
                    )
                )
            }
        )
    }
}
