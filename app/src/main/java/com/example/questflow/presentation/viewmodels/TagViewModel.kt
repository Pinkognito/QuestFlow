package com.example.questflow.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questflow.data.database.entity.MetadataTagEntity
import com.example.questflow.data.database.entity.TagType
import com.example.questflow.data.repository.ContactTagRepository
import com.example.questflow.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val contactTagRepository: ContactTagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagUiState())
    val uiState: StateFlow<TagUiState> = _uiState.asStateFlow()

    private val _selectedType = MutableStateFlow<TagType?>(null)
    val selectedType: StateFlow<TagType?> = _selectedType.asStateFlow()

    // All tags
    val allTags = tagRepository.getAllTagsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered tags based on selected type
    val filteredTags = combine(_selectedType, allTags) { type, tags ->
        if (type == null) tags else tags.filter { it.type == type }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun filterByType(type: TagType?) {
        _selectedType.value = type
    }

    fun createTag(
        name: String,
        type: TagType,
        parentTagId: Long? = null,
        color: String? = null,
        icon: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            try {
                val tag = MetadataTagEntity(
                    name = name,
                    type = type,
                    parentTagId = parentTagId,
                    color = color,
                    icon = icon,
                    description = description
                )
                tagRepository.insertTag(tag)
                _uiState.value = _uiState.value.copy(success = "Tag '$name' erstellt")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateTag(tag: MetadataTagEntity) {
        viewModelScope.launch {
            try {
                tagRepository.updateTag(tag)
                _uiState.value = _uiState.value.copy(success = "Tag aktualisiert")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteTag(tag: MetadataTagEntity) {
        viewModelScope.launch {
            try {
                tagRepository.deleteTag(tag)
                _uiState.value = _uiState.value.copy(success = "Tag gelöscht")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    suspend fun getTagById(tagId: Long): MetadataTagEntity? {
        return tagRepository.getTagById(tagId)
    }

    suspend fun getChildTags(parentId: Long): List<MetadataTagEntity> {
        return tagRepository.getChildTags(parentId)
    }

    suspend fun getParentTag(childId: Long): MetadataTagEntity? {
        return tagRepository.getParentTag(childId)
    }

    suspend fun getTagPath(tagId: Long): List<MetadataTagEntity> {
        return tagRepository.getTagPath(tagId)
    }

    suspend fun hasChildren(tagId: Long): Boolean {
        return tagRepository.hasChildren(tagId)
    }

    suspend fun searchTags(query: String): List<MetadataTagEntity> {
        return tagRepository.searchTags(query)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(success = null, error = null)
    }

    // ========== Contact Tag Management ==========

    /**
     * Erstellt ein neues CONTACT-Tag mit Standard-Farbe
     */
    fun createContactTag(name: String) {
        createTag(
            name = name,
            type = TagType.CONTACT,
            color = "#2196F3", // Standard-Blau für Kontakt-Tags
            description = "Benutzer-erstelltes Kontakt-Tag"
        )
    }

    /**
     * Liefert alle CONTACT-Typ Tags
     */
    fun getContactTags(): Flow<List<MetadataTagEntity>> {
        return allTags.map { tags ->
            tags.filter { it.type == TagType.CONTACT }
        }
    }

    /**
     * Fügt einem Kontakt ein Tag hinzu
     */
    fun addTagToContact(contactId: Long, tagId: Long) {
        viewModelScope.launch {
            try {
                contactTagRepository.addTagToContact(contactId, tagId)
                _uiState.value = _uiState.value.copy(success = "Tag hinzugefügt")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Entfernt ein Tag von einem Kontakt
     */
    fun removeTagFromContact(contactId: Long, tagId: Long) {
        viewModelScope.launch {
            try {
                contactTagRepository.removeTagFromContact(contactId, tagId)
                _uiState.value = _uiState.value.copy(success = "Tag entfernt")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Setzt alle Tags für einen Kontakt (ersetzt bestehende)
     */
    fun setContactTags(contactId: Long, tagIds: Set<Long>) {
        viewModelScope.launch {
            try {
                contactTagRepository.setContactTags(contactId, tagIds.toList())
                _uiState.value = _uiState.value.copy(success = "Tags aktualisiert")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Liefert alle Tags für einen Kontakt
     */
    fun getTagsForContact(contactId: Long): Flow<List<MetadataTagEntity>> {
        return contactTagRepository.getTagsForContactFlow(contactId)
    }

    /**
     * Liefert nur verwendete CONTACT-Tags (die mindestens einem Kontakt zugewiesen sind)
     */
    suspend fun getUsedContactTags(): List<MetadataTagEntity> {
        return contactTagRepository.getUsedContactTags()
    }

    /**
     * Erstellt eine Map: ContactId -> List<MetadataTagEntity>
     */
    suspend fun getContactTagsMap(contactIds: List<Long>): Map<Long, List<MetadataTagEntity>> {
        return contactTagRepository.getContactTagsMap(contactIds)
    }
}

data class TagUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)
