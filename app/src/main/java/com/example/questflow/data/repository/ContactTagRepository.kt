package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.ContactTagDao
import com.example.questflow.data.database.dao.MetadataContactDao
import com.example.questflow.data.database.entity.ContactTagEntity
import com.example.questflow.data.database.entity.MetadataTagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactTagRepository @Inject constructor(
    private val contactTagDao: ContactTagDao,
    private val contactDao: MetadataContactDao,
    private val tagRepository: TagRepository
) {

    // ========== CRUD ==========

    suspend fun addTagToContact(contactId: Long, tagId: Long) {
        contactTagDao.addTagToContact(contactId, tagId)
    }

    suspend fun removeTagFromContact(contactId: Long, tagId: Long) {
        contactTagDao.deleteByContactAndTag(contactId, tagId)
    }

    suspend fun setContactTags(contactId: Long, tagIds: List<Long>) {
        contactTagDao.setTagsForContact(contactId, tagIds)
    }

    suspend fun clearContactTags(contactId: Long) {
        contactTagDao.deleteAllByContact(contactId)
    }

    // ========== Queries: Tags for Contact ==========

    suspend fun getTagIdsForContact(contactId: Long): List<Long> {
        return contactTagDao.getTagIds(contactId)
    }

    fun getTagIdsForContactFlow(contactId: Long): Flow<List<Long>> {
        return contactTagDao.getTagIdsFlow(contactId)
    }

    suspend fun getTagsForContact(contactId: Long): List<MetadataTagEntity> {
        return contactTagDao.getTagsForContact(contactId)
    }

    fun getTagsForContactFlow(contactId: Long): Flow<List<MetadataTagEntity>> {
        return contactTagDao.getTagsForContactFlow(contactId)
    }

    /**
     * Tags für Kontakt MIT Hierarchie-Expansion
     * Wenn Kontakt "Kunde" hat → auch "VIP-Kunde", "Neukunde" inkludieren
     */
    suspend fun getExpandedTagsForContact(contactId: Long): List<MetadataTagEntity> {
        val directTags = contactTagDao.getTagIds(contactId)
        return tagRepository.getTagsWithDescendants(directTags)
    }

    // ========== Queries: Contacts for Tag ==========

    suspend fun getContactIdsWithTag(tagId: Long): List<Long> {
        return contactTagDao.getContactIds(tagId)
    }

    suspend fun getContactCountForTag(tagId: Long): Int {
        return contactTagDao.getContactCountForTag(tagId)
    }

    /**
     * Kontakte die ein Tag haben (MIT Hierarchie)
     * Wenn nach "Kunde" gesucht wird → auch Kontakte mit "VIP-Kunde", "Neukunde"
     */
    suspend fun getContactIdsWithTagExpanded(tagId: Long): List<Long> {
        val expandedTags = tagRepository.getTagWithDescendants(tagId)
        val expandedTagIds = expandedTags.map { it.id }
        return contactTagDao.findContactsWithAnyTag(expandedTagIds)
    }

    // ========== Search & Filter ==========

    /**
     * Finde Kontakte die ALLE Tags haben (AND)
     */
    suspend fun findContactsWithAllTags(tagIds: List<Long>): List<Long> {
        if (tagIds.isEmpty()) return emptyList()
        return contactTagDao.findContactsWithAllTags(tagIds)
    }

    /**
     * Finde Kontakte die MINDESTENS EINEN Tag haben (OR)
     */
    suspend fun findContactsWithAnyTag(tagIds: List<Long>): List<Long> {
        if (tagIds.isEmpty()) return emptyList()
        return contactTagDao.findContactsWithAnyTag(tagIds)
    }

    /**
     * Finde Kontakte mit Tag-Suche MIT Hierarchie
     */
    suspend fun findContactsWithTagsExpanded(tagIds: List<Long>, matchAll: Boolean = false): List<Long> {
        if (tagIds.isEmpty()) return emptyList()

        // Expandiere alle Tags mit ihren Children
        val expandedTagIds = tagRepository.getTagsWithDescendants(tagIds).map { it.id }

        return if (matchAll) {
            contactTagDao.findContactsWithAllTags(expandedTagIds)
        } else {
            contactTagDao.findContactsWithAnyTag(expandedTagIds)
        }
    }

    // ========== Batch Operations ==========

    /**
     * Kopiere Tags von einem Kontakt zu einem anderen
     */
    suspend fun copyTagsToContact(fromContactId: Long, toContactId: Long) {
        val tagIds = contactTagDao.getTagIds(fromContactId)
        contactTagDao.setTagsForContact(toContactId, tagIds)
    }

    /**
     * Merge Tags: Füge Tags hinzu ohne bestehende zu entfernen
     */
    suspend fun mergeTagsToContact(contactId: Long, additionalTagIds: List<Long>) {
        val existingTagIds = contactTagDao.getTagIds(contactId)
        val mergedTagIds = (existingTagIds + additionalTagIds).distinct()
        contactTagDao.setTagsForContact(contactId, mergedTagIds)
    }

    // ========== Utility Functions ==========

    /**
     * Liefert alle Tags vom Typ CONTACT die tatsächlich verwendet werden
     * (d.h. mindestens einem Kontakt zugewiesen sind)
     */
    suspend fun getUsedContactTags(): List<MetadataTagEntity> {
        // Hole alle Tag-IDs die verwendet werden
        val usedTagIds = contactTagDao.getAllUsedTagIds()
        if (usedTagIds.isEmpty()) return emptyList()

        // Hole die vollständigen Tag-Entities
        return usedTagIds.mapNotNull { tagId ->
            tagRepository.getTagById(tagId)
        }.filter { it.type == com.example.questflow.data.database.entity.TagType.CONTACT }
    }

    /**
     * Erstellt eine Map: ContactId -> List<MetadataTagEntity>
     * Für alle übergebenen Kontakte
     */
    suspend fun getContactTagsMap(contactIds: List<Long>): Map<Long, List<MetadataTagEntity>> {
        return contactIds.associateWith { contactId ->
            getTagsForContact(contactId)
        }
    }
}
