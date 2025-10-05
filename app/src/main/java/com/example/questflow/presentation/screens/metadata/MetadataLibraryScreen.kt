package com.example.questflow.presentation.screens.metadata

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.questflow.data.database.entity.*
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.viewmodels.MetadataLibraryViewModel

/**
 * Main screen for managing the Metadata Library
 * Displays tabs for different metadata types with CRUD operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataLibraryScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: MetadataLibraryViewModel = hiltViewModel(),
    showTopBar: Boolean = true
) {
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var previousXp by remember { mutableStateOf(globalStats?.xp ?: 0L) }

    // Track XP changes for animation
    LaunchedEffect(globalStats?.xp) {
        globalStats?.xp?.let { currentXp ->
            if (currentXp != previousXp) {
                previousXp = currentXp
            }
        }
    }

    val tabs = listOf(
        MetadataTab("Standorte", Icons.Default.LocationOn, MetadataType.LOCATION),
        MetadataTab("Kontakte", Icons.Default.Person, MetadataType.CONTACT),
        MetadataTab("Telefone", Icons.Default.Phone, MetadataType.PHONE),
        MetadataTab("Adressen", Icons.Default.Home, MetadataType.ADDRESS),
        MetadataTab("E-Mails", Icons.Default.Email, MetadataType.EMAIL),
        MetadataTab("Links", Icons.Default.Star, MetadataType.URL),
        MetadataTab("Notizen", Icons.Default.Info, MetadataType.NOTE),
        MetadataTab("Dateien", Icons.Default.Settings, MetadataType.FILE_ATTACHMENT)
    )

    Scaffold(
        topBar = if (showTopBar) {
            {
                Column {
                    QuestFlowTopBar(
                        title = "Metadaten-Bibliothek",
                        selectedCategory = selectedCategory,
                        categories = categories,
                        onCategorySelected = appViewModel::selectCategory,
                        onManageCategoriesClick = {
                            navController.navigate("categories")
                        },
                        level = globalStats?.level ?: 1,
                        totalXp = globalStats?.xp ?: 0,
                        previousXp = previousXp
                    )

                    // Scrollable Tab Row
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(tab.title) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) }
                            )
                        }
                    }
                }
            }
        } else {
            {
                // Just the tab row when embedded
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(tab.title) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (tabs[selectedTabIndex].type) {
                MetadataType.LOCATION -> LocationTab(viewModel)
                MetadataType.CONTACT -> ContactTab(viewModel)
                MetadataType.PHONE -> PhoneTab(viewModel)
                MetadataType.ADDRESS -> AddressTab(viewModel)
                MetadataType.EMAIL -> EmailTab(viewModel)
                MetadataType.URL -> UrlTab(viewModel)
                MetadataType.NOTE -> NoteTab(viewModel)
                MetadataType.FILE_ATTACHMENT -> FileTab(viewModel)
            }
        }
    }
}

private data class MetadataTab(
    val title: String,
    val icon: ImageVector,
    val type: MetadataType
)

// Location Tab
@Composable
private fun LocationTab(viewModel: MetadataLibraryViewModel) {
    val locations by viewModel.locations.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    MetadataTabContent(
        items = locations,
        onAddClick = { showAddDialog = true },
        emptyText = "Keine Standorte vorhanden",
        itemContent = { location ->
            LocationCard(
                location = location,
                onDelete = { viewModel.deleteLocation(location) }
            )
        }
    )

    if (showAddDialog) {
        LocationFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { location ->
                viewModel.addLocation(location)
                showAddDialog = false
            }
        )
    }
}

// Phone Tab
@Composable
private fun PhoneTab(viewModel: MetadataLibraryViewModel) {
    val phoneNumbers by viewModel.phoneNumbers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    MetadataTabContent(
        items = phoneNumbers,
        onAddClick = { showAddDialog = true },
        emptyText = "Keine Telefonnummern vorhanden",
        itemContent = { phone ->
            PhoneCard(
                phone = phone,
                onDelete = { viewModel.deletePhone(phone) }
            )
        }
    )

    if (showAddDialog) {
        PhoneFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { phone ->
                viewModel.addPhone(phone)
                showAddDialog = false
            }
        )
    }
}

// Note Tab
@Composable
private fun NoteTab(viewModel: MetadataLibraryViewModel) {
    val notes by viewModel.notes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    MetadataTabContent(
        items = notes,
        onAddClick = { showAddDialog = true },
        emptyText = "Keine Notizen vorhanden",
        itemContent = { note ->
            NoteCard(
                note = note,
                onDelete = { viewModel.deleteNote(note) }
            )
        }
    )

    if (showAddDialog) {
        NoteFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { note ->
                viewModel.addNote(note)
                showAddDialog = false
            }
        )
    }
}

// Contact Tab (Placeholder)
@Composable
private fun ContactTab(viewModel: MetadataLibraryViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Kontakte-Verwaltung (In Entwicklung)")
    }
}

// Address Tab (Placeholder)
@Composable
private fun AddressTab(viewModel: MetadataLibraryViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Adressen-Verwaltung (In Entwicklung)")
    }
}

// Email Tab (Placeholder)
@Composable
private fun EmailTab(viewModel: MetadataLibraryViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("E-Mail-Verwaltung (In Entwicklung)")
    }
}

// URL Tab (Placeholder)
@Composable
private fun UrlTab(viewModel: MetadataLibraryViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Link-Verwaltung (In Entwicklung)")
    }
}

// File Tab (Placeholder)
@Composable
private fun FileTab(viewModel: MetadataLibraryViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Datei-Verwaltung (In Entwicklung)")
    }
}

// Generic Tab Content Layout
@Composable
private fun <T> MetadataTabContent(
    items: List<T>,
    onAddClick: () -> Unit,
    emptyText: String,
    itemContent: @Composable (T) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hinzufügen")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    itemContent(item)
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
        }
    }
}

// Location Card
@Composable
private fun LocationCard(
    location: MetadataLocationEntity,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = location.placeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    location.formattedAddress?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (location.latitude != 0.0 && location.longitude != 0.0) {
                        Text(
                            text = "${location.latitude}, ${location.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Phone Card
@Composable
private fun PhoneCard(
    phone: MetadataPhoneEntity,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = phone.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = phone.phoneType.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Note Card
@Composable
private fun NoteCard(
    note: MetadataNoteEntity,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = note.content.take(100) + if (note.content.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = note.format.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Location Form Dialog
@Composable
private fun LocationFormDialog(
    location: MetadataLocationEntity? = null,
    onDismiss: () -> Unit,
    onSave: (MetadataLocationEntity) -> Unit
) {
    var placeName by remember { mutableStateOf(location?.placeName ?: "") }
    var address by remember { mutableStateOf(location?.formattedAddress ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (location == null) "Standort hinzufügen" else "Standort bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = placeName,
                    onValueChange = { placeName = it },
                    label = { Text("Ortsname") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adresse (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (placeName.isNotBlank()) {
                        onSave(
                            MetadataLocationEntity(
                                id = location?.id ?: 0,
                                placeName = placeName,
                                latitude = 0.0,
                                longitude = 0.0,
                                formattedAddress = address.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = placeName.isNotBlank()
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

// Phone Form Dialog
@Composable
private fun PhoneFormDialog(
    phone: MetadataPhoneEntity? = null,
    onDismiss: () -> Unit,
    onSave: (MetadataPhoneEntity) -> Unit
) {
    var phoneNumber by remember { mutableStateOf(phone?.phoneNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (phone == null) "Telefonnummer hinzufügen" else "Telefonnummer bearbeiten") },
        text = {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Telefonnummer") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (phoneNumber.isNotBlank()) {
                        onSave(
                            MetadataPhoneEntity(
                                id = phone?.id ?: 0,
                                phoneNumber = phoneNumber,
                                phoneType = PhoneType.MOBILE
                            )
                        )
                    }
                },
                enabled = phoneNumber.isNotBlank()
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

// Note Form Dialog
@Composable
private fun NoteFormDialog(
    note: MetadataNoteEntity? = null,
    onDismiss: () -> Unit,
    onSave: (MetadataNoteEntity) -> Unit
) {
    var noteContent by remember { mutableStateOf(note?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "Notiz hinzufügen" else "Notiz bearbeiten") },
        text = {
            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                label = { Text("Notizinhalt") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 8
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (noteContent.isNotBlank()) {
                        onSave(
                            MetadataNoteEntity(
                                id = note?.id ?: 0,
                                content = noteContent,
                                format = NoteFormat.PLAIN_TEXT
                            )
                        )
                    }
                },
                enabled = noteContent.isNotBlank()
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
