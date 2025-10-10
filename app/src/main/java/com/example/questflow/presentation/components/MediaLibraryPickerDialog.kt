package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import java.io.File

/**
 * Wiederverwendbarer Dialog zur Auswahl von Medien aus der Bibliothek
 *
 * @param mediaList Liste aller verfügbaren Medien
 * @param filterTypes Nur diese MediaTypes anzeigen (null = alle)
 * @param selectedMediaId Aktuell ausgewählte Media-ID (für Vorauswahl)
 * @param onDismiss Dialog schließen
 * @param onMediaSelected Media ausgewählt (mediaId)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryPickerDialog(
    mediaList: List<MediaLibraryEntity>,
    filterTypes: Set<MediaType>? = null,
    selectedMediaId: String? = null,
    onDismiss: () -> Unit,
    onMediaSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<MediaType?>(null) }

    val filteredMedia = remember(mediaList, searchQuery, selectedType, filterTypes) {
        mediaList
            .filter { media ->
                // Filter nach erlaubten Typen
                if (filterTypes != null && media.mediaType !in filterTypes) {
                    return@filter false
                }

                // Filter nach ausgewähltem Typ
                if (selectedType != null && media.mediaType != selectedType) {
                    return@filter false
                }

                // Filter nach Suchquery
                if (searchQuery.isNotBlank()) {
                    val query = searchQuery.lowercase()
                    media.fileName.lowercase().contains(query) ||
                    media.displayName.lowercase().contains(query) ||
                    media.tags.lowercase().contains(query)
                } else {
                    true
                }
            }
            .sortedByDescending { it.uploadedAt }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Medien-Bibliothek",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Suchen") },
                    placeholder = { Text("Dateiname, Tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Type Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("Alle") }
                    )

                    val availableTypes = filterTypes ?: MediaType.values().toSet()
                    availableTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = if (selectedType == type) null else type },
                            label = {
                                Text(when (type) {
                                    MediaType.IMAGE -> "Bilder"
                                    MediaType.GIF -> "GIFs"
                                    MediaType.AUDIO -> "Audio"
                                    MediaType.VIDEO -> "Videos"
                                })
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info
                Text(
                    text = "${filteredMedia.size} Dateien",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Media Grid
                if (filteredMedia.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine Medien gefunden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredMedia) { media ->
                            LibraryMediaItem(
                                media = media,
                                isSelected = media.id == selectedMediaId,
                                onClick = {
                                    onMediaSelected(media.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryMediaItem(
    media: MediaLibraryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (media.mediaType) {
                MediaType.IMAGE, MediaType.GIF -> {
                    AsyncImage(
                        model = File(media.filePath),
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
                            Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                MediaType.VIDEO -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Selected indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Ausgewählt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Type badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = when (media.mediaType) {
                        MediaType.IMAGE -> "IMG"
                        MediaType.GIF -> "GIF"
                        MediaType.AUDIO -> "AUD"
                        MediaType.VIDEO -> "VID"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
