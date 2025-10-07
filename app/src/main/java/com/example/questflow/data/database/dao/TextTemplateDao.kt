package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.TextTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextTemplateDao {
    @Query("SELECT * FROM text_templates ORDER BY lastUsedAt DESC, usageCount DESC")
    fun getAllTemplatesFlow(): Flow<List<TextTemplateEntity>>

    @Query("SELECT * FROM text_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): TextTemplateEntity?

    @Query("""
        SELECT DISTINCT t.* FROM text_templates t
        LEFT JOIN text_template_tags tt ON t.id = tt.templateId
        WHERE t.title LIKE '%' || :query || '%'
           OR t.content LIKE '%' || :query || '%'
           OR tt.tag LIKE '%' || :query || '%'
        ORDER BY t.usageCount DESC, t.lastUsedAt DESC
    """)
    suspend fun searchTemplates(query: String): List<TextTemplateEntity>

    @Query("""
        SELECT t.* FROM text_templates t
        INNER JOIN text_template_tags tt ON t.id = tt.templateId
        WHERE tt.tag = :tag
        ORDER BY t.usageCount DESC
    """)
    suspend fun getTemplatesByTag(tag: String): List<TextTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TextTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TextTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: TextTemplateEntity)

    @Query("UPDATE text_templates SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: java.time.LocalDateTime)
}
