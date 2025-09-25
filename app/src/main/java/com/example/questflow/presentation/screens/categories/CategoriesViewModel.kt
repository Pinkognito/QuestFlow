package com.example.questflow.presentation.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.domain.usecase.category.CreateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val createCategoryUseCase: CreateCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    val categories = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            editingCategory = null
        )
    }

    fun showEditDialog(category: CategoryEntity) {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            editingCategory = category
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = false,
            editingCategory = null
        )
    }

    fun createCategory(
        name: String,
        description: String,
        color: String,
        emoji: String,
        levelScalingFactor: Float
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            if (_uiState.value.editingCategory != null) {
                // Update existing category
                val updatedCategory = _uiState.value.editingCategory!!.copy(
                    name = name,
                    description = description,
                    color = color,
                    emoji = emoji,
                    levelScalingFactor = levelScalingFactor
                )
                categoryRepository.updateCategory(updatedCategory)
            } else {
                // Create new category
                createCategoryUseCase(
                    name = name,
                    description = description,
                    color = color,
                    emoji = emoji,
                    levelScalingFactor = levelScalingFactor
                )
            }
            dismissDialog()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}

data class CategoriesUiState(
    val showCreateDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val isLoading: Boolean = false
)