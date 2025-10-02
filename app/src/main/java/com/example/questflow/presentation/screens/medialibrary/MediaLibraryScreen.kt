package com.example.questflow.presentation.screens.medialibrary

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var showMultiDeleteDialog by remember { mutableStateOf(false) }
    var showUploadMenu by remember { mutableStateOf(false) }
    var showMediaViewer by remember { mutableStateOf<MediaLibraryEntity?>(null) }

    // Usage management dialog state
    var showUsageManagement by remember { mutableStateOf(false) }
    var openUsageManagementDirectly by remember { mutableStateOf(false) }

    // State for metadata dialog
    var showMetadataDialog by remember { mutableStateOf(false) }
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadUris by remember { mutableStateOf<List<Uri>?>(null) }
    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }

    // State for editing metadata
    var showEditMetadataDialog by remember { mutableStateOf(false) }
    var mediaToEdit by remember { mutableStateOf<MediaLibraryEntity?>(null) }

    // Multi-select state
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedMediaIds by remember { mutableStateOf(setOf<String>()) }
    var showCollectionTransferDialog by remember { mutableStateOf(false) }

    // Category filter
    var selectedCategoryFilter by remember { mutableStateOf<Long?>(null) }

    // Filter expansion state
    var isFiltersExpanded by remember { mutableStateOf(false) }

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

    // State for storage info dialog
    var showStorageInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSelectMode) {
                TopAppBar(
                    title = { Text("${selectedMediaIds.size} ausgewählt") },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectMode = false
                            selectedMediaIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Abbrechen")
                        }
                    },
                    actions = {
                        // Delete selected
                        IconButton(
                            onClick = { showMultiDeleteDialog = true },
                            enabled = selectedMediaIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Ausgewählte löschen")
                        }
                        // Add to Collection
                        IconButton(
                            onClick = { showCollectionTransferDialog = true },
                            enabled = selectedMediaIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zu Collection hinzufügen")
                        }
                        // Select All / Deselect All
                        IconButton(onClick = {
                            selectedMediaIds = if (selectedMediaIds.isNotEmpty()) {
                                emptySet()
                            } else {
                                // Will be populated when filteredMedia is available
                                selectedMediaIds
                            }
                        }) {
                            Icon(
                                if (selectedMediaIds.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Clear,
                                contentDescription = if (selectedMediaIds.isEmpty()) "Alle auswählen" else "Alle abwählen"
                            )
                        }
                    }
                )
            } else {
                QuestFlowTopBar(
                    title = "Mediathek",
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelected = appViewModel::selectCategory,
                    onManageCategoriesClick = { navController.navigate("categories") },
                    level = globalStats?.level ?: 1,
                    totalXp = globalStats?.xp ?: 0,
                    actions = {
                        // Info button for storage stats
                        IconButton(onClick = { showStorageInfoDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Speicher-Info")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSelectMode) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar (always visible) - Compact version
            var searchQuery by remember { mutableStateOf("") }
            var selectedTag by remember { mutableStateOf<String?>(null) }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(48.dp),
                placeholder = {
                    Text(
                        "Suchen...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Suche löschen",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )

            // Filter toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                IconButton(onClick = { isFiltersExpanded = !isFiltersExpanded }) {
                    Icon(
                        imageVector = if (isFiltersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isFiltersExpanded) "Filter einklappen" else "Filter ausklappen"
                    )
                }
            }

            // Collapsible filters section
            AnimatedVisibility(
                visible = isFiltersExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
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

                    // Category filter
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Nach Kategorie filtern:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == null,
                                    onClick = { selectedCategoryFilter = null },
                                    label = { Text("Alle") }
                                )
                            }
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategoryFilter == category.id,
                                    onClick = { selectedCategoryFilter = category.id },
                                    label = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(category.emoji)
                                            Text(category.name)
                                        }
                                    }
                                )
                            }
                        }
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
                }
            }

            // Get base media list based on category filter
            val categoryFilteredMedia by remember(selectedCategoryFilter) {
                if (selectedCategoryFilter != null) {
                    viewModel.getMediaByCategory(selectedCategoryFilter)
                } else {
                    viewModel.getAllMedia()
                }
            }.collectAsState(initial = emptyList())

            // Media grid with filtering
            val filteredMedia = remember(categoryFilteredMedia, selectedFilter, searchQuery, selectedTag) {
                var media = categoryFilteredMedia

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
                            isSelectMode = isSelectMode,
                            isSelected = selectedMediaIds.contains(media.id),
                            onSelect = {
                                selectedMediaIds = if (selectedMediaIds.contains(media.id)) {
                                    selectedMediaIds - media.id
                                } else {
                                    selectedMediaIds + media.id
                                }
                            },
                            onDelete = { showDeleteDialog = media },
                            onClick = {
                                if (isSelectMode) {
                                    // Toggle selection in select mode
                                    selectedMediaIds = if (selectedMediaIds.contains(media.id)) {
                                        selectedMediaIds - media.id
                                    } else {
                                        selectedMediaIds + media.id
                                    }
                                } else {
                                    showMediaViewer = media
                                }
                            },
                            onEdit = {
                                mediaToEdit = media
                                showEditMetadataDialog = true
                            },
                            onShowInfo = {
                                viewModel.loadMediaDetails(media.id)
                            },
                            onManageUsages = {
                                openUsageManagementDirectly = true
                                viewModel.loadMediaDetails(media.id)
                            },
                            onLongPress = {
                                // Activate select mode and select this item
                                if (!isSelectMode) {
                                    isSelectMode = true
                                    selectedMediaIds = setOf(media.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog (single)
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

    // Delete confirmation dialog (multiple)
    if (showMultiDeleteDialog && selectedMediaIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("${selectedMediaIds.size} Dateien löschen?") },
            text = {
                Text("Möchtest du ${selectedMediaIds.size} Datei${if (selectedMediaIds.size != 1) "en" else ""} wirklich löschen? Alle zugehörigen Collection Items werden ebenfalls entfernt. Diese Aktion kann nicht rückgängig gemacht werden.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMultipleMedia(context, selectedMediaIds.toList())
                        showMultiDeleteDialog = false
                        isSelectMode = false
                        selectedMediaIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Alle löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDeleteDialog = false }) {
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

    // Auto-open usage management when requested from dropdown menu
    LaunchedEffect(uiState.selectedMediaWithUsage, openUsageManagementDirectly) {
        if (openUsageManagementDirectly && uiState.selectedMediaWithUsage != null) {
            showUsageManagement = true
            openUsageManagementDirectly = false
        }
    }

    uiState.selectedMediaWithUsage?.let { mediaWithUsage ->
        if (showUsageManagement) {
            com.example.questflow.presentation.components.UsageManagementDialog(
                mediaFileName = mediaWithUsage.media.displayName.ifBlank { mediaWithUsage.media.fileName },
                usages = mediaWithUsage.usages,
                categories = mediaWithUsage.categories,
                onDismiss = {
                    showUsageManagement = false
                    viewModel.clearMediaDetails()
                },
                onDeleteUsage = { usage ->
                    viewModel.deleteUsage(usage)
                },
                onEditCollectionItem = { referenceId, categoryId ->
                    // TODO: Navigate to collection item edit screen
                    showUsageManagement = false
                    viewModel.clearMediaDetails()
                }
            )
        } else {
            MediaDetailsDialog(
                media = mediaWithUsage.media,
                usages = mediaWithUsage.usages,
                categories = mediaWithUsage.categories,
                onDismiss = { viewModel.clearMediaDetails() },
                onManageUsages = {
                    showUsageManagement = true
                }
            )
        }
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

    // Collection Transfer Dialog
    if (showCollectionTransferDialog && selectedMediaIds.isNotEmpty()) {
        com.example.questflow.presentation.components.CollectionTransferDialog(
            mediaCount = selectedMediaIds.size,
            categories = categories,
            onDismiss = { showCollectionTransferDialog = false },
            onConfirm = { categoryId, name, description, rarity ->
                viewModel.addMediaToCollection(
                    mediaIds = selectedMediaIds.toList(),
                    categoryId = categoryId,
                    name = name,
                    description = description,
                    rarity = rarity
                )
                showCollectionTransferDialog = false
                isSelectMode = false
                selectedMediaIds = emptySet()
            }
        )
    }

    // Storage Info Dialog
    if (showStorageInfoDialog) {
        AlertDialog(
            onDismissRequest = { showStorageInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Mediathek Statistiken") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gespeicherte Dateien:")
                        Text(
                            "${uiState.mediaCount}",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gesamtgröße:")
                        Text(
                            formatFileSize(uiState.totalSize),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageInfoDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridItem(
    media: MediaLibraryEntity,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onShowInfo: () -> Unit,
    onManageUsages: () -> Unit,
    onLongPress: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelectMode && isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongPress() }
            )
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

        // Selection indicator or More menu button
        if (isSelectMode) {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Ausgewählt",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.White, CircleShape),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
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
                text = { Text("Verwendungen verwalten") },
                onClick = {
                    showMenu = false
                    onManageUsages()
                },
                leadingIcon = {
                    Icon(Icons.Default.Settings, contentDescription = null)
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
