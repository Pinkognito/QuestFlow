package com.example.questflow.presentation.screens.medialibrary

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.questflow.presentation.components.QuestFlowTopBar
import java.io.File
import java.io.FileOutputStream

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

    var selectedMediaType by remember { mutableStateOf(MediaType.IMAGE) }
    var showDeleteDialog by remember { mutableStateOf<MediaLibraryEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadMedia(context, it, MediaType.IMAGE)
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
            FloatingActionButton(
                onClick = {
                    when (selectedMediaType) {
                        MediaType.IMAGE, MediaType.GIF -> imagePickerLauncher.launch("image/*")
                        MediaType.AUDIO -> { /* TODO: Audio picker */ }
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Datei hochladen")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Media type tabs
            TabRow(
                selectedTabIndex = when (selectedMediaType) {
                    MediaType.IMAGE -> 0
                    MediaType.GIF -> 1
                    MediaType.AUDIO -> 2
                }
            ) {
                Tab(
                    selected = selectedMediaType == MediaType.IMAGE,
                    onClick = { selectedMediaType = MediaType.IMAGE },
                    text = { Text("Bilder") }
                )
                Tab(
                    selected = selectedMediaType == MediaType.GIF,
                    onClick = { selectedMediaType = MediaType.GIF },
                    text = { Text("GIFs") }
                )
                Tab(
                    selected = selectedMediaType == MediaType.AUDIO,
                    onClick = { selectedMediaType = MediaType.AUDIO },
                    text = { Text("Audio") }
                )
            }

            // Storage info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        text = "${formatFileSize(uiState.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Media grid
            val filteredMedia = uiState.allMedia.filter { it.mediaType == selectedMediaType }

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
                            text = "Keine ${
                                when (selectedMediaType) {
                                    MediaType.IMAGE -> "Bilder"
                                    MediaType.GIF -> "GIFs"
                                    MediaType.AUDIO -> "Audio-Dateien"
                                }
                            } vorhanden",
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
                            onDelete = { showDeleteDialog = media }
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
}

@Composable
fun MediaGridItem(
    media: MediaLibraryEntity,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when (media.mediaType) {
            MediaType.IMAGE, MediaType.GIF -> {
                Image(
                    painter = rememberAsyncImagePainter(model = File(media.filePath)),
                    contentDescription = media.fileName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showMenu = true },
                    contentScale = ContentScale.Crop
                )
            }
            MediaType.AUDIO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showMenu = true },
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

        // File info overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.6f))
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

        // More menu
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
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
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
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
