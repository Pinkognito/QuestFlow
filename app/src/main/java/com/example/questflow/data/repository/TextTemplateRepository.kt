package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.TextTemplateDao
import com.example.questflow.data.database.dao.TextTemplateTagDao
import com.example.questflow.data.database.entity.TextTemplateEntity
import com.example.questflow.data.database.entity.TextTemplateTagEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextTemplateRepository @Inject constructor(
    private val templateDao: TextTemplateDao,
    private val tagDao: TextTemplateTagDao
) {
    fun getAllTemplatesFlow(): Flow<List<TextTemplateEntity>> = templateDao.getAllTemplatesFlow()

    suspend fun getTemplateById(id: Long): TextTemplateEntity? = templateDao.getTemplateById(id)

    suspend fun searchTemplates(query: String): List<TextTemplateEntity> = templateDao.searchTemplates(query)

    suspend fun getTemplatesByTag(tag: String): List<TextTemplateEntity> = templateDao.getTemplatesByTag(tag)

    suspend fun insertTemplate(template: TextTemplateEntity): Long = templateDao.insertTemplate(template)

    suspend fun updateTemplate(template: TextTemplateEntity) = templateDao.updateTemplate(template)

    suspend fun deleteTemplate(template: TextTemplateEntity) = templateDao.deleteTemplate(template)

    suspend fun incrementTemplateUsage(id: Long) {
        templateDao.incrementUsage(id, LocalDateTime.now())
    }

    // Tag Management
    fun getTagsForTemplate(templateId: Long): Flow<List<TextTemplateTagEntity>> =
        tagDao.getTagsForTemplate(templateId)

    suspend fun getTagsForTemplateSync(templateId: Long): List<TextTemplateTagEntity> =
        tagDao.getTagsForTemplateSync(templateId)

    suspend fun insertTag(tag: TextTemplateTagEntity) = tagDao.insertTag(tag)

    suspend fun insertTags(tags: List<TextTemplateTagEntity>) = tagDao.insertTags(tags)

    suspend fun deleteTag(tag: TextTemplateTagEntity) = tagDao.deleteTag(tag)

    suspend fun deleteTagsForTemplate(templateId: Long) = tagDao.deleteTagsForTemplate(templateId)

    suspend fun deleteSpecificTag(templateId: Long, tag: String) =
        tagDao.deleteSpecificTag(templateId, tag)

    /**
     * Saves template with tags in a single transaction
     */
    suspend fun saveTemplateWithTags(template: TextTemplateEntity, tags: List<String>): Long {
        val templateId = if (template.id == 0L) {
            insertTemplate(template)
        } else {
            updateTemplate(template.copy(updatedAt = LocalDateTime.now()))
            template.id
        }

        // Replace all tags
        deleteTagsForTemplate(templateId)
        if (tags.isNotEmpty()) {
            insertTags(tags.map { TextTemplateTagEntity(templateId = templateId, tag = it) })
        }

        return templateId
    }
}
