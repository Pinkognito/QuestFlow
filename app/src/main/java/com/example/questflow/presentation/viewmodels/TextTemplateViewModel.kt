package com.example.questflow.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.TextTemplateEntity
import com.example.questflow.data.database.entity.TextTemplateTagEntity
import com.example.questflow.data.repository.TextTemplateRepository
import com.example.questflow.data.repository.TagUsageRepository
import com.example.questflow.data.repository.TagSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TextTemplateViewModel @Inject constructor(
    private val templateRepository: TextTemplateRepository,
    private val tagUsageRepository: TagUsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextTemplateUiState())
    val uiState: StateFlow<TextTemplateUiState> = _uiState.asStateFlow()

    val templates: StateFlow<List<TextTemplateEntity>> = templateRepository.getAllTemplatesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    val filteredTemplates: StateFlow<List<TextTemplateEntity>> = combine(
        templates,
        _searchQuery,
        _selectedTagFilter
    ) { templates, query, tagFilter ->
        var filtered = templates

        // Filter by tag
        if (tagFilter != null) {
            val taggedTemplateIds = templateRepository.getTemplatesByTag(tagFilter).map { it.id }
            filtered = filtered.filter { it.id in taggedTemplateIds }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
            }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun createTemplate(
        title: String,
        content: String,
        description: String?,
        subject: String?,
        type: String,
        isDefault: Boolean,
        tags: List<String>
    ) {
        viewModelScope.launch {
            try {
                val template = TextTemplateEntity(
                    title = title,
                    content = content,
                    description = description,
                    subject = subject,
                    type = type,
                    isDefault = isDefault,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )

                val templateId = templateRepository.saveTemplateWithTags(template, tags)

                // Update tag usage stats
                tagUsageRepository.incrementUsageForMultiple(tags)

                _uiState.value = _uiState.value.copy(
                    showSuccess = true,
                    successMessage = "Textbaustein erstellt!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showError = true,
                    errorMessage = "Fehler: ${e.message}"
                )
            }
        }
    }

    fun updateTemplate(template: TextTemplateEntity, tags: List<String>) {
        viewModelScope.launch {
            try {
                templateRepository.saveTemplateWithTags(template, tags)
                tagUsageRepository.incrementUsageForMultiple(tags)

                _uiState.value = _uiState.value.copy(
                    showSuccess = true,
                    successMessage = "Textbaustein aktualisiert!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showError = true,
                    errorMessage = "Fehler: ${e.message}"
                )
            }
        }
    }

    fun deleteTemplate(template: TextTemplateEntity) {
        viewModelScope.launch {
            try {
                templateRepository.deleteTemplate(template)
                _uiState.value = _uiState.value.copy(
                    showSuccess = true,
                    successMessage = "Textbaustein gelöscht!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showError = true,
                    errorMessage = "Fehler: ${e.message}"
                )
            }
        }
    }

    suspend fun getTagsForTemplate(templateId: Long): List<String> {
        return templateRepository.getTagsForTemplateSync(templateId).map { it.tag }
    }

    suspend fun getTagSuggestions(query: String): List<TagSuggestion> {
        return tagUsageRepository.getRankedSuggestions(query, 10)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(showSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(showError = false)
    }
}

data class TextTemplateUiState(
    val showSuccess: Boolean = false,
    val successMessage: String = "",
    val showError: Boolean = false,
    val errorMessage: String = ""
)
