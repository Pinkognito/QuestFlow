package com.example.questflow.data.debug

import android.content.Context
import android.util.Log
import com.example.questflow.data.database.QuestFlowDatabase
import com.example.questflow.data.database.TaskEntity
import com.example.questflow.data.database.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
        val categories = database.categoryDao().getAllCategoriesOnce()
        val hasDebugCategory = categories.any { it.name == DEBUG_CATEGORY_NAME }
        !hasDebugCategory
    }

    /**
     * Initialisiert ALLE Debug-Testdaten
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting debug data initialization...")

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
        val contacts = listOf(
            MetadataContactEntity(
                displayName = "Fabian Test 1",
                givenName = "Fabian",
                familyName = "Test",
                primaryPhone = "+4915159031829",
                primaryEmail = "fabian1@test.com",
                iconEmoji = "👨‍💻",
                organization = "Test Corp",
                jobTitle = "Developer",
                note = "Test-Kontakt 1 für Debug-Zwecke"
            ),
            MetadataContactEntity(
                displayName = "Fabian Test 2",
                givenName = "Fabian",
                familyName = "Test2",
                primaryPhone = "+4915159031830",
                primaryEmail = "fabian2@test.com",
                iconEmoji = "🚀",
                organization = "Test GmbH",
                jobTitle = "Project Manager",
                note = "Test-Kontakt 2 für Debug-Zwecke"
            )
        )

        val contactIds = contacts.map { contact ->
            database.metadataContactDao().insert(contact)
        }

        // VIP-Tag für fabian1 hinzufügen (nach dem Einfügen aller Kontakte)
        if (contactIds.isNotEmpty()) {
            tagIds["VIP"]?.let { vipTagId ->
                try {
                    database.contactTagDao().insert(
                        ContactTagEntity(
                            contactId = contactIds[0], // fabian1
                            tagId = vipTagId
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not add VIP tag to contact: ${e.message}")
                }
            }
        }

        return contactIds
    }

    // ========== 4. STANDORTE ==========
    private suspend fun initializeLocations(): List<Long> {
        val locations = listOf(
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

        val tasks = listOf(
            // Task 1: Täglich, 14:00 Uhr, nach Fertigstellung
            TaskEntity(
                title = "Tägliche Standup-Notizen",
                description = "Notizen für das tägliche Standup-Meeting vorbereiten",
                priority = "HIGH",
                xpPercentage = 40, // Einfach
                xpReward = 2000,
                categoryId = categoryId,
                dueDate = now.withHour(14).withMinute(0),
                isRecurring = true,
                recurringType = "DAILY",
                recurringInterval = 1 * 24 * 60, // 1 Tag in Minuten
                specificTime = "14:00",
                triggerMode = "AFTER_COMPLETION"
            ),
            // Task 2: Wöchentlich (Mo, Mi, Fr), 09:00 Uhr, fest
            TaskEntity(
                title = "Wöchentliches Team-Meeting",
                description = "Team-Meeting mit allen Entwicklern",
                priority = "MEDIUM",
                xpPercentage = 60, // Mittel
                xpReward = 3500,
                categoryId = categoryId,
                dueDate = now.withHour(9).withMinute(0),
                isRecurring = true,
                recurringType = "WEEKLY",
                recurringInterval = 7 * 24 * 60, // 1 Woche in Minuten
                recurringDays = "MONDAY,WEDNESDAY,FRIDAY",
                specificTime = "09:00",
                triggerMode = "FIXED_INTERVAL"
            ),
            // Task 3: Monatlich (15. des Monats), 12:00 Uhr, nach Ablauf
            TaskEntity(
                title = "Monats-Report erstellen",
                description = "Monatlicher Fortschrittsbericht für das Management",
                priority = "HIGH",
                xpPercentage = 80, // Schwer
                xpReward = 5000,
                categoryId = categoryId,
                dueDate = now.withDayOfMonth(15).withHour(12).withMinute(0),
                isRecurring = true,
                recurringType = "MONTHLY",
                recurringInterval = 15 * 24 * 60, // 15. Tag in Minuten
                specificTime = "12:00",
                triggerMode = "AFTER_EXPIRY"
            ),
            // Task 4: Benutzerdefiniert (alle 90 Minuten), nach Fertigstellung
            TaskEntity(
                title = "Kurze Lerneinheit",
                description = "15 Minuten neue Technologie lernen",
                priority = "LOW",
                xpPercentage = 20, // Trivial
                xpReward = 1000,
                categoryId = categoryId,
                dueDate = now.plusMinutes(90),
                isRecurring = true,
                recurringType = "CUSTOM",
                recurringInterval = 90, // 90 Minuten
                triggerMode = "AFTER_COMPLETION"
            ),
            // Task 5: Einmalige Task mit Kontakt und Standort
            TaskEntity(
                title = "Meeting mit Fabian Test 1",
                description = "Projekt-Kickoff besprechen",
                priority = "HIGH",
                xpPercentage = 60,
                xpReward = 3500,
                categoryId = categoryId,
                dueDate = now.plusDays(1).withHour(10).withMinute(0),
                isRecurring = false
            ),
            // Task 6: Erledigte Task (für Statistiken)
            TaskEntity(
                title = "Code-Review durchführen",
                description = "Pull Request #123 reviewen",
                priority = "MEDIUM",
                xpPercentage = 40,
                xpReward = 2000,
                categoryId = categoryId,
                dueDate = now.minusDays(1),
                isCompleted = true,
                completedAt = now.minusDays(1).plusHours(2)
            )
        )

        val taskIds = mutableListOf<Long>()
        tasks.forEachIndexed { index, task ->
            val taskId = database.taskDao().insertTask(task)
            taskIds.add(taskId)

            // Kontakt zu Task 5 hinzufügen (Meeting mit Fabian Test 1)
            if (index == 4 && contactIds.isNotEmpty()) {
                database.taskContactLinkDao().insert(
                    TaskContactLinkEntity(
                        taskId = taskId,
                        contactId = contactIds[0]
                    )
                )
            }

            // Note: Task-Metadaten werden bei Bedarf separat verknüpft
            // Hier erstellen wir keine leeren TaskMetadata-Einträge, da sie einen MetadataType brauchen
        }

        return taskIds
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
