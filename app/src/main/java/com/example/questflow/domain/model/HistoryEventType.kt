package com.example.questflow.domain.model

/**
 * Alle verfügbaren History Event-Typen mit Metadaten
 */
enum class HistoryEventType(
    val displayName: String,
    val icon: String,
    val category: HistoryCategory,
    val priority: HistoryPriority,
    val estimatedBytesPerEvent: Int,
    val dependencies: List<HistoryEventType> = emptyList(),
    val description: String
) {
    // === Status-Änderungen ===
    EXPIRED(
        displayName = "Abgelaufen",
        icon = "⏰",
        category = HistoryCategory.STATUS,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 32,
        description = "Task-Frist ist verstrichen"
    ),
    COMPLETED(
        displayName = "Abgeschlossen",
        icon = "✅",
        category = HistoryCategory.STATUS,
        priority = HistoryPriority.CRITICAL,
        estimatedBytesPerEvent = 32,
        description = "Task wurde manuell erledigt - KRITISCH für XP-System"
    ),
    UNCOMPLETED(
        displayName = "Erledigung rückgängig",
        icon = "↩️",
        category = HistoryCategory.STATUS,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 32,
        description = "Task wurde als unerledigt markiert"
    ),
    DELETED(
        displayName = "Gelöscht",
        icon = "🗑️",
        category = HistoryCategory.STATUS,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 32,
        description = "Task wurde gelöscht"
    ),
    RESTORED(
        displayName = "Wiederhergestellt",
        icon = "📥",
        category = HistoryCategory.STATUS,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 32,
        description = "Task aus Papierkorb wiederhergestellt"
    ),

    // === XP & Rewards ===
    CLAIMED(
        displayName = "XP beansprucht",
        icon = "💎",
        category = HistoryCategory.XP_REWARDS,
        priority = HistoryPriority.CRITICAL,
        estimatedBytesPerEvent = 40,
        description = "XP wurde geclaimed - KRITISCH für XP-Tracking"
    ),
    RECLAIMED(
        displayName = "XP erneut beansprucht",
        icon = "🔄",
        category = HistoryCategory.XP_REWARDS,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 40,
        dependencies = listOf(CLAIMED),
        description = "Nach Reactivate wieder geclaimed"
    ),
    XP_RECLAIMABLE(
        displayName = "XP wieder verfügbar",
        icon = "💚",
        category = HistoryCategory.XP_REWARDS,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 32,
        dependencies = listOf(CLAIMED),
        description = "XP-Beanspruchung wurde rückgängig gemacht"
    ),

    // === Eigenschaften ===
    PRIORITY_CHANGED(
        displayName = "Priorität geändert",
        icon = "⚡",
        category = HistoryCategory.PROPERTIES,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 64,
        description = "Task-Priorität wurde angepasst (z.B. LOW→HIGH)"
    ),
    DIFFICULTY_CHANGED(
        displayName = "Schwierigkeit geändert",
        icon = "🎯",
        category = HistoryCategory.PROPERTIES,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 48,
        description = "XP-Prozentsatz wurde geändert (z.B. 20%→100%)"
    ),
    CATEGORY_CHANGED(
        displayName = "Kategorie geändert",
        icon = "📂",
        category = HistoryCategory.PROPERTIES,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 64,
        description = "Task wurde in andere Kategorie verschoben"
    ),
    DUE_DATE_CHANGED(
        displayName = "Fälligkeit geändert",
        icon = "📆",
        category = HistoryCategory.PROPERTIES,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 48,
        description = "Start- oder Enddatum wurde verschoben"
    ),
    TITLE_CHANGED(
        displayName = "Titel geändert",
        icon = "✏️",
        category = HistoryCategory.PROPERTIES,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 128,
        description = "Task-Titel wurde umbenannt"
    ),
    DESCRIPTION_CHANGED(
        displayName = "Beschreibung geändert",
        icon = "📝",
        category = HistoryCategory.PROPERTIES,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 256,
        description = "Task-Beschreibung wurde bearbeitet - SPEICHER-INTENSIV"
    ),

    // === Beziehungen ===
    PARENT_ASSIGNED(
        displayName = "Überordnung zugewiesen",
        icon = "📎",
        category = HistoryCategory.RELATIONSHIPS,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 48,
        description = "Task wurde einem Parent-Task zugeordnet"
    ),
    PARENT_REMOVED(
        displayName = "Überordnung entfernt",
        icon = "🔓",
        category = HistoryCategory.RELATIONSHIPS,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 48,
        description = "Parent-Task-Zuordnung wurde aufgehoben"
    ),
    SUBTASK_ADDED(
        displayName = "Untertask hinzugefügt",
        icon = "➕",
        category = HistoryCategory.RELATIONSHIPS,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 48,
        description = "Neuer Subtask wurde erstellt"
    ),
    SUBTASK_REMOVED(
        displayName = "Untertask entfernt",
        icon = "➖",
        category = HistoryCategory.RELATIONSHIPS,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 48,
        description = "Subtask wurde gelöscht oder ausgegliedert"
    ),

    // === Kalender & Wiederholung ===
    CALENDAR_ADDED(
        displayName = "Zu Kalender hinzugefügt",
        icon = "📅➕",
        category = HistoryCategory.CALENDAR_RECURRING,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 32,
        description = "Google Calendar Event wurde erstellt"
    ),
    CALENDAR_REMOVED(
        displayName = "Aus Kalender entfernt",
        icon = "📅❌",
        category = HistoryCategory.CALENDAR_RECURRING,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 32,
        description = "Google Calendar Event wurde gelöscht"
    ),
    RECURRING_ENABLED(
        displayName = "Wiederholung aktiviert",
        icon = "🔁➕",
        category = HistoryCategory.CALENDAR_RECURRING,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 96,
        description = "Task wurde als wiederkehrend konfiguriert"
    ),
    RECURRING_DISABLED(
        displayName = "Wiederholung deaktiviert",
        icon = "🔁❌",
        category = HistoryCategory.CALENDAR_RECURRING,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 32,
        description = "Wiederholungs-Modus wurde ausgeschaltet"
    ),
    RECURRING_CREATED(
        displayName = "Wiederkehrend erstellt",
        icon = "🔁",
        category = HistoryCategory.CALENDAR_RECURRING,
        priority = HistoryPriority.HIGH,
        estimatedBytesPerEvent = 48,
        description = "Neue Instanz durch Wiederholung erzeugt"
    ),
    RECURRING_CONFIG_CHANGED(
        displayName = "Wiederholungs-Config geändert",
        icon = "⚙️",
        category = HistoryCategory.CALENDAR_RECURRING,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 128,
        description = "Intervall, Tage oder Zeitpunkt angepasst"
    ),
    RESCHEDULED(
        displayName = "Neu geplant",
        icon = "📅",
        category = HistoryCategory.CALENDAR_RECURRING,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 48,
        description = "Fälligkeit bei Recurring Task verschoben"
    ),

    // === Kontakte & Aktionen ===
    CONTACT_ADDED(
        displayName = "Kontakt hinzugefügt",
        icon = "👤➕",
        category = HistoryCategory.CONTACTS_ACTIONS,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 48,
        description = "Kontakt mit Task verknüpft"
    ),
    CONTACT_REMOVED(
        displayName = "Kontakt entfernt",
        icon = "👤❌",
        category = HistoryCategory.CONTACTS_ACTIONS,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 48,
        description = "Kontakt-Verknüpfung aufgehoben"
    ),
    ACTION_EXECUTED(
        displayName = "Aktion ausgeführt",
        icon = "🚀",
        category = HistoryCategory.CONTACTS_ACTIONS,
        priority = HistoryPriority.MEDIUM,
        estimatedBytesPerEvent = 96,
        description = "Email, SMS, Anruf etc. wurde getriggert"
    ),

    // === Tags & Notizen ===
    TAG_ADDED(
        displayName = "Tag hinzugefügt",
        icon = "🏷️➕",
        category = HistoryCategory.TAGS_NOTES,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 64,
        description = "Tag wurde zugeordnet"
    ),
    TAG_REMOVED(
        displayName = "Tag entfernt",
        icon = "🏷️❌",
        category = HistoryCategory.TAGS_NOTES,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 64,
        description = "Tag-Zuordnung aufgehoben"
    ),
    NOTE_ADDED(
        displayName = "Notiz hinzugefügt",
        icon = "💬",
        category = HistoryCategory.TAGS_NOTES,
        priority = HistoryPriority.LOW,
        estimatedBytesPerEvent = 256,
        description = "Kommentar/Notiz wurde erfasst - SPEICHER-INTENSIV"
    );

    companion object {
        fun fromString(value: String): HistoryEventType? {
            return values().find { it.name == value }
        }

        fun getByCategory(category: HistoryCategory): List<HistoryEventType> {
            return values().filter { it.category == category }
        }

        fun getCritical(): List<HistoryEventType> {
            return values().filter { it.priority == HistoryPriority.CRITICAL }
        }
    }
}

