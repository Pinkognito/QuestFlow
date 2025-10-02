package com.example.questflow.presentation.screens.medialibrary

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.MediaDetailsDialog
import com.example.questflow.presentation.components.MediaViewerDialog
import com.example.questflow.presentation.components.QuestFlowTopBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: MediaLibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()

    var selectedFilter by remember { mutableStateOf<MediaType?>(null) } // null = Alle
    var showDeleteDialog by remember { mutableStateOf<MediaLibraryEntity?>(null) }
    var showUploadMenu by remember { mutableStateOf(false) }
    var showMediaViewer by remember { mutableStateOf<MediaLibraryEntity?>(null) }

    // State for metadata dialog
    var showMetadataDialog by remember { mutableStateOf(false) }
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadUris by remember { mutableStateOf<List<Uri>?>(null) }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }

    // State for editing metadata
    var showEditMetadataDialog by remember { mutableStateOf(false) }
    var mediaToEdit by remember { mutableStateOf<MediaLibraryEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Single file picker
    val singleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingUploadUri = it
            showMetadataDialog = true
        }
    }

    // Multiple files picker
    val multipleFilesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingUploadUris = uris
            showMetadataDialog = true
        }
    }

    // ZIP file picker
    val zipFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingZipUri = it
            showMetadataDialog = true
        }
    }

    // Show notifications
    LaunchedEffect(uiState.notification) {
        uiState.notification?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            QuestFlowTopBar(
                title = "Mediathek",
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = appViewModel::selectCategory,
                onManageCategoriesClick = { navController.navigate("categories") },
                level = globalStats?.level ?: 1,
                totalXp = globalStats?.xp ?: 0
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Upload options menu
                if (showUploadMenu) {
                    // ZIP Upload
                    SmallFloatingActionButton(
                        onClick = {
                            showUploadMenu = false
                            zipFilePicker.launch("application/zip")
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("ZIP", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Multiple Files Upload
                    SmallFloatingActionButton(
                        onClick = {
                            showUploadMenu = false
                            multipleFilesPicker.launch("*/*")
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Mehrere", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Single File Upload
                    SmallFloatingActionButton(
                        onClick = {
                            showUploadMenu = false
                            singleFilePicker.launch("*/*")
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Einzeln", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Main FAB
                FloatingActionButton(
                    onClick = { showUploadMenu = !showUploadMenu }
                ) {
                    Icon(
                        if (showUploadMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Upload"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            var searchQuery by remember { mutableStateOf("") }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Nach Name, Tags oder Beschreibung suchen...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Suche löschen")
                        }
                    }
                },
                singleLine = true
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Alle Filter
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Alle") },
                    leadingIcon = if (selectedFilter == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )

                // Bilder Filter
                FilterChip(
                    selected = selectedFilter == MediaType.IMAGE,
                    onClick = { selectedFilter = MediaType.IMAGE },
                    label = { Text("Bilder") },
                    leadingIcon = if (selectedFilter == MediaType.IMAGE) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )

                // GIFs Filter
                FilterChip(
                    selected = selectedFilter == MediaType.GIF,
                    onClick = { selectedFilter = MediaType.GIF },
                    label = { Text("GIFs") },
                    leadingIcon = if (selectedFilter == MediaType.GIF) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )

                // Audio Filter
                FilterChip(
                    selected = selectedFilter == MediaType.AUDIO,
                    onClick = { selectedFilter = MediaType.AUDIO },
                    label = { Text("Audio") },
                    leadingIcon = if (selectedFilter == MediaType.AUDIO) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }

            // Tags filter
            val availableTags = remember(uiState.allMedia) {
                uiState.allMedia
                    .flatMap { it.tags.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
            }

            var selectedTag by remember { mutableStateOf<String?>(null) }

            if (availableTags.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Nach Tag filtern:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = { Text("Alle Tags") }
                            )
                        }
                        items(availableTags.take(10)) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }

            // Storage info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Text(
                        text = "Gespeichert: ${uiState.mediaCount} Dateien",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatFileSize(uiState.totalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Media grid with filtering
            val filteredMedia = remember(uiState.allMedia, selectedFilter, searchQuery, selectedTag) {
                var media = uiState.allMedia

                // Apply type filter
                if (selectedFilter != null) {
                    media = media.filter { it.mediaType == selectedFilter }
                }

                // Apply search query (partial string matching)
                if (searchQuery.isNotBlank()) {
                    val query = searchQuery.lowercase()
                    media = media.filter { item ->
                        item.displayName.lowercase().contains(query) ||
                        item.fileName.lowercase().contains(query) ||
                        item.description.lowercase().contains(query) ||
                        item.tags.lowercase().contains(query)
                    }
                }

                // Apply tag filter
                if (selectedTag != null) {
                    media = media.filter { it.tags.contains(selectedTag!!) }
                }

                media
            }

            if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (selectedFilter == null) {
                                "Keine Dateien vorhanden"
                            } else {
                                "Keine ${when (selectedFilter) {
                                    MediaType.IMAGE -> "Bilder"
                                    MediaType.GIF -> "GIFs"
                                    MediaType.AUDIO -> "Audio-Dateien"
                                    else -> "Dateien"
                                }} vorhanden"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tippe auf + um Dateien hochzuladen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMedia) { media ->
                        MediaGridItem(
                            media = media,
                            onDelete = { showDeleteDialog = media },
                            onClick = { showMediaViewer = media },
                            onEdit = {
                                mediaToEdit = media
                                showEditMetadataDialog = true
                            },
                            onShowInfo = {
                                viewModel.loadMediaDetails(media.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { media ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Datei löschen?") },
            text = {
                Text("Möchtest du \"${media.fileName}\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedia(context, media)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Media viewer dialog
    showMediaViewer?.let { media ->
        MediaViewerDialog(
            media = media,
            onDismiss = { showMediaViewer = null },
            onShowDetails = {
                showMediaViewer = null
                viewModel.loadMediaDetails(media.id)
            }
        )
    }

    // Media details dialog
    uiState.selectedMediaWithUsage?.let { mediaWithUsage ->
        MediaDetailsDialog(
            media = mediaWithUsage.media,
            usages = mediaWithUsage.usages,
            onDismiss = { viewModel.clearMediaDetails() }
        )
    }

    // Metadata dialog for upload
    if (showMetadataDialog) {
        com.example.questflow.presentation.components.MediaMetadataDialog(
            title = when {
                pendingUploadUri != null -> "Metadaten für Datei hinzufügen"
                pendingUploadUris != null -> "Metadaten für ${pendingUploadUris?.size} Dateien hinzufügen"
                pendingZipUri != null -> "Metadaten für ZIP-Inhalt hinzufügen"
                else -> "Metadaten hinzufügen"
            },
            confirmText = "Hochladen",
            onDismiss = {
                showMetadataDialog = false
                pendingUploadUri = null
                pendingUploadUris = null
                pendingZipUri = null
            },
            onConfirm = { metadata ->
                when {
                    pendingUploadUri != null -> {
                        viewModel.uploadMedia(
                            context = context,
                            uri = pendingUploadUri!!,
                            mediaType = MediaType.IMAGE,
                            displayName = metadata.displayName,
                            description = metadata.description,
                            tags = metadata.tags
                        )
                    }
                    pendingUploadUris != null -> {
                        viewModel.uploadMultipleMedia(
                            context = context,
                            uris = pendingUploadUris!!,
                            displayName = metadata.displayName,
                            description = metadata.description,
                            tags = metadata.tags
                        )
                    }
                    pendingZipUri != null -> {
                        viewModel.uploadFromZip(
                            context = context,
                            zipUri = pendingZipUri!!,
                            displayName = metadata.displayName,
                            description = metadata.description,
                            tags = metadata.tags
                        )
                    }
                }
                showMetadataDialog = false
                pendingUploadUri = null
                pendingUploadUris = null
                pendingZipUri = null
            }
        )
    }

    // Edit metadata dialog
    if (showEditMetadataDialog && mediaToEdit != null) {
        com.example.questflow.presentation.components.MediaMetadataDialog(
            initialMetadata = com.example.questflow.presentation.components.MediaMetadata(
                displayName = mediaToEdit!!.displayName,
                description = mediaToEdit!!.description,
                tags = mediaToEdit!!.tags
            ),
            title = "Metadaten bearbeiten",
            confirmText = "Speichern",
            onDismiss = {
                showEditMetadataDialog = false
                mediaToEdit = null
            },
            onConfirm = { metadata ->
                viewModel.updateMediaMetadata(
                    mediaId = mediaToEdit!!.id,
                    displayName = metadata.displayName,
                    description = metadata.description,
                    tags = metadata.tags
                )
                showEditMetadataDialog = false
                mediaToEdit = null
            }
        )
    }
}

@Composable
fun MediaGridItem(
    media: MediaLibraryEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onShowInfo: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        when (media.mediaType) {
            MediaType.IMAGE, MediaType.GIF -> {
                Image(
                    painter = rememberAsyncImagePainter(model = File(media.filePath)),
                    contentDescription = media.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            MediaType.AUDIO -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = media.fileName,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Media type badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = when (media.mediaType) {
                    MediaType.IMAGE -> "IMG"
                    MediaType.GIF -> "GIF"
                    MediaType.AUDIO -> "AUD"
                },
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // File info overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(4.dp)
        ) {
            Text(
                text = media.fileName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More menu button
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Mehr",
                tint = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(4.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Anzeigen") },
                onClick = {
                    showMenu = false
                    onClick()
                },
                leadingIcon = {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Informationen anzeigen") },
                onClick = {
                    showMenu = false
                    onShowInfo()
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Informationen bearbeiten") },
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Löschen") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
