package com.example.questflow.presentation.screens.collection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import com.example.questflow.presentation.screens.medialibrary.MediaLibraryViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionManageScreen(
    navController: NavController,
    appViewModel: com.example.questflow.presentation.AppViewModel,
    viewModel: CollectionManageViewModel = hiltViewModel(),
    mediaViewModel: MediaLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val mediaUiState by mediaViewModel.uiState.collectAsState()

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // Filter state
    var selectedMediaType by remember { mutableStateOf<MediaType?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showAllTags by remember { mutableStateOf(false) }

    // Multi-select state
    var selectedMediaIds by remember { mutableStateOf(setOf<String>()) }

    // Available tags
    val availableTags = remember(mediaUiState.allMedia) {
        mediaUiState.allMedia
            .flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // Filtered media
    val filteredMedia = remember(mediaUiState.allMedia, selectedMediaType, selectedTag) {
        var media = mediaUiState.allMedia.filter {
            it.mediaType == MediaType.IMAGE || it.mediaType == MediaType.GIF
        }

        if (selectedMediaType != null) {
            media = media.filter { it.mediaType == selectedMediaType }
        }

        if (selectedTag != null) {
            media = media.filter { it.tags.contains(selectedTag!!) }
        }

        media
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Set initial category from appViewModel
    LaunchedEffect(selectedCategory) {
        viewModel.setCategoryId(selectedCategory?.id)
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(snackbarMessage)
            showSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zu Collection hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (selectedMediaIds.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedMediaIds = emptySet() }
                        ) {
                            Text("${selectedMediaIds.size} ausgewählt")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filters Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media Type Filter
                    Text(
                        "Medientyp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMediaType == null,
                            onClick = { selectedMediaType = null },
                            label = { Text("Alle") }
                        )
                        FilterChip(
                            selected = selectedMediaType == MediaType.IMAGE,
                            onClick = { selectedMediaType = MediaType.IMAGE },
                            label = { Text("Bilder") }
                        )
                        FilterChip(
                            selected = selectedMediaType == MediaType.GIF,
                            onClick = { selectedMediaType = MediaType.GIF },
                            label = { Text("GIFs") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags Filter
                    if (availableTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tags",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { showAllTags = !showAllTags }) {
                                Text(if (showAllTags) "Weniger" else "Alle anzeigen")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        val displayTags = if (showAllTags) availableTags else availableTags.take(5)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { selectedTag = null },
                                    label = { Text("Alle") }
                                )
                            }
                            items(displayTags) { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Select All / Deselect All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedMediaIds = filteredMedia.map { it.id }.toSet()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = filteredMedia.isNotEmpty()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alle auswählen")
                        }
                        OutlinedButton(
                            onClick = { selectedMediaIds = emptySet() },
                            modifier = Modifier.weight(1f),
                            enabled = selectedMediaIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Abwählen")
                        }
                    }
                }
            }

            // Media Grid
            if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Keine Medien gefunden",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Gehe zur Mediathek um Dateien hochzuladen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMedia) { media ->
                        MediaSelectableItem(
                            media = media,
                            isSelected = selectedMediaIds.contains(media.id),
                            onClick = {
                                selectedMediaIds = if (selectedMediaIds.contains(media.id)) {
                                    selectedMediaIds - media.id
                                } else {
                                    selectedMediaIds + media.id
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Action Bar
            if (selectedMediaIds.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Category Selection
                        Text(
                            "Collection-Kategorie",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.categoryId == null,
                                onClick = { viewModel.setCategoryId(null) },
                                label = { Text("Global") },
                                modifier = Modifier.weight(1f)
                            )
                            selectedCategory?.let { category ->
                                FilterChip(
                                    selected = uiState.categoryId == category.id,
                                    onClick = { viewModel.setCategoryId(category.id) },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(category.emoji)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(category.name)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Add Button
                        Button(
                            onClick = {
                                viewModel.addMultipleFromMediaLibrary(
                                    mediaIds = selectedMediaIds.toList()
                                ) { success, message ->
                                    snackbarMessage = message
                                    showSnackbar = true
                                    if (success) {
                                        selectedMediaIds = emptySet()
                                        navController.navigateUp()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isUploading
                        ) {
                            if (uiState.isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wird hinzugefügt...")
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${selectedMediaIds.size} zu Collection hinzufügen")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaSelectableItem(
    media: MediaLibraryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = File(media.filePath)),
            contentDescription = media.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )

            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Ausgewählt",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Show media type indicator
        if (media.mediaType == MediaType.GIF) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "GIF",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
