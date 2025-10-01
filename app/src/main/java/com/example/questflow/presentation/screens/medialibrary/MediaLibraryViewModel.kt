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
import java.io.File
import java.io.FileOutputStream
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
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.value = _uiState.value.copy(notification = "Fehler beim Lesen der Datei")
                    return@launch
                }

                // Get file info
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val fileName = uri.lastPathSegment ?: "unknown_${System.currentTimeMillis()}"

                // Create internal storage directory
                val mediaDir = File(context.filesDir, "media_library")
                if (!mediaDir.exists()) {
                    mediaDir.mkdirs()
                }

                // Generate unique filename
                val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
                val destFile = File(mediaDir, uniqueFileName)

                // Copy file
                FileOutputStream(destFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()

                // Add to database
                val mediaId = mediaLibraryRepository.addMedia(
                    fileName = fileName,
                    filePath = destFile.absolutePath,
                    mediaType = mediaType,
                    fileSize = destFile.length(),
                    mimeType = mimeType
                )

                _uiState.value = _uiState.value.copy(
                    notification = "Datei erfolgreich hochgeladen"
                )

                // Reload stats
                loadStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Fehler beim Hochladen: ${e.message}"
                )
            }
        }
    }

    fun deleteMedia(context: Context, media: MediaLibraryEntity) {
        viewModelScope.launch {
            try {
                // Delete file from storage
                val file = File(media.filePath)
                if (file.exists()) {
                    file.delete()
                }

                // Delete thumbnail if exists
                media.thumbnailPath?.let { thumbnailPath ->
                    val thumbnailFile = File(thumbnailPath)
                    if (thumbnailFile.exists()) {
                        thumbnailFile.delete()
                    }
                }

                // Delete from database
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
}

data class MediaLibraryUiState(
    val allMedia: List<MediaLibraryEntity> = emptyList(),
    val mediaCount: Int = 0,
    val totalSize: Long = 0,
    val notification: String? = null
)
