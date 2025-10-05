package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores note/text information for tasks
 */
@Entity(tableName = "metadata_notes")
data class MetadataNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val content: String,
    val format: NoteFormat = NoteFormat.PLAIN_TEXT,
    val isPinned: Boolean = false
)

enum class NoteFormat {
    PLAIN_TEXT,
    MARKDOWN,
    HTML
}
