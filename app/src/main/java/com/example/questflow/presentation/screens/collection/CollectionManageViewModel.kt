package com.example.questflow.presentation.screens.collection

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

    private val _uiState = MutableStateFlow(CollectionManageUiState())
    val uiState: StateFlow<CollectionManageUiState> = _uiState.asStateFlow()

    fun setCategoryId(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun addMultipleFromMediaLibrary(
        mediaIds: List<String>,
        onComplete: (Boolean, String) -> Unit
    ) {
        val state = _uiState.value

        if (mediaIds.isEmpty()) {
            onComplete(false, "Keine Medien ausgewählt")
            return
        }

        _uiState.value = state.copy(isUploading = true)

        viewModelScope.launch {
            try {
                val result = addCollectionItemsUseCase.addMultipleFromMediaLibrary(
                    mediaIds = mediaIds,
                    categoryId = state.categoryId
                )

                when (result) {
                    is AddCollectionItemsUseCase.Result.Success -> {
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(true, "${result.itemsAdded} Items erfolgreich zur Collection hinzugefügt")
                    }
                    is AddCollectionItemsUseCase.Result.Error -> {
                        _uiState.value = state.copy(isUploading = false)
                        onComplete(false, result.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = state.copy(isUploading = false)
                onComplete(false, "Fehler: ${e.message}")
            }
        }
    }
}

data class CollectionManageUiState(
    val categoryId: Long? = null,
    val isUploading: Boolean = false
)
