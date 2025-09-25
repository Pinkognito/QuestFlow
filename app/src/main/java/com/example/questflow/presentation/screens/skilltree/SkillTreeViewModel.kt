package com.example.questflow.presentation.screens.skilltree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.repository.SkillNodeWithStatus
import com.example.questflow.data.repository.SkillRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.domain.usecase.UnlockSkillNodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillTreeViewModel @Inject constructor(
    private val skillRepository: SkillRepository,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val unlockSkillNodeUseCase: UnlockSkillNodeUseCase
) : ViewModel() {

    private var selectedCategoryId: Long? = null

    private val _uiState = MutableStateFlow(SkillTreeUiState())
    val uiState: StateFlow<SkillTreeUiState> = _uiState.asStateFlow()

    init {
        loadSkills()
        loadStats()
    }

    fun updateSelectedCategory(categoryId: Long?) {
        selectedCategoryId = categoryId
        loadStats()
    }

    private fun loadSkills() {
        viewModelScope.launch {
            skillRepository.getSkillTreeStatus().collect { skills ->
                _uiState.value = _uiState.value.copy(skills = skills)
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            if (selectedCategoryId != null) {
                // Load category-specific stats
                val category = categoryRepository.getCategoryById(selectedCategoryId!!)
                if (category != null) {
                    _uiState.value = _uiState.value.copy(
                        availablePoints = category.skillPoints,
                        totalXp = category.totalXp.toLong(),
                        level = category.currentLevel
                    )
                }
            } else {
                // Load general stats
                statsRepository.getStatsFlow().collect { stats ->
                    _uiState.value = _uiState.value.copy(
                        availablePoints = stats.points,
                        totalXp = stats.xp,
                        level = stats.level
                    )
                }
            }
        }
    }

    fun unlockSkill(nodeId: String) {
        viewModelScope.launch {
            val result = unlockSkillNodeUseCase(nodeId)
            val notification = if (result.success) {
                "Skill unlocked! ${result.remainingPoints} points remaining"
            } else {
                result.message ?: "Failed to unlock skill"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }
}

data class SkillTreeUiState(
    val skills: List<SkillNodeWithStatus> = emptyList(),
    val availablePoints: Int = 0,
    val notification: String? = null,
    val totalXp: Long = 0,
    val level: Int = 1
)