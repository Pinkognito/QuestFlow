package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.TextTemplateTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextTemplateTagDao {
    @Query("SELECT * FROM text_template_tags WHERE templateId = :templateId")
    fun getTagsForTemplate(templateId: Long): Flow<List<TextTemplateTagEntity>>

    @Query("SELECT * FROM text_template_tags WHERE templateId = :templateId")
    suspend fun getTagsForTemplateSync(templateId: Long): List<TextTemplateTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TextTemplateTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TextTemplateTagEntity>)

    @Delete
    suspend fun deleteTag(tag: TextTemplateTagEntity)

    @Query("DELETE FROM text_template_tags WHERE templateId = :templateId")
    suspend fun deleteTagsForTemplate(templateId: Long)

    @Query("DELETE FROM text_template_tags WHERE templateId = :templateId AND tag = :tag")
    suspend fun deleteSpecificTag(templateId: Long, tag: String)
}
