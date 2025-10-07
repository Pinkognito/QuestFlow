package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.MetadataTagDao
import com.example.questflow.data.database.entity.MetadataTagEntity
import com.example.questflow.data.database.entity.TagType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: MetadataTagDao
) {

    // ========== CRUD ==========

    suspend fun insertTag(tag: MetadataTagEntity): Long = tagDao.insert(tag)

    suspend fun insertTags(tags: List<MetadataTagEntity>) = tagDao.insertAll(tags)

    suspend fun updateTag(tag: MetadataTagEntity) = tagDao.update(tag)

    suspend fun deleteTag(tag: MetadataTagEntity) = tagDao.delete(tag)

    suspend fun deleteTagById(tagId: Long) = tagDao.deleteById(tagId)

    // ========== Basic Queries ==========

    suspend fun getTagById(tagId: Long): MetadataTagEntity? = tagDao.getById(tagId)

    suspend fun getTagByName(name: String): MetadataTagEntity? = tagDao.getByName(name)

    fun getAllTagsFlow(): Flow<List<MetadataTagEntity>> = tagDao.getAllFlow()

    suspend fun getAllTags(): List<MetadataTagEntity> = tagDao.getAll()

    // ========== Filter by Type ==========

    fun getTagsByTypeFlow(type: TagType): Flow<List<MetadataTagEntity>> = tagDao.getByTypeFlow(type)

    suspend fun getTagsByType(type: TagType): List<MetadataTagEntity> = tagDao.getByType(type)

    suspend fun getTagsByTypes(types: List<TagType>): List<MetadataTagEntity> = tagDao.getByTypes(types)

    /**
     * Hole Tags für Kontext (z.B. Task-Dialog zeigt TASK + GENERAL Tags)
     */
    suspend fun getTagsForContext(primaryType: TagType): List<MetadataTagEntity> {
        return tagDao.getByTypes(listOf(primaryType, TagType.GENERAL))
    }

    // ========== Hierarchie ==========

    fun getRootTagsFlow(): Flow<List<MetadataTagEntity>> = tagDao.getRootTagsFlow()

    suspend fun getRootTags(): List<MetadataTagEntity> = tagDao.getRootTags()

    suspend fun getChildTags(parentId: Long): List<MetadataTagEntity> = tagDao.getChildTags(parentId)

    fun getChildTagsFlow(parentId: Long): Flow<List<MetadataTagEntity>> = tagDao.getChildTagsFlow(parentId)

    suspend fun getParentTag(childId: Long): MetadataTagEntity? = tagDao.getParentTag(childId)

    /**
     * Rekursiv alle Descendants (Kinder, Enkel, etc.) eines Tags holen
     *
     * Beispiel: "Kunde" → ["VIP-Kunde", "Neukunde", "Stammkunde"]
     */
    suspend fun getAllDescendants(parentId: Long): List<MetadataTagEntity> {
        val result = mutableListOf<MetadataTagEntity>()
        val queue = ArrayDeque<Long>()
        queue.add(parentId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val children = tagDao.getChildTags(currentId)
            result.addAll(children)
            queue.addAll(children.map { it.id })
        }

        return result
    }

    /**
     * Tag MIT allen Descendants (inkl. sich selbst)
     * Für automatische Expansion bei Suchen
     */
    suspend fun getTagWithDescendants(tagId: Long): List<MetadataTagEntity> {
        val parent = tagDao.getById(tagId) ?: return emptyList()
        val descendants = getAllDescendants(tagId)
        return listOf(parent) + descendants
    }

    /**
     * Mehrere Tags MIT allen ihren Descendants
     */
    suspend fun getTagsWithDescendants(tagIds: List<Long>): List<MetadataTagEntity> {
        val allTags = mutableSetOf<MetadataTagEntity>()
        tagIds.forEach { tagId ->
            allTags.addAll(getTagWithDescendants(tagId))
        }
        return allTags.toList()
    }

    /**
     * Rekursiv alle Ancestors (Parent, Großparent, etc.) eines Tags holen
     */
    suspend fun getAllAncestors(childId: Long): List<MetadataTagEntity> {
        val result = mutableListOf<MetadataTagEntity>()
        var currentId: Long? = childId

        while (currentId != null) {
            val parent = tagDao.getParentTag(currentId)
            if (parent != null) {
                result.add(parent)
                currentId = parent.id
            } else {
                currentId = null
            }
        }

        return result
    }

    /**
     * Vollständiger Pfad von Root bis zu diesem Tag
     * Beispiel: ["Kunde", "VIP-Kunde", "Platinum-Kunde"]
     */
    suspend fun getTagPath(tagId: Long): List<MetadataTagEntity> {
        val tag = tagDao.getById(tagId) ?: return emptyList()
        val ancestors = getAllAncestors(tagId).reversed() // Von Root nach unten
        return ancestors + tag
    }

    // ========== Search ==========

    suspend fun searchTags(query: String, limit: Int = 50): List<MetadataTagEntity> {
        return tagDao.search(query, limit)
    }

    suspend fun searchTagsByType(query: String, type: TagType, limit: Int = 50): List<MetadataTagEntity> {
        return tagDao.searchByType(query, type, limit)
    }

    // ========== Statistics ==========

    suspend fun getTagCount(): Int = tagDao.getTagCount()

    suspend fun getTagCountByType(type: TagType): Int = tagDao.getTagCountByType(type)

    suspend fun getChildCount(parentId: Long): Int = tagDao.getChildCount(parentId)

    /**
     * Prüfe ob Tag Kinder hat
     */
    suspend fun hasChildren(tagId: Long): Boolean = getChildCount(tagId) > 0

    // ========== Helper: Get or Create ==========

    /**
     * Hole Tag nach Name oder erstelle neuen
     */
    suspend fun getOrCreateTag(name: String, type: TagType, parentId: Long? = null): MetadataTagEntity {
        val existing = tagDao.getByName(name)
        if (existing != null) return existing

        val newTag = MetadataTagEntity(
            name = name,
            type = type,
            parentTagId = parentId
        )
        val id = tagDao.insert(newTag)
        return newTag.copy(id = id)
    }

    /**
     * Initialisiere Standard-Tags beim ersten App-Start
     * Wird automatisch von QuestFlowApplication aufgerufen
     */
    suspend fun initializeStandardTagsIfEmpty() {
        val count = getTagCount()
        if (count > 0) return // Tags existieren bereits

        // CONTACT Tags mit Hierarchie
        val kundeTag = MetadataTagEntity(
            name = "Kunde",
            type = TagType.CONTACT,
            color = "#2196F3",
            icon = "business",
            description = "Geschäftskunden"
        )
        val kundeId = insertTag(kundeTag)

        insertTags(listOf(
            MetadataTagEntity(name = "VIP-Kunde", type = TagType.CONTACT, parentTagId = kundeId, color = "#FF9800", icon = "star", description = "VIP Geschäftskunde"),
            MetadataTagEntity(name = "Neukunde", type = TagType.CONTACT, parentTagId = kundeId, color = "#4CAF50", icon = "new_releases", description = "Neuer Kunde"),
            MetadataTagEntity(name = "Stammkunde", type = TagType.CONTACT, parentTagId = kundeId, color = "#9C27B0", icon = "loyalty", description = "Treuer Stammkunde"),
            MetadataTagEntity(name = "Lieferant", type = TagType.CONTACT, color = "#FF5722", icon = "local_shipping", description = "Lieferanten und Partner"),
            MetadataTagEntity(name = "Kollege", type = TagType.CONTACT, color = "#795548", icon = "people", description = "Arbeitskollegen"),
            MetadataTagEntity(name = "Familie", type = TagType.CONTACT, color = "#E91E63", icon = "family_restroom", description = "Familienmitglieder"),
            MetadataTagEntity(name = "Freund", type = TagType.CONTACT, color = "#3F51B5", icon = "favorite", description = "Persönliche Freunde")
        ))

        // TASK Tags mit Hierarchie
        val dringendTag = MetadataTagEntity(
            name = "Dringend",
            type = TagType.TASK,
            color = "#F44336",
            icon = "priority_high",
            description = "Dringende Aufgaben"
        )
        val dringendId = insertTag(dringendTag)

        insertTags(listOf(
            MetadataTagEntity(name = "Sofort", type = TagType.TASK, parentTagId = dringendId, color = "#D32F2F", icon = "notification_important", description = "Sofort erledigen"),
            MetadataTagEntity(name = "Heute", type = TagType.TASK, parentTagId = dringendId, color = "#FF5722", icon = "today", description = "Heute erledigen"),
            MetadataTagEntity(name = "Wichtig", type = TagType.TASK, color = "#FF9800", icon = "label_important", description = "Wichtige Aufgaben"),
            MetadataTagEntity(name = "Optional", type = TagType.TASK, color = "#9E9E9E", icon = "low_priority", description = "Optionale Aufgaben"),
            MetadataTagEntity(name = "Privat", type = TagType.TASK, color = "#E91E63", icon = "lock", description = "Private Aufgaben"),
            MetadataTagEntity(name = "Geschäftlich", type = TagType.TASK, color = "#2196F3", icon = "work", description = "Geschäftliche Aufgaben"),
            MetadataTagEntity(name = "Projekt", type = TagType.TASK, color = "#9C27B0", icon = "folder", description = "Projekt-Aufgaben")
        ))

        // LOCATION Tags
        insertTags(listOf(
            MetadataTagEntity(name = "Büro", type = TagType.LOCATION, color = "#607D8B", icon = "business_center", description = "Büro-Standorte"),
            MetadataTagEntity(name = "Home-Office", type = TagType.LOCATION, color = "#4CAF50", icon = "home", description = "Zuhause arbeiten"),
            MetadataTagEntity(name = "Remote", type = TagType.LOCATION, color = "#00BCD4", icon = "laptop", description = "Remote Arbeit"),
            MetadataTagEntity(name = "Baustelle", type = TagType.LOCATION, color = "#FF9800", icon = "construction", description = "Baustellen"),
            MetadataTagEntity(name = "Filiale", type = TagType.LOCATION, color = "#3F51B5", icon = "store", description = "Filialen")
        ))

        // TEMPLATE Tags
        insertTags(listOf(
            MetadataTagEntity(name = "Business", type = TagType.TEMPLATE, color = "#2196F3", icon = "business", description = "Geschäftliche Templates"),
            MetadataTagEntity(name = "Privat", type = TagType.TEMPLATE, color = "#E91E63", icon = "person", description = "Private Templates"),
            MetadataTagEntity(name = "Meeting", type = TagType.TEMPLATE, color = "#9C27B0", icon = "event", description = "Meeting Templates"),
            MetadataTagEntity(name = "E-Mail", type = TagType.TEMPLATE, color = "#FF5722", icon = "email", description = "E-Mail Templates")
        ))

        // GENERAL Tags
        insertTags(listOf(
            MetadataTagEntity(name = "Favorit", type = TagType.GENERAL, color = "#FFC107", icon = "star", description = "Favoriten"),
            MetadataTagEntity(name = "Archiv", type = TagType.GENERAL, color = "#9E9E9E", icon = "archive", description = "Archiviert"),
            MetadataTagEntity(name = "Review", type = TagType.GENERAL, color = "#00BCD4", icon = "rate_review", description = "Zu überprüfen")
        ))
    }
}
