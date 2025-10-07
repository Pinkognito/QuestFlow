package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tags für Textbausteine zur Organisation
 * Beispiel: "Meeting", "Status", "Erinnerung"
 */
@Entity(
    tableName = "text_template_tags",
    foreignKeys = [
        ForeignKey(
            entity = TextTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId"), Index("tag")]
)
data class TextTemplateTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val templateId: Long,
    val tag: String                         // Tag-Name
)
