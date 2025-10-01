package com.example.questflow.presentation.screens.skilltree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.repository.SkillNodeWithStatus
import com.example.questflow.data.repository.SkillRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.domain.usecase.InvestSkillPointUseCase
import com.example.questflow.domain.usecase.RefundSkillPointUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillTreeViewModel @Inject constructor(
    private val skillRepository: SkillRepository,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val investSkillPointUseCase: InvestSkillPointUseCase,
    private val refundSkillPointUseCase: RefundSkillPointUseCase,
    private val manageSkillNodeUseCase: com.example.questflow.domain.usecase.ManageSkillNodeUseCase
) : ViewModel() {

    private var selectedCategoryId: Long? = null

    private val _uiState = MutableStateFlow(SkillTreeUiState())
    val uiState: StateFlow<SkillTreeUiState> = _uiState.asStateFlow()

    private val _edges = MutableStateFlow<List<com.example.questflow.data.database.entity.SkillEdgeEntity>>(emptyList())
    val edges: StateFlow<List<com.example.questflow.data.database.entity.SkillEdgeEntity>> = _edges.asStateFlow()

    init {
        loadSkills()
        loadStats()
        loadEdges()
    }

    fun updateSelectedCategory(categoryId: Long?) {
        selectedCategoryId = categoryId
        loadSkills()
        loadStats()
    }

    private fun loadSkills() {
        viewModelScope.launch {
            skillRepository.getSkillTreeStatus(selectedCategoryId).collect { skills ->
                _uiState.value = _uiState.value.copy(skills = skills)
            }
        }
    }

    private fun loadEdges() {
        viewModelScope.launch {
            // Load all edges for visualization
            skillRepository.skillDao.getAllEdges().collect { edgesList ->
                _edges.value = edgesList
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            if (selectedCategoryId != null) {
                // Load category-specific stats - reactive updates
                categoryRepository.getCategoryFlow(selectedCategoryId!!).collect { category ->
                    if (category != null) {
                        _uiState.value = _uiState.value.copy(
                            availablePoints = category.skillPoints,
                            totalXp = category.totalXp.toLong(),
                            level = category.currentLevel
                        )
                        android.util.Log.d("SkillTreeVM", "Category stats loaded: SP=${category.skillPoints}, Level=${category.currentLevel}")
                    }
                }
            } else {
                // Load general stats - reactive updates
                statsRepository.getStatsFlow().collect { stats ->
                    _uiState.value = _uiState.value.copy(
                        availablePoints = stats.points,
                        totalXp = stats.xp,
                        level = stats.level
                    )
                    android.util.Log.d("SkillTreeVM", "Global stats loaded: SP=${stats.points}, Level=${stats.level}")
                }
            }
        }
    }

    fun investSkillPoint(nodeId: String) {
        viewModelScope.launch {
            val result = investSkillPointUseCase(nodeId, selectedCategoryId)
            val notification = if (result.success) {
                "Investiert! ${result.currentInvestment}/${result.maxInvestment} | ${result.remainingPoints} Punkte übrig"
            } else {
                result.message ?: "Investierung fehlgeschlagen"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }

    fun refundSkillPoint(nodeId: String) {
        viewModelScope.launch {
            val result = refundSkillPointUseCase(nodeId, selectedCategoryId)
            val notification = if (result.success) {
                "Punkt zurückerstattet! ${result.remainingPoints} Punkte verfügbar"
            } else {
                result.message ?: "Rückerstattung fehlgeschlagen"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(notification = null)
    }

    fun createSkill(
        title: String,
        description: String,
        effectType: com.example.questflow.data.database.entity.SkillEffectType,
        baseValue: Float,
        scalingPerPoint: Float,
        maxInvestment: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            val result = manageSkillNodeUseCase.createSkill(
                title = title,
                description = description,
                effectType = effectType,
                baseValue = baseValue,
                scalingPerPoint = scalingPerPoint,
                maxInvestment = maxInvestment,
                iconName = "star",
                positionX = 80f + ((uiState.value.skills.size % 4) * 100f),
                positionY = 80f + ((uiState.value.skills.size / 4) * 120f),
                colorHex = colorHex,
                categoryId = selectedCategoryId
            )

            val notification = if (result.success) {
                "Skill '$title' erstellt!"
            } else {
                result.message ?: "Fehler beim Erstellen"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }

    fun createSkillWithParents(
        title: String,
        description: String,
        effectType: com.example.questflow.data.database.entity.SkillEffectType,
        baseValue: Float,
        scalingPerPoint: Float,
        maxInvestment: Int,
        colorHex: String,
        parentSkills: List<Pair<String, Int>>
    ) {
        viewModelScope.launch {
            val result = manageSkillNodeUseCase.createSkill(
                title = title,
                description = description,
                effectType = effectType,
                baseValue = baseValue,
                scalingPerPoint = scalingPerPoint,
                maxInvestment = maxInvestment,
                iconName = "star",
                positionX = 80f + ((uiState.value.skills.size % 4) * 100f),
                positionY = 80f + ((uiState.value.skills.size / 4) * 120f),
                colorHex = colorHex,
                categoryId = selectedCategoryId
            )

            if (result.success && result.nodeId != null) {
                // Create edges for parent skills
                parentSkills.forEach { (parentId, minInvestment) ->
                    manageSkillNodeUseCase.createEdge(parentId, result.nodeId!!, minInvestment)
                }

                _uiState.value = _uiState.value.copy(
                    notification = "Skill '$title' erstellt mit ${parentSkills.size} Voraussetzung(en)!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    notification = result.message ?: "Fehler beim Erstellen"
                )
            }
        }
    }

    fun linkSkill(childId: String, parentId: String, minInvestment: Int) {
        viewModelScope.launch {
            val result = manageSkillNodeUseCase.createEdge(parentId, childId, minInvestment)
            val notification = if (result.success) {
                "Skills verknüpft!"
            } else {
                result.message ?: "Fehler beim Verknüpfen"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }

    fun unlinkSkill(childId: String, parentId: String) {
        viewModelScope.launch {
            val result = manageSkillNodeUseCase.deleteEdge(parentId, childId)
            val notification = if (result.success) {
                "Verknüpfung entfernt!"
            } else {
                result.message ?: "Fehler beim Entfernen"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }

    fun updateSkill(
        nodeId: String,
        title: String,
        description: String,
        baseValue: Float,
        scalingPerPoint: Float,
        maxInvestment: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            val node = skillRepository.getSkillNode(nodeId) ?: return@launch
            val updated = node.copy(
                title = title,
                description = description,
                baseValue = baseValue,
                scalingPerPoint = scalingPerPoint,
                maxInvestment = maxInvestment,
                colorHex = colorHex
            )
            val result = manageSkillNodeUseCase.updateSkill(updated)
            val notification = if (result.success) {
                "Skill '$title' aktualisiert!"
            } else {
                result.message ?: "Fehler beim Aktualisieren"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }

    fun deleteSkill(nodeId: String) {
        viewModelScope.launch {
            val result = manageSkillNodeUseCase.deleteSkill(nodeId)
            val notification = if (result.success) {
                "Skill gelöscht!"
            } else {
                result.message ?: "Fehler beim Löschen"
            }
            _uiState.value = _uiState.value.copy(notification = notification)
        }
    }

    fun updateSkillPosition(nodeId: String, x: Float, y: Float) {
        viewModelScope.launch {
            val node = skillRepository.getSkillNode(nodeId) ?: return@launch
            val updated = node.copy(positionX = x, positionY = y)
            skillRepository.updateSkillNode(updated)
        }
    }
}

data class SkillTreeUiState(
    val skills: List<SkillNodeWithStatus> = emptyList(),
    val availablePoints: Int = 0,
    val notification: String? = null,
    val totalXp: Long = 0,
    val level: Int = 1,
    val editMode: Boolean = false
)