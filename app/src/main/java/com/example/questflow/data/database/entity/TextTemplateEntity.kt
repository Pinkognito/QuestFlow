package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Textbaustein für wiederverwendbare Nachrichten/Templates
 * Unterstützt dynamische Platzhalter wie {kontakt.name}, {task.title}, etc.
 */
@Entity(tableName = "text_templates")
data class TextTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,                      // "Meeting-Einladung", "Status-Update"
    val content: String,                    // Template-Text mit Platzhaltern
    val description: String? = null,        // Optional: Was macht dieser Baustein?
    val subject: String? = null,            // Optional: Betreff für E-Mails/Termine

    val type: String = "GENERAL",           // GENERAL, WHATSAPP, EMAIL, CALENDAR
    val isDefault: Boolean = false,         // Standard-Vorlage für den Typ

    val usageCount: Int = 0,                // Wie oft wurde er verwendet?
    val lastUsedAt: LocalDateTime? = null,  // Wann zuletzt verwendet?

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class TemplateType {
    GENERAL,    // Allgemeine Vorlage
    WHATSAPP,   // WhatsApp-Nachricht
    EMAIL,      // E-Mail
    CALENDAR    // Kalender-Event
}
