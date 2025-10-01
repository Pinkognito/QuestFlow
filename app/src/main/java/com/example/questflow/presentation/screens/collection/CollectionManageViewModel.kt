package com.example.questflow.presentation.screens.collection

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.domain.usecase.collection.AddCollectionItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionManageViewModel @Inject constructor(
    private val addCollectionItemsUseCase: AddCollectionItemsUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "CollectionManageViewModel"
    }

    private val _uiState = MutableStateFlow(CollectionManageUiState())
    val uiState: StateFlow<CollectionManageUiState> = _uiState.asStateFlow()

    fun setUploadMode(mode: UploadMode) {
        Log.d(TAG, "Setting upload mode: $mode")
        _uiState.value = _uiState.value.copy(uploadMode = mode)
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun setRarity(rarity: String) {
        _uiState.value = _uiState.value.copy(rarity = rarity)
    }

    fun setRequiredLevel(level: Int) {
        _uiState.value = _uiState.value.copy(requiredLevel = level)
    }

    fun setCategoryId(categoryId: Long?) {
        Log.d(TAG, "Setting category ID: $categoryId")
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun setSelectedImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }

    fun setSelectedMediaLibraryId(mediaId: String?) {
        _uiState.value = _uiState.value.copy(selectedMediaLibraryId = mediaId)
    }

    fun setSelectedImageUris(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(selectedImageUris = uris)
    }

    fun setSelectedZipUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedZipUri = uri)
    }

    fun uploadSingleImage(onComplete: (Boolean, String) -> Unit) {
        val state = _uiState.value
        val mediaLibraryId = state.selectedMediaLibraryId

        if (mediaLibraryId == null) {
            Log.w(TAG, "No media selected from library")
            onComplete(false, "No media selected")
            return
        }

        if (state.name.isBlank()) {
            Log.w(TAG, "Name is blank")
            onComplete(false, "Name is required")
            return
        }

        _uiState.value = state.copy(isUploading = true)

        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding collection item: ${state.name} with mediaLibraryId: $mediaLibraryId")

                val result = addCollectionItemsUseCase.addSingleItem(
                    mediaLibraryId = mediaLibraryId,
                    name = state.name,
                    description = state.description,
                    rarity = state.rarity,
                    requiredLevel = state.requiredLevel,
                    categoryId = state.categoryId
                )

                when (result) {
                    is AddCollectionItemsUseCase.Result.Success -> {
                        Log.d(TAG, "Successfully added collection item")
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(true, "Successfully added collection item")
                    }
                    is AddCollectionItemsUseCase.Result.Error -> {
                        Log.e(TAG, "Failed to add: ${result.message}")
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(false, result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding collection item", e)
                _uiState.value = state.copy(isUploading = false)
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    fun uploadMultipleImages(onComplete: (Boolean, String) -> Unit) {
        val state = _uiState.value
        val uris = state.selectedImageUris

        if (uris.isEmpty()) {
            Log.w(TAG, "No images selected")
            onComplete(false, "No images selected")
            return
        }

        if (state.name.isBlank()) {
            Log.w(TAG, "Name prefix is blank")
            onComplete(false, "Name prefix is required")
            return
        }

        _uiState.value = state.copy(isUploading = true)

        viewModelScope.launch {
            try {
                Log.d(TAG, "Uploading ${uris.size} images with prefix: ${state.name}")

                val result = addCollectionItemsUseCase.addMultipleImages(
                    uris = uris,
                    baseNamePrefix = state.name,
                    rarity = state.rarity,
                    startRequiredLevel = state.requiredLevel,
                    categoryId = state.categoryId
                )

                when (result) {
                    is AddCollectionItemsUseCase.Result.Success -> {
                        Log.d(TAG, "Successfully uploaded ${result.itemsAdded} images")
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(true, "Successfully added ${result.itemsAdded} items")
                    }
                    is AddCollectionItemsUseCase.Result.Error -> {
                        Log.e(TAG, "Failed to upload: ${result.message}")
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(false, result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading multiple images", e)
                _uiState.value = state.copy(isUploading = false)
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    fun uploadZip(onComplete: (Boolean, String) -> Unit) {
        val state = _uiState.value
        val uri = state.selectedZipUri

        if (uri == null) {
            Log.w(TAG, "No ZIP file selected")
            onComplete(false, "No ZIP file selected")
            return
        }

        if (state.name.isBlank()) {
            Log.w(TAG, "Name prefix is blank")
            onComplete(false, "Name prefix is required")
            return
        }

        _uiState.value = state.copy(isUploading = true)

        viewModelScope.launch {
            try {
                Log.d(TAG, "Uploading ZIP file with prefix: ${state.name}")

                val result = addCollectionItemsUseCase.addFromZip(
                    zipUri = uri,
                    baseNamePrefix = state.name,
                    rarity = state.rarity,
                    startRequiredLevel = state.requiredLevel,
                    categoryId = state.categoryId
                )

                when (result) {
                    is AddCollectionItemsUseCase.Result.Success -> {
                        Log.d(TAG, "Successfully uploaded ${result.itemsAdded} items from ZIP")
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(true, "Successfully added ${result.itemsAdded} items from ZIP")
                    }
                    is AddCollectionItemsUseCase.Result.Error -> {
                        Log.e(TAG, "Failed to upload ZIP: ${result.message}")
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(false, result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading ZIP", e)
                _uiState.value = state.copy(isUploading = false)
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
}

data class CollectionManageUiState(
    val uploadMode: UploadMode = UploadMode.SINGLE,
    val name: String = "",
    val description: String = "",
    val rarity: String = "COMMON",
    val requiredLevel: Int = 1,
    val categoryId: Long? = null,
    val selectedMediaLibraryId: String? = null, // NEW: Reference to media library
    val selectedImageUri: Uri? = null,
    val selectedImageUris: List<Uri> = emptyList(),
    val selectedZipUri: Uri? = null,
    val isUploading: Boolean = false
)

enum class UploadMode {
    SINGLE,
    MULTIPLE,
    ZIP
}
