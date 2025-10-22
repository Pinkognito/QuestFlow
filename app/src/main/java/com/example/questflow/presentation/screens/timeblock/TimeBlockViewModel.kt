package com.example.questflow.presentation.screens.timeblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.dao.TimeBlockWithTags
import com.example.questflow.data.database.entity.TimeBlockEntity
import com.example.questflow.data.repository.TimeBlockRepository
import com.example.questflow.domain.usecase.timeblock.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeBlockViewModel @Inject constructor(
    private val repository: TimeBlockRepository,
    private val createTimeBlockUseCase: CreateTimeBlockUseCase,
    private val updateTimeBlockUseCase: UpdateTimeBlockUseCase,
    private val deleteTimeBlockUseCase: DeleteTimeBlockUseCase,
    private val getActiveTimeBlocksUseCase: GetActiveTimeBlocksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeBlockUiState())
    val uiState: StateFlow<TimeBlockUiState> = _uiState.asStateFlow()

    // Alle TimeBlocks (mit Tags)
    val timeBlocks = repository.getAllTimeBlocksWithTagsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Nur aktive TimeBlocks
    val activeTimeBlocks = getActiveTimeBlocksUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // === UI ACTIONS ===

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingTimeBlock = null
        )
    }

    fun showEditDialog(timeBlock: TimeBlockEntity) {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingTimeBlock = timeBlock
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = false,
            editingTimeBlock = null
        )
    }

    fun showDeleteConfirmation(timeBlock: TimeBlockEntity) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            timeBlockToDelete = timeBlock
        )
    }

    fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            timeBlockToDelete = null
        )
    }

    // === CRUD OPERATIONS ===

    fun saveTimeBlock(
        timeBlock: TimeBlockEntity,
        tagIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                if (timeBlock.id == 0L) {
                    createTimeBlockUseCase(timeBlock, tagIds)
                } else {
                    updateTimeBlockUseCase(timeBlock, tagIds)
                }

                dismissDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Fehler beim Speichern"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteTimeBlock() {
        val timeBlock = _uiState.value.timeBlockToDelete ?: return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                deleteTimeBlockUseCase(timeBlock.id)
                dismissDeleteConfirmation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Fehler beim Löschen"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleActiveStatus(timeBlock: TimeBlockEntity) {
        viewModelScope.launch {
            repository.updateActiveStatus(timeBlock.id, !timeBlock.isActive)
        }
    }

    // === FILTERING ===

    fun setFilterType(type: String?) {
        _uiState.value = _uiState.value.copy(filterType = type)
    }

    fun setShowOnlyActive(showOnlyActive: Boolean) {
        _uiState.value = _uiState.value.copy(showOnlyActive = showOnlyActive)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilterTag(tagId: Long?) {
        _uiState.value = _uiState.value.copy(filterTagId = tagId)
    }

    // Gefilterte TimeBlocks
    val filteredTimeBlocks = combine(
        timeBlocks,
        uiState
    ) { blocks, state ->
        var filtered = blocks

        // Filter: Nur aktive
        if (state.showOnlyActive) {
            filtered = filtered.filter { it.timeBlock.isActive }
        }

        // Filter: Typ
        if (state.filterType != null) {
            filtered = filtered.filter { it.timeBlock.type == state.filterType }
        }

        // Filter: Tag
        if (state.filterTagId != null) {
            filtered = filtered.filter { block ->
                block.tags.any { it.id == state.filterTagId }
            }
        }

        // Filter: Suche
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.timeBlock.name.contains(state.searchQuery, ignoreCase = true) ||
                it.timeBlock.description?.contains(state.searchQuery, ignoreCase = true) == true ||
                it.timeBlock.type?.contains(state.searchQuery, ignoreCase = true) == true ||
                it.tags.any { tag -> tag.name.contains(state.searchQuery, ignoreCase = true) }
            }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class TimeBlockUiState(
    val showDialog: Boolean = false,
    val editingTimeBlock: TimeBlockEntity? = null,
    val showDeleteConfirmation: Boolean = false,
    val timeBlockToDelete: TimeBlockEntity? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showOnlyActive: Boolean = false,
    val filterType: String? = null,
    val filterTagId: Long? = null,
    val searchQuery: String = ""
)
