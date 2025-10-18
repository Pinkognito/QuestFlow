package com.example.questflow.data.debug

import android.content.Context
import android.util.Log
import com.example.questflow.data.database.QuestFlowDatabase
import com.example.questflow.data.database.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initialisiert minimale Startdaten für die App
 * Wird beim ersten Start ausgeführt wenn die Datenbank leer ist
 */
@Singleton
class DebugDataInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: QuestFlowDatabase
) {
    companion object {
        private const val TAG = "DebugDataInitializer"
        private const val INIT_MARKER_CATEGORY = "📱 QuestFlow"
    }

    /**
     * Prüft ob Initialisierung bereits erfolgt ist
     */
    suspend fun shouldInitialize(): Boolean = withContext(Dispatchers.IO) {
        val categories = database.categoryDao().getAllCategoriesOnce()
        !categories.any { it.name == INIT_MARKER_CATEGORY }
    }

    /**
     * Initialisiert minimale Startdaten
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting minimal data initialization...")

            database.runInTransaction {
                runBlocking {
                    // 1. Standard-Kategorie erstellen (als Marker dass Init erfolgt ist)
                    initializeDefaultCategory()
                    Log.d(TAG, "✅ Default category created")

                    // 2. Standard-Textbaustein für Google Calendar
                    initializeDefaultTemplate()
                    Log.d(TAG, "✅ Default template created")

                    // 3. User Stats initialisieren
                    initializeUserStats()
                    Log.d(TAG, "✅ User stats initialized")

                    Log.d(TAG, "🎉 Minimal data initialization completed!")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize data", e)
            throw e
        }
    }

    /**
     * Standard-Kategorie erstellen
     */
    private suspend fun initializeDefaultCategory(): Long {
        val category = CategoryEntity(
            name = INIT_MARKER_CATEGORY,
            description = "Standard-Kategorie für deine Aufgaben",
            color = "#2196F3", // Blue
            emoji = "📱",
            currentXp = 0,
            currentLevel = 1,
            totalXp = 0,
            skillPoints = 0,
            isActive = true
        )
        return database.categoryDao().insertCategory(category)
    }

    /**
     * Standard-Textbaustein für Google Calendar Events
     */
    private suspend fun initializeDefaultTemplate() {
        val template = TextTemplateEntity(
            title = "Google Calendar Termin",
            subject = "Termin: {task.title}",
            content = """📅 Google Calendar Eintrag

Titel: {task.title}
Zeitpunkt: {task.dueDate}
${"\n"}Beschreibung:
{task.description}
${"\n"}📍 Ort: {location.name}
👤 Teilnehmer: {contact.name}
${"\n"}Kategorie: {task.category}
${"\n"}---
Erstellt mit QuestFlow""",
            description = "Standard-Textbaustein für Google Calendar Termine mit allen wichtigen Platzhaltern"
        )
        database.textTemplateDao().insertTemplate(template)
    }

    /**
     * User Stats initialisieren (Level 1, 0 XP)
     */
    private suspend fun initializeUserStats() {
        val stats = UserStatsEntity(
            id = 0,
            xp = 0,
            level = 1,
            points = 0
        )
        database.userStatsDao().upsert(stats)
    }
}
