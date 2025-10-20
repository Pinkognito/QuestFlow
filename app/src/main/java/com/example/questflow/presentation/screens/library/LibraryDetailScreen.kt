package com.example.questflow.presentation.screens.library

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.questflow.data.database.entity.*
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.viewmodels.MetadataLibraryViewModel

/**
 * Generic detail screen for a specific library type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDetailScreen(
    type: String,
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: MetadataLibraryViewModel = hiltViewModel()
) {
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()

    var previousXp by remember { mutableStateOf(globalStats?.xp ?: 0L) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportContactDialog by remember { mutableStateOf(false) }

    LaunchedEffect(globalStats?.xp) {
        globalStats?.xp?.let { currentXp ->
            if (currentXp != previousXp) {
                previousXp = currentXp
            }
        }
    }

    val (title, icon) = when (type) {
        "locations" -> "Standorte" to Icons.Default.LocationOn
        "contacts" -> "Kontakte" to Icons.Default.Person
        "phones" -> "Telefone" to Icons.Default.Phone
        "addresses" -> "Adressen" to Icons.Default.Home
        "emails" -> "E-Mails" to Icons.Default.Email
        "urls" -> "Links" to Icons.Default.Star
        "notes" -> "Notizen" to Icons.Default.Info
        "files" -> "Dateien" to Icons.Default.Settings
        else -> "Bibliothek" to Icons.Default.Star
    }

    Scaffold(
        topBar = {
            QuestFlowTopBar(
                title = title,
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = appViewModel::selectCategory,
                onManageCategoriesClick = {
                    navController.navigate("categories")
                },
                level = globalStats?.level ?: 1,
                totalXp = globalStats?.xp ?: 0,
                previousXp = previousXp,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB for implemented types
            if (type in listOf("locations", "phones", "notes", "contacts")) {
                FloatingActionButton(
                    onClick = {
                        // For contacts, show import dialog instead of manual add
                        if (type == "contacts") {
                            showImportContactDialog = true
                        } else {
                            showAddDialog = true
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, "Hinzufügen")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (type) {
                "locations" -> LocationsList(viewModel)
                "phones" -> PhonesList(viewModel)
                "notes" -> NotesList(viewModel)
                "contacts" -> ContactsList(viewModel, navController)
                else -> EmptyPlaceholder(title)
            }
        }
    }

    // Show appropriate add dialog based on type
    if (showAddDialog) {
        when (type) {
            "locations" -> AddLocationDialog(
                onDismiss = { showAddDialog = false },
                onSave = { location -> viewModel.addLocation(location) }
            )
            "phones" -> AddPhoneDialog(
                onDismiss = { showAddDialog = false },
                onSave = { phone -> viewModel.addPhone(phone) }
            )
            "notes" -> AddNoteDialog(
                onDismiss = { showAddDialog = false },
                onSave = { note -> viewModel.addNote(note) }
            )
        }
    }

    // Show import contact dialog
    if (showImportContactDialog) {
        ImportContactDialog(
            onDismiss = { showImportContactDialog = false },
            onContactSelected = { contactData ->
                viewModel.importContact(contactData)
            }
        )
    }
}

@Composable
private fun LocationsList(viewModel: MetadataLibraryViewModel) {
    val locations by viewModel.locations.collectAsState()

    if (locations.isEmpty()) {
        EmptyState("Keine Standorte vorhanden")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(locations) { location ->
                LocationCard(
                    location = location,
                    onDelete = { viewModel.deleteLocation(location) }
                )
            }
        }
    }
}

@Composable
private fun PhonesList(viewModel: MetadataLibraryViewModel) {
    val phones by viewModel.phoneNumbers.collectAsState()

    if (phones.isEmpty()) {
        EmptyState("Keine Telefonnummern vorhanden")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(phones) { phone ->
                PhoneCard(
                    phone = phone,
                    onDelete = { viewModel.deletePhone(phone) }
                )
            }
        }
    }
}

@Composable
private fun NotesList(viewModel: MetadataLibraryViewModel) {
    val notes by viewModel.notes.collectAsState()

    if (notes.isEmpty()) {
        EmptyState("Keine Notizen vorhanden")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                NoteCard(
                    note = note,
                    onDelete = { viewModel.deleteNote(note) }
                )
            }
        }
    }
}

@Composable
private fun ContactsList(
    viewModel: MetadataLibraryViewModel,
    navController: NavController
) {
    val contacts by viewModel.contacts.collectAsState()
    val allMedia by viewModel.allMedia.collectAsState()

    if (contacts.isEmpty()) {
        EmptyState("Keine Kontakte vorhanden")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts) { contact ->
                ContactCard(
                    contact = contact,
                    allMedia = allMedia,
                    onDelete = { viewModel.deleteContact(contact) },
                    onClick = { navController.navigate("contact_detail/${contact.id}") }
                )
            }
        }
    }
}

// Card Components
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

@Composable
private fun ContactCard(
    contact: MetadataContactEntity,
    allMedia: List<MediaLibraryEntity>,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
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
                // Photo from Media Library or fallback to photoUri
                val photoMedia = contact.photoMediaId?.let { mediaId ->
                    allMedia.find { it.id == mediaId }
                }

                if (photoMedia != null) {
                    AsyncImage(
                        model = java.io.File(photoMedia.filePath),
                        contentDescription = "Kontaktfoto",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (contact.photoUri != null) {
                    AsyncImage(
                        model = Uri.parse(contact.photoUri),
                        contentDescription = "Kontaktfoto",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Column {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    contact.organization?.let {
                        Text(
                            text = it,
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

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPlaceholder(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title (In Entwicklung)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
