package com.example.questflow.presentation.screens.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionManageScreen(
    navController: NavController,
    appViewModel: com.example.questflow.presentation.AppViewModel,
    viewModel: CollectionManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showMediaPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Set initial category from appViewModel
    LaunchedEffect(selectedCategory) {
        viewModel.setCategoryId(selectedCategory?.id)
    }

    // Multiple images picker
    val multipleImagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        viewModel.setSelectedImageUris(uris)
    }

    // ZIP file picker
    val zipFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSelectedZipUri(it) }
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
                title = { Text("Add Collection Items") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Upload Mode Selection
            Text(
                "Upload Mode",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.uploadMode == UploadMode.SINGLE,
                    onClick = { viewModel.setUploadMode(UploadMode.SINGLE) },
                    label = { Text("Single") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.uploadMode == UploadMode.MULTIPLE,
                    onClick = { viewModel.setUploadMode(UploadMode.MULTIPLE) },
                    label = { Text("Multiple") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.uploadMode == UploadMode.ZIP,
                    onClick = { viewModel.setUploadMode(UploadMode.ZIP) },
                    label = { Text("ZIP") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // File Selection
            when (uiState.uploadMode) {
                UploadMode.SINGLE -> {
                    Button(
                        onClick = { showMediaPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.selectedMediaLibraryId != null) "Bild ausgewählt" else "Bild aus Mediathek wählen")
                    }

                    if (uiState.selectedMediaLibraryId != null) {
                        Text(
                            "Bild ausgewählt aus Mediathek",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                UploadMode.MULTIPLE -> {
                    Button(
                        onClick = { multipleImagesPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.selectedImageUris.isNotEmpty()) "${uiState.selectedImageUris.size} Images Selected" else "Select Images")
                    }

                    if (uiState.selectedImageUris.isNotEmpty()) {
                        Text(
                            "Selected ${uiState.selectedImageUris.size} images",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                UploadMode.ZIP -> {
                    Button(
                        onClick = { zipFilePicker.launch("application/zip") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.selectedZipUri != null) "ZIP Selected" else "Select ZIP File")
                    }

                    if (uiState.selectedZipUri != null) {
                        Text(
                            "Selected: ${uiState.selectedZipUri}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name Field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.setName(it) },
                label = {
                    Text(
                        when (uiState.uploadMode) {
                            UploadMode.SINGLE -> "Name"
                            else -> "Name Prefix"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description (only for single)
            if (uiState.uploadMode == UploadMode.SINGLE) {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.setDescription(it) },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Rarity Selection
            Text(
                "Rarity",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("COMMON", "RARE", "EPIC", "LEGENDARY").forEach { rarity ->
                    FilterChip(
                        selected = uiState.rarity == rarity,
                        onClick = { viewModel.setRarity(rarity) },
                        label = { Text(rarity) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Required Level
            OutlinedTextField(
                value = uiState.requiredLevel.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { level -> viewModel.setRequiredLevel(level) }
                },
                label = {
                    Text(
                        when (uiState.uploadMode) {
                            UploadMode.SINGLE -> "Required Level"
                            else -> "Starting Required Level"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Text(
                "Collection Type",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Global option
            FilterChip(
                selected = uiState.categoryId == null,
                onClick = { viewModel.setCategoryId(null) },
                label = { Text("Global (All Categories)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category options
            categories.forEach { category ->
                FilterChip(
                    selected = uiState.categoryId == category.id,
                    onClick = { viewModel.setCategoryId(category.id) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(category.emoji)
                            Text(category.name)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Upload Button
            Button(
                onClick = {
                    when (uiState.uploadMode) {
                        UploadMode.SINGLE -> {
                            viewModel.uploadSingleImage { success, message ->
                                snackbarMessage = message
                                showSnackbar = true
                                if (success) {
                                    navController.navigateUp()
                                }
                            }
                        }
                        UploadMode.MULTIPLE -> {
                            viewModel.uploadMultipleImages { success, message ->
                                snackbarMessage = message
                                showSnackbar = true
                                if (success) {
                                    navController.navigateUp()
                                }
                            }
                        }
                        UploadMode.ZIP -> {
                            viewModel.uploadZip { success, message ->
                                snackbarMessage = message
                                showSnackbar = true
                                if (success) {
                                    navController.navigateUp()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isUploading && when (uiState.uploadMode) {
                    UploadMode.SINGLE -> uiState.selectedMediaLibraryId != null && uiState.name.isNotBlank()
                    UploadMode.MULTIPLE -> uiState.selectedImageUris.isNotEmpty() && uiState.name.isNotBlank()
                    UploadMode.ZIP -> uiState.selectedZipUri != null && uiState.name.isNotBlank()
                }
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
                    Text("Zu Collection hinzufügen")
                }
            }

            if (uiState.uploadMode != UploadMode.SINGLE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Note: Items will be named '${uiState.name.ifBlank { "Item" }} 1', '${uiState.name.ifBlank { "Item" }} 2', etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Media Picker Dialog
    if (showMediaPicker) {
        com.example.questflow.presentation.components.MediaPickerDialog(
            onDismiss = { showMediaPicker = false },
            onMediaSelected = { media ->
                viewModel.setSelectedMediaLibraryId(media.id)
                showMediaPicker = false
            }
        )
    }
}
