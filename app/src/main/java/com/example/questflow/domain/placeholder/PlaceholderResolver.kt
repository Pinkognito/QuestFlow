package com.example.questflow.domain.placeholder

import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.TaskDao
import com.example.questflow.data.database.dao.MetadataContactDao
import com.example.questflow.data.database.dao.MetadataLocationDao
import com.example.questflow.data.database.entity.MetadataContactEntity
import com.example.questflow.data.database.entity.MetadataLocationEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamisches Platzhalter-System mit Metadaten-Verschachtelung
 *
 * Unterstützte Platzhalter:
 * - {kontakt.name}, {kontakt.firma}, {kontakt.telefon}
 * - {task.title}, {task.description}
 * - {standort.name}, {standort.postleitzahl}, {standort.straße}
 * - {standort[N].feldname} - Array-Zugriff für mehrere verknüpfte Standorte
 * - {datum}, {zeit}, {datum.zeit}
 */
@Singleton
class PlaceholderResolver @Inject constructor(
    private val contactDao: MetadataContactDao,
    private val locationDao: MetadataLocationDao,
    private val taskDao: TaskDao
) {
    companion object {
        private val PLACEHOLDER_REGEX = Regex("""\{([^}]+)}""")
        private val ARRAY_ACCESS_REGEX = Regex("""(\w+)\[(\d+)]\.(\w+)""")
    }

    /**
     * Resolve all placeholders in a template for a specific task and contact
     */
    suspend fun resolve(
        template: String,
        taskId: Long,
        contactId: Long? = null
    ): String {
        var resolved = template

        val task = taskDao.getTaskById(taskId)
        val contact = contactId?.let { contactDao.getById(it) }
        // TODO: Add task-location linking in future
        val locations = emptyList<MetadataLocationEntity>()

        // Find all placeholders
        val placeholders = PLACEHOLDER_REGEX.findAll(template).map { it.groupValues[1] }.toList()

        for (placeholder in placeholders) {
            val value = resolvePlaceholder(placeholder, task, contact, locations)
            resolved = resolved.replace("{$placeholder}", value)
        }

        return resolved
    }

    /**
     * Resolve placeholders for multiple contacts (batch operation)
     * Returns a map: contactId -> resolved text
     */
    suspend fun resolveForContacts(
        template: String,
        taskId: Long,
        contactIds: List<Long>
    ): Map<Long, String> {
        return contactIds.associateWith { contactId ->
            resolve(template, taskId, contactId)
        }
    }

    private suspend fun resolvePlaceholder(
        placeholder: String,
        task: TaskEntity?,
        contact: MetadataContactEntity?,
        locations: List<MetadataLocationEntity>
    ): String {
        // Check for array access: standort[2].postleitzahl
        val arrayMatch = ARRAY_ACCESS_REGEX.matchEntire(placeholder)
        if (arrayMatch != null) {
            val (_, entity, indexStr, field) = arrayMatch.groupValues
            val index = indexStr.toIntOrNull() ?: return "{$placeholder}"

            return when (entity.lowercase()) {
                "standort" -> resolveLocationArrayAccess(locations, index, field)
                else -> "{$placeholder}" // Unknown entity
            }
        }

        // Split by dot for nested access
        val parts = placeholder.split(".")
        if (parts.size < 2) return "{$placeholder}"

        val entity = parts[0].lowercase()
        val field = parts[1].lowercase()

        return when (entity) {
            "kontakt" -> resolveContactField(contact, field)
            "task" -> resolveTaskField(task, field)
            "standort" -> resolveLocationField(locations.firstOrNull(), field)
            "datum" -> resolveDateField(field)
            "zeit" -> resolveTimeField(field)
            else -> "{$placeholder}" // Unknown entity
        }
    }

    private fun resolveContactField(contact: MetadataContactEntity?, field: String): String {
        if (contact == null) return ""

        return when (field) {
            "name" -> contact.displayName
            "firma", "organization" -> contact.organization ?: ""
            "telefon", "phone" -> contact.primaryPhone ?: ""
            "email" -> contact.primaryEmail ?: ""
            "notizen", "notes" -> contact.note ?: ""
            else -> "{kontakt.$field}"
        }
    }

    private fun resolveTaskField(task: TaskEntity?, field: String): String {
        if (task == null) return ""

        return when (field) {
            "title", "titel" -> task.title
            "description", "beschreibung" -> task.description ?: ""
            "fällig", "due" -> task.dueDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: ""
            else -> "{task.$field}"
        }
    }

    private fun resolveLocationField(location: MetadataLocationEntity?, field: String): String {
        if (location == null) return ""

        return when (field) {
            "name" -> location.placeName
            "straße", "street", "strasse" -> location.street ?: ""
            "plz", "postleitzahl", "zip" -> location.postalCode ?: ""
            "stadt", "city" -> location.city ?: ""
            "land", "country" -> location.country ?: ""
            else -> "{standort.$field}"
        }
    }

    private fun resolveLocationArrayAccess(
        locations: List<MetadataLocationEntity>,
        index: Int,
        field: String
    ): String {
        if (index >= locations.size) return ""
        return resolveLocationField(locations[index], field)
    }

    private fun resolveDateField(field: String): String {
        val now = LocalDateTime.now()
        return when (field) {
            "heute", "today" -> now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            "morgen", "tomorrow" -> now.plusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            "zeit" -> now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            else -> now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
    }

    private fun resolveTimeField(field: String): String {
        val now = LocalDateTime.now()
        return when (field) {
            "jetzt", "now" -> now.format(DateTimeFormatter.ofPattern("HH:mm"))
            else -> now.format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }

    /**
     * Get list of available placeholders for UI help
     */
    fun getAvailablePlaceholders(): List<PlaceholderInfo> {
        return listOf(
            PlaceholderInfo("Kontakt", listOf(
                "{kontakt.name}" to "Name des Kontakts",
                "{kontakt.firma}" to "Firmenname",
                "{kontakt.notizen}" to "Notizen zum Kontakt"
            )),
            PlaceholderInfo("Task", listOf(
                "{task.title}" to "Task-Titel",
                "{task.beschreibung}" to "Task-Beschreibung",
                "{task.fällig}" to "Fälligkeitsdatum"
            )),
            PlaceholderInfo("Standort", listOf(
                "{standort.name}" to "Standortname",
                "{standort.straße}" to "Straße",
                "{standort.plz}" to "Postleitzahl",
                "{standort.stadt}" to "Stadt",
                "{standort[0].plz}" to "PLZ des ersten Standorts"
            )),
            PlaceholderInfo("Datum/Zeit", listOf(
                "{datum.heute}" to "Heutiges Datum",
                "{datum.morgen}" to "Morgiges Datum",
                "{zeit.jetzt}" to "Aktuelle Uhrzeit",
                "{datum.zeit}" to "Datum und Uhrzeit"
            ))
        )
    }
}

data class PlaceholderInfo(
    val category: String,
    val placeholders: List<Pair<String, String>> // placeholder to description
)
