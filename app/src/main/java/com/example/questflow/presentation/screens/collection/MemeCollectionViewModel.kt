package com.example.questflow.presentation.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.repository.MemeRepository
import com.example.questflow.data.repository.MemeWithUnlock
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.database.entity.MemeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemeCollectionViewModel @Inject constructor(
    private val memeRepository: MemeRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemeCollectionUiState())
    val uiState: StateFlow<MemeCollectionUiState> = _uiState.asStateFlow()

    init {
        loadMemes()
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            statsRepository.getStatsFlow().collect { stats ->
                _uiState.value = _uiState.value.copy(
                    totalXp = stats.xp,
                    level = stats.level
                )
            }
        }
    }

    fun selectMeme(meme: MemeEntity) {
        _uiState.value = _uiState.value.copy(selectedMeme = meme)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedMeme = null)
    }

    private fun loadMemes() {
        viewModelScope.launch {
            memeRepository.getAllMemesWithUnlocks().collect { memesWithUnlocks ->
                val unlockedCount = memesWithUnlocks.count { it.isUnlocked }
                _uiState.value = MemeCollectionUiState(
                    memes = memesWithUnlocks,
                    unlockedCount = unlockedCount,
                    totalCount = memesWithUnlocks.size
                )
            }
        }
    }
}

data class MemeCollectionUiState(
    val memes: List<MemeWithUnlock> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val totalXp: Long = 0,
    val level: Int = 1,
    val selectedMeme: MemeEntity? = null
)