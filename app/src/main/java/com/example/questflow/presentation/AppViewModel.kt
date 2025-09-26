package com.example.questflow.presentation

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.domain.model.UserStats
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
    private val _globalStats = MutableStateFlow<UserStats?>(null)
    val globalStats: StateFlow<UserStats?> = _globalStats.asStateFlow()

    // XP animation state
    private val _xpAnimationState = MutableStateFlow<XpAnimationState?>(null)
    val xpAnimationState: StateFlow<XpAnimationState?> = _xpAnimationState.asStateFlow()

    // Previous XP for animation
    private var previousGlobalXp = 0L
    private var previousCategoryXp = mutableMapOf<Long, Long>()

    init {
        // Ensure default category exists
        viewModelScope.launch {
            android.util.Log.d("AppViewModel", "Init: Creating default category")
            val defaultCat = categoryRepository.getOrCreateDefaultCategory()
            android.util.Log.d("AppViewModel", "Default category created: ${defaultCat.name}")
            loadSelectedCategory()
            observeStats()
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            statsRepository.getStatsFlow().collect { stats ->
                _globalStats.value = stats

                // Trigger animation if XP increased
                if (stats != null && stats.xp > previousGlobalXp && previousGlobalXp > 0) {
                    _xpAnimationState.value = XpAnimationState(
                        previousXp = previousGlobalXp,
                        newXp = stats.xp,
                        previousLevel = stats.level,
                        newLevel = stats.level,
                        isCategory = false
                    )
                }
                previousGlobalXp = stats?.xp ?: 0L
            }
        }

        viewModelScope.launch {
            selectedCategory.collect { category ->
                if (category != null) {
                    categoryRepository.getCategoryFlow(category.id).collect { updatedCategory ->
                        if (updatedCategory != null) {
                            val prevXp = previousCategoryXp[category.id] ?: 0L
                            if (updatedCategory.totalXp > prevXp && prevXp > 0) {
                                _xpAnimationState.value = XpAnimationState(
                                    previousXp = prevXp.toLong(),
                                    newXp = updatedCategory.totalXp.toLong(),
                                    previousLevel = updatedCategory.currentLevel,
                                    newLevel = updatedCategory.currentLevel,
                                    isCategory = true
                                )
                                // Update selected category with new values
                                _selectedCategory.value = updatedCategory
                            }
                            previousCategoryXp[category.id] = updatedCategory.totalXp.toLong()
                        }
                    }
                }
            }
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

    fun clearXpAnimation() {
        _xpAnimationState.value = null
    }

    fun refreshStats() {
        viewModelScope.launch {
            // Force refresh of current stats
            val stats = statsRepository.getOrCreateStats()
            _globalStats.value = stats

            // Refresh category if selected
            _selectedCategory.value?.let { category ->
                val updated = categoryRepository.getCategoryById(category.id)
                _selectedCategory.value = updated
            }
        }
    }
}

data class XpAnimationState(
    val previousXp: Long,
    val newXp: Long,
    val previousLevel: Int,
    val newLevel: Int,
    val isCategory: Boolean
)