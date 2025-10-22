package com.example.questflow.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Vordefinierte TimeBlock-Typen mit Icon und Farbe
 * Nutzer können auch eigene Typen erstellen
 */
enum class TimeBlockType(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val color: Long // ARGB format
) {
    WORK(
        displayName = "Arbeit",
        description = "Reguläre Arbeitszeiten",
        icon = Icons.Default.Build,
        color = 0xFF2196F3 // Blue
    ),
    BREAK(
        displayName = "Pause",
        description = "Mittagspause, Kaffeepause",
        icon = Icons.Default.Star,
        color = 0xFF4CAF50 // Green
    ),
    MEETING(
        displayName = "Meeting",
        description = "Besprechungen, Konferenzen",
        icon = Icons.Default.DateRange,
        color = 0xFFF44336 // Red
    ),
    VACATION(
        displayName = "Urlaub",
        description = "Urlaubszeit",
        icon = Icons.Default.Place,
        color = 0xFFFF9800 // Orange
    ),
    PERSONAL(
        displayName = "Privat",
        description = "Persönliche Zeit, Freizeit",
        icon = Icons.Default.Person,
        color = 0xFF9C27B0 // Purple
    ),
    COMMUTE(
        displayName = "Pendelzeit",
        description = "Fahrtzeit zur Arbeit",
        icon = Icons.Default.Star,
        color = 0xFF607D8B // Blue Grey
    ),
    SLEEP(
        displayName = "Schlafenszeit",
        description = "Nachtruhe",
        icon = Icons.Default.Star,
        color = 0xFF3F51B5 // Indigo
    ),
    EXERCISE(
        displayName = "Sport",
        description = "Trainingszeit",
        icon = Icons.Default.Star,
        color = 0xFFE91E63 // Pink
    ),
    FAMILY(
        displayName = "Familie",
        description = "Familienzeit",
        icon = Icons.Default.Home,
        color = 0xFFFF5722 // Deep Orange
    ),
    STUDY(
        displayName = "Lernen",
        description = "Lernzeit, Weiterbildung",
        icon = Icons.Default.Star,
        color = 0xFF00BCD4 // Cyan
    ),
    CUSTOM(
        displayName = "Benutzerdefiniert",
        description = "Eigener Typ",
        icon = Icons.Default.Settings,
        color = 0xFF9E9E9E // Grey
    );

    companion object {
        fun fromString(value: String?): TimeBlockType {
            if (value == null) return CUSTOM
            return entries.find { it.name == value } ?: CUSTOM
        }

        fun getAllTypes(): List<TimeBlockType> = entries.toList()
    }
}
