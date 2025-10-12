package com.example.questflow.data.debug

import android.content.Context
import android.util.Log
import com.example.questflow.data.database.QuestFlowDatabase
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initialisiert Debug-Testdaten für die App
 * Wird beim ersten Start automatisch ausgeführt wenn die Datenbank leer ist
 */
@Singleton
class DebugDataInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: QuestFlowDatabase
) {
    companion object {
        private const val TAG = "DebugDataInitializer"
        private const val DEBUG_CATEGORY_NAME = "🐛 Debug"
    }

    /**
     * Prüft ob Debug-Daten bereits existieren (anhand Debug-Kategorie)
     */
    suspend fun shouldInitialize(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "📋 Checking if debug data should be initialized...")
        Log.d(TAG, "📋 Looking for category: $DEBUG_CATEGORY_NAME")

        val categories = database.categoryDao().getAllCategoriesOnce()
        Log.d(TAG, "📋 Found ${categories.size} categories in database")
        categories.forEach { category ->
            Log.d(TAG, "📋   - Category: ${category.name}")
        }

        val hasDebugCategory = categories.any { it.name == DEBUG_CATEGORY_NAME }
        Log.d(TAG, "📋 Has debug category: $hasDebugCategory")

        val shouldInit = !hasDebugCategory
        Log.d(TAG, "📋 Should initialize: $shouldInit")

        shouldInit
    }

    /**
     * Initialisiert ALLE Debug-Testdaten
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting debug data initialization...")

            // Run ALL initialization in a single database transaction
            database.runInTransaction {
                runBlocking {
                    // 1. Debug-Kategorie erstellen
                    val categoryId = initializeDebugCategory()
                    Log.d(TAG, "✅ Debug category created: $categoryId")

                    // 2. Globale Tags erstellen
                    val tagIds = initializeTags()
                    Log.d(TAG, "✅ Tags created: ${tagIds.size}")

                    // 3. Kontakte erstellen (fabian1, fabian2)
                    val contactIds = initializeContacts(tagIds)
                    Log.d(TAG, "✅ Contacts created: ${contactIds.size}")

                    // 4. Standorte erstellen
                    val locationIds = initializeLocations()
                    Log.d(TAG, "✅ Locations created: ${locationIds.size}")

                    // 5. Textbausteine erstellen
                    val templateIds = initializeTextTemplates()
                    Log.d(TAG, "✅ Text templates created: ${templateIds.size}")

                    // 6. Tasks erstellen (mit verschiedenen Recurring-Konfigurationen)
                    val taskIds = initializeTasks(categoryId, contactIds, locationIds)
                    Log.d(TAG, "✅ Tasks created: ${taskIds.size}")

                    // 6b. Calendar Links für Tasks erstellen (für XP-Claiming)
                    val linkIds = initializeCalendarLinks(taskIds, categoryId)
                    Log.d(TAG, "✅ Calendar links created: ${linkIds.size}")

                    // 6c. XP-Transaktionen für abgeschlossene Tasks
                    initializeXpTransactionsForCompletedTasks(taskIds)
                    Log.d(TAG, "✅ XP transactions created for completed tasks")

                    // 7. Skills erstellen
                    val skillIds = initializeSkills(categoryId)
                    Log.d(TAG, "✅ Skills created: ${skillIds.size}")

                    // 8. User Stats initialisieren (Level 5, etwas XP)
                    initializeUserStats()
                    Log.d(TAG, "✅ User stats initialized")

                    // 9. Media Library Items (Placeholder-Bilder/GIFs)
                    val mediaIds = initializeMediaLibrary()
                    Log.d(TAG, "✅ Media library items created: ${mediaIds.size}")

                    // 10. Statistiken und Charts erstellen
                    initializeStatistics()
                    Log.d(TAG, "✅ Statistics initialized")

                    Log.d(TAG, "🎉 Debug data initialization completed successfully!")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize debug data", e)
            throw e
        }
    }

    // ========== 1. DEBUG KATEGORIE ==========
    private suspend fun initializeDebugCategory(): Long {
        val category = CategoryEntity(
            name = DEBUG_CATEGORY_NAME,
            description = "Automatisch generierte Test-Daten für Entwicklung",
            color = "#9C27B0", // Purple
            emoji = "🐛",
            currentXp = 5000,
            currentLevel = 3,
            totalXp = 5000,
            skillPoints = 10,
            isActive = true
        )
        return database.categoryDao().insertCategory(category)
    }

    // ========== 2. GLOBALE TAGS ==========
    private suspend fun initializeTags(): Map<String, Long> {
        val tags = listOf(
            MetadataTagEntity(
                name = "VIP",
                type = TagType.CONTACT,
                color = "#FFD700",
                icon = "star",
                description = "Wichtige Kontakte"
            ),
            MetadataTagEntity(
                name = "Dringend",
                type = TagType.TASK,
                color = "#F44336",
                icon = "priority_high",
                description = "Dringende Aufgaben"
            ),
            MetadataTagEntity(
                name = "Privat",
                type = TagType.GENERAL,
                color = "#2196F3",
                icon = "home",
                description = "Private Einträge"
            ),
            MetadataTagEntity(
                name = "Arbeit",
                type = TagType.GENERAL,
                color = "#FF9800",
                icon = "work",
                description = "Arbeitsbezogene Einträge"
            ),
            MetadataTagEntity(
                name = "Büro",
                type = TagType.LOCATION,
                color = "#607D8B",
                icon = "business",
                description = "Büro-Standorte"
            )
        )

        return tags.associate { tag ->
            val id = database.metadataTagDao().insert(tag)
            tag.name to id
        }
    }

    // ========== 3. KONTAKTE ==========
    private suspend fun initializeContacts(tagIds: Map<String, Long>): List<Long> {
        // Kontakt 1: Fabian Test 1 mit echten Daten
        val contact1 = MetadataContactEntity(
            displayName = "Fabian Test 1",
            givenName = "Fabian",
            familyName = "Beckmann",
            primaryPhone = "+4915159031829",
            primaryEmail = "fabian_beckmann@outlook.de",
            iconEmoji = "👨‍💻",
            organization = "QuestFlow Dev",
            jobTitle = "Developer",
            note = "Haupt-Test-Kontakt mit vollständigen Metadaten"
        )
        val contactId1 = database.metadataContactDao().insert(contact1)

        // Telefon für Kontakt 1
        try {
            database.metadataPhoneDao().insert(
                MetadataPhoneEntity(
                    contactId = contactId1,
                    phoneNumber = "+4915159031829",
                    phoneType = PhoneType.MOBILE,
                    label = "Mobil"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert phone for contact 1", e)
        }

        // Email für Kontakt 1
        try {
            database.metadataEmailDao().insert(
                MetadataEmailEntity(
                    contactId = contactId1,
                    emailAddress = "fabian_beckmann@outlook.de",
                    emailType = EmailType.PERSONAL,
                    label = "Privat"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert email for contact 1", e)
        }

        // Adresse für Kontakt 1
        try {
            database.metadataAddressDao().insert(
                MetadataAddressEntity(
                    contactId = contactId1,
                    street = "Baumgarten 42",
                    city = "Bremerhaven",
                    postalCode = "27654",
                    country = "Deutschland",
                    addressType = AddressType.HOME,
                    label = "Zuhause"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert address for contact 1", e)
        }

        // Kontakt 2: Fabian Test 2
        val contact2 = MetadataContactEntity(
            displayName = "Fabian Test 2",
            givenName = "Fabian",
            familyName = "Test",
            primaryPhone = "+4915159031829",
            primaryEmail = "fabian_beckmann@outlook.de",
            iconEmoji = "🚀",
            organization = "Test GmbH",
            jobTitle = "Project Manager",
            note = "Zweiter Test-Kontakt"
        )
        val contactId2 = database.metadataContactDao().insert(contact2)

        // Telefon für Kontakt 2
        try {
            database.metadataPhoneDao().insert(
                MetadataPhoneEntity(
                    contactId = contactId2,
                    phoneNumber = "+4915159031829",
                    phoneType = PhoneType.WORK,
                    label = "Geschäftlich"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert phone for contact 2", e)
        }

        // Email für Kontakt 2
        try {
            database.metadataEmailDao().insert(
                MetadataEmailEntity(
                    contactId = contactId2,
                    emailAddress = "fabian_beckmann@outlook.de",
                    emailType = EmailType.WORK,
                    label = "Arbeit"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert email for contact 2", e)
        }

        // Adresse für Kontakt 2 (gleiche wie Kontakt 1)
        try {
            database.metadataAddressDao().insert(
                MetadataAddressEntity(
                    contactId = contactId2,
                    street = "Baumgarten 42",
                    city = "Bremerhaven",
                    postalCode = "27654",
                    country = "Deutschland",
                    addressType = AddressType.WORK,
                    label = "Büro"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert address for contact 2", e)
        }

        val contactIds = listOf(contactId1, contactId2)

        // Note: VIP-Tag linking removed due to async transaction issues
        // Tags can be added manually in the UI for testing

        return contactIds
    }

    // ========== 4. STANDORTE ==========
    private suspend fun initializeLocations(): List<Long> {
        val locations = listOf(
            MetadataLocationEntity(
                placeName = "Zuhause Bremerhaven",
                latitude = 53.5395,
                longitude = 8.5809,
                formattedAddress = "Baumgarten 42, 27654 Bremerhaven",
                street = "Baumgarten 42",
                city = "Bremerhaven",
                postalCode = "27654",
                country = "Deutschland",
                customLabel = "Privat"
            ),
            MetadataLocationEntity(
                placeName = "Test Büro Berlin",
                latitude = 52.5200,
                longitude = 13.4050,
                formattedAddress = "Alexanderplatz 1, 10178 Berlin",
                street = "Alexanderplatz 1",
                city = "Berlin",
                postalCode = "10178",
                country = "Deutschland",
                customLabel = "Hauptbüro"
            ),
            MetadataLocationEntity(
                placeName = "Test Standort München",
                latitude = 48.1351,
                longitude = 11.5820,
                formattedAddress = "Marienplatz 1, 80331 München",
                street = "Marienplatz 1",
                city = "München",
                postalCode = "80331",
                country = "Deutschland",
                customLabel = "Zweigstelle Süd"
            )
        )

        return locations.map { database.metadataLocationDao().insert(it) }
    }

    // ========== 5. TEXTBAUSTEINE ==========
    private suspend fun initializeTextTemplates(): List<Long> {
        val templates = listOf(
            TextTemplateEntity(
                title = "Meeting-Einladung",
                subject = "Meeting: {task.title}",
                content = """Hallo {contact.name},

ich möchte dich gerne zu folgendem Meeting einladen:

📅 Thema: {task.title}
🕐 Zeitpunkt: {task.dueDate}
📍 Ort: {location.name}

Bitte bestätige kurz deine Teilnahme.

Viele Grüße,
{user.name}""",
                description = "Einladung zu einem Meeting mit allen Platzhaltern"
            ),
            TextTemplateEntity(
                title = "Task-Erinnerung",
                subject = "Erinnerung: {task.title}",
                content = """Hi {contact.name},

nur eine kurze Erinnerung an folgende Aufgabe:

✅ {task.title}
📝 {task.description}
⏰ Fällig am: {task.dueDate}

Danke und viel Erfolg!""",
                description = "Erinnerung für anstehende Tasks"
            ),
            TextTemplateEntity(
                title = "Status-Update",
                subject = "Status-Update: {task.title}",
                content = """Hallo Team,

kurzes Update zur Aufgabe "{task.title}":

Status: ✅ Abgeschlossen
Kategorie: {task.category}
XP erhalten: {task.xp} 🎮

Nächste Schritte folgen in Kürze.

Grüße,
{user.name}""",
                description = "Status-Update nach Task-Fertigstellung"
            ),
            TextTemplateEntity(
                title = "Telefon-Notiz",
                subject = "Telefonat mit {contact.name}",
                content = """📞 Telefonat mit {contact.name}

Datum: {datetime.now}
Dauer: ~15 Min

Wichtige Punkte:
-
-
-

Nächste Schritte:
{task.description}

Status: {task.title}""",
                description = "Schnelle Telefon-Notiz mit Kontakt"
            ),
            TextTemplateEntity(
                title = "Projekt-Kickoff",
                subject = "Projekt Start: {task.title}",
                content = """🚀 Projekt-Kickoff

Projekt: {task.title}
Team: {contact.name}
Standort: {location.name}

Ziele:
{task.description}

Start: {task.dueDate}
Kategorie: {task.category}

Viel Erfolg! 💪""",
                description = "Template für Projekt-Starts"
            )
        )

        return templates.map { database.textTemplateDao().insertTemplate(it) }
    }

    // ========== 6. TASKS ==========
    private suspend fun initializeTasks(
        categoryId: Long,
        contactIds: List<Long>,
        locationIds: List<Long>
    ): List<Long> {
        val now = LocalDateTime.now()
        val taskIds = mutableListOf<Long>()

        // ===== COMPLETED TASKS IN THE PAST (für Statistiken) =====

        // -30 Tage: Projekt-Planung abgeschlossen
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Projekt-Roadmap erstellen",
                description = "Jahresplanung und Meilensteine definieren",
                priority = "HIGH",
                xpPercentage = 80,
                xpReward = 6000,
                categoryId = categoryId,
                dueDate = now.minusDays(30).withHour(10).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(30).withHour(15).withMinute(30),
                isRecurring = false
            )
        ))

        // -25 Tage: Design Review
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ UI/UX Design Review",
                description = "Designs mit Team besprechen und finalisieren",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 4500,
                categoryId = categoryId,
                dueDate = now.minusDays(25).withHour(14).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(25).withHour(16).withMinute(45),
                isRecurring = false
            )
        ))

        // -20 Tage: Sprint 1 abgeschlossen
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Sprint 1: Grundfunktionen",
                description = "Core-Features implementieren und testen",
                priority = "URGENT",
                xpPercentage = 100,
                xpReward = 8000,
                categoryId = categoryId,
                dueDate = now.minusDays(20).withHour(17).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(20).withHour(18).withMinute(20),
                isRecurring = false
            )
        ))

        // -18 Tage: Team Meeting
        val teamMeetingId = database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Weekly Team Standup",
                description = "Wöchentliches Sync-Meeting mit allen",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 2000,
                categoryId = categoryId,
                dueDate = now.minusDays(18).withHour(9).withMinute(30),
                isCompleted = true,
                completedAt = now.minusDays(18).withHour(10).withMinute(15),
                isRecurring = false
            )
        )
        taskIds.add(teamMeetingId)
        if (contactIds.isNotEmpty()) {
            database.taskContactLinkDao().insert(
                TaskContactLinkEntity(taskId = teamMeetingId, contactId = contactIds[0])
            )
        }

        // -15 Tage: Code Review
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Code Review: Authentication",
                description = "Login und Registrierung überprüfen",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 3500,
                categoryId = categoryId,
                dueDate = now.minusDays(15).withHour(11).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(15).withHour(13).withMinute(0),
                isRecurring = false
            )
        ))

        // -12 Tage: Testing
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Unit Tests schreiben",
                description = "Test-Coverage auf 80% erhöhen",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 3000,
                categoryId = categoryId,
                dueDate = now.minusDays(12).withHour(15).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(12).withHour(17).withMinute(30),
                isRecurring = false
            )
        ))

        // -10 Tage: Sprint 2 abgeschlossen
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Sprint 2: XP System",
                description = "Gamification-System implementieren",
                priority = "URGENT",
                xpPercentage = 100,
                xpReward = 10000,
                categoryId = categoryId,
                dueDate = now.minusDays(10).withHour(17).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(10).withHour(19).withMinute(0),
                isRecurring = false
            )
        ))

        // -8 Tage: Dokumentation
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ API-Dokumentation aktualisieren",
                description = "Swagger Docs für neue Endpoints",
                priority = "LOW",
                xpPercentage = 20,
                xpReward = 1500,
                categoryId = categoryId,
                dueDate = now.minusDays(8).withHour(13).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(8).withHour(14).withMinute(45),
                isRecurring = false
            )
        ))

        // -6 Tage: Performance Optimization
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Performance-Optimierung",
                description = "Ladezeiten um 30% reduzieren",
                priority = "HIGH",
                xpPercentage = 80,
                xpReward = 5500,
                categoryId = categoryId,
                dueDate = now.minusDays(6).withHour(16).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(6).withHour(18).withMinute(30),
                isRecurring = false
            )
        ))

        // -5 Tage: Prototyp abgeschlossen
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Beta-Version deployen",
                description = "Erste Beta für Tester bereitstellen",
                priority = "URGENT",
                xpPercentage = 80,
                xpReward = 7000,
                categoryId = categoryId,
                dueDate = now.minusDays(5).withHour(12).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(5).withHour(14).withMinute(20),
                isRecurring = false
            )
        ))

        // -3 Tage: Bug Fixing
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Kritische Bugs beheben",
                description = "15 High-Priority Bugs aus Beta-Feedback",
                priority = "URGENT",
                xpPercentage = 100,
                xpReward = 8500,
                categoryId = categoryId,
                dueDate = now.minusDays(3).withHour(10).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(3).withHour(16).withMinute(0),
                isRecurring = false
            )
        ))

        // -2 Tage: Dokumentation schreiben
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ User-Dokumentation erstellen",
                description = "Benutzerhandbuch und FAQs schreiben",
                priority = "MEDIUM",
                xpPercentage = 60,
                xpReward = 3500,
                categoryId = categoryId,
                dueDate = now.minusDays(2).withHour(11).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(2).withHour(15).withMinute(30),
                isRecurring = false
            )
        ))

        // -1 Tag: Final Testing
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Abschluss-Tests durchführen",
                description = "Alle Features nochmal komplett testen",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 4000,
                categoryId = categoryId,
                dueDate = now.minusDays(1).withHour(14).withMinute(0),
                isCompleted = true,
                completedAt = now.minusDays(1).withHour(17).withMinute(45),
                isRecurring = false
            )
        ))

        // ===== AKTUELLE TASKS (Heute und nahe Zukunft) =====

        // Heute: Dringende Tasks
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Release Notes vorbereiten",
                description = "Changelog für v1.0 erstellen",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 3000,
                categoryId = categoryId,
                dueDate = now.withHour(16).withMinute(0),
                isRecurring = false
            )
        ))

        val todayMeetingId = database.taskDao().insertTask(
            TaskEntity(
                title = "Launch-Meeting mit Fabian",
                description = "Final Go/No-Go Entscheidung treffen",
                priority = "URGENT",
                xpPercentage = 80,
                xpReward = 5000,
                categoryId = categoryId,
                dueDate = now.withHour(10).withMinute(0),
                isRecurring = false
            )
        )
        taskIds.add(todayMeetingId)
        if (contactIds.isNotEmpty() && locationIds.isNotEmpty()) {
            database.taskContactLinkDao().insert(
                TaskContactLinkEntity(taskId = todayMeetingId, contactId = contactIds[0])
            )
        }

        // ===== PARENT TASK 1: Projekt "App Entwicklung" mit Subtasks =====
        val parentTask1Id = database.taskDao().insertTask(
            TaskEntity(
                title = "📱 Projekt: QuestFlow App Entwicklung",
                description = "Haupt-Projekt für die Entwicklung der QuestFlow-App mit mehreren Subtasks",
                priority = "URGENT",
                xpPercentage = 100, // Episch
                xpReward = 10000,
                categoryId = categoryId,
                dueDate = now.plusDays(30),
                isRecurring = false,
                autoCompleteParent = false // Parent muss manuell abgeschlossen werden
            )
        )
        taskIds.add(parentTask1Id)

        // Subtask 1.1: Täglich recurring, AFTER_COMPLETION
        val subtask11Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Daily Code Review",
                description = "Code-Qualität täglich überprüfen",
                priority = "HIGH",
                xpPercentage = 40,
                xpReward = 2000,
                categoryId = categoryId,
                dueDate = now.withHour(16).withMinute(0),
                isRecurring = true,
                recurringType = "DAILY",
                recurringInterval = 1 * 24 * 60,
                specificTime = "16:00",
                triggerMode = "AFTER_COMPLETION",
                parentTaskId = parentTask1Id,
                autoCompleteParent = false
            )
        )
        taskIds.add(subtask11Id)

        // Subtask 1.2: Wöchentlich recurring, FIXED_INTERVAL
        val subtask12Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Wöchentliches Sprint-Planning",
                description = "Sprint planen mit dem Team (Mo, Mi, Fr)",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 3500,
                categoryId = categoryId,
                dueDate = now.withHour(9).withMinute(30),
                isRecurring = true,
                recurringType = "WEEKLY",
                recurringInterval = 7 * 24 * 60,
                recurringDays = "MONDAY,WEDNESDAY,FRIDAY",
                specificTime = "09:30",
                triggerMode = "FIXED_INTERVAL",
                parentTaskId = parentTask1Id,
                autoCompleteParent = false
            )
        )
        taskIds.add(subtask12Id)

        // Kontakt zu Subtask 1.2 hinzufügen
        if (contactIds.isNotEmpty()) {
            database.taskContactLinkDao().insert(
                TaskContactLinkEntity(
                    taskId = subtask12Id,
                    contactId = contactIds[0]
                )
            )
        }

        // Subtask 1.3: Einmalig, bereits abgeschlossen
        val subtask13Id = database.taskDao().insertTask(
            TaskEntity(
                title = "✅ Prototyp fertigstellen",
                description = "Ersten funktionierenden Prototyp entwickeln",
                priority = "HIGH",
                xpPercentage = 80,
                xpReward = 5000,
                categoryId = categoryId,
                dueDate = now.minusDays(5),
                isRecurring = false,
                isCompleted = true,
                completedAt = now.minusDays(5).plusHours(3),
                parentTaskId = parentTask1Id,
                autoCompleteParent = false
            )
        )
        taskIds.add(subtask13Id)

        // ===== PARENT TASK 2: "Marketing Kampagne" mit Auto-Complete =====
        val parentTask2Id = database.taskDao().insertTask(
            TaskEntity(
                title = "📣 Marketing: Launch Kampagne",
                description = "Marketing-Kampagne für App-Launch vorbereiten und durchführen",
                priority = "HIGH",
                xpPercentage = 80,
                xpReward = 8000,
                categoryId = categoryId,
                dueDate = now.plusDays(14),
                isRecurring = false,
                autoCompleteParent = true // Wird automatisch abgeschlossen wenn alle Subtasks fertig
            )
        )
        taskIds.add(parentTask2Id)

        // Subtask 2.1: Monatlich recurring, AFTER_EXPIRY
        val subtask21Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Monatlicher Newsletter",
                description = "Newsletter an alle Subscriber senden",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 2500,
                categoryId = categoryId,
                dueDate = now.withDayOfMonth(1).withHour(10).withMinute(0),
                isRecurring = true,
                recurringType = "MONTHLY",
                recurringInterval = 30 * 24 * 60,
                specificTime = "10:00",
                triggerMode = "AFTER_EXPIRY",
                parentTaskId = parentTask2Id,
                autoCompleteParent = true
            )
        )
        taskIds.add(subtask21Id)

        // Subtask 2.2: Custom recurring (alle 2 Stunden), AFTER_COMPLETION
        val subtask22Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Social Media Check",
                description = "Social Media Kanäle überprüfen und beantworten (alle 2h)",
                priority = "LOW",
                xpPercentage = 20,
                xpReward = 1000,
                categoryId = categoryId,
                dueDate = now.plusHours(2),
                isRecurring = true,
                recurringType = "CUSTOM",
                recurringInterval = 120, // 2 Stunden in Minuten
                triggerMode = "AFTER_COMPLETION",
                parentTaskId = parentTask2Id,
                autoCompleteParent = true
            )
        )
        taskIds.add(subtask22Id)

        // ===== STANDALONE TASKS (ohne Parent) mit verschiedenen Konfigurationen =====

        // Task mit Kontakt + Location
        val task1Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Meeting mit Fabian in Bremerhaven",
                description = "Projekt-Kickoff und Anforderungsanalyse besprechen",
                priority = "URGENT",
                xpPercentage = 60,
                xpReward = 4000,
                categoryId = categoryId,
                dueDate = now.plusDays(2).withHour(14).withMinute(0),
                isRecurring = false
            )
        )
        taskIds.add(task1Id)

        // Kontakt und Location hinzufügen
        if (contactIds.isNotEmpty()) {
            database.taskContactLinkDao().insert(
                TaskContactLinkEntity(
                    taskId = task1Id,
                    contactId = contactIds[0]
                )
            )
        }

        // Weekly recurring, FIXED_INTERVAL
        val task2Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Wöchentliches Backup (Di, Do)",
                description = "Datenbank-Backup durchführen",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 2000,
                categoryId = categoryId,
                dueDate = now.withHour(22).withMinute(0),
                isRecurring = true,
                recurringType = "WEEKLY",
                recurringInterval = 7 * 24 * 60,
                recurringDays = "TUESDAY,THURSDAY",
                specificTime = "22:00",
                triggerMode = "FIXED_INTERVAL"
            )
        )
        taskIds.add(task2Id)

        // Daily recurring, AFTER_COMPLETION
        val task3Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Tägliche Lerneinheit",
                description = "30 Minuten etwas Neues lernen",
                priority = "LOW",
                xpPercentage = 20,
                xpReward = 1500,
                categoryId = categoryId,
                dueDate = now.plusDays(1).withHour(18).withMinute(0),
                isRecurring = true,
                recurringType = "DAILY",
                recurringInterval = 1 * 24 * 60,
                specificTime = "18:00",
                triggerMode = "AFTER_COMPLETION"
            )
        )
        taskIds.add(task3Id)

        // Custom recurring (alle 3 Tage), AFTER_EXPIRY
        val task4Id = database.taskDao().insertTask(
            TaskEntity(
                title = "Wohnung aufräumen",
                description = "Grundlegendes Aufräumen und Putzen",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 2500,
                categoryId = categoryId,
                dueDate = now.plusDays(3),
                isRecurring = true,
                recurringType = "CUSTOM",
                recurringInterval = 3 * 24 * 60, // 3 Tage
                triggerMode = "AFTER_EXPIRY"
            )
        )
        taskIds.add(task4Id)

        // ===== ZUKÜNFTIGE TASKS (+1 bis +30 Tage) =====

        // +1 Tag: App Store Submission vorbereiten
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "App Store Screenshots erstellen",
                description = "Marketing-Material für Store-Listings",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 3500,
                categoryId = categoryId,
                dueDate = now.plusDays(1).withHour(11).withMinute(0),
                isRecurring = false
            )
        ))

        // +3 Tage: Marketing Vorbereitung
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Social Media Kampagne planen",
                description = "Content-Plan für Launch-Woche",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 2500,
                categoryId = categoryId,
                dueDate = now.plusDays(3).withHour(10).withMinute(0),
                isRecurring = false
            )
        ))

        // +5 Tage: Monitoring Setup
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Monitoring und Analytics einrichten",
                description = "Firebase Analytics und Crashlytics konfigurieren",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 4000,
                categoryId = categoryId,
                dueDate = now.plusDays(5).withHour(14).withMinute(0),
                isRecurring = false
            )
        ))

        // +7 Tage: Pressemitteilung
        val pressMeetingId = database.taskDao().insertTask(
            TaskEntity(
                title = "Presse-Meeting in Berlin",
                description = "Product Demo für Tech-Journalisten",
                priority = "URGENT",
                xpPercentage = 80,
                xpReward = 6000,
                categoryId = categoryId,
                dueDate = now.plusDays(7).withHour(15).withMinute(0),
                isRecurring = false
            )
        )
        taskIds.add(pressMeetingId)
        if (contactIds.size >= 2 && locationIds.size >= 2) {
            database.taskContactLinkDao().insert(
                TaskContactLinkEntity(taskId = pressMeetingId, contactId = contactIds[1])
            )
        }

        // +10 Tage: Community Management
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Discord Community aufbauen",
                description = "Community-Server einrichten und moderieren",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 2000,
                categoryId = categoryId,
                dueDate = now.plusDays(10).withHour(16).withMinute(0),
                isRecurring = false
            )
        ))

        // +12 Tage: User Feedback auswerten
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "User Feedback analysieren",
                description = "Erste User-Reviews auswerten und priorisieren",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 3500,
                categoryId = categoryId,
                dueDate = now.plusDays(12).withHour(13).withMinute(0),
                isRecurring = false
            )
        ))

        // +15 Tage: Feature-Planung
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Roadmap für v1.1 erstellen",
                description = "Nächste Features basierend auf Feedback planen",
                priority = "MEDIUM",
                xpPercentage = 60,
                xpReward = 4000,
                categoryId = categoryId,
                dueDate = now.plusDays(15).withHour(10).withMinute(0),
                isRecurring = false
            )
        ))

        // +17 Tage: Security Audit
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Security-Audit durchführen",
                description = "Externe Security-Prüfung organisieren",
                priority = "HIGH",
                xpPercentage = 80,
                xpReward = 5500,
                categoryId = categoryId,
                dueDate = now.plusDays(17).withHour(9).withMinute(0),
                isRecurring = false
            )
        ))

        // +20 Tage: Internationalisierung
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "App für weitere Sprachen vorbereiten",
                description = "i18n-System erweitern (EN, FR, ES)",
                priority = "MEDIUM",
                xpPercentage = 60,
                xpReward = 4500,
                categoryId = categoryId,
                dueDate = now.plusDays(20).withHour(11).withMinute(0),
                isRecurring = false
            )
        ))

        // +22 Tage: A/B Testing
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "A/B Tests für Onboarding",
                description = "Verschiedene Onboarding-Flows testen",
                priority = "LOW",
                xpPercentage = 40,
                xpReward = 2500,
                categoryId = categoryId,
                dueDate = now.plusDays(22).withHour(15).withMinute(0),
                isRecurring = false
            )
        ))

        // +25 Tage: Partner-Meeting
        val partnerMeetingId = database.taskDao().insertTask(
            TaskEntity(
                title = "Partnership-Gespräch in München",
                description = "Kooperationsmöglichkeiten mit Tech-Partnern",
                priority = "HIGH",
                xpPercentage = 80,
                xpReward = 5000,
                categoryId = categoryId,
                dueDate = now.plusDays(25).withHour(14).withMinute(30),
                isRecurring = false
            )
        )
        taskIds.add(partnerMeetingId)
        if (contactIds.size >= 2 && locationIds.size >= 3) {
            database.taskContactLinkDao().insert(
                TaskContactLinkEntity(taskId = partnerMeetingId, contactId = contactIds[1])
            )
        }

        // +27 Tage: Performance Monitoring
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Performance-Metriken reviewen",
                description = "30-Tage Performance-Report erstellen",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 3000,
                categoryId = categoryId,
                dueDate = now.plusDays(27).withHour(10).withMinute(0),
                isRecurring = false
            )
        ))

        // +30 Tage: Sprint Planning für v1.1
        taskIds.add(database.taskDao().insertTask(
            TaskEntity(
                title = "Sprint Planning für v1.1",
                description = "Team-Meeting für nächste Entwicklungsphase",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 4000,
                categoryId = categoryId,
                dueDate = now.plusDays(30).withHour(9).withMinute(0),
                isRecurring = false
            )
        ))

        return taskIds
    }

    // ========== 6b. CALENDAR LINKS ==========
    private suspend fun initializeCalendarLinks(
        taskIds: List<Long>,
        categoryId: Long
    ): List<Long> {
        val linkIds = mutableListOf<Long>()
        val now = LocalDateTime.now()

        taskIds.forEach { taskId ->
            val task = database.taskDao().getTaskById(taskId) ?: return@forEach

            // Nur Tasks mit dueDate bekommen Calendar Links
            task.dueDate?.let { dueDate ->
                // Status basierend auf Datum und Completion
                val status = when {
                    task.isCompleted -> "CLAIMED"
                    dueDate < now -> "EXPIRED"
                    else -> "PENDING"
                }

                val link = CalendarEventLinkEntity(
                    calendarEventId = 0, // Kein echter Kalender-Event, nur XP-Tracking
                    title = task.title,
                    startsAt = dueDate,
                    endsAt = dueDate.plusHours(1),
                    xp = task.xpReward,
                    xpPercentage = task.xpPercentage ?: 60,
                    categoryId = task.categoryId,
                    taskId = task.id,
                    status = status,
                    rewarded = task.isCompleted
                )

                val linkId = database.calendarEventLinkDao().insert(link)
                linkIds.add(linkId)
            }
        }

        return linkIds
    }

    // ========== 6c. XP-TRANSAKTIONEN FÜR ABGESCHLOSSENE TASKS ==========
    private suspend fun initializeXpTransactionsForCompletedTasks(taskIds: List<Long>) {
        taskIds.forEach { taskId ->
            val task = database.taskDao().getTaskById(taskId) ?: return@forEach

            // Nur für abgeschlossene Tasks XP-Transaktion erstellen
            if (task.isCompleted) {
                val transaction = XpTransactionEntity(
                    amount = task.xpReward,
                    source = XpSource.TASK,
                    referenceId = task.id,
                    timestamp = task.completedAt ?: LocalDateTime.now()
                )
                database.xpTransactionDao().insert(transaction)
            }
        }
    }

    // ========== 7. SKILLS ==========
    private suspend fun initializeSkills(categoryId: Long): List<String> {
        val skills = listOf(
            // Globale Skills
            SkillNodeEntity(
                id = "global_xp_boost",
                title = "XP Verstärker",
                description = "Erhöhe alle XP-Gewinne um 10% pro Punkt",
                effectType = SkillEffectType.XP_MULTIPLIER,
                baseValue = 1.0f,
                scalingPerPoint = 0.1f,
                maxInvestment = 10,
                iconName = "trending_up",
                positionX = 0f,
                positionY = 0f,
                categoryId = null, // Global
                colorHex = "#4CAF50"
            ),
            SkillNodeEntity(
                id = "task_master",
                title = "Task-Meister",
                description = "Zusätzliche 15% XP für abgeschlossene Tasks",
                effectType = SkillEffectType.TASK_XP_BONUS,
                baseValue = 1.0f,
                scalingPerPoint = 0.15f,
                maxInvestment = 5,
                iconName = "check_circle",
                positionX = 100f,
                positionY = 0f,
                categoryId = null,
                colorHex = "#2196F3"
            ),
            SkillNodeEntity(
                id = "streak_guardian",
                title = "Streak-Beschützer",
                description = "Schütze deine Streak vor Verlust (einmalig aktiv)",
                effectType = SkillEffectType.STREAK_PROTECTION,
                baseValue = 0f,
                scalingPerPoint = 1f,
                maxInvestment = 1,
                iconName = "shield",
                positionX = -100f,
                positionY = 100f,
                categoryId = null,
                colorHex = "#FF9800"
            ),
            // Kategorie-spezifischer Skill
            SkillNodeEntity(
                id = "debug_specialist",
                title = "Debug-Spezialist",
                description = "Extra XP für Debug-Kategorie Tasks",
                effectType = SkillEffectType.CATEGORY_XP_BOOST,
                baseValue = 1.0f,
                scalingPerPoint = 0.2f,
                maxInvestment = 5,
                iconName = "bug_report",
                positionX = 0f,
                positionY = 100f,
                categoryId = categoryId,
                colorHex = "#9C27B0"
            )
        )

        skills.forEach { database.skillDao().insertNode(it) }

        // Ein paar Skills bereits freigeschaltet
        database.skillDao().insertOrUpdateUnlock(
            SkillUnlockEntity(
                nodeId = "global_xp_boost",
                investedPoints = 3,
                unlockedAt = LocalDateTime.now()
            )
        )

        return skills.map { it.id }
    }

    // ========== 8. USER STATS ==========
    private suspend fun initializeUserStats() {
        val stats = UserStatsEntity(
            id = 0,
            xp = 15000,
            level = 5,
            points = 12 // Skillpunkte verfügbar
        )
        database.userStatsDao().upsert(stats)

        // Ein paar XP-Transaktionen für Historie
        val transactions = listOf(
            XpTransactionEntity(
                amount = 5000,
                source = XpSource.TASK,
                referenceId = null,
                timestamp = LocalDateTime.now().minusDays(2)
            ),
            XpTransactionEntity(
                amount = 3500,
                source = XpSource.CALENDAR,
                referenceId = null,
                timestamp = LocalDateTime.now().minusDays(1)
            ),
            XpTransactionEntity(
                amount = 2000,
                source = XpSource.SYSTEM,
                referenceId = null,
                timestamp = LocalDateTime.now()
            )
        )
        transactions.forEach { database.xpTransactionDao().insert(it) }
    }

    // ========== 9. MEDIA LIBRARY ==========
    private suspend fun initializeMediaLibrary(): List<String> {
        // Placeholder-Einträge für Bilder/GIFs
        // In einer echten App würden hier Dateien ins interne Speicher kopiert
        val mediaItems = listOf(
            MediaLibraryEntity(
                id = UUID.randomUUID().toString(),
                fileName = "debug_image_1.png",
                filePath = "debug/debug_image_1.png",
                mediaType = MediaType.IMAGE,
                displayName = "Debug Test-Bild 1",
                description = "Platzhalter-Bild für Tests",
                tags = "debug,test,placeholder",
                mimeType = "image/png"
            ),
            MediaLibraryEntity(
                id = UUID.randomUUID().toString(),
                fileName = "debug_image_2.png",
                filePath = "debug/debug_image_2.png",
                mediaType = MediaType.IMAGE,
                displayName = "Debug Test-Bild 2",
                description = "Zweites Platzhalter-Bild",
                tags = "debug,test,placeholder",
                mimeType = "image/png"
            ),
            MediaLibraryEntity(
                id = UUID.randomUUID().toString(),
                fileName = "debug_animation.gif",
                filePath = "debug/debug_animation.gif",
                mediaType = MediaType.GIF,
                displayName = "Debug Test-GIF",
                description = "Animiertes GIF für Tests",
                tags = "debug,test,animation,gif",
                mimeType = "image/gif"
            )
        )

        mediaItems.forEach { database.mediaLibraryDao().insertMedia(it) }
        return mediaItems.map { it.id }
    }

    // ========== 10. STATISTIKEN ==========
    private suspend fun initializeStatistics() {
        // Statistik-Konfiguration erstellen (Single-Row Config)
        val statsConfig = StatisticsConfigEntity(
            id = 1,
            visibleCharts = "xp_trend,task_completion,category_distribution",
            defaultTimeRange = "WEEK",
            chartOrder = "xp_trend,task_completion,category_distribution",
            aggregationLevel = "DAILY"
        )
        database.statisticsDao().saveConfig(statsConfig)
    }
}
