package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.TaskContactLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskContactLinkDao {

    @Query("SELECT * FROM task_contact_links WHERE taskId = :taskId")
    fun getLinksByTaskId(taskId: Long): Flow<List<TaskContactLinkEntity>>

    @Query("SELECT * FROM task_contact_links WHERE contactId = :contactId")
    fun getLinksByContactId(contactId: Long): Flow<List<TaskContactLinkEntity>>

    @Query("""
        SELECT tasks.* FROM tasks
        INNER JOIN task_contact_links ON tasks.id = task_contact_links.taskId
        WHERE task_contact_links.contactId = :contactId
        ORDER BY tasks.createdAt DESC
    """)
    fun getTasksByContactId(contactId: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT metadata_contacts.* FROM metadata_contacts
        INNER JOIN task_contact_links ON metadata_contacts.id = task_contact_links.contactId
        WHERE task_contact_links.taskId = :taskId
        ORDER BY metadata_contacts.displayName ASC
    """)
    fun getContactsByTaskId(taskId: Long): Flow<List<MetadataContactEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: TaskContactLinkEntity): Long

    @Delete
    suspend fun delete(link: TaskContactLinkEntity)

    @Query("DELETE FROM task_contact_links WHERE taskId = :taskId AND contactId = :contactId")
    suspend fun deleteLink(taskId: Long, contactId: Long)

    @Query("DELETE FROM task_contact_links WHERE taskId = :taskId")
    suspend fun deleteAllLinksForTask(taskId: Long)

    @Query("DELETE FROM task_contact_links WHERE contactId = :contactId")
    suspend fun deleteAllLinksForContact(contactId: Long)
}
