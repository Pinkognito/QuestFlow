package com.example.questflow.data.initializer

import com.example.questflow.data.repository.TextTemplateRepository
import com.example.questflow.data.repository.TagUsageRepository
import com.example.questflow.data.database.entity.TextTemplateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes default text templates on first app launch
 */
@Singleton
class TextTemplateInitializer @Inject constructor(
    private val templateRepository: TextTemplateRepository,
    private val tagUsageRepository: TagUsageRepository
) {
    private val defaultTemplates = listOf(
        TextTemplateEntity(
            title = "Meeting-Einladung Standard",
            content = """Hallo {kontakt.name},

hiermit lade ich dich zum Meeting für "{task.title}" ein.

Termin: {datum.heute}
Ort: {standort.name}

Bitte bestätige kurz deine Teilnahme.

Viele Grüße""",
            description = "Standard-Einladung für Meetings",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        ) to listOf("Meeting", "Einladung"),

        TextTemplateEntity(
            title = "Status-Update",
            content = """Hi {kontakt.name},

Update zum Task "{task.title}":

[Hier dein Status einfügen]

Bei Fragen melde dich gerne!""",
            description = "Kurzes Status-Update teilen",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        ) to listOf("Status", "Update"),

        TextTemplateEntity(
            title = "Dringende Rückmeldung",
            content = """WICHTIG: {kontakt.name}

Bzgl. "{task.title}" benötige ich dringend deine Rückmeldung.

Bitte melde dich so schnell wie möglich!

Danke!""",
            description = "Für dringende Anfragen",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        ) to listOf("Dringend", "Erinnerung"),

        TextTemplateEntity(
            title = "Termin-Bestätigung",
            content = """Hallo {kontakt.name},

zur Bestätigung: Unser Termin am {datum.heute} um {zeit.jetzt} Uhr.

Ort: {standort.straße}, {standort.plz} {standort.stadt}

Bis dann!""",
            description = "Termin bestätigen mit Ort",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        ) to listOf("Termin", "Bestätigung"),

        TextTemplateEntity(
            title = "Projekt-Einladung",
            content = """Hallo {kontakt.name},

ich möchte dich gerne für das Projekt "{task.title}" gewinnen.

{task.beschreibung}

Hast du Zeit und Interesse? Lass uns kurz telefonieren!

Viele Grüße""",
            description = "Einladung zur Projekt-Mitarbeit",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        ) to listOf("Projekt", "Einladung"),

        TextTemplateEntity(
            title = "Danke & Follow-Up",
            content = """Hallo {kontakt.name},

vielen Dank für deine Unterstützung bei "{task.title}"!

Ich melde mich in den nächsten Tagen mit dem nächsten Update.

Beste Grüße""",
            description = "Dankeschön mit Follow-Up Ankündigung",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        ) to listOf("Danke", "Follow-Up")
    )

    /**
     * Initialize default templates if none exist
     */
    suspend fun initializeIfNeeded() {
        try {
            val templates = templateRepository.getAllTemplatesFlow()
            var existingCount = 0

            // Collect the first emission to check count
            templates.collect { templateList ->
                existingCount = templateList.size
                return@collect // Exit after first emission
            }

            if (existingCount == 0) {
                android.util.Log.d("TextTemplateInit", "Initializing default templates...")

                defaultTemplates.forEach { (template, tags) ->
                    templateRepository.saveTemplateWithTags(template, tags)
                    tagUsageRepository.incrementUsageForMultiple(tags)
                }

                android.util.Log.d("TextTemplateInit", "Default templates initialized successfully!")
            }
        } catch (e: Exception) {
            android.util.Log.e("TextTemplateInit", "Failed to initialize templates", e)
        }
    }

    /**
     * Call this from Application onCreate
     */
    fun initializeAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            initializeIfNeeded()
        }
    }
}
