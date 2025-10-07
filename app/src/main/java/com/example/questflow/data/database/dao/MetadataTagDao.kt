package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataTagEntity
import com.example.questflow.data.database.entity.TagType
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataTagDao {

    // ========== CRUD Operations ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: MetadataTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<MetadataTagEntity>)

    @Update
    suspend fun update(tag: MetadataTagEntity)

    @Delete
    suspend fun delete(tag: MetadataTagEntity)

    @Query("DELETE FROM metadata_tags WHERE id = :tagId")
    suspend fun deleteById(tagId: Long)

    // ========== Basic Queries ==========

    @Query("SELECT * FROM metadata_tags WHERE id = :tagId")
    suspend fun getById(tagId: Long): MetadataTagEntity?

    @Query("SELECT * FROM metadata_tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): MetadataTagEntity?

    @Query("SELECT * FROM metadata_tags ORDER BY name ASC")
    fun getAllFlow(): Flow<List<MetadataTagEntity>>

    @Query("SELECT * FROM metadata_tags ORDER BY name ASC")
    suspend fun getAll(): List<MetadataTagEntity>

    // ========== Filter by Type ==========

    @Query("SELECT * FROM metadata_tags WHERE type = :type ORDER BY name ASC")
    fun getByTypeFlow(type: TagType): Flow<List<MetadataTagEntity>>

    @Query("SELECT * FROM metadata_tags WHERE type = :type ORDER BY name ASC")
    suspend fun getByType(type: TagType): List<MetadataTagEntity>

    @Query("""
        SELECT * FROM metadata_tags
        WHERE type IN (:types)
        ORDER BY name ASC
    """)
    suspend fun getByTypes(types: List<TagType>): List<MetadataTagEntity>

    // ========== Hierarchie Queries ==========

    /**
     * Alle Root-Tags (ohne Parent)
     */
    @Query("SELECT * FROM metadata_tags WHERE parentTagId IS NULL ORDER BY name ASC")
    fun getRootTagsFlow(): Flow<List<MetadataTagEntity>>

    @Query("SELECT * FROM metadata_tags WHERE parentTagId IS NULL ORDER BY name ASC")
    suspend fun getRootTags(): List<MetadataTagEntity>

    /**
     * Alle Child-Tags eines Parents
     */
    @Query("SELECT * FROM metadata_tags WHERE parentTagId = :parentId ORDER BY name ASC")
    suspend fun getChildTags(parentId: Long): List<MetadataTagEntity>

    @Query("SELECT * FROM metadata_tags WHERE parentTagId = :parentId ORDER BY name ASC")
    fun getChildTagsFlow(parentId: Long): Flow<List<MetadataTagEntity>>

    /**
     * Parent-Tag eines Tags
     */
    @Query("""
        SELECT parent.* FROM metadata_tags child
        INNER JOIN metadata_tags parent ON child.parentTagId = parent.id
        WHERE child.id = :childId
    """)
    suspend fun getParentTag(childId: Long): MetadataTagEntity?

    /**
     * Alle Ancestors (rekursiv nach oben)
     * Benötigt WITH RECURSIVE - alternative Implementierung in Repository
     */

    // ========== Search ==========

    @Query("""
        SELECT * FROM metadata_tags
        WHERE name LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 1 ELSE 2 END,
            name ASC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<MetadataTagEntity>

    @Query("""
        SELECT * FROM metadata_tags
        WHERE type = :type AND name LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 1 ELSE 2 END,
            name ASC
        LIMIT :limit
    """)
    suspend fun searchByType(query: String, type: TagType, limit: Int = 50): List<MetadataTagEntity>

    // ========== Statistics ==========

    @Query("SELECT COUNT(*) FROM metadata_tags")
    suspend fun getTagCount(): Int

    @Query("SELECT COUNT(*) FROM metadata_tags WHERE type = :type")
    suspend fun getTagCountByType(type: TagType): Int

    @Query("SELECT COUNT(*) FROM metadata_tags WHERE parentTagId = :parentId")
    suspend fun getChildCount(parentId: Long): Int
}
