package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.ContactTagEntity
import com.example.questflow.data.database.entity.MetadataTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactTagDao {

    // ========== CRUD Operations ==========

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contactTag: ContactTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contactTags: List<ContactTagEntity>)

    @Delete
    suspend fun delete(contactTag: ContactTagEntity)

    @Query("DELETE FROM contact_tags WHERE contactId = :contactId AND tagId = :tagId")
    suspend fun deleteByContactAndTag(contactId: Long, tagId: Long)

    @Query("DELETE FROM contact_tags WHERE contactId = :contactId")
    suspend fun deleteAllByContact(contactId: Long)

    // ========== Queries: Tags for Contact ==========

    /**
     * Alle Tag-IDs eines Kontakts
     */
    @Query("SELECT tagId FROM contact_tags WHERE contactId = :contactId")
    suspend fun getTagIds(contactId: Long): List<Long>

    @Query("SELECT tagId FROM contact_tags WHERE contactId = :contactId")
    fun getTagIdsFlow(contactId: Long): Flow<List<Long>>

    /**
     * Alle Tag-Entities eines Kontakts (mit JOIN)
     */
    @Query("""
        SELECT t.* FROM metadata_tags t
        INNER JOIN contact_tags ct ON t.id = ct.tagId
        WHERE ct.contactId = :contactId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsForContact(contactId: Long): List<MetadataTagEntity>

    @Query("""
        SELECT t.* FROM metadata_tags t
        INNER JOIN contact_tags ct ON t.id = ct.tagId
        WHERE ct.contactId = :contactId
        ORDER BY t.name ASC
    """)
    fun getTagsForContactFlow(contactId: Long): Flow<List<MetadataTagEntity>>

    // ========== Queries: Contacts for Tag ==========

    /**
     * Alle Kontakt-IDs die ein bestimmtes Tag haben
     */
    @Query("SELECT contactId FROM contact_tags WHERE tagId = :tagId")
    suspend fun getContactIds(tagId: Long): List<Long>

    /**
     * Anzahl Kontakte mit einem Tag
     */
    @Query("SELECT COUNT(*) FROM contact_tags WHERE tagId = :tagId")
    suspend fun getContactCountForTag(tagId: Long): Int

    // ========== Batch Operations ==========

    /**
     * Setze Tags für einen Kontakt (ersetzt alle bestehenden)
     */
    @Transaction
    suspend fun setTagsForContact(contactId: Long, tagIds: List<Long>) {
        deleteAllByContact(contactId)
        if (tagIds.isNotEmpty()) {
            val entities = tagIds.map { tagId ->
                ContactTagEntity(contactId = contactId, tagId = tagId)
            }
            insertAll(entities)
        }
    }

    /**
     * Füge Tag zu Kontakt hinzu (wenn nicht vorhanden)
     */
    suspend fun addTagToContact(contactId: Long, tagId: Long) {
        insert(ContactTagEntity(contactId = contactId, tagId = tagId))
    }

    // ========== Search & Filter ==========

    /**
     * Suche Kontakte die ALLE angegebenen Tags haben (AND)
     */
    @Query("""
        SELECT contactId FROM contact_tags
        WHERE tagId IN (:tagIds)
        GROUP BY contactId
        HAVING COUNT(DISTINCT tagId) = :tagCount
    """)
    suspend fun findContactsWithAllTags(tagIds: List<Long>, tagCount: Int = tagIds.size): List<Long>

    /**
     * Suche Kontakte die MINDESTENS EINEN der Tags haben (OR)
     */
    @Query("""
        SELECT DISTINCT contactId FROM contact_tags
        WHERE tagId IN (:tagIds)
    """)
    suspend fun findContactsWithAnyTag(tagIds: List<Long>): List<Long>

    // ========== Utility Queries ==========

    /**
     * Liefert alle Tag-IDs die tatsächlich verwendet werden
     * (d.h. mindestens einem Kontakt zugewiesen sind)
     */
    @Query("SELECT DISTINCT tagId FROM contact_tags ORDER BY tagId")
    suspend fun getAllUsedTagIds(): List<Long>
}
