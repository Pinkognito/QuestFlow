package com.example.questflow.presentation.screens.collection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.CollectionItemEntity
import com.example.questflow.data.repository.CollectionItemWithUnlock
import com.example.questflow.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CollectionViewModel"
    }

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var currentCategoryFilter: Long? = null
    private var isInitialized = false

    init {
        Log.d(TAG, "CollectionViewModel initialized - waiting for category filter")
        // Don't load items here - wait for setCategoryFilter() call from LaunchedEffect
    }

    fun setCategoryFilter(categoryId: Long?) {
        Log.d(TAG, "📂 [CATEGORY_FILTER] Setting category filter: ${categoryId ?: "ALL"}")

        // Only reload if category actually changed or this is first load
        if (!isInitialized || currentCategoryFilter != categoryId) {
            currentCategoryFilter = categoryId
            isInitialized = true
            loadItems(categoryId)
        } else {
            Log.d(TAG, "📂 [CATEGORY_FILTER] Category unchanged, skipping reload")
        }
    }

    private fun loadItems(categoryId: Long?) {
        viewModelScope.launch {
            try {
                val flow = if (categoryId == null) {
                    // No category selected: show ALL items (global + all category-specific)
                    collectionRepository.getAllItemsWithUnlocks()
                } else {
                    // Category selected: show global items + category-specific items
                    collectionRepository.getGlobalAndCategoryItemsWithUnlocks(categoryId)
                }

                flow.collect { itemsWithUnlocks ->
                    val unlockedCount = itemsWithUnlocks.count { it.isUnlocked }

                    Log.d(TAG, "Loaded ${itemsWithUnlocks.size} items ($unlockedCount unlocked) for category: $categoryId")

                    _uiState.value = _uiState.value.copy(
                        items = itemsWithUnlocks,
                        unlockedCount = unlockedCount,
                        totalCount = itemsWithUnlocks.size
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading collection items", e)
            }
        }
    }

    fun selectItem(item: CollectionItemEntity) {
        Log.d(TAG, "Selected item: ${item.id} - ${item.name}")
        _uiState.value = _uiState.value.copy(selectedItem = item)
    }

    fun clearSelection() {
        Log.d(TAG, "Cleared selection")
        _uiState.value = _uiState.value.copy(selectedItem = null)
    }
}

data class CollectionUiState(
    val items: List<CollectionItemWithUnlock> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val selectedItem: CollectionItemEntity? = null
)
