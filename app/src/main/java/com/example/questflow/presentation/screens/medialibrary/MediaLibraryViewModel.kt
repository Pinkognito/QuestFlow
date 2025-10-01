package com.example.questflow.presentation.screens.medialibrary

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import com.example.questflow.data.repository.MediaLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val mediaLibraryRepository: MediaLibraryRepository
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

    fun uploadMedia(context: Context, uri: Uri, mediaType: MediaType) {
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
                    mimeType = mimeType
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

    fun uploadMultipleMedia(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(notification = "Lade ${uris.size} Dateien hoch...")

                val addedIds = mediaLibraryRepository.addMultipleMediaFromUris(uris)

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

    fun uploadFromZip(context: Context, zipUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(notification = "Extrahiere ZIP-Datei...")

                // Extract and save files
                val savedFiles = mediaLibraryRepository.extractAndSaveZip(zipUri)

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
                _uiState.value = _uiState.value.copy(
                    selectedMediaWithUsage = mediaWithUsage?.let {
                        MediaWithUsage(it.media, it.usages)
                    }
                )
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
}

data class MediaLibraryUiState(
    val allMedia: List<MediaLibraryEntity> = emptyList(),
    val mediaCount: Int = 0,
    val totalSize: Long = 0,
    val notification: String? = null,
    val selectedMediaWithUsage: MediaWithUsage? = null
)

data class MediaWithUsage(
    val media: MediaLibraryEntity,
    val usages: List<com.example.questflow.data.database.entity.MediaUsageEntity>
)