/**
 * Kategorien für Gruppierung in UI
 */
enum class HistoryCategory(val displayName: String, val icon: String) {
    STATUS("Status", "📊"),
    XP_REWARDS("XP & Rewards", "💎"),
    PROPERTIES("Eigenschaften", "⚙️"),
    RELATIONSHIPS("Beziehungen", "🔗"),
    CALENDAR_RECURRING("Kalender & Wiederholung", "📅"),
    CONTACTS_ACTIONS("Kontakte & Aktionen", "👥"),
    TAGS_NOTES("Tags & Notizen", "🏷️")
}

/**
 * Prioritätsstufen für System-Relevanz
 */
enum class HistoryPriority(
    val displayName: String,
    val color: String,
    val description: String
) {
    CRITICAL(
        displayName = "Kritisch",
        color = "#FF0000",
        description = "System-notwendig - Deaktivierung kann Funktionen beeinträchtigen"
    ),
    HIGH(
        displayName = "Hoch",
        color = "#FF8800",
        description = "Wichtig für Analysen und Nachverfolgung"
    ),
    MEDIUM(
        displayName = "Mittel",
        color = "#FFAA00",
        description = "Nützlich für vollständige History"
    ),
    LOW(
        displayName = "Niedrig",
        color = "#00AA00",
        description = "Optional - kann Speicher sparen wenn deaktiviert"
    )
}
