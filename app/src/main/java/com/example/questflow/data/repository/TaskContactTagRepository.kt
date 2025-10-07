package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.TaskContactTagDao
import com.example.questflow.data.database.entity.TaskContactTagEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskContactTagRepository @Inject constructor(
    private val taskContactTagDao: TaskContactTagDao
) {
    fun getTagsForTask(taskId: Long): Flow<List<TaskContactTagEntity>> =
        taskContactTagDao.getTagsForTask(taskId)

    suspend fun getTagsForTaskSync(taskId: Long): List<TaskContactTagEntity> =
        taskContactTagDao.getTagsForTaskSync(taskId)

    suspend fun getTagsForContact(taskId: Long, contactId: Long): List<TaskContactTagEntity> =
        taskContactTagDao.getTagsForContact(taskId, contactId)

    suspend fun getContactIdsByTag(taskId: Long, tag: String): List<Long> =
        taskContactTagDao.getContactIdsByTag(taskId, tag)

    suspend fun getAllTagsInTask(taskId: Long): List<String> =
        taskContactTagDao.getAllTagsInTask(taskId)

    suspend fun insertTag(tag: TaskContactTagEntity) = taskContactTagDao.insertTag(tag)

    suspend fun insertTags(tags: List<TaskContactTagEntity>) = taskContactTagDao.insertTags(tags)

    suspend fun deleteTag(tag: TaskContactTagEntity) = taskContactTagDao.deleteTag(tag)

    suspend fun deleteTagsForContact(taskId: Long, contactId: Long) =
        taskContactTagDao.deleteTagsForContact(taskId, contactId)

    suspend fun deleteSpecificTag(taskId: Long, contactId: Long, tag: String) =
        taskContactTagDao.deleteSpecificTag(taskId, contactId, tag)

    suspend fun deleteAllTagsInTask(taskId: Long) = taskContactTagDao.deleteAllTagsInTask(taskId)

    /**
     * Save tags for a contact in a task
     * Replaces all existing tags for this contact
     */
    suspend fun saveTagsForContact(taskId: Long, contactId: Long, tags: List<String>) {
        deleteTagsForContact(taskId, contactId)
        if (tags.isNotEmpty()) {
            insertTags(tags.map {
                TaskContactTagEntity(
                    taskId = taskId,
                    contactId = contactId,
                    tag = it,
                    createdAt = LocalDateTime.now()
                )
            })
        }
    }

    /**
     * Get all contacts grouped by their tags in a task
     */
    suspend fun getContactsByTags(taskId: Long): Map<String, List<Long>> {
        val allTags = getTagsForTaskSync(taskId)
        return allTags.groupBy { it.tag }.mapValues { (_, tagEntities) ->
            tagEntities.map { it.contactId }
        }
    }

    /**
     * Save all task-contact-tag relationships
     * contactTagMap: Map<contactId, List<tags>>
     */
    suspend fun saveTaskContactTags(taskId: Long, contactTagMap: Map<Long, List<String>>) {
        contactTagMap.forEach { (contactId, tags) ->
            saveTagsForContact(taskId, contactId, tags)
        }
    }
}
