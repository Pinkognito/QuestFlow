package com.example.questflow.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val categoryRepository: CategoryRepository,
    private val statsRepository: StatsRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("quest_flow_prefs", Context.MODE_PRIVATE)

    // Global category state
    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory.asStateFlow()

    // All available categories
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Global stats for when no category is selected
    val globalStats = statsRepository.getStatsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Ensure default category exists
        viewModelScope.launch {
            android.util.Log.d("AppViewModel", "Init: Creating default category")
            val defaultCat = categoryRepository.getOrCreateDefaultCategory()
            android.util.Log.d("AppViewModel", "Default category created: ${defaultCat.name}")
            loadSelectedCategory()
        }
    }

    fun selectCategory(category: CategoryEntity?) {
        _selectedCategory.value = category
        // Persist selection to SharedPreferences
        saveSelectedCategoryId(category?.id)
    }

    private fun saveSelectedCategoryId(categoryId: Long?) {
        prefs.edit().apply {
            if (categoryId != null) {
                putLong("selected_category_id", categoryId)
            } else {
                remove("selected_category_id")
            }
            apply()
        }
    }

    private fun loadSelectedCategory() {
        viewModelScope.launch {
            val savedCategoryId = if (prefs.contains("selected_category_id")) {
                prefs.getLong("selected_category_id", -1L).takeIf { it != -1L }
            } else {
                null
            }

            android.util.Log.d("AppViewModel", "Loading selected category, savedId: $savedCategoryId")

            if (savedCategoryId != null) {
                val category = categoryRepository.getCategoryById(savedCategoryId)
                _selectedCategory.value = category
                android.util.Log.d("AppViewModel", "Loaded category: ${category?.name}")
            } else {
                _selectedCategory.value = null
                android.util.Log.d("AppViewModel", "No saved category, using general")
            }
        }
    }
}