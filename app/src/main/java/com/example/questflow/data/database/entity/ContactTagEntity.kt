package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Globale Tags für Kontakte (taskübergreifend)
 *
 * Ein Kontakt kann permanent Tags haben wie "VIP", "Kunde", "Lieferant"
 * Diese Tags sind unabhängig von Tasks und immer am Kontakt
 */
@Entity(
    tableName = "contact_tags",
    foreignKeys = [
        ForeignKey(
            entity = MetadataContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MetadataTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("contactId"),
        Index("tagId"),
        Index(value = ["contactId", "tagId"], unique = true) // Keine Duplikate
    ]
)
data class ContactTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val contactId: Long,
    val tagId: Long,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
