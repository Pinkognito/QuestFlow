package com.example.questflow.presentation.screens.medialibrary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.repository.MediaLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaLibraryUiState())
    val uiState: StateFlow<MediaLibraryUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
        loadStats()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            mediaLibraryRepository.getAllMedia().collect { mediaList ->
                _uiState.value = _uiState.value.copy(allMedia = mediaList)
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val count = mediaLibraryRepository.getMediaCount()
            val size = mediaLibraryRepository.getTotalMediaSize()
            _uiState.value = _uiState.value.copy(
                mediaCount = count,
                totalSize = size
            )
        }
    }

    fun uploadMedia(
        context: Context,
        uri: Uri,
        mediaType: MediaType,
        displayName: String = "",
        description: String = "",
        tags: String = ""
    ) {
        viewModelScope.launch {
            try {
                // Get file info
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val fileName = uri.lastPathSegment ?: "unknown_${System.currentTimeMillis()}"

                // Auto-detect media type from MIME type
                val detectedMediaType = when {
                    mimeType.startsWith("image/gif") -> MediaType.GIF
                    mimeType.startsWith("image/") -> MediaType.IMAGE
                    mimeType.startsWith("audio/") -> MediaType.AUDIO
                    else -> mediaType // Fallback to provided type
                }

                // Use repository to handle file storage and database entry
                val mediaId = mediaLibraryRepository.addMediaFromUri(
                    uri = uri,
                    fileName = fileName,
                    mediaType = detectedMediaType,
                    mimeType = mimeType,
                    displayName = displayName,
                    description = description,
                    tags = tags
                )

                if (mediaId != null) {
                    _uiState.value = _uiState.value.copy(
                        notification = "Datei erfolgreich hochgeladen"
                    )
                    loadStats()
                } else {
                    _uiState.value = _uiState.value.copy(
                        notification = "Fehler beim Hochladen der Datei"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Hochladen: ${e.message}"
                )
            }
        }
    }

    fun uploadMultipleMedia(
        context: Context,
        uris: List<Uri>,
        displayName: String = "",
        description: String = "",
        tags: String = ""
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(notification = "Lade ${uris.size} Dateien hoch...")

                val addedIds = mediaLibraryRepository.addMultipleMediaFromUris(
                    uris = uris,
                    displayName = displayName,
                    description = description,
                    tags = tags
                )

                _uiState.value = _uiState.value.copy(
                    notification = "${addedIds.size} von ${uris.size} Dateien erfolgreich hochgeladen"
                )
                loadStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Bulk-Upload: ${e.message}"
                )
            }
        }
    }

    fun uploadFromZip(
        context: Context,
        zipUri: Uri,
        displayName: String = "",
        description: String = "",
        tags: String = ""
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(notification = "Extrahiere ZIP-Datei...")

                // Extract and save files
                val savedFiles = mediaLibraryRepository.extractAndSaveZip(
                    zipUri = zipUri,
                    displayName = displayName,
                    description = description,
                    tags = tags
                )

                _uiState.value = _uiState.value.copy(
                    notification = "${savedFiles.size} Dateien aus ZIP extrahiert und hochgeladen"
                )
                loadStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim ZIP-Upload: ${e.message}"
                )
            }
        }
    }

    fun updateMediaMetadata(
        mediaId: String,
        displayName: String,
        description: String,
        tags: String
    ) {
        viewModelScope.launch {
            try {
                mediaLibraryRepository.updateMediaMetadata(
                    mediaId = mediaId,
                    displayName = displayName,
                    description = description,
                    tags = tags
                )
                _uiState.value = _uiState.value.copy(
                    notification = "Metadaten erfolgreich aktualisiert"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Aktualisieren: ${e.message}"
                )
            }
        }
    }

    fun deleteMedia(context: Context, media: MediaLibraryEntity) {
        viewModelScope.launch {
            try {
                // Repository handles file and database deletion
                mediaLibraryRepository.deleteMedia(media)

                _uiState.value = _uiState.value.copy(
                    notification = "Datei gelöscht"
                )

                // Reload stats
                loadStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Löschen: ${e.message}"
                )
            }
        }
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(notification = null)
    }

    fun loadMediaDetails(mediaId: String) {
        viewModelScope.launch {
            try {
                val mediaWithUsage = mediaLibraryRepository.getMediaWithUsage(mediaId)
                if (mediaWithUsage != null) {
                    // Get all categories to map IDs to names
                    val allCategories = categoryRepository.getAllCategories().first()
                    val categoryMap = allCategories.associate { it.id to it.name }

                    _uiState.value = _uiState.value.copy(
                        selectedMediaWithUsage = MediaWithUsageAndCategories(
                            media = mediaWithUsage.media,
                            usages = mediaWithUsage.usages,
                            categories = categoryMap
                        )
                    )
                } else {
                    _uiState.value = _uiState.value.copy(selectedMediaWithUsage = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Laden der Details: ${e.message}"
                )
            }
        }
    }

    fun clearMediaDetails() {
        _uiState.value = _uiState.value.copy(selectedMediaWithUsage = null)
    }

    fun addMediaToCollection(
        mediaIds: List<String>,
        categoryId: Long?,
        name: String,
        description: String,
        rarity: String
    ) {
        viewModelScope.launch {
            try {
                val count = mediaLibraryRepository.addMediaToCollection(
                    mediaIds = mediaIds,
                    categoryId = categoryId,
                    name = name,
                    description = description,
                    rarity = rarity
                )
                _uiState.value = _uiState.value.copy(
                    notification = "$count ${if (count == 1) "Item" else "Items"} zur Collection hinzugefügt"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Hinzufügen: ${e.message}"
                )
            }
        }
    }

    fun deleteUsage(usage: com.example.questflow.data.database.entity.MediaUsageEntity) {
        viewModelScope.launch {
            try {
                mediaLibraryRepository.removeUsageTracking(
                    mediaId = usage.mediaLibraryId,
                    usageType = usage.usageType,
                    referenceId = usage.referenceId
                )
                _uiState.value = _uiState.value.copy(
                    notification = "Verwendung entfernt"
                )
                // Reload details
                loadMediaDetails(usage.mediaLibraryId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Entfernen: ${e.message}"
                )
            }
        }
    }

    /**
     * Get all media (for category filter: null = all)
     */
    fun getAllMedia(): Flow<List<MediaLibraryEntity>> {
        return mediaLibraryRepository.getAllMedia()
    }

    /**
     * Get media filtered by category
     */
    fun getMediaByCategory(categoryId: Long?): Flow<List<MediaLibraryEntity>> {
        return if (categoryId == null) {
            mediaLibraryRepository.getAllMedia()
        } else {
            mediaLibraryRepository.getMediaByCategory(categoryId)
        }
    }
}

data class MediaLibraryUiState(
    val allMedia: List<MediaLibraryEntity> = emptyList(),
    val mediaCount: Int = 0,
    val totalSize: Long = 0,
    val notification: String? = null,
    val selectedMediaWithUsage: MediaWithUsageAndCategories? = null
)

data class MediaWithUsage(
    val media: MediaLibraryEntity,
    val usages: List<com.example.questflow.data.database.entity.MediaUsageEntity>
)

data class MediaWithUsageAndCategories(
    val media: MediaLibraryEntity,
    val usages: List<com.example.questflow.data.database.entity.MediaUsageEntity>,
    val categories: Map<Long, String> // categoryId -> categoryName
)
