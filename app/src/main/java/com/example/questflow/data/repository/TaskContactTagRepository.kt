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
        android.util.Log.d("TaskTags", "    saveTagsForContact - deleting old tags for contact $contactId")
        deleteTagsForContact(taskId, contactId)
        if (tags.isNotEmpty()) {
            android.util.Log.d("TaskTags", "    saveTagsForContact - inserting ${tags.size} new tags: $tags")
            insertTags(tags.map {
                TaskContactTagEntity(
                    taskId = taskId,
                    contactId = contactId,
                    tag = it,
                    createdAt = LocalDateTime.now()
                )
            })
        } else {
            android.util.Log.d("TaskTags", "    saveTagsForContact - no tags to insert (empty list)")
        }
    }

    /**
     * Save all task-contact-tag relationships
     * contactTagMap: Map<contactId, List<tags>>
     */
    suspend fun saveTaskContactTags(taskId: Long, contactTagMap: Map<Long, List<String>>) {
        android.util.Log.d("TaskTags", "Repository.saveTaskContactTags - taskId: $taskId, contactTagMap: $contactTagMap")
        contactTagMap.forEach { (contactId, tags) ->
            android.util.Log.d("TaskTags", "  Saving tags for contact $contactId: $tags")
            saveTagsForContact(taskId, contactId, tags)
        }
        android.util.Log.d("TaskTags", "Repository.saveTaskContactTags - completed")
    }

    /**
     * Load all task-contact-tags as Map<ContactId, List<TagNames>>
     */
    suspend fun loadTaskContactTagsMap(taskId: Long): Map<Long, List<String>> {
        android.util.Log.d("TaskTags", "Repository.loadTaskContactTagsMap - taskId: $taskId")
        val allTags = getTagsForTaskSync(taskId)
        android.util.Log.d("TaskTags", "  Found ${allTags.size} tag entities in DB")
        val result = allTags.groupBy { it.contactId }.mapValues { (_, tagEntities) ->
            tagEntities.map { it.tag }
        }
        android.util.Log.d("TaskTags", "Repository.loadTaskContactTagsMap - returning: $result")
        return result
    }
}
