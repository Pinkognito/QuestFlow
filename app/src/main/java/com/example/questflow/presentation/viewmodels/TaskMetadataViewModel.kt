package com.example.questflow.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.domain.model.TaskMetadataItem
import com.example.questflow.domain.usecase.metadata.AddTaskMetadataUseCase
import com.example.questflow.domain.usecase.metadata.DeleteTaskMetadataUseCase
import com.example.questflow.domain.usecase.metadata.GetTaskMetadataUseCase
import com.example.questflow.domain.usecase.metadata.ReorderTaskMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing task metadata (locations, contacts, notes, etc.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskMetadataViewModel @Inject constructor(
    private val getTaskMetadata: GetTaskMetadataUseCase,
    private val addTaskMetadata: AddTaskMetadataUseCase,
    private val deleteTaskMetadata: DeleteTaskMetadataUseCase,
    private val reorderTaskMetadata: ReorderTaskMetadataUseCase
) : ViewModel() {

    private val _currentTaskId = MutableStateFlow<Long?>(null)

    private val _metadataItems = _currentTaskId
        .filterNotNull()
        .flatMapLatest { taskId ->
            Log.d(TAG, "Loading metadata for task $taskId")
            getTaskMetadata(taskId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val metadataItems: StateFlow<List<TaskMetadataItem>> = _metadataItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Sets the task to display metadata for
     */
    fun setTaskId(taskId: Long) {
        Log.d(TAG, "Setting task ID: $taskId")
        _currentTaskId.value = taskId
    }

    /**
     * Adds metadata to the current task
     */
    fun addMetadata(item: TaskMetadataItem) {
        val taskId = _currentTaskId.value
        if (taskId == null) {
            Log.e(TAG, "Cannot add metadata: No task selected")
            _error.value = "No task selected"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d(TAG, "Adding metadata to task $taskId")
            addTaskMetadata(taskId, item)
                .onSuccess {
                    Log.d(TAG, "Successfully added metadata")
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to add metadata", exception)
                    _error.value = "Failed to add metadata: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    /**
     * Deletes a metadata item
     */
    fun deleteMetadata(item: TaskMetadataItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d(TAG, "Deleting metadata ${item.metadataId}")
            deleteTaskMetadata(item)
                .onSuccess {
                    Log.d(TAG, "Successfully deleted metadata")
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to delete metadata", exception)
                    _error.value = "Failed to delete metadata: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    /**
     * Reorders metadata items
     */
    fun reorderMetadata(items: List<TaskMetadataItem>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            Log.d(TAG, "Reordering ${items.size} metadata items")
            reorderTaskMetadata(items)
                .onSuccess {
                    Log.d(TAG, "Successfully reordered metadata")
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to reorder metadata", exception)
                    _error.value = "Failed to reorder metadata: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    /**
     * Clears any error message
     */
    fun clearError() {
        _error.value = null
    }

    companion object {
        private const val TAG = "TaskMetadataViewModel"
    }
}
