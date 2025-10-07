package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.TaskContactTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskContactTagDao {
    @Query("SELECT * FROM task_contact_tags WHERE taskId = :taskId")
    fun getTagsForTask(taskId: Long): Flow<List<TaskContactTagEntity>>

    @Query("SELECT * FROM task_contact_tags WHERE taskId = :taskId")
    suspend fun getTagsForTaskSync(taskId: Long): List<TaskContactTagEntity>

    @Query("SELECT * FROM task_contact_tags WHERE taskId = :taskId AND contactId = :contactId")
    suspend fun getTagsForContact(taskId: Long, contactId: Long): List<TaskContactTagEntity>

    @Query("SELECT contactId FROM task_contact_tags WHERE taskId = :taskId AND tag = :tag")
    suspend fun getContactIdsByTag(taskId: Long, tag: String): List<Long>

    @Query("SELECT DISTINCT tag FROM task_contact_tags WHERE taskId = :taskId")
    suspend fun getAllTagsInTask(taskId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TaskContactTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TaskContactTagEntity>)

    @Delete
    suspend fun deleteTag(tag: TaskContactTagEntity)

    @Query("DELETE FROM task_contact_tags WHERE taskId = :taskId AND contactId = :contactId")
    suspend fun deleteTagsForContact(taskId: Long, contactId: Long)

    @Query("DELETE FROM task_contact_tags WHERE taskId = :taskId AND contactId = :contactId AND tag = :tag")
    suspend fun deleteSpecificTag(taskId: Long, contactId: Long, tag: String)

    @Query("DELETE FROM task_contact_tags WHERE taskId = :taskId")
    suspend fun deleteAllTagsInTask(taskId: Long)
}
